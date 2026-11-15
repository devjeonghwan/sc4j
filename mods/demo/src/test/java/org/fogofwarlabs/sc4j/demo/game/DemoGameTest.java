package org.fogofwarlabs.sc4j.demo.game;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

/**
 * Tests full games of the demo without the runtime: two engine default computer players of Dawnhold and Cindermaw on Ashen Ford gather gold,
 * build, train and fight until the annihilation victory decides the game, and the same seed gives the same state hash after every game loop,
 * also with a person's training and cancels at a town hall.
 */
final class DemoGameTest {
    private static final long SEED = 0x5C4A_2026_1107L;
    private static final int TRAIN_LOOP = 300;
    private static final int CANCEL_LOOP = 310;

    @Test
    void two_computer_players_build_train_and_fight_until_one_is_annihilated() throws IOException {
        try (DemoGameScenario scenario = new DemoGameScenario()) {
            DemoGameScenario.Outcome outcome = scenario.play(scenario.createSetup(SEED), List.of(), null);

            System.out.println("demo game loops " + outcome.getLoops() + ", " + outcome.getOutcomes() + ", built " + outcome.getBuiltTypes());

            Assertions.assertTrue(outcome.getLoops() < DemoGameScenario.LOOP_LIMIT, outcome.getOutcomes());
            Assertions.assertTrue(outcome.getOutcomes().equals("0=VICTORY 1=DEFEAT 2=PLAYING") ||
                                  outcome.getOutcomes().equals("0=DEFEAT 1=VICTORY 2=PLAYING"), outcome.getOutcomes());
            Assertions.assertTrue(outcome.getBuiltTypes().containsAll(List.of("muster-yard", "brawl-pit")), outcome.getBuiltTypes().toString());
            Assertions.assertTrue(outcome.getBuiltTypes().contains("pikeward") || outcome.getBuiltTypes().contains("longeye"),
                                  outcome.getBuiltTypes().toString());
            Assertions.assertTrue(outcome.getBuiltTypes().contains("cleaver") || outcome.getBuiltTypes().contains("stonehurler"),
                                  outcome.getBuiltTypes().toString());
        }
    }

    @Test
    void the_same_seed_gives_the_same_state_hash_after_every_game_loop() throws IOException {
        try (DemoGameScenario first = new DemoGameScenario(); DemoGameScenario second = new DemoGameScenario()) {
            DemoGameScenario.Outcome firstOutcome = first.play(first.createSetup(SEED), List.of(), null);
            DemoGameScenario.Outcome secondOutcome = second.play(second.createSetup(SEED), List.of(), null);

            DemoGameAssert.assertSameGame(firstOutcome, secondOutcome);
        }
    }

    @Test
    void a_game_with_queued_and_cancelled_entries_gives_the_same_state_hash_after_every_game_loop() throws IOException {
        try (DemoGameScenario first = new DemoGameScenario(); DemoGameScenario second = new DemoGameScenario()) {
            DemoGameScenario.QueueCommands firstCommands = new DemoGameScenario.QueueCommands(first.getContent(), TRAIN_LOOP, CANCEL_LOOP);
            DemoGameScenario.QueueCommands secondCommands = new DemoGameScenario.QueueCommands(second.getContent(), TRAIN_LOOP, CANCEL_LOOP);
            DemoGameScenario.Outcome firstOutcome = first.play(first.createSetup(SEED), List.of(firstCommands), null);
            DemoGameScenario.Outcome secondOutcome = second.play(second.createSetup(SEED), List.of(secondCommands), null);
            DemoGameScenario.Outcome withoutCommands = first.play(first.createSetup(SEED), List.of(), null);

            Assertions.assertTrue(firstCommands.getCancelCount() > 0, "the queue held entries to cancel");
            Assertions.assertEquals(firstCommands.getCancelCount(), secondCommands.getCancelCount());
            Assertions.assertNotEquals(withoutCommands.getFrameHashes(), firstOutcome.getFrameHashes(), "the commands change the game");
            DemoGameAssert.assertSameGame(firstOutcome, secondOutcome);
        }
    }
}
