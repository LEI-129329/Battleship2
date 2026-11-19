package battleship;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.Scanner;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for the Tasks command-input helpers.
 *
 * @author luisf
 * Date: 2026-11-16
 * Class under test: battleship.Tasks
 */
class TasksTest {

    @Test
    @DisplayName("Tasks can be instantiated")
    void constructorCreatesInstance() {
        assertNotNull(new Tasks(), "Error: expected the public Tasks constructor to create an instance.");
    }

    @Test
    @DisplayName("menu processes commands before and after building the fleet")
    void menuProcessesInteractiveCommands() throws Exception {
        String input = "estado\nmapa\nmapagrafico\nrajada\nsimula\ntiros\ndesconhecido\najuda\nlefrota\n"
                + fleetInput() + "\n"
                + "estado\nmapa\ntiros\n"
                + "rajada A1 A3 A5\n"
                + "rajada A7 A9 C1\n"
                + "rajada C3 C5 C7\n"
                + "rajada C9 E1 E3\n";
        String output = runMenuInChildProcess(input, false);

        assertAll(
                () -> assertTrue(output.contains("Repete"),
                        "Error: expected the menu to report an unknown command."),
                () -> assertTrue(output.contains("Estado da Frota:"),
                        "Error: expected the menu to print the fleet status after fleet creation."),
                () -> assertTrue(output.contains("Passaram"),
                        "Error: expected a later salvo to report the time since the previous salvo.")
        );
    }

    @Test
    @DisplayName("menu generates a random fleet and accepts the quit command")
    void menuGeneratesFleetAndQuits() throws Exception {
        String output = runMenuInChildProcess("gerafrota\ndesisto\n", false);

        assertTrue(output.contains("LEGENDA"),
                "Error: expected random fleet creation to print the board legend.");
    }

    @Test
    @DisplayName("menu simulation ends after sinking every ship")
    void menuSimulationEndsAfterFleetIsSunk() throws Exception {
        String output = runMenuInChildProcess("lefrota\n" + fleetInput() + "\nsimula\n", true);

        assertAll(
                () -> assertTrue(output.contains("Probabilidade de acertar"),
                        "Error: expected simulation to report salvo hit probabilities."),
                () -> assertTrue(output.contains("Maldito sejas"),
                        "Error: expected simulation to display the game-over message.")
        );
    }

    @Test
    @DisplayName("menuHelp prints the available commands")
    void menuHelpPrintsCommands() {
        String output = captureOutput(Tasks::menuHelp);

        assertAll(
                () -> assertTrue(output.contains("Digite um dos comandos abaixo"),
                        "Error: expected the menu introduction to be printed."),
                () -> assertTrue(output.contains("- desisto:"),
                        "Error: expected the exit command to be listed.")
        );
    }

    @Test
    @DisplayName("buildFleet skips unknown and conflicting ships and builds the full fleet")
    void buildFleetSkipsInvalidShipsAndFillsFleet() {
        String input = "unknown 7 7 n "
                + "barca 0 0 n "
                + "barca 0 1 n "
                + "barca 0 2 n "
                + "barca 0 4 n "
                + "barca 0 6 n "
                + "barca 0 8 n "
                + "barca 2 0 n "
                + "barca 2 2 n "
                + "barca 2 4 n "
                + "barca 2 6 n "
                + "barca 2 8 n "
                + "barca 4 0 n "
                + "barca 4 2 n";

        Fleet fleet = Tasks.buildFleet(new Scanner(input));

        assertEquals(Fleet.FLEET_SIZE, fleet.getShips().size(),
                "Error: expected buildFleet to add exactly the configured fleet size.");
    }

    @Test
    @DisplayName("buildFleet assertion rejects a null scanner")
    void buildFleetRejectsNullScanner() {
        assertThrows(AssertionError.class, () -> Tasks.buildFleet(null),
                "Error: expected a null scanner to trigger the buildFleet assertion.");
    }

    @Test
    @DisplayName("readShip creates ships for every valid compass bearing")
    void readShipAcceptsEveryBearing() {
        assertAll(
                () -> assertSame(Compass.NORTH, Tasks.readShip(new Scanner("barca 1 2 n")).getBearing(),
                        "Error: expected bearing token 'n' to select NORTH."),
                () -> assertSame(Compass.SOUTH, Tasks.readShip(new Scanner("barca 1 2 s")).getBearing(),
                        "Error: expected bearing token 's' to select SOUTH."),
                () -> assertSame(Compass.EAST, Tasks.readShip(new Scanner("barca 1 2 e")).getBearing(),
                        "Error: expected bearing token 'e' to select EAST."),
                () -> assertSame(Compass.WEST, Tasks.readShip(new Scanner("barca 1 2 o")).getBearing(),
                        "Error: expected bearing token 'o' to select WEST.")
        );
    }

    @Test
    @DisplayName("readShip returns null for an unknown ship kind")
    void readShipReturnsNullForUnknownKind() {
        assertNull(Tasks.readShip(new Scanner("unknown 1 2 n")),
                "Error: expected an unknown ship kind to produce null.");
    }

    @Test
    @DisplayName("readShip assertion rejects an invalid bearing")
    void readShipRejectsInvalidBearing() {
        assertThrows(AssertionError.class, () -> Tasks.readShip(new Scanner("barca 1 2 x")),
                "Error: expected an invalid bearing to trigger the ship-building assertion.");
    }

    @Test
    @DisplayName("readShip assertion rejects a null scanner")
    void readShipRejectsNullScanner() {
        assertThrows(AssertionError.class, () -> Tasks.readShip(null),
                "Error: expected a null scanner to trigger the readShip assertion.");
    }

    @Test
    @DisplayName("readPosition reads the supplied numeric row and column")
    void readPositionReadsCoordinates() {
        Position position = Tasks.readPosition(new Scanner("3 7"));

        assertAll(
                () -> assertEquals(3, position.getRow(), "Error: expected row 3."),
                () -> assertEquals(7, position.getColumn(), "Error: expected column 7.")
        );
    }

    @Test
    @DisplayName("readPosition rejects a non-numeric coordinate")
    void readPositionRejectsNonNumericCoordinate() {
        assertThrows(java.util.InputMismatchException.class,
                () -> Tasks.readPosition(new Scanner("row 7")),
                "Error: expected a non-numeric row to trigger InputMismatchException.");
    }

    @Test
    @DisplayName("readPosition assertion rejects a null scanner")
    void readPositionRejectsNullScanner() {
        assertThrows(AssertionError.class, () -> Tasks.readPosition(null),
                "Error: expected a null scanner to trigger the readPosition assertion.");
    }

    @Test
    @DisplayName("readClassicPosition accepts compact uppercase and lowercase coordinates")
    void readClassicPositionAcceptsCompactCoordinates() {
        IPosition uppercase = Tasks.readClassicPosition(new Scanner("A3"));
        IPosition lowercase = Tasks.readClassicPosition(new Scanner("b10"));

        assertAll(
                () -> assertAll(
                        () -> assertEquals(0, uppercase.getRow(), "Error: expected A3 to map to row 0."),
                        () -> assertEquals(2, uppercase.getColumn(), "Error: expected A3 to map to column 2.")
                ),
                () -> assertAll(
                        () -> assertEquals(1, lowercase.getRow(), "Error: expected b10 to normalize to row 1."),
                        () -> assertEquals(9, lowercase.getColumn(), "Error: expected b10 to map to column 9.")
                )
        );
    }

    @Test
    @DisplayName("readClassicPosition accepts a coordinate separated by whitespace")
    void readClassicPositionAcceptsSpacedCoordinates() {
        IPosition position = Tasks.readClassicPosition(new Scanner("C 4"));

        assertAll(
                () -> assertEquals(2, position.getRow(), "Error: expected C 4 to map to row 2."),
                () -> assertEquals(3, position.getColumn(), "Error: expected C 4 to map to column 3.")
        );
    }

    @Test
    @DisplayName("readClassicPosition rejects input with no tokens")
    void readClassicPositionRejectsEmptyInput() {
        assertThrows(IllegalArgumentException.class,
                () -> Tasks.readClassicPosition(new Scanner("")),
                "Error: expected empty input to throw IllegalArgumentException.");
    }

    @Test
    @DisplayName("readClassicPosition rejects malformed coordinates")
    void readClassicPositionRejectsMalformedCoordinates() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> Tasks.readClassicPosition(new Scanner("A word")),
                        "Error: expected a non-numeric second token to make the coordinate invalid."),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> Tasks.readClassicPosition(new Scanner("word 3")),
                        "Error: expected a non-letter row token to make the coordinate invalid."),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> Tasks.readClassicPosition(new Scanner("A -3")),
                        "Error: expected a signed spaced row to fail the unsigned row format.")
        );
    }

    @Test
    @DisplayName("readClassicPosition rejects a compact coordinate whose row overflows an integer")
    void readClassicPositionRejectsOverflowingRow() {
        assertThrows(NumberFormatException.class,
                () -> Tasks.readClassicPosition(new Scanner("A999999999999999999999")),
                "Error: expected an overflowing row number to throw NumberFormatException.");
    }

    private static String captureOutput(Runnable action) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOutput = System.out;
        try {
            System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
            action.run();
        } finally {
            System.setOut(originalOutput);
        }
        return output.toString(StandardCharsets.UTF_8);
    }

    private static String fleetInput() {
        return "barca 0 0 n\nbarca 0 2 n\nbarca 0 4 n\nbarca 0 6 n\nbarca 0 8 n\n"
                + "barca 2 0 n\nbarca 2 2 n\nbarca 2 4 n\nbarca 2 6 n\nbarca 2 8 n\n"
                + "barca 4 0 n\nbarca 4 2 n";
    }

    private static String runMenuInChildProcess(String input, boolean simulation) throws Exception {
        String javaExecutable = Path.of(System.getProperty("java.home"), "bin", "java").toString();
        String classPath = System.getProperty("java.class.path");
        ProcessBuilder processBuilder = new ProcessBuilder(javaExecutable, "-cp", classPath,
                "battleship.TasksTest$MenuProcess");
        if (simulation) {
            processBuilder.command().add("simulation");
        }
        Process process = processBuilder.redirectErrorStream(true).start();
        CompletableFuture<byte[]> output = CompletableFuture.supplyAsync(() -> {
            try {
                return process.getInputStream().readAllBytes();
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        });
        try (PrintStream childInput = new PrintStream(process.getOutputStream(), true, StandardCharsets.UTF_8)) {
            childInput.print(input);
        }

        boolean exited = process.waitFor(30, TimeUnit.SECONDS);
        if (!exited) {
            process.destroyForcibly();
            fail("Error: expected the menu subprocess to exit within 30 seconds.");
        }
        String outputText = new String(output.join(), StandardCharsets.UTF_8);
        assertEquals(0, process.exitValue(),
                "Error: expected the menu subprocess to exit successfully. Child output: " + outputText);
        return outputText;
    }

    public static final class MenuProcess {
        public static void main(String[] args) {
            new Tasks();
            if (args.length > 0) {
                Thread.currentThread().interrupt();
            }
            Tasks.menu();
        }
    }
}
