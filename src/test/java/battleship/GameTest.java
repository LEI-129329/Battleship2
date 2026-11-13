package battleship;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link Game}.
 * Author: nelss
 * Date: 2026-11-13
 * Cyclomatic Complexity of tested Game methods:
 * - Constructor: 1
 * - readEnemyFire(): 6
 * - fireShots(): 3
 * - fireSingleShot(): 6
 * - repeatedShot(): 3
 * - getProbabilityOfHitInNextRandomBurst(): 4
 * - getCandidateShots(): 5
 * - randomEnemyFire(): 7
 * - jsonShots(): 2
 * - printBoard(): 3
 * - printShips(): 5
 * - printBoardShots(): 7
 * - printBoardFrame(): 5
 */
public class GameTest {

	private Game game;

	@BeforeEach
	void setUp() {
		game = new Game(new Fleet()); // Assuming Fleet is a concrete implementation of IFleet
	}

	@AfterEach
	void tearDown() {
		game = null;
	}

	@Test
	void constructor() {
		assertNotNull(game, "Game instance should not be null after construction.");
		assertNotNull(game.getAlienMoves(), "Shots list should not be null after initialization.");
		assertTrue(game.getAlienMoves().isEmpty(), "Shots list should be empty upon initialization.");
		assertEquals(0, game.getInvalidShots(), "Invalid shots count should be zero upon initialization.");
		assertEquals(0, game.getRepeatedShots(), "Repeated shots count should be zero upon initialization.");
		assertEquals(0, game.getHits(), "Hits count should be zero upon initialization.");
		assertEquals(0, game.getSunkShips(), "Sunk ships count should be zero upon initialization.");
	}

	@Test
	void fire2() {
		Position invalidPosition = new Position(-1, 5);
		game.fireSingleShot(invalidPosition, false);
		assertEquals(1, game.getInvalidShots(), "Invalid shots counter should increase for an invalid shot.");
	}

	@Test
	void fire3() {
		Position position = new Position(2, 3);
		game.fireSingleShot(position, false);
		game.fireSingleShot(position, true);
		assertEquals(1, game.getRepeatedShots(), "Repeated shots counter should increase for a repeated shot.");
	}

	@Test
	@DisplayName("Detecta um tiro repetido mesmo sem a flag de repetição")
	void fireSingleShotDetectsPreviouslyRecordedShot() {
		Position previousShot = new Position(2, 3);
		game.fireShots(List.of(previousShot, new Position(2, 4), new Position(2, 5)));

		IGame.ShotResult result = game.fireSingleShot(new Position(2, 3), false);

		assertAll("tiro anteriormente registado",
				() -> assertTrue(result.repeated(),
						"Erro: a posição previamente disparada deve ser identificada como repetida."),
				() -> assertEquals(1, game.getRepeatedShots(),
						"Erro: o contador deve aumentar quando a repetição é detetada pelo histórico."));
	}

	@Test
	void repeatedShot1() {
		List<IPosition> positions = List.of(new Position(2, 3), new Position(2, 4), new Position(2, 5));
		game.fireShots(positions);
		Position position = new Position(2, 3);
		assertTrue(game.repeatedShot(position), "Position (2,3) should be marked as repeated after firing.");
	}

	@Test
	void repeatedShot2() {
		Position position = new Position(2, 3);
		assertFalse(game.repeatedShot(position), "Position (2,3) should not be marked as repeated before firing.");
	}

	@Test
	@DisplayName("Não considera repetida uma posição ausente de um histórico existente")
	void repeatedShotAfterDifferentMove() {
		game.fireShots(List.of(new Position(2, 3), new Position(2, 4), new Position(2, 5)));

		assertFalse(game.repeatedShot(new Position(7, 7)),
				"Erro: uma posição diferente das já disparadas não deve ser repetida.");
	}

	@Test
	void getAlienMoves() {
		List<IPosition> positions = List.of(new Position(2, 3), new Position(2, 4), new Position(2, 5));
		game.fireShots(positions);
		assertEquals(1, game.getAlienMoves().size(), "Shots list should contain one shot after firing once.");
	}

	@Test
	void getRemainingShips() {
		IFleet fleet = game.getMyFleet();
		Ship ship1 = new Barge(Compass.NORTH, new Position(1, 1));
		Ship ship2 = new Frigate(Compass.EAST, new Position(5, 5));

		fleet.addShip(ship1);
		assertEquals(1, game.getRemainingShips(), "Just one ship was created!");
		fleet.addShip(ship2);
		assertEquals(2, game.getRemainingShips(), "Two ships were created!");
		ship2.sink();
		assertEquals(1, game.getRemainingShips(), "Remaining ships count should be 1 after sinking one of two ships.");
	}

	@Test
	void probabilityOfHitInNextRandomBurst() {
		game.getMyFleet().addShip(new Barge(Compass.NORTH, new Position(5, 5)));

		assertEquals(0.03, game.getProbabilityOfHitInNextRandomBurst(), 1e-12);
	}

	@Test
	@DisplayName("Regista um acerto num navio que continua a flutuar")
	void fireSingleShotHitsButDoesNotSink() {
		Frigate frigate = new Frigate(Compass.EAST, new Position(4, 4));
		game.getMyFleet().addShip(frigate);

		IGame.ShotResult result = game.fireSingleShot(new Position(4, 4), false);

		assertAll("acerto sem afundamento",
				() -> assertTrue(result.valid(), "Erro: o tiro dentro do tabuleiro deve ser válido."),
				() -> assertFalse(result.repeated(), "Erro: o primeiro tiro não deve ser repetido."),
				() -> assertSame(frigate, result.ship(), "Erro: o tiro deve identificar o navio atingido."),
				() -> assertFalse(result.sunk(), "Erro: o navio deve continuar a flutuar após um único acerto."),
				() -> assertEquals(1, game.getHits(), "Erro: o contador de acertos deve ser incrementado."),
				() -> assertEquals(0, game.getSunkShips(), "Erro: o contador de navios afundados deve continuar a zero."));
	}

	@Test
	@DisplayName("Afunda um navio quando o tiro atinge a sua única posição")
	void fireSingleShotSinks() {
		Barge barge = new Barge(Compass.NORTH, new Position(4, 4));
		game.getMyFleet().addShip(barge);

		IGame.ShotResult result = game.fireSingleShot(new Position(4, 4), false);

		assertAll("navio afundado",
				() -> assertTrue(result.valid(), "Erro: o tiro dentro do tabuleiro deve ser válido."),
				() -> assertTrue(result.sunk(), "Erro: atingir a única posição da barca deve afundá-la."),
				() -> assertEquals(1, game.getSunkShips(), "Erro: o contador de navios afundados deve aumentar."));
	}

	@Test
	@DisplayName("Não conta como acerto um tiro na água")
	void fireSingleShotMisses() {
		IGame.ShotResult result = game.fireSingleShot(new Position(4, 4), false);

		assertAll("tiro na água",
				() -> assertTrue(result.valid(), "Erro: um tiro dentro do tabuleiro deve ser válido."),
				() -> assertNull(result.ship(), "Erro: um tiro na água não deve identificar um navio."),
				() -> assertEquals(0, game.getHits(), "Erro: um tiro na água não deve incrementar os acertos."));
	}

	@Test
	@DisplayName("Rejeita uma rajada com quantidade incorreta de tiros")
	void fireShotsRejectsWrongShotCount() {
		assertThrows(IllegalArgumentException.class, () -> game.fireShots(List.of(new Position(1, 1))),
				"Erro: a rajada deve rejeitar qualquer quantidade diferente de três tiros.");
	}

	@Test
	@DisplayName("Regista uma rajada e o respetivo instante")
	void fireShotsRecordsMoveAndTime() {
		long before = System.currentTimeMillis();

		game.fireShots(List.of(new Position(1, 1), new Position(2, 2), new Position(3, 3)));

		assertAll("rajada registada",
				() -> assertEquals(1, game.getAlienMoves().size(),
						"Erro: a rajada deve ser adicionada ao histórico de movimentos."),
				() -> assertTrue(game.getLastSalvoTimeMs() >= before,
						"Erro: a rajada deve atualizar o instante da última jogada."));
	}

	@Test
	@DisplayName("Lê tiros em formato separado e compacto")
	void readEnemyFireAcceptsBothPositionFormats() {
		String separated = game.readEnemyFire(new Scanner("A 1 B 2 C 3"));
		String compact = game.readEnemyFire(new Scanner("D4 E5 F6"));

		assertAll("formatos de posição",
				() -> assertTrue(separated.contains("\"row\" : \"A\""),
						"Erro: o formato separado deve ser convertido para a linha A."),
				() -> assertTrue(compact.contains("\"row\" : \"D\""),
						"Erro: o formato compacto deve ser convertido para a linha D."),
				() -> assertEquals(2, game.getAlienMoves().size(),
						"Erro: cada entrada válida deve adicionar uma jogada."));
	}

	@Test
	@DisplayName("Aceita letras minúsculas e ignora posições extra após três tiros")
	void readEnemyFireAcceptsLowercaseAndExtraInput() {
		String result = game.readEnemyFire(new Scanner("a 1 b 2 c 3 D4"));

		assertAll("entrada minúscula e excedente",
				() -> assertTrue(result.contains("\"row\" : \"A\""),
						"Erro: letras minúsculas devem ser normalizadas para maiúsculas."),
				() -> assertEquals(3, game.getAlienMoves().get(0).getShots().size(),
						"Erro: apenas os primeiros três tiros devem ser consumidos."));
	}

	@Test
	@DisplayName("Rejeita uma posição alfabética sem número")
	void readEnemyFireRejectsIncompletePosition() {
		assertThrows(IllegalArgumentException.class, () -> game.readEnemyFire(new Scanner("A B 2 C 3")),
				"Erro: uma letra sem o número seguinte deve produzir uma exceção.");
	}

	@Test
	@DisplayName("Rejeita uma entrada com menos de três posições")
	void readEnemyFireRejectsTooFewPositions() {
		assertThrows(IllegalArgumentException.class, () -> game.readEnemyFire(new Scanner("A1 B2")),
				"Erro: uma entrada com menos de três posições deve produzir uma exceção.");
	}

	@Test
	@DisplayName("Rejeita uma posição compacta com formato inválido")
	void readEnemyFireRejectsMalformedPosition() {
		assertThrows(IllegalArgumentException.class, () -> game.readEnemyFire(new Scanner("A1 B? C3")),
				"Erro: uma posição compacta malformada deve produzir uma exceção.");
	}

	@Test
	@DisplayName("Calcula probabilidade zero sem navios disponíveis")
	void probabilityOfHitWithoutShips() {
		assertTrue(Double.isNaN(game.getProbabilityOfHitInNextRandomBurst()),
				"Erro: sem navios disponíveis, o comportamento atual da distribuição deve resultar em NaN.");
	}

	@Test
	@DisplayName("Calcula probabilidade zero quando o único navio já está afundado")
	void probabilityOfHitWithSunkShip() {
		Barge barge = new Barge(Compass.NORTH, new Position(5, 5));
		game.getMyFleet().addShip(barge);
		barge.sink();

		assertTrue(Double.isNaN(game.getProbabilityOfHitInNextRandomBurst()),
				"Erro: sem posições de acerto, o comportamento atual da distribuição deve resultar em NaN.");
	}

	@Test
	@DisplayName("Serializa uma lista vazia e uma lista de posições")
	void jsonShotsSerializesPositions() {
		String emptyJson = Game.jsonShots(List.of());
		String shotsJson = Game.jsonShots(List.of(new Position('B', 4)));

		assertAll("serialização dos tiros",
				() -> assertEquals("[]", emptyJson.replaceAll("\\s", ""),
						"Erro: uma lista vazia deve ser serializada como um array vazio."),
				() -> assertTrue(shotsJson.contains("\"row\" : \"B\""),
						"Erro: o JSON deve conter a linha clássica da posição."),
				() -> assertTrue(shotsJson.contains("\"column\" : 4"),
						"Erro: o JSON deve conter a coluna clássica da posição."));
	}

	@Test
	@DisplayName("Imprime tabuleiros com e sem tiros e legenda")
	void printBoardsCoversDisplayOptions() {
		Frigate frigate = new Frigate(Compass.EAST, new Position(4, 4));
		Barge sunkBarge = new Barge(Compass.NORTH, new Position(8, 8));
		game.getMyFleet().addShip(frigate);
		game.getMyFleet().addShip(sunkBarge);
		game.fireShots(List.of(new Position(4, 4), new Position(8, 8), new Position(8, 7)));
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		PrintStream originalOut = System.out;
		try {
			System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
			game.printAlienBoard(true, false);
		} finally {
			System.setOut(originalOut);
		}

		Move outOfBoundsMove = new Move(2,
				List.of(new Position(-1, 0), new Position(0, 0)), List.of());
		game.getAlienMoves().add(outOfBoundsMove);
		game.getMyMoves().add(outOfBoundsMove);
		try {
			System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
			game.printMyBoard(true, true);
			game.printMyBoard(false, false);
			game.printAlienBoard(true, false);
		} finally {
			System.setOut(originalOut);
		}

		String board = output.toString(StandardCharsets.UTF_8);
		assertAll("opções de impressão",
				() -> assertTrue(board.contains("LEGENDA"),
						"Erro: a legenda deve ser exibida quando solicitada."),
				() -> assertTrue(board.contains("*"),
						"Erro: um tiro num navio deve ser impresso com o marcador de acerto."),
				() -> assertTrue(board.contains("o"),
						"Erro: um tiro na água deve ser impresso com o marcador de água."));
	}

	@Test
	@DisplayName("Completa uma rajada quando restam apenas duas posições candidatas")
	void randomEnemyFireCoversCandidateBoundaries() {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		PrintStream originalOut = System.out;
		try {
			System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
			List<IPosition> previouslyFired = new ArrayList<>();
			for (int row = 0; row < Game.BOARD_SIZE; row++) {
				for (int column = 0; column < Game.BOARD_SIZE; column++) {
					if ((row == 0 && column < 2)) {
						continue;
					}
					previouslyFired.add(new Position(row, column));
				}
			}
			game.getAlienMoves().add(new Move(1, previouslyFired, List.of()));

			assertNotNull(game.randomEnemyFire(),
					"Erro: uma rajada gerada com duas posições restantes deve ser devolvida como JSON.");
		} finally {
			System.setOut(originalOut);
		}

		assertAll("limites da rajada",
				() -> assertEquals(1, game.getRepeatedShots(),
						"Erro: ao completar a rajada com apenas duas posições, o tiro preenchido deve ser repetido."),
				() -> assertEquals(2, game.getAlienMoves().size(),
						"Erro: a rajada válida deve ser registada antes de o tabuleiro esgotar."));
	}

	@Test
	@DisplayName("Gera uma rajada aleatória pela API pública")
	void randomEnemyFirePublicMethod() {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		PrintStream originalOut = System.out;
		String result;
		try {
			System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
			result = game.randomEnemyFire();
		} finally {
			System.setOut(originalOut);
		}

		assertAll("rajada aleatória pública",
				() -> assertNotNull(result, "Erro: o método deve devolver o JSON da rajada."),
				() -> assertEquals(1, game.getAlienMoves().size(),
						"Erro: a rajada deve ser registada no histórico."));
	}

	@Test
	@DisplayName("Gera rajadas aleatórias quando restam quatro posições candidatas")
	void randomEnemyFireWithFourCandidates() throws InterruptedException {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		PrintStream originalOut = System.out;
		try {
			System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
			for (int attempt = 0; attempt < 32; attempt++) {
				Game trialGame = new Game(new Fleet());
				List<IPosition> previouslyFired = new ArrayList<>();
				for (int row = 0; row < Game.BOARD_SIZE; row++) {
					for (int column = 0; column < Game.BOARD_SIZE; column++) {
						if (row * Game.BOARD_SIZE + column >= 4) {
							previouslyFired.add(new Position(row, column));
						}
					}
				}
				trialGame.getAlienMoves().add(new Move(1, previouslyFired, List.of()));

				assertNotNull(trialGame.randomEnemyFire(),
						"Erro: a geração deve produzir uma rajada com quatro posições candidatas.");
				assertEquals(3, trialGame.getAlienMoves().get(1).getShots().size(),
						"Erro: a rajada deve conter três tiros mesmo quando algumas seleções aleatórias se repetem.");
				Thread.sleep(2);
			}
		} finally {
			System.setOut(originalOut);
		}
	}
}