package battleship;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link Move}.
 * Author: Simão Espanha
 * Date: 2026-11-14 18:02
 * Cyclomatic Complexity:
 * - constructor: 1
 * - toString(): 1
 * - getNumber(): 1
 * - getShots(): 1
 * - getShotResults(): 1
 * - processEnemyFire(): 31
 */
class MoveTest {

	private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

	private Move move;

	@BeforeEach
	void setUp() {
		move = new Move(7, new ArrayList<>(List.of(new Position(2, 4))), new ArrayList<>());
	}

	@AfterEach
	void tearDown() {
		move = null;
	}

	@Test
	void constructor() {
		assertAll("constructed move",
				() -> assertNotNull(move, "Error: the Move instance should be created before each test."),
				() -> assertEquals(7, move.getNumber(), "Error: the constructor should retain the move number."),
				() -> assertEquals(1, move.getShots().size(), "Error: the constructor should retain the shot list."),
				() -> assertTrue(move.getShotResults().isEmpty(),
						"Error: the constructor should retain the empty result list."));
	}

	@Test
	void toStringTest() {
		String text = move.toString();

		assertEquals("Move{number=7, shots=1, results=0}", text,
				"Error: toString should report the move number and the number of shots and results.");
	}

	@Test
	void getNumber() {
		assertEquals(7, move.getNumber(), "Error: getNumber should return the number supplied to the constructor.");
	}

	@Test
	void getShots() {
		assertEquals(List.of(new Position(2, 4)), move.getShots(),
				"Error: getShots should return the positions supplied to the constructor.");
	}

	@Test
	void getShotResults() {
		assertTrue(move.getShotResults().isEmpty(),
				"Error: getShotResults should return the empty result list supplied to the constructor.");
	}

	@Test
	void processEnemyFire1() {
		assertSummary(run(false), 0, 0, 0, 3, 0, 0,
				"Error: an empty result list should report all three shots as outside.");
	}

	@Test
	void processEnemyFire2() {
		move = withResults(result(false, false, null, false));

		assertSummary(run(false), 0, 0, 0, 3, 0, 0,
				"Error: an invalid shot should not count as valid, repeated, or missed.");
	}

	@Test
	void processEnemyFire3() {
		move = withResults(result(true, true, null, false));
		ProcessOutput output = run(true);

		assertSummary(output, 0, 1, 0, 2, 0, 0,
				"Error: one repeated shot should count as repeated and reduce outside shots.");
		assertTrue(output.console().contains("1 tiro repetido"),
				"Error: verbose output should use the singular form for one repeated shot.");
	}

	@Test
	void processEnemyFire4() {
		move = withResults(result(true, true, null, false),
				result(true, true, null, false),
				result(true, true, null, false));
		ProcessOutput output = run(true);

		assertSummary(output, 0, 3, 0, 0, 0, 0,
				"Error: three repeated shots should leave no outside shots.");
		assertTrue(output.console().contains("3 tiros repetidos"),
				"Error: verbose output should pluralize repeated shots.");
	}

	@Test
	void processEnemyFire5() {
		move = withResults(result(true, false, null, false));

		assertSummary(run(false), 1, 0, 1, 2, 0, 0,
				"Error: one valid miss should count as both valid and missed.");
	}

	@Test
	void processEnemyFire6() {
		move = withResults(result(true, false, null, false),
				result(true, false, null, false));
		ProcessOutput output = run(true);

		assertSummary(output, 2, 0, 2, 1, 0, 0,
				"Error: two valid misses should produce two missed shots and one outside shot.");
		assertTrue(output.console().contains("2 tiros na água"),
				"Error: verbose output should pluralize the count of missed shots.");
	}

	@Test
	void processEnemyFire7() {
		move = withResults(result(true, false, "Frigate", false));

		assertSummary(run(false), 1, 0, 0, 2, 0, 1,
				"Error: a hit on an afloat ship should count as a hit but not a sunk boat.");
	}

	@Test
	void processEnemyFire8() {
		move = withResults(result(true, false, "Frigate", true));

		ProcessOutput output = run(true);
		assertSummary(output, 1, 0, 0, 2, 1, 0,
				"Error: a sunk hit should be included as sunk and excluded from afloat-ship hits.");
		assertTrue(output.console().contains("Frigate ao fundo"),
				"Error: verbose output should name a ship that was sunk.");
	}

	@Test
	void processEnemyFire9() {
		move = withResults(result(true, false, "Frigate", true),
				result(true, false, "Frigate", true));

		ProcessOutput output = run(true);
		assertSummary(output, 2, 0, 0, 1, 1, 0,
				"Error: repeated sinks of one category should be aggregated into one boat entry.");
		assertTrue(output.console().contains("2 Frigates ao fundo"),
				"Error: verbose output should pluralize a category sunk more than once.");
	}

	@Test
	void processEnemyFire10() {
		move = withResults(result(true, false, "Frigate", true),
				result(true, false, "Barge", true));

		assertSummary(run(false), 2, 0, 0, 1, 2, 0,
				"Error: sunk boats of different categories should be represented by separate entries.");
	}

	@Test
	void processEnemyFire11() {
		move = withResults(result(true, false, "Frigate", false),
				result(true, false, "Frigate", false));

		assertSummary(run(false), 2, 0, 0, 1, 0, 1,
				"Error: multiple hits on an afloat ship should aggregate into one boat-hit entry.");
	}

	@Test
	void processEnemyFire12() {
		move = withResults(result(true, false, "Frigate", false),
				result(true, false, "Barge", false));

		assertSummary(run(false), 2, 0, 0, 1, 0, 2,
				"Error: hits on different afloat ship categories should produce separate entries.");
	}

	@Test
	void processEnemyFire13() {
		move = withResults(result(true, false, "Frigate", true),
				result(true, false, "Barge", false));
		ProcessOutput output = run(true);

		assertSummary(output, 2, 0, 0, 1, 1, 1,
				"Error: a sunk category and a still-afloat category should appear in their separate summaries.");
		assertTrue(output.console().contains("Frigate ao fundo")
						&& output.console().contains("1 tiro num(a) Barge"),
				"Error: verbose output should summarize the sunk ship and the unsunk hit.");
	}

	@Test
	void processEnemyFire14() {
		move = withResults(result(true, false, "Frigate", false),
				result(true, false, null, false));

		ProcessOutput output = run(true);
		assertSummary(output, 2, 0, 1, 1, 0, 1,
				"Error: a hit and a miss should both be included in the JSON counts.");
		assertTrue(output.console().contains("1 tiro na água"),
				"Error: verbose output should retain the miss after reporting a hit.");
	}

	@Test
	void processEnemyFire15() {
		move = withResults(result(true, false, null, false),
				result(true, false, null, false),
				result(true, false, null, false));
		ProcessOutput output = run(true);

		assertSummary(output, 3, 0, 3, 0, 0, 0,
				"Error: three valid misses should produce no outside shots.");
		assertTrue(output.console().contains("3 tiros na água"),
				"Error: verbose output should pluralize three missed shots.");
	}

	@Test
	void processEnemyFire16() {
		move = withResults(result(false, false, null, false),
				result(false, false, null, false),
				result(false, false, null, false));
		ProcessOutput output = run(true);

		assertSummary(output, 0, 0, 0, 3, 0, 0,
				"Error: three invalid shots should all be counted as outside.");
		assertTrue(output.console().contains("3 tiros exteriores"),
				"Error: verbose output should pluralize the count of outside shots.");
	}

	@Test
	void processEnemyFire17() {
		move = withResults(result(false, false, null, false),
				result(true, false, null, false));
		ProcessOutput output = run(true);

		assertSummary(output, 1, 0, 1, 2, 0, 0,
				"Error: invalid results should be excluded while a valid miss remains counted.");
		assertTrue(output.console().contains("1 tiro válido"),
				"Error: verbose output should use singular wording for one valid shot.");
	}

	@Test
	void processEnemyFire18() {
		move = withResults(result(true, false, null, false),
				result(true, false, "Frigate", false));
		ProcessOutput output = run(true);

		assertSummary(output, 2, 0, 1, 1, 0, 1,
				"Error: valid shots should be counted whether they hit or miss.");
		assertTrue(output.console().contains("2 tiros válidos"),
				"Error: verbose output should pluralize multiple valid shots.");
	}

	@Test
	void processEnemyFire19() {
		move = withResults(result(true, false, null, false),
				result(true, false, null, false),
				result(true, false, null, false));
		ProcessOutput output = run(true);

		assertFalse(output.console().contains("exterior"),
				"Error: when all shots are valid, verbose output should not report outside shots.");
		assertSummary(output, 3, 0, 3, 0, 0, 0,
				"Error: all valid misses should leave the outside-shot count at zero.");
	}

	@Test
	void processEnemyFire20() {
		move = withResults(result(true, true, null, false));
		ProcessOutput output = run(true);

		assertEquals("1 tiro repetido, 2 tiros exteriores", summaryLine(output.console()),
				"Error: the repeated-only summary should be followed by outside shots without a leading comma.");
	}

	@Test
	void processEnemyFire21() {
		move = withResults(result(true, false, "Frigate", false));
		ProcessOutput output = run(true);

		assertTrue(output.console().contains("1 tiro válido: 1 tiro num(a) Frigate"),
				"Error: an afloat-ship hit should be appended after the singular valid-shot label.");
		assertSummary(output, 1, 0, 0, 2, 0, 1,
				"Error: an afloat-ship hit should be included in the JSON hit list.");
	}

	@Test
	void processEnemyFire22() {
		move = withResults(result(true, false, "Frigate", true));
		ProcessOutput output = run(true);

		assertTrue(output.console().contains("1 tiro válido: 1 Frigate ao fundo"),
				"Error: the sunk-ship summary should follow the singular valid-shot label.");
		assertSummary(output, 1, 0, 0, 2, 1, 0,
				"Error: a sunk-ship result should be included in the JSON sunk list.");
	}

	@Test
	void processEnemyFire23() {
		move = withResults(result(true, true, null, false),
				result(true, false, null, false));
		ProcessOutput output = run(true);

		assertTrue(output.console().contains("1 tiro válido: 1 tiro na água, 1 tiro repetido"),
				"Error: repeated shots should be comma-separated when valid shots are also present.");
		assertSummary(output, 1, 1, 1, 1, 0, 0,
				"Error: valid, missed, repeated, and outside shot counts should be reported together.");
	}

	@Test
	void processEnemyFire24() {
		move = withResults(result(true, false, null, false));
		ProcessOutput output = run(true);

		assertTrue(output.console().contains("1 tiro válido: 1 tiro na água, 2 tiros exteriores"),
				"Error: outside shots should be comma-separated when a summary already exists.");
		assertSummary(output, 1, 0, 1, 2, 0, 0,
				"Error: the outside-shot count should equal three minus the valid shots.");
	}

	@Test
	void processEnemyFire25() {
		move = withResults(result(false, false, null, false),
				result(false, false, null, false),
				result(false, false, null, false));
		ProcessOutput output = run(true);

		assertEquals("3 tiros exteriores", summaryLine(output.console()),
				"Error: an empty verbose summary should contain only the outside-shot description.");
		assertSummary(output, 0, 0, 0, 3, 0, 0,
				"Error: three invalid shots should leave three outside shots.");
	}

	@Test
	void processEnemyFire26() {
		move = withResults(result(true, true, null, false),
				result(true, true, null, false),
				result(true, true, null, false));
		ProcessOutput output = run(true);

		assertEquals("3 tiros repetidos", summaryLine(output.console()),
				"Error: all repeated shots should use the dedicated repeated-only summary.");
		assertSummary(output, 0, 3, 0, 0, 0, 0,
				"Error: repeated-only results should not count as valid or outside.");
	}

	@Test
	void processEnemyFire27() {
		move = withResults(result(true, false, "Frigate", false),
				result(true, true, null, false),
				result(true, false, null, false));
		ProcessOutput output = run(true);

		assertTrue(output.console().contains("2 tiros válidos: 1 tiro num(a) Frigate + 1 tiro na água, 1 tiro repetido"),
				"Error: hits, misses, and repeated shots should be joined in summary order.");
		assertSummary(output, 2, 1, 1, 0, 0, 1,
				"Error: valid, missed, repeated, and hit counts should be consistent.");
	}

	@Test
	void processEnemyFire28() {
		move = withResults(result(true, false, "Frigate", false),
				result(true, false, "Barge", true));
		ProcessOutput output = run(true);

		assertTrue(output.console().contains("2 tiros válidos: 1 Barge ao fundo + 1 tiro num(a) Frigate"),
				"Error: verbose output should summarize sunk boats before unsunk hits.");
		assertSummary(output, 2, 0, 0, 1, 1, 1,
				"Error: one sunk boat and one unsunk hit should appear in their respective JSON arrays.");
	}

	@Test
	void processEnemyFire29() {
		move = withResults(result(true, false, "Frigate", true),
				result(true, false, "Frigate", false));
		ProcessOutput output = run(true);

		assertTrue(output.console().contains("1 Frigate ao fundo"),
				"Error: a sunk boat should be reported even when there is also a hit on that category.");
		assertFalse(output.console().contains("tiro num(a) Frigate"),
				"Error: hits on a category that was sunk should not also be reported as afloat hits.");
		assertSummary(output, 2, 0, 0, 1, 1, 0,
				"Error: a sunk category should be omitted from the afloat-hit JSON array.");
	}

	@Test
	void processEnemyFire30() {
		move = withResults(result(true, false, "Frigate", false),
				result(true, false, null, false));
		ProcessOutput output = run(true);

		assertSummary(output, 2, 0, 1, 1, 0, 1,
				"Error: JSON generation should include a hit entry and the missed-shot count.");
		assertEquals("Frigate", output.json().get("hitsOnBoats").get(0).get("type").asText(),
				"Error: the JSON hit entry should identify the ship category.");
	}

	@Test
	void processEnemyFire31() {
		move = withResults(result(true, false, "Frigate", true),
				result(true, false, "Barge", true));
		ProcessOutput output = run(false);

		assertSummary(output, 2, 0, 0, 1, 2, 0,
				"Error: JSON generation should preserve all sunk category entries when verbose output is disabled.");
		assertTrue(output.json().get("sunkBoats").get(0).has("count")
						&& output.json().get("sunkBoats").get(1).has("count"),
				"Error: every sunk-boat JSON entry should include its sunk count.");
	}

	@Test
	void processEnemyFireReportsMultipleHitsOnAfloatShip() {
		move = withResults(result(true, false, "Frigate", false),
				result(true, false, "Frigate", false));
		ProcessOutput output = run(true);

		assertTrue(output.console().contains("2 tiros num(a) Frigate"),
				"Error: verbose output should pluralize multiple hits on an afloat ship.");
		assertSummary(output, 2, 0, 0, 1, 0, 1,
				"Error: multiple hits on one afloat ship should remain one JSON entry.");
	}

	@Test
	void processEnemyFireReportsMultipleRepeatedShotsWithValidShots() {
		move = withResults(result(true, false, null, false),
				result(true, true, null, false),
				result(true, true, null, false));
		ProcessOutput output = run(true);

		assertTrue(output.console().contains("1 tiro válido: 1 tiro na água, 2 tiros repetidos"),
				"Error: verbose output should pluralize repeated shots alongside valid shots.");
		assertSummary(output, 1, 2, 1, 0, 0, 0,
				"Error: repeated shots should be reported separately from the valid miss.");
	}

	@Test
	void processEnemyFireThrowsWhenJsonSerializationFails() {
		JsonProcessingException serializationFailure = new JsonProcessingException("forced serialization failure") { };
		Move failingMove = new Move(7, List.of(), List.of()) {
			@Override
			protected ObjectMapper createObjectMapper() {
				return new ObjectMapper() {
					@Override
					public String writeValueAsString(Object value) throws JsonProcessingException {
						throw serializationFailure;
					}
				};
			}
		};

		RuntimeException exception = assertThrows(RuntimeException.class,
				() -> failingMove.processEnemyFire(false));

		assertAll("serialization failure",
				() -> assertEquals("Erro ao serializar o JSON dos resultados da jogada", exception.getMessage()),
				() -> assertSame(serializationFailure, exception.getCause()));
	}

	private Move withResults(IGame.ShotResult... results) {
		return new Move(7, List.of(), List.of(results));
	}

	private static IGame.ShotResult result(boolean valid, boolean repeated, String category, boolean sunk) {
		IShip ship = category == null ? null : ship(category);
		return new IGame.ShotResult(valid, repeated, ship, sunk);
	}

	private static IShip ship(String category) {
		return (IShip) Proxy.newProxyInstance(
				IShip.class.getClassLoader(),
				new Class<?>[]{IShip.class},
				(proxy, method, args) -> {
					if (method.getName().equals("getCategory")) {
						return category;
					}
					throw new UnsupportedOperationException("Unexpected IShip method: " + method.getName());
				});
	}

	private ProcessOutput run(boolean verbose) {
		PrintStream originalOut = System.out;
		ByteArrayOutputStream captured = new ByteArrayOutputStream();
		String json;
		try (PrintStream temporaryOut = new PrintStream(captured, true, StandardCharsets.UTF_8)) {
			System.setOut(temporaryOut);
			json = move.processEnemyFire(verbose);
		} finally {
			System.setOut(originalOut);
		}

		try {
			return new ProcessOutput(OBJECT_MAPPER.readTree(json), captured.toString(StandardCharsets.UTF_8));
		} catch (Exception exception) {
			throw new AssertionError("Error: processEnemyFire should return valid JSON.", exception);
		}
	}

	private static void assertSummary(ProcessOutput output, int valid, int repeated, int missed,
			int outside, int sunkEntries, int hitEntries, String message) {
		JsonNode json = output.json();
		assertAll(message,
				() -> assertEquals(valid, json.get("validShots").asInt(),
						"Error: validShots should match the number of valid non-repeated shots."),
				() -> assertEquals(repeated, json.get("repeatedShots").asInt(),
						"Error: repeatedShots should match the number of repeated shots."),
				() -> assertEquals(missed, json.get("missedShots").asInt(),
						"Error: missedShots should match valid shots that hit no ship."),
				() -> assertEquals(outside, json.get("outsideShots").asInt(),
						"Error: outsideShots should be the remaining shots in the salvo."),
				() -> assertEquals(sunkEntries, json.get("sunkBoats").size(),
						"Error: sunkBoats should contain one entry per sunk ship category."),
				() -> assertEquals(hitEntries, json.get("hitsOnBoats").size(),
						"Error: hitsOnBoats should contain one entry per non-sunk ship category."));
	}

	private static String summaryLine(String console) {
		int separator = console.indexOf(" -> ");
		int endOfLine = console.indexOf(System.lineSeparator(), separator);
		return console.substring(separator + 4, endOfLine);
	}

	private record ProcessOutput(JsonNode json, String console) {
	}
}
