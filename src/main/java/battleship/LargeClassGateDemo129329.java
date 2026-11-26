package battleship;

public final class LargeClassGateDemo129329 {
    private LargeClassGateDemo129329() {
    }

    public static int countReachedMilestones(int score) {
        return Math.max(0, Math.min(score, 60));
    }
}
