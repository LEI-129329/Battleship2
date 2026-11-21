package restserver;

import battleship.*;
import restserver.Dtos.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import com.sun.net.httpserver.HttpServer;

import java.io.IOException;
import java.net.InetSocketAddress;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Testes unitários da classe GameController (sem bibliotecas de mocking).
 * Usa objetos reais; como a frota da IA é aleatória, o jogo completo é percorrido
 * disparando sobre todas as quadrículas do tabuleiro.
 * Author: 101283
 */
public class GameControllerTest {

    private static final String UNREACHABLE_CALLBACK = "http://localhost:1/callback";
    private static final String ROWS = "ABCDEFGHIJ";

    private GameController controller;

    @BeforeEach
    void setUp() {
        controller = new GameController(new GameRegistry(), new CallbackClient(), new AiShotStrategy());
    }

    // ------------------------------------------------------------- auxiliares

    private int status(ResponseEntity<?> response) {
        return response.getStatusCode().value();
    }

    private RegistrationRequest registration(String name, String url) {
        RegistrationRequest r = new RegistrationRequest();
        r.setPlayerName(name);
        r.setCallbackUrl(url);
        return r;
    }

    /** Regista um jogador e devolve o gameId. */
    private String registerPlayer() {
        ResponseEntity<?> response = controller.register(registration("Ana", UNREACHABLE_CALLBACK));
        assertEquals(200, status(response));
        return ((RegistrationResponse) response.getBody()).getGameId();
    }

    private ShotPosition shotPos(String row, int column) {
        ShotPosition sp = new ShotPosition();
        sp.setRow(row);
        sp.setColumn(column);
        return sp;
    }

    private ShotRequest request(List<ShotPosition> shots) {
        ShotRequest r = new ShotRequest();
        r.setShots(shots);
        return r;
    }

    /** Pedido com o número certo de tiros em posições diferentes (A1, A2, ...). */
    private ShotRequest validRequest() {
        List<ShotPosition> shots = new ArrayList<>();
        for (int i = 0; i < Game.NUMBER_SHOTS; i++)
            shots.add(shotPos("A", i + 1));
        return request(shots);
    }

    /** Pedido com o número certo de tiros em que o primeiro é a posição dada. */
    private ShotRequest requestWithFirst(String row, int column) {
        ShotRequest r = validRequest();
        r.getShots().set(0, shotPos(row, column));
        return r;
    }

    // ---------------------------------------------------------------- register

    @Test
    @DisplayName("register rejeita playerName nulo")
    void registerNullPlayerName() {
        assertEquals(400, status(controller.register(registration(null, UNREACHABLE_CALLBACK))));
    }

    @Test
    @DisplayName("register rejeita playerName em branco")
    void registerBlankPlayerName() {
        assertEquals(400, status(controller.register(registration("   ", UNREACHABLE_CALLBACK))));
    }

    @Test
    @DisplayName("register rejeita callbackUrl nulo")
    void registerNullCallbackUrl() {
        assertEquals(400, status(controller.register(registration("Ana", null))));
    }

    @Test
    @DisplayName("register rejeita callbackUrl em branco")
    void registerBlankCallbackUrl() {
        assertEquals(400, status(controller.register(registration("Ana", " "))));
    }

    @Test
    @DisplayName("register com dados válidos cria o jogo e devolve 200")
    void registerValid() {
        ResponseEntity<?> response = controller.register(registration("Ana", UNREACHABLE_CALLBACK));

        assertEquals(200, status(response));
        assertInstanceOf(RegistrationResponse.class, response.getBody());
        RegistrationResponse body = (RegistrationResponse) response.getBody();
        assertNotNull(body.getGameId());
        assertEquals("REGISTERED", body.getStatus());
        assertTrue(body.getMessage().contains("Ana"));
    }

    // ------------------------------------------------------------ receiveShots

    @Test
    @DisplayName("receiveShots devolve 404 se o jogo não existe")
    void receiveShotsGameNotFound() {
        assertEquals(404, status(controller.receiveShots("nao-existe", validRequest())));
    }

    @Test
    @DisplayName("receiveShots devolve 400 se a lista de tiros for nula")
    void receiveShotsNullShots() {
        assertEquals(400, status(controller.receiveShots(registerPlayer(), request(null))));
    }

    @Test
    @DisplayName("receiveShots devolve 400 se o número de tiros for errado")
    void receiveShotsWrongNumberOfShots() {
        List<ShotPosition> shots = new ArrayList<>();
        shots.add(shotPos("A", 1));
        assertEquals(400, status(controller.receiveShots(registerPlayer(), request(shots))));
    }

    @Test
    @DisplayName("receiveShots devolve 400 se a linha for nula")
    void receiveShotsNullRow() {
        assertEquals(400, status(controller.receiveShots(registerPlayer(), requestWithFirst(null, 1))));
    }

    @Test
    @DisplayName("receiveShots devolve 400 se a linha estiver em branco")
    void receiveShotsBlankRow() {
        assertEquals(400, status(controller.receiveShots(registerPlayer(), requestWithFirst(" ", 1))));
    }

    @Test
    @DisplayName("receiveShots devolve 400 se a coluna for menor que 1")
    void receiveShotsColumnTooLow() {
        assertEquals(400, status(controller.receiveShots(registerPlayer(), requestWithFirst("A", 0))));
    }

    @Test
    @DisplayName("receiveShots devolve 400 se a coluna for maior que 10")
    void receiveShotsColumnTooHigh() {
        assertEquals(400, status(controller.receiveShots(registerPlayer(), requestWithFirst("A", 11))));
    }

    @Test
    @DisplayName("receiveShots numa jogada normal devolve o jogo a decorrer")
    void receiveShotsOngoing() {
        ResponseEntity<?> response = controller.receiveShots(registerPlayer(), validRequest());

        assertEquals(200, status(response));
        ShotResponse body = (ShotResponse) response.getBody();
        assertEquals(Game.NUMBER_SHOTS, body.getResults().size());
        assertEquals("ONGOING", body.getGameStatus());
        assertNull(body.getWinner());
        assertTrue(body.getShipsRemaining() > 0);
    }

    @Test
    @DisplayName("receiveShots converte a linha para maiúscula")
    void receiveShotsLowercaseRow() {
        ResponseEntity<?> response = controller.receiveShots(registerPlayer(), requestWithFirst("b", 4));

        ShotResponse body = (ShotResponse) response.getBody();
        assertEquals("B", body.getResults().get(0).getRow());
        assertEquals(4, body.getResults().get(0).getColumn());
    }

    @Test
    @DisplayName("receiveShots marca como REPEATED o mesmo tiro dentro da mesma jogada")
    void receiveShotsRepeatedInSameTurn() {
        List<ShotPosition> shots = new ArrayList<>();
        for (int i = 0; i < Game.NUMBER_SHOTS; i++)
            shots.add(shotPos("A", 1)); // sempre a mesma posição

        ShotResponse body = (ShotResponse) controller.receiveShots(registerPlayer(), request(shots)).getBody();

        assertEquals("REPEATED", body.getResults().get(1).getOutcome());
    }

    @Test
    @DisplayName("Jogo completo: disparar em todo o tabuleiro dá HIT, SUNK, MISS e vitória do estudante")
    void fullGameStudentWins() {
        String gameId = registerPlayer();

        // Todas as quadrículas do tabuleiro, de A1 a J10
        List<ShotPosition> cells = new ArrayList<>();
        for (char row : ROWS.toCharArray())
            for (int col = 1; col <= 10; col++)
                cells.add(shotPos(String.valueOf(row), col));

        Set<String> outcomes = new HashSet<>();
        ShotResponse last = null;
        int next = 0;

        for (int turn = 0; turn < 100; turn++) {
            List<ShotPosition> shots = new ArrayList<>();
            for (int k = 0; k < Game.NUMBER_SHOTS; k++)
                shots.add(cells.get(Math.min(next + k, cells.size() - 1)));
            next += Game.NUMBER_SHOTS;

            ResponseEntity<?> response = controller.receiveShots(gameId, request(shots));
            assertEquals(200, status(response));
            last = (ShotResponse) response.getBody();
            for (ShotResult r : last.getResults())
                outcomes.add(r.getOutcome());

            if ("GAME_OVER".equals(last.getGameStatus()))
                break;
        }

        assertNotNull(last);
        assertEquals("GAME_OVER", last.getGameStatus());
        assertEquals("STUDENT_WINS", last.getWinner());
        assertEquals(0, last.getShipsRemaining());
        assertTrue(outcomes.contains("MISS"));
        assertTrue(outcomes.contains("HIT"));
        assertTrue(outcomes.contains("SUNK"));

        // Depois de acabar, o jogo já não aceita mais tiros
        assertEquals(410, status(controller.receiveShots(gameId, validRequest())));
    }

    @Test
    @DisplayName("receiveShots devolve INVALID para uma posição fora do tabuleiro")
    void receiveShotsInvalidPosition() {
        ShotResponse body = (ShotResponse) controller
                .receiveShots(registerPlayer(), requestWithFirst("Z", 5)).getBody();
        assertEquals("INVALID", body.getResults().get(0).getOutcome());
    }

    @Test
    @DisplayName("receiveShots chama o callback do estudante com sucesso")
    void receiveShotsCallbackSucceeds() throws IOException {
        HttpServer server = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        server.createContext("/", exchange -> {
            byte[] body = "{}".getBytes();
            exchange.sendResponseHeaders(200, body.length);
            exchange.getResponseBody().write(body);
            exchange.close();
        });
        server.start();
        try {
            String url = "http://localhost:" + server.getAddress().getPort() + "/callback";
            ResponseEntity<?> reg = controller.register(registration("Ana", url));
            String gameId = ((RegistrationResponse) reg.getBody()).getGameId();

            assertEquals(200, status(controller.receiveShots(gameId, validRequest())));
        } finally {
            server.stop(0);
        }
    }

    // --------------------------------------------------------------- forceWin

    @Test
    @DisplayName("forceWin devolve 404 se o jogo não existe")
    void forceWinGameNotFound() {
        assertEquals(404, status(controller.forceWin("nao-existe")));
    }

    @Test
    @DisplayName("forceWin força a vitória do estudante")
    void forceWinSuccess() {
        String gameId = registerPlayer();

        ResponseEntity<?> response = controller.forceWin(gameId);

        assertEquals(200, status(response));
        assertTrue(String.valueOf(response.getBody()).contains(gameId));
        // O jogo passa a estar terminado
        assertEquals(410, status(controller.receiveShots(gameId, validRequest())));
    }
}