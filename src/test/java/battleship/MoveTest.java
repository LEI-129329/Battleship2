package battleship;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MoveTest {
	private record CapturedOutput(String json, String console) {
	}

	private CapturedOutput processAndCapture(Move move, boolean verbose) {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		PrintStream originalOut = System.out;
		String json;
		try (PrintStream capture = new PrintStream(output, true, StandardCharsets.UTF_8)) {
			System.setOut(capture);
			json = move.processEnemyFire(verbose);
		} finally {
			System.setOut(originalOut);
		}
		return new CapturedOutput(json, output.toString(StandardCharsets.UTF_8));
	}

	@Test
	void processEnemyFireCountsValidRepeatedAndInvalidShots() {
		Move move = new Move(1, List.of(), List.of(
				new IGame.ShotResult(true, false, null, false),
				new IGame.ShotResult(true, true, null, false),
				new IGame.ShotResult(false, false, null, false)));

		String response = processAndCapture(move, false).json();

		assertAll("contagens de tiros",
				() -> assertTrue(response.contains("\"validShots\" : 1")),
				() -> assertTrue(response.contains("\"repeatedShots\" : 1")),
				() -> assertTrue(response.contains("\"outsideShots\" : 1")),
				() -> assertTrue(response.contains("\"missedShots\" : 1")));
	}

	@Test
	void processEnemyFireReportsSunkAndUnfinishedShips() {
		IShip sunkShip = new Barge(Compass.NORTH, new Position(0, 0));
		IShip floatingShip = new Frigate(Compass.NORTH, new Position(2, 2));
		Move move = new Move(2, List.of(), List.of(
				new IGame.ShotResult(true, false, sunkShip, true),
				new IGame.ShotResult(true, false, floatingShip, false),
				new IGame.ShotResult(true, false, null, false)));

		String response = processAndCapture(move, false).json();

		assertAll("detalhes dos resultados",
				() -> assertTrue(response.contains("\"type\" : \"Barca\"")),
				() -> assertTrue(response.contains("\"count\" : 1")),
				() -> assertTrue(response.contains("\"type\" : \"Fragata\"")),
				() -> assertTrue(response.contains("\"hits\" : 1")));
	}

	@Test
	void processEnemyFireFormatsPluralSunkShipsAndSuppressesHitsForSunkType() {
		IShip firstBarge = new Barge(Compass.NORTH, new Position(0, 0));
		IShip secondBarge = new Barge(Compass.NORTH, new Position(1, 0));
		Move move = new Move(3, List.of(), List.of(
				new IGame.ShotResult(true, false, firstBarge, true),
				new IGame.ShotResult(true, false, secondBarge, true),
				new IGame.ShotResult(true, false, firstBarge, false)));

		CapturedOutput result = processAndCapture(move, true);

		assertAll("formatação de navios afundados",
				() -> assertTrue(result.json().contains("\"count\" : 2")),
				() -> assertTrue(result.json().contains("\"hitsOnBoats\" : [ ]")),
				() -> assertTrue(result.console().contains("2 Barcas ao fundo")),
				() -> assertTrue(result.console().contains("3 tiros válidos")));
	}

	@Test
	void processEnemyFireFormatsSingularHitRepeatedAndOutsideShots() {
		IShip frigate = new Frigate(Compass.NORTH, new Position(2, 2));
		Move move = new Move(4, List.of(), List.of(
				new IGame.ShotResult(true, false, frigate, false),
				new IGame.ShotResult(true, true, null, false),
				new IGame.ShotResult(false, false, null, false)));

		CapturedOutput result = processAndCapture(move, true);

		assertAll("formatação de contagens singulares",
				() -> assertTrue(result.json().contains("\"validShots\" : 1")),
				() -> assertTrue(result.json().contains("\"repeatedShots\" : 1")),
				() -> assertTrue(result.json().contains("\"outsideShots\" : 1")),
				() -> assertTrue(result.console().contains("1 tiro válido")),
				() -> assertTrue(result.console().contains("1 tiro num(a) Fragata")),
				() -> assertTrue(result.console().contains("1 tiro repetido")),
				() -> assertTrue(result.console().contains("1 tiro exterior")));
	}

	@Test
	void processEnemyFireFormatsPluralHitsOnAnUnfinishedShip() {
		IShip frigate = new Frigate(Compass.NORTH, new Position(2, 2));
		Move move = new Move(8, List.of(), List.of(
				new IGame.ShotResult(true, false, frigate, false),
				new IGame.ShotResult(true, false, frigate, false),
				new IGame.ShotResult(true, false, null, false)));

		CapturedOutput result = processAndCapture(move, true);

		assertAll("múltiplos acertos no mesmo navio",
				() -> assertTrue(result.json().contains("\"hits\" : 2")),
				() -> assertTrue(result.console().contains("2 tiros num(a) Fragata")),
				() -> assertTrue(result.console().contains("1 tiro na água")));
	}

	@Test
	void processEnemyFireFormatsOnlyRepeatedShots() {
		Move move = new Move(5, List.of(), List.of(
				new IGame.ShotResult(true, true, null, false),
				new IGame.ShotResult(true, true, null, false),
				new IGame.ShotResult(true, true, null, false)));

		CapturedOutput result = processAndCapture(move, true);

		assertAll("rajada só com tiros repetidos",
				() -> assertTrue(result.json().contains("\"validShots\" : 0")),
				() -> assertTrue(result.json().contains("\"repeatedShots\" : 3")),
				() -> assertTrue(result.json().contains("\"outsideShots\" : 0")),
				() -> assertTrue(result.console().contains("3 tiros repetidos")),
				() -> assertFalse(result.console().contains("tiros exteriores")));
	}

	@Test
	void processEnemyFireFormatsOnlyInvalidShotsAsOutside() {
		Move move = new Move(6, List.of(), List.of(
				new IGame.ShotResult(false, false, null, false),
				new IGame.ShotResult(false, false, null, false),
				new IGame.ShotResult(false, false, null, false)));

		CapturedOutput result = processAndCapture(move, true);

		assertAll("rajada só com tiros inválidos",
				() -> assertTrue(result.json().contains("\"validShots\" : 0")),
				() -> assertTrue(result.json().contains("\"outsideShots\" : 3")),
				() -> assertTrue(result.console().contains("3 tiros exteriores")));
	}

	@Test
	void processEnemyFireFormatsPluralMissesWithoutVerboseSummaryWhenDisabled() {
		Move move = new Move(7, List.of(), List.of(
				new IGame.ShotResult(true, false, null, false),
				new IGame.ShotResult(true, false, null, false),
				new IGame.ShotResult(true, false, null, false)));

		CapturedOutput result = processAndCapture(move, false);

		assertAll("rajada só com tiros na água",
				() -> assertTrue(result.json().contains("\"validShots\" : 3")),
				() -> assertTrue(result.json().contains("\"missedShots\" : 3")),
				() -> assertFalse(result.console().contains("Jogada nº7")));
	}
}
