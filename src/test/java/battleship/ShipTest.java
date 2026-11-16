package battleship;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for Ship.
 * Author: luisf
 * Date: 2026-11-13 18:32
 * Cyclomatic Complexity:
 * - buildShip: 6
 * - constructor: 1
 * - getCategory: 1
 * - getPositions: 1
 * - getAdjacentPositions: 5
 * - getPosition: 1
 * - getBearing: 1
 * - getSize: 1
 * - stillFloating: 3
 * - getTopMostPos: 3
 * - getBottomMostPos: 3
 * - getLeftMostPos: 3
 * - getRightMostPos: 3
 * - occupies: 3
 * - tooCloseTo(IShip): 3
 * - tooCloseTo(IPosition): 3
 * - isInsideBoard: 5
 * - shoot: 3
 * - sink: 2
 * - toString: 1
 * - verificarNavio: 2
 */
class ShipTest {

    private Ship ship;

    @BeforeEach
    void setUp() {
        ship = new Barge(Compass.NORTH, new Position(5, 5));
    }

    @AfterEach
    void tearDown() {
        ship = null;
    }

    @Test
    @DisplayName("buildShip creates a barge")
    void buildShip1() {
        Ship built = Ship.buildShip("barca", Compass.NORTH, new Position(1, 1));
        assertAll(
                () -> assertInstanceOf(Barge.class, built, "Error: expected a Barge for 'barca'."),
                () -> assertEquals("Barca", built.getCategory(), "Error: expected the barge category.")
        );
    }

    @Test
    @DisplayName("buildShip creates a caravel")
    void buildShip2() {
        Ship built = Ship.buildShip("caravela", Compass.SOUTH, new Position(1, 1));
        assertInstanceOf(Caravel.class, built, "Error: expected a Caravel for 'caravela'.");
    }

    @Test
    @DisplayName("buildShip creates a carrack")
    void buildShip3() {
        Ship built = Ship.buildShip("nau", Compass.EAST, new Position(1, 1));
        assertInstanceOf(Carrack.class, built, "Error: expected a Carrack for 'nau'.");
    }

    @Test
    @DisplayName("buildShip creates a frigate")
    void buildShip4() {
        Ship built = Ship.buildShip("fragata", Compass.WEST, new Position(1, 1));
        assertInstanceOf(Frigate.class, built, "Error: expected a Frigate for 'fragata'.");
    }

    @Test
    @DisplayName("buildShip creates a galleon")
    void buildShip5() {
        Ship built = Ship.buildShip("galeao", Compass.NORTH, new Position(1, 1));
        assertInstanceOf(Galleon.class, built, "Error: expected a Galleon for 'galeao'.");
    }

    @Test
    @DisplayName("buildShip returns null for an unknown kind")
    void buildShip6() {
        assertNull(Ship.buildShip("unknown", Compass.NORTH, new Position(1, 1)),
                "Error: expected null for an unknown ship kind.");
    }

    @Test
    @DisplayName("buildShip assertions reject null arguments")
    void buildShip7() {
        Position position = new Position(1, 1);
        assertAll(
                () -> assertThrows(AssertionError.class,
                        () -> Ship.buildShip(null, Compass.NORTH, position),
                        "Error: expected a null ship kind to trigger its assertion."),
                () -> assertThrows(AssertionError.class,
                        () -> Ship.buildShip("barca", null, position),
                        "Error: expected a null bearing to trigger its assertion."),
                () -> assertThrows(AssertionError.class,
                        () -> Ship.buildShip("barca", Compass.NORTH, null),
                        "Error: expected a null position to trigger its assertion.")
        );
    }

    @Test
    @DisplayName("constructor stores the supplied values and rejects null arguments")
    void constructor() {
        IPosition position = new Position(2, 3);
        Ship constructed = createShip("Test", Compass.SOUTH, position, 2);

        assertAll(
                () -> assertEquals("Test", constructed.getCategory(), "Error: expected the supplied category."),
                () -> assertSame(Compass.SOUTH, constructed.getBearing(), "Error: expected the supplied bearing."),
                () -> assertSame(position, constructed.getPosition(), "Error: expected the supplied position."),
                () -> assertEquals(2, constructed.getSize(), "Error: expected the supplied size."),
                () -> assertNotNull(constructed.getPositions(), "Error: expected an initialized positions list."),
                () -> assertThrows(NullPointerException.class,
                        () -> createShip(null, Compass.NORTH, position, 1),
                        "Error: expected a null category to throw NullPointerException."),
                () -> assertThrows(NullPointerException.class,
                        () -> createShip("Test", null, position, 1),
                        "Error: expected a null bearing to throw NullPointerException."),
                () -> assertThrows(NullPointerException.class,
                        () -> createShip("Test", Compass.NORTH, null, 1),
                        "Error: expected a null position to throw NullPointerException.")
        );
    }

    @Test
    @DisplayName("getCategory returns the ship category")
    void getCategory() {
        assertEquals("Barca", ship.getCategory(), "Error: expected the category 'Barca'.");
    }

    @Test
    @DisplayName("getPositions returns the ship's mutable position list")
    void getPositions() {
        List<IPosition> positions = ship.getPositions();
        assertAll(
                () -> assertNotNull(positions, "Error: expected a non-null positions list."),
                () -> assertEquals(1, positions.size(), "Error: expected one occupied position."),
                () -> assertEquals(new Position(5, 5), positions.get(0),
                        "Error: expected the ship position at row 5, column 5.")
        );
    }

    @Test
    @DisplayName("getAdjacentPositions returns empty when there are no ship positions")
    void getAdjacentPositions1() {
        assertTrue(createShip(0).getAdjacentPositions().isEmpty(),
                "Error: expected no adjacent positions for an empty ship.");
    }

    @Test
    @DisplayName("getAdjacentPositions handles positions with no adjacent candidates")
    void getAdjacentPositions2() {
        Position isolated = new Position(5, 5) {
            @Override
            public List<IPosition> adjacentPositions() {
                return List.of();
            }
        };
        assertTrue(createShip(1, isolated).getAdjacentPositions().isEmpty(),
                "Error: expected no adjacent positions when the position supplies none.");
    }

    @Test
    @DisplayName("getAdjacentPositions excludes positions occupied by the ship")
    void getAdjacentPositions3() {
        Ship twoPositions = createShip(2, new Position(5, 5), new Position(5, 6));
        List<IPosition> adjacentPositions = twoPositions.getAdjacentPositions();
        assertAll(
                () -> assertFalse(adjacentPositions.contains(new Position(5, 5)),
                        "Error: expected the first occupied position to be excluded."),
                () -> assertFalse(adjacentPositions.contains(new Position(5, 6)),
                        "Error: expected the second occupied position to be excluded.")
        );
    }

    @Test
    @DisplayName("getAdjacentPositions suppresses duplicate adjacent positions")
    void getAdjacentPositions4() {
        List<IPosition> adjacentPositions = createShip(2, new Position(5, 5), new Position(5, 6))
                .getAdjacentPositions();
        long matchingCount = adjacentPositions.stream().filter(new Position(4, 5)::equals).count();
        assertEquals(1, matchingCount, "Error: expected a shared adjacent position to appear only once.");
    }

    @Test
    @DisplayName("getAdjacentPositions includes unique unoccupied neighbors")
    void getAdjacentPositions5() {
        assertEquals(8, ship.getAdjacentPositions().size(),
                "Error: expected eight adjacent positions around a middle-board barge.");
    }

    @Test
    @DisplayName("getPosition returns the original position reference")
    void getPosition() {
        assertEquals(new Position(5, 5), ship.getPosition(),
                "Error: expected the ship's original position at row 5, column 5.");
    }

    @Test
    @DisplayName("getBearing returns the ship bearing")
    void getBearing() {
        assertSame(Compass.NORTH, ship.getBearing(), "Error: expected the NORTH bearing.");
    }

    @Test
    @DisplayName("getSize returns the number of ship positions")
    void getSize() {
        assertEquals(1, ship.getSize(), "Error: expected a barge size of one.");
    }

    @Test
    @DisplayName("stillFloating returns false when the ship has no positions")
    void stillFloating1() {
        assertFalse(createShip(0).stillFloating(), "Error: expected an empty ship not to be floating.");
    }

    @Test
    @DisplayName("stillFloating returns true when an unhit position is found")
    void stillFloating2() {
        Ship partlyHit = createShip(2, new Position(5, 5), new Position(5, 6));
        partlyHit.getPositions().get(0).shoot();
        assertTrue(partlyHit.stillFloating(), "Error: expected a ship with an unhit position to be floating.");
    }

    @Test
    @DisplayName("stillFloating returns false when all positions are hit")
    void stillFloating3() {
        ship.getPositions().get(0).shoot();
        assertFalse(ship.stillFloating(), "Error: expected a fully hit ship not to be floating.");
    }

    @Test
    @DisplayName("getTopMostPos returns the only position's row")
    void getTopMostPos1() {
        assertEquals(5, ship.getTopMostPos(), "Error: expected the only row to be the topmost row.");
    }

    @Test
    @DisplayName("getTopMostPos keeps the first row when the next is lower priority")
    void getTopMostPos2() {
        assertEquals(5, createShip(2, new Position(5, 5), new Position(6, 5)).getTopMostPos(),
                "Error: expected the smaller first row to remain topmost.");
    }

    @Test
    @DisplayName("getTopMostPos updates to a smaller row")
    void getTopMostPos3() {
        assertEquals(5, createShip(2, new Position(6, 5), new Position(5, 5)).getTopMostPos(),
                "Error: expected the smaller later row to become topmost.");
    }

    @Test
    @DisplayName("getBottomMostPos returns the only position's row")
    void getBottomMostPos1() {
        assertEquals(5, ship.getBottomMostPos(), "Error: expected the only row to be the bottommost row.");
    }

    @Test
    @DisplayName("getBottomMostPos keeps the first row when the next is higher priority")
    void getBottomMostPos2() {
        assertEquals(6, createShip(2, new Position(6, 5), new Position(5, 5)).getBottomMostPos(),
                "Error: expected the larger first row to remain bottommost.");
    }

    @Test
    @DisplayName("getBottomMostPos updates to a larger row")
    void getBottomMostPos3() {
        assertEquals(6, createShip(2, new Position(5, 5), new Position(6, 5)).getBottomMostPos(),
                "Error: expected the larger later row to become bottommost.");
    }

    @Test
    @DisplayName("getLeftMostPos returns the only position's column")
    void getLeftMostPos1() {
        assertEquals(5, ship.getLeftMostPos(), "Error: expected the only column to be the leftmost column.");
    }

    @Test
    @DisplayName("getLeftMostPos keeps the first column when the next is larger")
    void getLeftMostPos2() {
        assertEquals(5, createShip(2, new Position(5, 5), new Position(5, 6)).getLeftMostPos(),
                "Error: expected the smaller first column to remain leftmost.");
    }

    @Test
    @DisplayName("getLeftMostPos updates to a smaller column")
    void getLeftMostPos3() {
        assertEquals(5, createShip(2, new Position(5, 6), new Position(5, 5)).getLeftMostPos(),
                "Error: expected the smaller later column to become leftmost.");
    }

    @Test
    @DisplayName("getRightMostPos returns the only position's column")
    void getRightMostPos1() {
        assertEquals(5, ship.getRightMostPos(), "Error: expected the only column to be the rightmost column.");
    }

    @Test
    @DisplayName("getRightMostPos keeps the first column when the next is smaller")
    void getRightMostPos2() {
        assertEquals(6, createShip(2, new Position(5, 6), new Position(5, 5)).getRightMostPos(),
                "Error: expected the larger first column to remain rightmost.");
    }

    @Test
    @DisplayName("getRightMostPos updates to a larger column")
    void getRightMostPos3() {
        assertEquals(6, createShip(2, new Position(5, 5), new Position(5, 6)).getRightMostPos(),
                "Error: expected the larger later column to become rightmost.");
    }

    @Test
    @DisplayName("occupies returns false when there are no positions")
    void occupies1() {
        assertFalse(createShip(0).occupies(new Position(5, 5)),
                "Error: expected an empty ship not to occupy any position.");
    }

    @Test
    @DisplayName("occupies returns true for an occupied position")
    void occupies2() {
        assertTrue(ship.occupies(new Position(5, 5)),
                "Error: expected the ship to occupy row 5, column 5.");
    }

    @Test
    @DisplayName("occupies returns false for an unoccupied position")
    void occupies3() {
        assertFalse(ship.occupies(new Position(1, 1)),
                "Error: expected the ship not to occupy row 1, column 1.");
    }

    @Test
    @DisplayName("occupies assertion rejects a null position")
    void occupies4() {
        assertThrows(AssertionError.class, () -> ship.occupies(null),
                "Error: expected a null position to trigger the occupies assertion.");
    }

    @Test
    @DisplayName("tooCloseTo(IShip) returns false when the other ship has no positions")
    void tooCloseToShip1() {
        assertFalse(ship.tooCloseTo(createShip(0)),
                "Error: expected an empty other ship not to be too close.");
    }

    @Test
    @DisplayName("tooCloseTo(IShip) returns true when a position is adjacent")
    void tooCloseToShip2() {
        assertTrue(ship.tooCloseTo(new Barge(Compass.SOUTH, new Position(5, 6))),
                "Error: expected neighboring ships to be too close.");
    }

    @Test
    @DisplayName("tooCloseTo(IShip) returns false when no position is adjacent")
    void tooCloseToShip3() {
        assertFalse(ship.tooCloseTo(new Barge(Compass.SOUTH, new Position(8, 8))),
                "Error: expected distant ships not to be too close.");
    }

    @Test
    @DisplayName("tooCloseTo(IShip) assertion rejects a null ship")
    void tooCloseToShip4() {
        assertThrows(AssertionError.class, () -> ship.tooCloseTo((IShip) null),
                "Error: expected a null ship to trigger the tooCloseTo assertion.");
    }

    @Test
    @DisplayName("tooCloseTo(IPosition) returns false when there are no ship positions")
    void tooCloseToPosition1() {
        assertFalse(createShip(0).tooCloseTo(new Position(5, 6)),
                "Error: expected an empty ship not to be too close to a position.");
    }

    @Test
    @DisplayName("tooCloseTo(IPosition) returns true for an adjacent position")
    void tooCloseToPosition2() {
        assertTrue(ship.tooCloseTo(new Position(5, 6)),
                "Error: expected the neighboring position to be too close.");
    }

    @Test
    @DisplayName("tooCloseTo(IPosition) returns false for a distant position")
    void tooCloseToPosition3() {
        assertFalse(ship.tooCloseTo(new Position(8, 8)),
                "Error: expected the distant position not to be too close.");
    }

    @Test
    @DisplayName("tooCloseTo(IPosition) assertion rejects a null position")
    void tooCloseToPosition4() {
        assertThrows(AssertionError.class, () -> ship.tooCloseTo((IPosition) null),
                "Error: expected a null position to trigger the tooCloseTo assertion.");
    }

    @Test
    @DisplayName("isInsideBoard accepts a ship whose bounds are inside the board")
    void isInsideBoard1() {
        assertTrue(ship.isInsideBoard(), "Error: expected a ship within the board to be inside.");
    }

    @Test
    @DisplayName("isInsideBoard rejects a negative leftmost column")
    void isInsideBoard2() {
        assertFalse(createShip(1, new Position(5, -1)).isInsideBoard(),
                "Error: expected a negative leftmost column to be outside.");
    }

    @Test
    @DisplayName("isInsideBoard rejects a rightmost column beyond the board")
    void isInsideBoard3() {
        assertFalse(createShip(1, new Position(5, Game.BOARD_SIZE)).isInsideBoard(),
                "Error: expected a column beyond the board to be outside.");
    }

    @Test
    @DisplayName("isInsideBoard rejects a negative topmost row")
    void isInsideBoard4() {
        assertFalse(createShip(1, new Position(-1, 5)).isInsideBoard(),
                "Error: expected a negative topmost row to be outside.");
    }

    @Test
    @DisplayName("isInsideBoard rejects a bottommost row beyond the board")
    void isInsideBoard5() {
        assertFalse(createShip(1, new Position(Game.BOARD_SIZE, 5)).isInsideBoard(),
                "Error: expected a row beyond the board to be outside.");
    }

    @Test
    @DisplayName("shoot does nothing when there are no ship positions")
    void shoot1() {
        assertDoesNotThrow(() -> createShip(0).shoot(new Position(5, 5)),
                "Error: expected shooting an empty ship to complete without an exception.");
    }

    @Test
    @DisplayName("shoot marks a matching ship position as hit")
    void shoot2() {
        ship.shoot(new Position(5, 5));
        assertTrue(ship.getPositions().get(0).isHit(), "Error: expected the matching position to be hit.");
    }

    @Test
    @DisplayName("shoot leaves ship positions unchanged when the target does not match")
    void shoot3() {
        ship.shoot(new Position(1, 1));
        assertFalse(ship.getPositions().get(0).isHit(),
                "Error: expected a nonmatching target not to hit the ship.");
    }

    @Test
    @DisplayName("shoot assertions reject null and out-of-board targets")
    void shoot4() {
        assertAll(
                () -> assertThrows(AssertionError.class, () -> ship.shoot(null),
                        "Error: expected a null target to trigger the shoot assertion."),
                () -> assertThrows(AssertionError.class, () -> ship.shoot(new Position(-1, 0)),
                        "Error: expected an out-of-board target to trigger the shoot assertion.")
        );
    }

    @Test
    @DisplayName("sink does nothing for a ship with no positions")
    void sink1() {
        Ship emptyShip = createShip(0);
        emptyShip.sink();
        assertTrue(emptyShip.getPositions().isEmpty(),
                "Error: expected an empty ship to remain without positions after sinking.");
    }

    @Test
    @DisplayName("sink marks every ship position as hit")
    void sink2() {
        Ship twoPositions = createShip(2, new Position(5, 5), new Position(5, 6));
        twoPositions.sink();
        assertTrue(twoPositions.getPositions().stream().allMatch(IPosition::isHit),
                "Error: expected sink to hit every ship position.");
    }

    @Test
    @DisplayName("toString includes the ship category, bearing, and position")
    void shipToString() {
        assertEquals("[Barca n F6]", ship.toString(),
                "Error: expected the ship description to include its category, bearing, and position.");
    }

    @Test
    @DisplayName("verificarNavio leaves a floating ship afloat without printing a sink message")
    void verificarNavio1() {
        ship.verificarNavio(ship);
        assertTrue(ship.stillFloating(), "Error: expected verificarNavio not to alter a floating ship.");
    }

    @Test
    @DisplayName("verificarNavio prints the sink message for a sunk ship")
    void verificarNavio2() {
        ship.sink();
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        PrintStream originalOutput = System.out;
        try {
            System.setOut(new PrintStream(output, true, StandardCharsets.UTF_8));
            ship.verificarNavio(ship);
        } finally {
            System.setOut(originalOutput);
        }
        assertEquals("O navio de tamanho 1 foi afundado. 5", output.toString(StandardCharsets.UTF_8).trim(),
                "Error: expected verificarNavio to print the sunk ship details.");
    }

    private static Ship createShip(int size, IPosition... positions) {
        IPosition origin = positions.length == 0 ? new Position(0, 0) : positions[0];
        Ship created = createShip("Test", Compass.NORTH, origin, size);
        created.getPositions().addAll(new ArrayList<>(List.of(positions)));
        return created;
    }

    private static Ship createShip(String category, Compass bearing, IPosition position, int size) {
        return new Ship(category, bearing, position, size) {
        };
    }
}
