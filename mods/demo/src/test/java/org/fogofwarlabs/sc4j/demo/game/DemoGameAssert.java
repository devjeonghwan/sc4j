package org.fogofwarlabs.sc4j.demo.game;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;

/**
 * Compares played games of the demo.
 */
public final class DemoGameAssert {
    /**
     * Asserts that two runs of a game are the same game: the same game loops, the same state hash after every loop and the same outcomes.
     *
     * @param expected the first run
     * @param actual   the second run
     */
    public static void assertSameGame(DemoGameScenario.@NotNull Outcome expected, DemoGameScenario.@NotNull Outcome actual) {
        Assertions.assertEquals(expected.getLoops(), actual.getLoops());
        Assertions.assertEquals(expected.getFrameHashes().size(), actual.getFrameHashes().size());

        for (int loop = 0; loop < expected.getFrameHashes().size(); loop++) {
            Assertions.assertEquals(expected.getFrameHashes().get(loop), actual.getFrameHashes().get(loop), "state hash after game loop " + loop);
        }

        Assertions.assertEquals(expected.getOutcomes(), actual.getOutcomes());
    }

    private DemoGameAssert() {
    }
}
