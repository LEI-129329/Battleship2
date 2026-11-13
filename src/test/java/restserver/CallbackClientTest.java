package restserver;

import battleship.IGame;
import battleship.IPosition;
import battleship.Position;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.ProxySelector;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.net.CookieHandler;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import javax.net.ssl.SSLSession;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link CallbackClient}.
 * Author: nelss
 * Date: 2026-11-13 15:04
 * Cyclomatic Complexity:
 * - CallbackClient constructor: 1
 * - sendShots(): 9
 */
class CallbackClientTest {

	private static final String ONGOING_RESPONSE =
			"{\"results\":[],\"shipsRemaining\":10,\"gameStatus\":\"ONGOING\"}";

	private CallbackClient client;
	private GameSession session;
	private HttpServer server;
	private AtomicReference<Integer> responseStatus;
	private AtomicReference<String> responseBody;
	private AtomicReference<String> requestBody;
	private AtomicReference<String> requestPath;

	@BeforeEach
	void setUp() throws IOException {
		client = new CallbackClient();
		responseStatus = new AtomicReference<>(200);
		responseBody = new AtomicReference<>(ONGOING_RESPONSE);
		requestBody = new AtomicReference<>();
		requestPath = new AtomicReference<>();

		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/", exchange -> {
			requestPath.set(exchange.getRequestURI().getPath());
			requestBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));

			byte[] body = responseBody.get().getBytes(StandardCharsets.UTF_8);
			exchange.sendResponseHeaders(responseStatus.get(), body.length);
			try (var output = exchange.getResponseBody()) {
				output.write(body);
			}
		});
		server.start();

		String callbackUrl = "http://127.0.0.1:" + server.getAddress().getPort();
		session = new GameSession("test-game", "Test Player", callbackUrl);
	}

	@AfterEach
	void tearDown() {
		if (server != null) {
			server.stop(0);
		}
		client = null;
		session = null;
		server = null;
	}

	@Test
	@DisplayName("Constrói uma instância do cliente")
	void constructor() {
		assertNotNull(client, "Erro: o cliente deve ser criado antes de cada teste.");
	}

	@Test
	@DisplayName("Envia os tiros no formato esperado e regista os resultados")
	void sendShots1() {
		List<IPosition> shots = List.of(new Position('B', 3), new Position('G', 7));
		responseBody.set("""
				{"results":[
				  {"row":"B","column":3,"outcome":"MISS"},
				  {"row":"G","column":7,"outcome":"SUNK"}
				],"shipsRemaining":9,"gameStatus":"ONGOING"}
				""");

		boolean result = client.sendShots(session, shots);

		assertAll("pedido e resultados",
				() -> assertTrue(result, "Erro: o cliente deve indicar sucesso para uma resposta HTTP válida."),
				() -> assertEquals("/game/test-game/shots", requestPath.get(),
						"Erro: o pedido deve ser enviado para o endpoint do jogo."),
				() -> assertTrue(requestBody.get().contains("\"row\":\"B\""),
						"Erro: o corpo do pedido deve conter a linha B do primeiro tiro."),
				() -> assertTrue(requestBody.get().contains("\"column\":7"),
						"Erro: o corpo do pedido deve conter a coluna 7 do segundo tiro."),
				() -> assertEquals(1, session.getGame().getAlienMoves().size(),
						"Erro: os resultados recebidos devem ser registados como uma jogada."),
				() -> assertEquals(2, session.getGame().getAlienMoves().get(0).getShotResults().size(),
						"Erro: deve ser criado um resultado para cada tiro recebido."));

		List<IGame.ShotResult> results = session.getGame().getAlienMoves().get(0).getShotResults();
		assertAll("interpretação dos resultados",
				() -> assertTrue(results.get(0).valid(),
						"Erro: um tiro MISS deve ser considerado válido."),
				() -> assertFalse(results.get(0).repeated(),
						"Erro: um tiro MISS não deve ser considerado repetido."),
				() -> assertTrue(results.get(1).sunk(),
						"Erro: um tiro SUNK deve ser marcado como navio afundado."));
	}

	@Test
	@DisplayName("Não marca o fim do jogo quando a resposta indica uma partida em curso")
	void sendShots2() {
		responseBody.set("{\"results\":[],\"shipsRemaining\":1,\"gameStatus\":\"ONGOING\"}");

		boolean result = client.sendShots(session, List.of());

		assertAll("partida em curso",
				() -> assertTrue(result, "Erro: o callback deve ser considerado bem-sucedido."),
				() -> assertFalse(session.isGameOver(),
						"Erro: uma resposta ONGOING com navios restantes não deve terminar o jogo."),
				() -> assertNull(session.getWinner(),
						"Erro: não deve existir vencedor enquanto a partida estiver em curso."));
	}

	@Test
	@DisplayName("Marca a vitória da IA quando não restam navios")
	void sendShots3() {
		responseBody.set("{\"results\":[],\"shipsRemaining\":0,\"gameStatus\":\"ONGOING\"}");

		boolean result = client.sendShots(session, List.of());

		assertAll("vitória por navios restantes",
				() -> assertTrue(result, "Erro: uma resposta válida deve retornar sucesso."),
				() -> assertTrue(session.isGameOver(),
						"Erro: zero navios restantes deve terminar a partida."),
				() -> assertEquals("AI_WINS", session.getWinner(),
						"Erro: a IA deve ser declarada vencedora quando não restam navios."));
	}

	@Test
	@DisplayName("Marca a vitória da IA quando o estado recebido é GAME_OVER")
	void sendShots4() {
		responseBody.set("{\"results\":[],\"shipsRemaining\":2,\"gameStatus\":\"GAME_OVER\"}");

		boolean result = client.sendShots(session, List.of());

		assertAll("vitória por estado final",
				() -> assertTrue(result, "Erro: uma resposta válida deve retornar sucesso."),
				() -> assertTrue(session.isGameOver(),
						"Erro: o estado GAME_OVER deve terminar a partida."),
				() -> assertEquals("AI_WINS", session.getWinner(),
						"Erro: a IA deve ser declarada vencedora quando o estado é GAME_OVER."));
	}

	@Test
	@DisplayName("Retorna falso para um estado HTTP abaixo de 200")
	void sendShots5() {
		client = new CallbackClient(new StubHttpClient(new StubHttpResponse(199, "")));

		boolean result = client.sendShots(session, List.of());

		assertAll("resposta HTTP abaixo do sucesso",
				() -> assertFalse(result, "Erro: um estado HTTP inferior a 200 deve retornar falso."),
				() -> assertTrue(session.getGame().getAlienMoves().isEmpty(),
						"Erro: uma resposta HTTP inválida não deve registar jogadas."));
	}

	@Test
	@DisplayName("Retorna falso para um estado HTTP igual ou superior a 300")
	void sendShots6() {
		responseStatus.set(500);

		boolean result = client.sendShots(session, List.of());

		assertAll("resposta HTTP de erro",
				() -> assertFalse(result, "Erro: um estado HTTP igual ou superior a 300 deve retornar falso."),
				() -> assertTrue(session.getGame().getAlienMoves().isEmpty(),
						"Erro: uma resposta HTTP de erro não deve registar jogadas."));
	}

	@Test
	@DisplayName("Retorna falso quando a resposta não contém JSON válido")
	void sendShots7() {
		responseBody.set("isto não é JSON");

		boolean result = client.sendShots(session, List.of());

		assertAll("resposta malformada",
				() -> assertFalse(result, "Erro: uma resposta JSON inválida deve retornar falso."),
				() -> assertTrue(session.getGame().getAlienMoves().isEmpty(),
						"Erro: uma resposta que não pode ser interpretada não deve registar jogadas."));
	}

	@Test
	@DisplayName("Retorna falso quando não consegue contactar o servidor")
	void sendShots8() {
		server.stop(0);

		boolean result = client.sendShots(session, List.of());

		assertFalse(result, "Erro: uma falha de ligação ao callback deve retornar falso.");
	}

	@Test
	@DisplayName("Retorna falso quando a chamada HTTP é interrompida")
	void sendShotsWhenInterrupted() {
		Thread.currentThread().interrupt();
		boolean result;
		try {
			result = client.sendShots(session, List.of());
		} finally {
			Thread.interrupted();
		}

		assertFalse(result, "Erro: uma chamada HTTP interrompida deve retornar falso.");
	}

	@Test
	@DisplayName("Regista resultados INVALID, REPEATED e resultados parciais")
	void sendShots9() {
		responseBody.set("""
				{"results":[
				  {"row":"A","column":1,"outcome":"INVALID"},
				  {"row":"A","column":2,"outcome":"REPEATED"}
				],"shipsRemaining":10,"gameStatus":"ONGOING"}
				""");

		boolean result = client.sendShots(session,
				List.of(new Position('A', 1), new Position('A', 2), new Position('A', 3)));

		assertAll("resultados parciais",
				() -> assertTrue(result, "Erro: uma resposta válida com resultados parciais deve retornar sucesso."),
				() -> assertEquals(2, session.getGame().getAlienMoves().get(0).getShotResults().size(),
						"Erro: só devem ser registados resultados efetivamente recebidos."));

		List<IGame.ShotResult> results = session.getGame().getAlienMoves().get(0).getShotResults();
		assertAll("interpretação de INVALID e REPEATED",
				() -> assertFalse(results.get(0).valid(),
						"Erro: INVALID deve ser interpretado como tiro não válido."),
				() -> assertTrue(results.get(1).valid(),
						"Erro: REPEATED deve manter a validade do resultado."),
				() -> assertTrue(results.get(1).repeated(),
						"Erro: REPEATED deve ser marcado como tiro repetido."));
	}

	@Test
	@DisplayName("Não regista uma jogada quando a resposta tem results nulo")
	void sendShotsWithNullResults() {
		responseBody.set("{\"results\":null,\"shipsRemaining\":10,\"gameStatus\":\"ONGOING\"}");

		boolean result = client.sendShots(session, List.of(new Position('C', 4)));

		assertAll("resultados nulos",
				() -> assertTrue(result, "Erro: uma resposta válida com results nulo deve retornar sucesso."),
				() -> assertTrue(session.getGame().getAlienMoves().isEmpty(),
						"Erro: results nulo não deve criar uma jogada."));
	}

	private static final class StubHttpClient extends HttpClient {
		private final HttpResponse<String> response;

		private StubHttpClient(HttpResponse<String> response) {
			this.response = response;
		}

		@Override
		public Optional<CookieHandler> cookieHandler() {
			return Optional.empty();
		}

		@Override
		public Optional<Duration> connectTimeout() {
			return Optional.empty();
		}

		@Override
		public Redirect followRedirects() {
			return Redirect.NEVER;
		}

		@Override
		public Optional<ProxySelector> proxy() {
			return Optional.empty();
		}

		@Override
		public SSLContext sslContext() {
			return null;
		}

		@Override
		public SSLParameters sslParameters() {
			return new SSLParameters();
		}

		@Override
		public Optional<Authenticator> authenticator() {
			return Optional.empty();
		}

		@Override
		public Version version() {
			return Version.HTTP_1_1;
		}

		@Override
		public Optional<Executor> executor() {
			return Optional.empty();
		}

		@Override
		@SuppressWarnings("unchecked")
		public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> bodyHandler) {
			return (HttpResponse<T>) response;
		}

		@Override
		public <T> CompletableFuture<HttpResponse<T>> sendAsync(
				HttpRequest request, HttpResponse.BodyHandler<T> bodyHandler) {
			throw new UnsupportedOperationException();
		}

		@Override
		public <T> CompletableFuture<HttpResponse<T>> sendAsync(
				HttpRequest request,
				HttpResponse.BodyHandler<T> bodyHandler,
				HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
			throw new UnsupportedOperationException();
		}
	}

	private static final class StubHttpResponse implements HttpResponse<String> {
		private final int statusCode;
		private final String body;

		private StubHttpResponse(int statusCode, String body) {
			this.statusCode = statusCode;
			this.body = body;
		}

		@Override
		public int statusCode() {
			return statusCode;
		}

		@Override
		public HttpRequest request() {
			return null;
		}

		@Override
		public Optional<HttpResponse<String>> previousResponse() {
			return Optional.empty();
		}

		@Override
		public HttpHeaders headers() {
			return HttpHeaders.of(Map.of(), (name, value) -> true);
		}

		@Override
		public String body() {
			return body;
		}

		@Override
		public Optional<SSLSession> sslSession() {
			return Optional.empty();
		}

		@Override
		public URI uri() {
			return URI.create("http://127.0.0.1/");
		}

		@Override
		public HttpClient.Version version() {
			return HttpClient.Version.HTTP_1_1;
		}
	}
}
