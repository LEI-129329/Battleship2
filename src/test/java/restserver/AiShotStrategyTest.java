package restserver;

import battleship.IGame;
import battleship.IMove;
import battleship.IPosition;
import battleship.IShip;
import battleship.Position;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test class for {@link AiShotStrategy}.
 * Author: Simão Espanha
 * Date: 2026-11-14 17:53
 * Cyclomatic Complexity:
 * - constructor: 1
 * - computeShots(): 18
 * - getHitButNotSunkPositions(): 10
 * - buildHuntPool(): 6
 */
class AiShotStrategyTest {

    private AiShotStrategy strategy;

    @BeforeEach
    void setUp() {
        strategy = new AiShotStrategy();
    }

    @AfterEach
    void tearDown() {
        strategy = null;
    }

    @Test
    void constructor() {
        assertNotNull(strategy, "Error: the strategy instance should be created before each test.");
    }

    @Test
    void computeShots1() {
        List<IPosition> shots = strategy.computeShots(game());

        assertHuntShots(shots, List.of());
    }

    @Test
    void computeShots2() {
        List<IPosition> shots = strategy.computeShots(game(move(List.of(), List.of())));

        assertHuntShots(shots, List.of());
    }

    @Test
    void computeShots3() {
        List<IPosition> previousShots = List.of(position(0, 0), position(2, 2), position(5, 5));

        List<IPosition> shots = strategy.computeShots(game(move(previousShots, List.of())));

        assertHuntShots(shots, previousShots);
    }

    @Test
    void computeShots4() {
        IPosition previousShot = position(4, 4);
        IMove invalidHit = move(List.of(previousShot), List.of(new IGame.ShotResult(false, false, ship(), false)));

        List<IPosition> shots = strategy.computeShots(game(invalidHit));

        assertHuntShots(shots, List.of(previousShot));
    }

    @Test
    void computeShots5() {
        IPosition previousShot = position(4, 4);
        IMove repeatedHit = move(List.of(previousShot), List.of(new IGame.ShotResult(true, true, ship(), false)));

        List<IPosition> shots = strategy.computeShots(game(repeatedHit));

        assertHuntShots(shots, List.of(previousShot));
    }

    @Test
    void computeShots6() {
        IPosition previousShot = position(4, 4);
        IMove miss = move(List.of(previousShot), List.of(new IGame.ShotResult(true, false, null, false)));

        List<IPosition> shots = strategy.computeShots(game(miss));

        assertHuntShots(shots, List.of(previousShot));
    }

    @Test
    void computeShots7() {
        IPosition previousShot = position(4, 4);
        IMove sunkHit = move(List.of(previousShot), List.of(new IGame.ShotResult(true, false, ship(), true)));

        List<IPosition> shots = strategy.computeShots(game(sunkHit));

        assertHuntShots(shots, List.of(previousShot));
    }

    @Test
    void computeShots8() {
        IPosition miss = position(1, 1);
        IPosition hit = position(5, 5);
        IMove moveWithPartialResults = move(
                List.of(miss, hit),
                List.of(new IGame.ShotResult(true, false, null, false),
                        new IGame.ShotResult(true, false, ship(), false)));

        List<IPosition> shots = strategy.computeShots(game(moveWithPartialResults));

        assertTargetShots(shots, moveWithPartialResults, hit);
    }

    @Test
    void computeShots9() {
        IPosition previousShot = position(4, 4);
        IMove moveWithoutResults = move(List.of(previousShot), List.of());

        List<IPosition> shots = strategy.computeShots(game(moveWithoutResults));

        assertHuntShots(shots, List.of(previousShot));
    }

    @Test
    void computeShots10() {
        IPosition cornerHit = position(0, 0);
        IMove hit = hitMove(cornerHit);

        List<IPosition> shots = strategy.computeShots(game(hit));

        assertTargetShots(shots, hit, cornerHit);
    }

    @Test
    void computeShots11() {
        IPosition hitPosition = position(4, 4);
        List<IPosition> neighbours = hitPosition.adjacentPositions();
        IPosition remainingNeighbour = neighbours.getFirst();
        List<IPosition> alreadyShot = new ArrayList<>(neighbours.subList(1, neighbours.size()));
        alreadyShot.add(hitPosition);
        IMove hitAndBlockedNeighbours = hitMoveWithExtraShots(hitPosition, alreadyShot);

        List<IPosition> shots = strategy.computeShots(game(hitAndBlockedNeighbours));

        assertEquals(3, shots.size(), "Error: targeting one remaining neighbour should be supplemented by two hunt shots.");
        assertTrue(shots.contains(remainingNeighbour),
                "Error: the only unshot neighbour of an active hit should be selected.");
        assertTrue(shots.stream().noneMatch(alreadyShot::contains),
                "Error: the strategy should not repeat a previous shot.");
    }

    @Test
    void computeShots12() {
        IPosition firstHit = position(4, 4);
        IPosition secondHit = position(4, 5);
        IMove adjacentHits = move(
                List.of(firstHit, secondHit),
                List.of(new IGame.ShotResult(true, false, ship(), false),
                        new IGame.ShotResult(true, false, ship(), false)));

        List<IPosition> shots = strategy.computeShots(game(adjacentHits));

        assertEquals(3, shots.size(), "Error: multiple active hits should still produce exactly three shots.");
        assertEquals(3, new HashSet<>(shots).size(),
                "Error: overlapping hit neighbours must not result in duplicate shots.");
        assertTrue(shots.stream().allMatch(shot -> firstHit.adjacentPositions().contains(shot)
                        || secondHit.adjacentPositions().contains(shot)),
                "Error: enough target neighbours exist, so all selected shots should target a hit.");
    }

    @Test
    void computeShots13() {
        IPosition firstHit = position(0, 0);
        IPosition secondHit = position(9, 9);
        IMove separatedHits = move(
                List.of(firstHit, secondHit),
                List.of(new IGame.ShotResult(true, false, ship(), false),
                        new IGame.ShotResult(true, false, ship(), false)));

        List<IPosition> shots = strategy.computeShots(game(separatedHits));

        assertEquals(3, shots.size(), "Error: the first target cluster should fill the salvo and stop further selection.");
        assertTrue(firstHit.adjacentPositions().containsAll(shots),
                "Error: the salvo should stop after filling from the first hit's available neighbours.");
    }

    @Test
    void computeShots14() {
        IPosition hitPosition = position(4, 4);
        List<IPosition> allNeighbours = hitPosition.adjacentPositions();
        List<IPosition> shotsAlreadyFired = new ArrayList<>();
        shotsAlreadyFired.add(hitPosition);
        shotsAlreadyFired.addAll(allNeighbours);
        IMove hitWithNoAvailableNeighbours = move(
                shotsAlreadyFired,
                List.of(new IGame.ShotResult(true, false, ship(), false)));

        List<IPosition> shots = strategy.computeShots(game(hitWithNoAvailableNeighbours));

        assertEquals(3, shots.size(), "Error: the hunt phase should fill the salvo when all target neighbours were tried.");
        assertTrue(shots.stream().noneMatch(shotsAlreadyFired::contains),
                "Error: hunt shots should not repeat any previous shot.");
    }

    @Test
    void computeShots15() {
        List<IPosition> allHuntPositions = checkerboardPositions();
        IMove exhaustedHuntPool = move(allHuntPositions, List.of());

        List<IPosition> shots = strategy.computeShots(game(exhaustedHuntPool));

        assertEquals(3, shots.size(), "Error: the safety fallback should provide three shots when the hunt pool is empty.");
        assertTrue(shots.stream().allMatch(shot -> (shot.getRow() + shot.getColumn()) % 2 != 0),
                "Error: fallback shots should come from unseen non-checkerboard cells.");
        assertTrue(shots.stream().noneMatch(allHuntPositions::contains),
                "Error: fallback shots should not repeat checkerboard shots.");
    }

    @Test
    void computeShots16() {
        List<IPosition> allBoardPositions = new ArrayList<>();
        for (int row = 0; row < 10; row++) {
            for (int column = 0; column < 10; column++) {
                allBoardPositions.add(position(row, column));
            }
        }

        List<IPosition> shots = strategy.computeShots(game(move(allBoardPositions, List.of())));

        assertTrue(shots.isEmpty(), "Error: no shots should be returned when every board cell has already been tried.");

        IPosition hit = position(4, 4);
        IPosition alreadyChosen = hit.adjacentPositions().getFirst();
        List<IPosition> previouslyShot = new ArrayList<>();
        for (int row = 0; row < 10; row++) {
            for (int column = 0; column < 10; column++) {
                IPosition position = position(row, column);
                if (!position.equals(hit) && !position.equals(alreadyChosen)) {
                    previouslyShot.add(position);
                }
            }
        }
        List<IPosition> targetShot = strategy.computeShots(
                game(hitMoveWithExtraShots(hit, previouslyShot)));

        assertEquals(List.of(alreadyChosen), targetShot,
                "Error: fallback should not add a target position that is already in the chosen shots.");
    }

    @Test
    void computeShots17() {
        List<IPosition> huntPositions = checkerboardPositions();
        IPosition firstRemaining = position(9, 9);
        IPosition secondRemaining = position(9, 7);
        huntPositions.remove(firstRemaining);
        huntPositions.remove(secondRemaining);

        List<IPosition> shots = strategy.computeShots(game(move(huntPositions, List.of())));

        assertEquals(3, shots.size(), "Error: one fallback shot should complete the salvo after two hunt shots.");
        assertTrue(shots.contains(firstRemaining) && shots.contains(secondRemaining),
                "Error: both remaining checkerboard cells should be selected before the safety fallback.");
    }

    @Test
    void computeShots18() {
        IPosition firstShot = position(2, 2);
        IPosition secondShot = position(6, 6);
        IMove hitOnSecondShot = move(
                List.of(firstShot, secondShot),
                List.of(new IGame.ShotResult(true, false, null, false),
                        new IGame.ShotResult(true, false, ship(), false)));

        List<IPosition> shots = strategy.computeShots(game(hitOnSecondShot));

        assertTargetShots(shots, hitOnSecondShot, secondShot);
    }

    @Test
    void computeShots19() {
        IPosition firstShot = position(2, 2);
        IPosition secondShot = position(6, 6);
        IMove noHits = move(
                List.of(firstShot, secondShot),
                List.of(new IGame.ShotResult(true, false, null, false),
                        new IGame.ShotResult(true, false, null, false)));

        List<IPosition> shots = strategy.computeShots(game(noHits));

        assertHuntShots(shots, List.of(firstShot, secondShot));
    }

    @Test
    void computeShots20() {
        NullPointerException exception = assertThrows(NullPointerException.class,
                () -> strategy.computeShots(null),
                "Error: a null game should raise NullPointerException instead of returning shots.");

        assertNotNull(exception, "Error: the expected NullPointerException should be captured.");
    }

    @Test
    void computeShots21() {
        IPosition firstHit = position(4, 4);
        IPosition secondHit = position(4, 5);
        List<IPosition> sharedAvailableNeighbours = List.of(position(3, 5), position(5, 5));
        List<IPosition> previousShots = new ArrayList<>(List.of(firstHit, secondHit));
        for (IPosition neighbour : firstHit.adjacentPositions()) {
            if (!sharedAvailableNeighbours.contains(neighbour)) {
                previousShots.add(neighbour);
            }
        }
        IMove overlappingHits = move(previousShots, List.of(
                new IGame.ShotResult(true, false, ship(), false),
                new IGame.ShotResult(true, false, ship(), false)));

        List<IPosition> shots = strategy.computeShots(game(overlappingHits));

        assertEquals(3, shots.size(),
                "Error: overlapping target neighbours should be skipped while enough other targets remain.");
        assertTrue(shots.containsAll(sharedAvailableNeighbours),
                "Error: both unshot neighbours shared by the hit positions should be selected once.");
        assertEquals(shots.size(), new HashSet<>(shots).size(),
                "Error: an overlapping target neighbour must not be added twice.");
        assertTrue(shots.stream().noneMatch(previousShots::contains),
                "Error: overlapping-target selection must not repeat previously fired positions.");
    }

    @Test
    void getHitButNotSunkPositions1() throws ReflectiveOperationException {
        List<IPosition> hits = invokeGetHitButNotSunkPositions(game(), Set.of());

        assertTrue(hits.isEmpty(), "Error: no previous moves should produce no hit targets.");
    }

    @Test
    void getHitButNotSunkPositions2() throws ReflectiveOperationException {
        List<IPosition> hits = invokeGetHitButNotSunkPositions(game(move(List.of(), List.of())), Set.of());

        assertTrue(hits.isEmpty(), "Error: an empty result list should produce no hit targets.");
    }

    @Test
    void getHitButNotSunkPositions3() throws ReflectiveOperationException {
        IPosition shot = position(3, 3);
        IMove invalidShot = move(List.of(shot), List.of(new IGame.ShotResult(false, false, ship(), false)));

        List<IPosition> hits = invokeGetHitButNotSunkPositions(game(invalidShot), Set.of(shot));

        assertTrue(hits.isEmpty(), "Error: an invalid shot must not be reported as a live hit.");
    }

    @Test
    void getHitButNotSunkPositions4() throws ReflectiveOperationException {
        IPosition shot = position(3, 3);
        IMove repeatedShot = move(List.of(shot), List.of(new IGame.ShotResult(true, true, ship(), false)));

        List<IPosition> hits = invokeGetHitButNotSunkPositions(game(repeatedShot), Set.of(shot));

        assertTrue(hits.isEmpty(), "Error: a repeated shot must not be reported as a live hit.");
    }

    @Test
    void getHitButNotSunkPositions5() throws ReflectiveOperationException {
        IPosition shot = position(3, 3);
        IMove miss = move(List.of(shot), List.of(new IGame.ShotResult(true, false, null, false)));

        List<IPosition> hits = invokeGetHitButNotSunkPositions(game(miss), Set.of(shot));

        assertTrue(hits.isEmpty(), "Error: a shot without a ship must not be reported as a live hit.");
    }

    @Test
    void getHitButNotSunkPositions6() throws ReflectiveOperationException {
        IPosition shot = position(3, 3);
        IMove sunkHit = move(List.of(shot), List.of(new IGame.ShotResult(true, false, ship(), true)));

        List<IPosition> hits = invokeGetHitButNotSunkPositions(game(sunkHit), Set.of(shot));

        assertTrue(hits.isEmpty(), "Error: a sunk ship hit must not be reported as a live target.");
    }

    @Test
    void getHitButNotSunkPositions7() throws ReflectiveOperationException {
        IPosition shot = position(3, 3);
        IMove liveHit = hitMove(shot);

        List<IPosition> hits = invokeGetHitButNotSunkPositions(game(liveHit), Set.of(shot));

        assertEquals(List.of(shot), hits, "Error: a valid hit on a floating ship should be returned as a target.");
    }

    @Test
    void getHitButNotSunkPositions8() throws ReflectiveOperationException {
        IPosition hit = position(3, 3);
        IPosition missingResultShot = position(3, 4);
        IMove partialResults = move(
                List.of(hit, missingResultShot),
                List.of(new IGame.ShotResult(true, false, ship(), false)));

        List<IPosition> hits = invokeGetHitButNotSunkPositions(game(partialResults), Set.of(hit, missingResultShot));

        assertEquals(List.of(hit), hits,
                "Error: a missing result should be ignored while the available valid hit is retained.");
    }

    @Test
    void getHitButNotSunkPositions9() throws ReflectiveOperationException {
        IPosition firstHit = position(2, 2);
        IPosition secondHit = position(6, 6);
        IMove firstMove = hitMove(firstHit);
        IMove secondMove = hitMove(secondHit);

        List<IPosition> hits = invokeGetHitButNotSunkPositions(
                game(firstMove, secondMove), Set.of(firstHit, secondHit));

        assertEquals(List.of(firstHit, secondHit), hits,
                "Error: live hits from multiple moves should all be returned in move order.");
    }

    @Test
    void buildHuntPool1() throws ReflectiveOperationException {
        List<IPosition> pool = invokeBuildHuntPool(Set.of(), List.of());

        assertEquals(50, pool.size(), "Error: an empty board should have 50 checkerboard hunt positions.");
        assertTrue(pool.stream().allMatch(position -> (position.getRow() + position.getColumn()) % 2 == 0),
                "Error: every hunt candidate should follow the checkerboard pattern.");
    }

    @Test
    void buildHuntPool2() throws ReflectiveOperationException {
        IPosition alreadyShot = position(0, 0);

        List<IPosition> pool = invokeBuildHuntPool(Set.of(alreadyShot), List.of());

        assertFalse(pool.contains(alreadyShot), "Error: an already-shot checkerboard cell must be excluded.");
    }

    @Test
    void buildHuntPool3() throws ReflectiveOperationException {
        IPosition alreadyChosen = position(0, 0);

        List<IPosition> pool = invokeBuildHuntPool(Set.of(), List.of(alreadyChosen));

        assertFalse(pool.contains(alreadyChosen), "Error: a position already chosen for this salvo must be excluded.");
    }

    @Test
    void buildHuntPool4() throws ReflectiveOperationException {
        IPosition nonCheckerboardPosition = position(0, 1);

        List<IPosition> pool = invokeBuildHuntPool(Set.of(), List.of());

        assertFalse(pool.contains(nonCheckerboardPosition),
                "Error: a non-checkerboard cell must not be included in the hunt pool.");
    }

    @Test
    void buildHuntPool5() throws ReflectiveOperationException {
        IPosition excludedPosition = position(0, 0);

        List<IPosition> pool = invokeBuildHuntPool(Set.of(excludedPosition), List.of(excludedPosition));

        assertFalse(pool.contains(excludedPosition),
                "Error: a position marked both shot and chosen must be excluded from the hunt pool.");
    }

    @Test
    void buildHuntPool6() throws ReflectiveOperationException {
        IPosition alreadyShot = position(0, 0);
        IPosition alreadyChosen = position(0, 2);

        List<IPosition> pool = invokeBuildHuntPool(Set.of(alreadyShot), List.of(alreadyChosen));

        assertFalse(pool.contains(alreadyShot) || pool.contains(alreadyChosen),
                "Error: both previously shot and previously chosen positions must be excluded.");
    }

    private static void assertHuntShots(List<IPosition> shots, List<IPosition> previousShots) {
        assertEquals(3, shots.size(),
                "Error: expected 3 shots but got " + shots.size() + ".");
        assertEquals(shots.size(), new HashSet<>(shots).size(),
                "Error: the strategy should not select a position more than once.");
        assertTrue(shots.stream().allMatch(shot -> (shot.getRow() + shot.getColumn()) % 2 == 0),
                "Error: hunt shots should follow the checkerboard pattern.");
        assertTrue(shots.stream().noneMatch(previousShots::contains),
                "Error: hunt shots should not repeat previous shots.");
    }

    private static void assertTargetShots(List<IPosition> shots, IMove previousMove, IPosition hit) {
        assertEquals(3, shots.size(), "Error: expected a complete salvo of three shots but got " + shots.size() + ".");
        assertTrue(shots.stream().anyMatch(hit.adjacentPositions()::contains),
                "Error: at least one available neighbour of the active hit should be selected.");
        assertTrue(shots.stream().noneMatch(previousMove.getShots()::contains),
                "Error: target shots should not repeat a previous shot.");
        assertEquals(shots.size(), new HashSet<>(shots).size(),
                "Error: target and hunt phases should not produce duplicate shots.");
    }

    private static IGame game(IMove... moves) {
        List<IMove> alienMoves = Arrays.asList(moves);
        return (IGame) Proxy.newProxyInstance(
                IGame.class.getClassLoader(),
                new Class<?>[]{IGame.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getAlienMoves")) {
                        return alienMoves;
                    }
                    throw new UnsupportedOperationException("Unexpected IGame method: " + method.getName());
                });
    }

    private static IMove move(List<IPosition> shots, List<IGame.ShotResult> results) {
        return (IMove) Proxy.newProxyInstance(
                IMove.class.getClassLoader(),
                new Class<?>[]{IMove.class},
                (proxy, method, args) -> {
                    if (method.getName().equals("getShots")) {
                        return shots;
                    }
                    if (method.getName().equals("getShotResults")) {
                        return results;
                    }
                    throw new UnsupportedOperationException("Unexpected IMove method: " + method.getName());
                });
    }

    @SuppressWarnings("unchecked")
    private List<IPosition> invokeGetHitButNotSunkPositions(IGame game, Set<IPosition> alreadyShot)
            throws ReflectiveOperationException {
        Method method = AiShotStrategy.class.getDeclaredMethod(
                "getHitButNotSunkPositions", IGame.class, Set.class);
        method.setAccessible(true);
        return (List<IPosition>) method.invoke(strategy, game, alreadyShot);
    }

    @SuppressWarnings("unchecked")
    private List<IPosition> invokeBuildHuntPool(Set<IPosition> alreadyShot, List<IPosition> alreadyChosen)
            throws ReflectiveOperationException {
        Method method = AiShotStrategy.class.getDeclaredMethod(
                "buildHuntPool", Set.class, List.class);
        method.setAccessible(true);
        return (List<IPosition>) method.invoke(strategy, alreadyShot, alreadyChosen);
    }

    private static IMove hitMove(IPosition hit) {
        return move(List.of(hit), List.of(new IGame.ShotResult(true, false, ship(), false)));
    }

    private static IMove hitMoveWithExtraShots(IPosition hit, List<IPosition> shots) {
        List<IPosition> allShots = new ArrayList<>();
        allShots.add(hit);
        allShots.addAll(shots);
        return move(allShots, List.of(new IGame.ShotResult(true, false, ship(), false)));
    }

    private static IShip ship() {
        return (IShip) Proxy.newProxyInstance(
                IShip.class.getClassLoader(),
                new Class<?>[]{IShip.class},
                (proxy, method, args) -> {
                    throw new UnsupportedOperationException("Unexpected IShip method: " + method.getName());
                });
    }

    private static IPosition position(int row, int column) {
        return new Position(row, column);
    }

    private static List<IPosition> checkerboardPositions() {
        List<IPosition> positions = new ArrayList<>();
        for (int row = 0; row < 10; row++) {
            for (int column = 0; column < 10; column++) {
                if ((row + column) % 2 == 0) {
                    positions.add(position(row, column));
                }
            }
        }
        return positions;
    }
}
