package battleship;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.NoSuchElementException;
import java.util.Scanner;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Clean unit and direct-JVM menu tests for {@link Tasks}.
 * Author: luisf (IDE path variable user.name)
 * Generated: 2026-11-20 18:12 UTC
 * Cyclomatic complexity (McCabe, decisions + 1; constructor is CC 1):
 * Tasks(): 1; menu(): 24; menuHelp(): 1; buildFleet(Scanner): 4;
 * readShip(Scanner): 1; readPosition(Scanner): 1; readClassicPosition(Scanner): 8.
 */
class TasksTest {
    private Tasks tasks;

    @BeforeEach
    void setUp() {
        tasks = new Tasks();
    }

    @AfterEach
    void tearDown() {
        tasks = null;
    }

    @Test
    @DisplayName("Tasks constructor creates a usable instance")
    void constructor() {
        assertNotNull(tasks, "Error: expected the public constructor to create an instance.");
    }

    @Test
    @DisplayName("menuHelp prints every documented command")
    void menuHelp() {
        String output = captureOutput(() -> Tasks.menuHelp(), "");
        assertAll(
                () -> assertTrue(output.contains("AJUDA DO MENU"), "Error: expected the menu heading."),
                () -> assertTrue(output.contains("- gerafrota:"), "Error: expected the random-fleet command."),
                () -> assertTrue(output.contains("- lefrota:"), "Error: expected the custom-fleet command."),
                () -> assertTrue(output.contains("- estado:"), "Error: expected the status command."),
                () -> assertTrue(output.contains("- mapa:"), "Error: expected the text-map command."),
                () -> assertTrue(output.contains("- mapagrafico:"), "Error: expected the graphical-map command."),
                () -> assertTrue(output.contains("- rajada:"), "Error: expected the salvo command."),
                () -> assertTrue(output.contains("- simula:"), "Error: expected the simulation command."),
                () -> assertTrue(output.contains("- tiros:"), "Error: expected the shots command."),
                () -> assertTrue(output.contains("- desisto:"), "Error: expected the quit command.")
        );
    }

    @Test
    @DisplayName("menu handles unknown and pre-game commands before EOF")
    void menu1() {
        String input = String.join("\n", "desconhecido", "ajuda", "estado", "mapa",
                "mapagrafico", "rajada", "simula", "tiros", "");
        MenuResult result = runMenuDirectly(input);
        assertAll(
                () -> assertInstanceOf(NoSuchElementException.class, result.expectedException,
                        "Error: expected menu termination by Scanner EOF."),
                () -> assertTrue(result.output.contains("Que comando é esse??? Repete ..."),
                        "Error: expected unknown-command feedback."),
                () -> assertTrue(result.output.contains("AJUDA DO MENU"),
                        "Error: expected the ajuda command to print help."),
                () -> assertTrue(result.output.contains(">"),
                        "Error: expected menu prompts before EOF.")
        );
    }

    @Test
    @DisplayName("menu gerafrota branch creates a game and prints a board")
    void menu2() {
        MenuResult result = runMenuDirectly("gerafrota\n");
        assertAll(
                () -> assertInstanceOf(NoSuchElementException.class, result.expectedException,
                        "Error: expected menu termination by Scanner EOF."),
                () -> assertTrue(result.output.contains("LEGENDA"),
                        "Error: expected gerafrota to print the board and legend.")
        );
    }

    @Test
    @DisplayName("menu lefrota branch creates and displays a valid custom fleet")
    void menu3() {
        MenuResult result = runMenuDirectly("lefrota\n" + fleetInput());
        assertAll(
                () -> assertInstanceOf(NoSuchElementException.class, result.expectedException,
                        "Error: expected custom-fleet setup to finish at Scanner EOF."),
                () -> assertTrue(result.output.contains("LEGENDA"),
                        "Error: expected lefrota to print the board and legend.")
        );
    }

    @Test
    @DisplayName("menu post-fleet commands exercise status, map, shots and salvos")
    void menu4() {
        String input = "lefrota\n" + fleetInput()
                + "estado\nmapa\ntiros\nrajada A1 A3 A5\nrajada A7 A9 C1\n";
        MenuResult result = runMenuDirectly(input);
        assertAll(
                () -> assertInstanceOf(NoSuchElementException.class, result.expectedException,
                        "Error: expected menu termination by Scanner EOF."),
                () -> assertTrue(result.output.contains("Estado da Frota:"),
                        "Error: expected post-fleet status to be printed."),
                () -> assertTrue(result.output.contains("LEGENDA"),
                        "Error: expected mapa/tiros to print board legends."),
                () -> assertTrue(result.output.contains("rajada"),
                        "Error: expected enemy-fire data to be printed."),
                () -> assertTrue(result.output.contains("Passaram"),
                        "Error: expected the second salvo to report elapsed time.")
        );
    }

    @Test
    @DisplayName("menu graphic-map command is exercised before and after fleet creation")
    void menu5() {
        String input = "mapagrafico\nlefrota\n" + fleetInput() + "mapagrafico\n";
        MenuResult result = runMenuDirectly(input);
        assertInstanceOf(NoSuchElementException.class, result.expectedException,
                "Error: expected menu termination by Scanner EOF.");
    }

    @Test
    @DisplayName("menu gerafrota supports post-game status map shots and a first salvo")
    void menu6() {
        String input = "gerafrota\nestado\nmapa\ntiros\nrajada A1 A2 A3\n";
        MenuResult result = runMenuDirectly(input);

        assertAll(
                () -> assertInstanceOf(NoSuchElementException.class, result.expectedException,
                        "Error: expected menu termination by Scanner EOF after the random-fleet scenario."),
                () -> assertTrue(result.output.contains("Estado da Frota:"),
                        "Error: expected status after gerafrota to print fleet state."),
                () -> assertTrue(result.output.contains("LEGENDA"),
                        "Error: expected mapa and tiros to print board legends."),
                () -> assertTrue(result.output.contains("rajada"),
                        "Error: expected the first salvo to print enemy-fire data.")
        );
    }

    @Test
    @DisplayName("buildFleet1 skips unknown and conflicting ships and fills 12 positions")
    void buildFleet1() {
        String input = "unknown 7 7 n "
                + "barca 0 0 n " + "barca 0 0 n "
                + "barca 0 2 n " + "barca 0 4 n "
                + "barca 0 6 n " + "barca 0 8 n "
                + "barca 2 0 n " + "barca 2 2 n "
                + "barca 2 4 n " + "barca 2 6 n "
                + "barca 2 8 n " + "barca 4 0 n "
                + "barca 4 2 n " + "barca 4 4 n ";
        Fleet fleet = Tasks.buildFleet(new Scanner(input));
        assertEquals(Fleet.FLEET_SIZE, fleet.getShips().size(),
                "Error: expected buildFleet to add exactly 12 ships.");
    }

    @Test
    @DisplayName("buildFleet2 rejects a null scanner")
    void buildFleet2() {
        assertThrows(AssertionError.class, () -> Tasks.buildFleet(null),
                "Error: expected buildFleet(null) to trigger the Java assertion.");
    }

    @Test
    @DisplayName("readShip1 creates each valid ship category")
    void readShip1() {
        assertAll(
                () -> assertEquals("Barca", Tasks.readShip(new Scanner("barca 1 2 n")).getCategory(),
                        "Error: expected barca to be created."),
                () -> assertEquals("Caravela", Tasks.readShip(new Scanner("caravela 1 2 s")).getCategory(),
                        "Error: expected caravela to be created."),
                () -> assertEquals("Nau", Tasks.readShip(new Scanner("nau 1 2 e")).getCategory(),
                        "Error: expected nau to be created."),
                () -> assertEquals("Fragata", Tasks.readShip(new Scanner("fragata 1 2 o")).getCategory(),
                        "Error: expected fragata to be created."),
                () -> assertEquals("Galeao", Tasks.readShip(new Scanner("galeao 1 2 n")).getCategory(),
                        "Error: expected galeao to be created.")
        );
    }

    @Test
    @DisplayName("readShip2 returns null for unknown kind and exercises invalid bearing")
    void readShip2() {
        assertAll(
                () -> assertNull(Tasks.readShip(new Scanner("unknown 1 2 n")),
                        "Error: expected unknown ship kind to return null."),
                () -> assertThrows(AssertionError.class,
                        () -> Tasks.readShip(new Scanner("barca 1 2 x")),
                        "Error: expected invalid bearing to trigger Ship/buildShip assertion."),
                () -> assertThrows(AssertionError.class,
                        () -> Tasks.readShip(null),
                        "Error: expected readShip(null) to trigger the Java assertion."),
                () -> assertThrows(NoSuchElementException.class,
                        () -> Tasks.readShip(new Scanner("barca 1 2")),
                        "Error: expected a missing bearing token to produce NoSuchElementException.")
        );
    }

    @Test
    @DisplayName("readPosition1 reads numeric coordinates")
    void readPosition1() {
        Position position = Tasks.readPosition(new Scanner("3 7"));
        assertAll(
                () -> assertEquals(3, position.getRow(), "Error: expected row 3."),
                () -> assertEquals(7, position.getColumn(), "Error: expected column 7.")
        );
    }

    @Test
    @DisplayName("readPosition2 rejects malformed coordinates and null scanner")
    void readPosition2() {
        assertAll(
                () -> assertThrows(java.util.InputMismatchException.class,
                        () -> Tasks.readPosition(new Scanner("row 7")),
                        "Error: expected a non-numeric row to produce InputMismatchException."),
                () -> assertThrows(java.util.InputMismatchException.class,
                        () -> Tasks.readPosition(new Scanner("999999999999999999999 7")),
                        "Error: expected numeric overflow to produce InputMismatchException."),
                () -> assertThrows(NoSuchElementException.class,
                        () -> Tasks.readPosition(new Scanner("")),
                        "Error: expected missing coordinates to produce NoSuchElementException."),
                () -> assertThrows(AssertionError.class,
                        () -> Tasks.readPosition(null),
                        "Error: expected readPosition(null) to trigger the Java assertion."),
                () -> assertAll(
                        () -> assertEquals(-1, Tasks.readPosition(new Scanner("-1 0")).getRow(),
                                "Error: readPosition must preserve negative numeric input for fleet validation."),
                        () -> assertEquals(10, Tasks.readPosition(new Scanner("0 10")).getColumn(),
                                "Error: readPosition must preserve an out-of-board numeric column."))
        );
    }

    @Test
    @DisplayName("readClassicPosition1 accepts compact and spaced coordinates")
    void readClassicPosition1() {
        IPosition compact = Tasks.readClassicPosition(new Scanner("a3"));
        IPosition spaced = Tasks.readClassicPosition(new Scanner("C 4"));
        IPosition boundary = Tasks.readClassicPosition(new Scanner("J 10"));
        assertAll(
                () -> assertAll(
                        () -> assertEquals(0, compact.getRow(), "Error: expected A3 to map to row 0."),
                        () -> assertEquals(2, compact.getColumn(), "Error: expected A3 to map to column 2.")),
                () -> assertAll(
                        () -> assertEquals(2, spaced.getRow(), "Error: expected C 4 to map to row 2."),
                        () -> assertEquals(3, spaced.getColumn(), "Error: expected C 4 to map to column 3.")),
                () -> assertAll(
                        () -> assertEquals(9, boundary.getRow(), "Error: expected J 10 to map to row 9."),
                        () -> assertEquals(9, boundary.getColumn(), "Error: expected J 10 to map to column 10."))
        );
    }

    @Test
    @DisplayName("readClassicPosition2 rejects empty and malformed input")
    void readClassicPosition2() {
        assertAll(
                () -> assertThrows(IllegalArgumentException.class,
                        () -> Tasks.readClassicPosition(new Scanner("")),
                        "Error: expected empty input to be rejected."),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> Tasks.readClassicPosition(new Scanner("A word")),
                        "Error: expected a non-numeric spaced row to be rejected."),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> Tasks.readClassicPosition(new Scanner("word 3")),
                        "Error: expected a malformed letter/number pair to be rejected."),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> Tasks.readClassicPosition(new Scanner("A -3")),
                        "Error: expected a negative spaced row to be rejected.")
        );
    }

    @Test
    @DisplayName("readClassicPosition3 rejects numeric overflow in both formats")
    void readClassicPosition3() {
        assertAll(
                () -> assertThrows(NumberFormatException.class,
                        () -> Tasks.readClassicPosition(new Scanner("A999999999999999999999")),
                        "Error: expected compact integer overflow to throw NumberFormatException."),
                () -> assertThrows(IllegalArgumentException.class,
                        () -> Tasks.readClassicPosition(new Scanner("A 999999999999999999999")),
                        "Error: expected spaced integer overflow to be rejected as malformed input.")
        );
    }

    private static String fleetInput() {
        return "barca 0 0 n\nbarca 0 2 n\nbarca 0 4 n\nbarca 0 6 n\nbarca 0 8 n\n"
                + "barca 2 0 n\nbarca 2 2 n\nbarca 2 4 n\nbarca 2 6 n\nbarca 2 8 n\n"
                + "barca 4 0 n\nbarca 4 2 n\n";
    }

    private static String captureOutput(Runnable action, String input) {
        InputStream originalIn = System.in;
        PrintStream originalOut = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try {
            System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
            System.setOut(new PrintStream(bytes, true, StandardCharsets.UTF_8));
            action.run();
        } finally {
            System.setOut(originalOut);
            System.setIn(originalIn);
        }
        return bytes.toString(StandardCharsets.UTF_8);
    }

    private static MenuResult runMenuDirectly(String input) {
        InputStream originalIn = System.in;
        PrintStream originalOut = System.out;
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        Throwable exception = null;
        try {
            System.setIn(new ByteArrayInputStream(input.getBytes(StandardCharsets.UTF_8)));
            System.setOut(new PrintStream(bytes, true, StandardCharsets.UTF_8));
            Tasks.menu();
        } catch (Throwable caught) {
            exception = caught;
        } finally {
            System.setOut(originalOut);
            System.setIn(originalIn);
        }
        return new MenuResult(bytes.toString(StandardCharsets.UTF_8), exception);
    }

    private record MenuResult(String output, Throwable expectedException) {}
}