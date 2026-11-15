package org.fogofwarlabs.sc4j.engine.economy;

import org.fogofwarlabs.sc4j.engine.content.economy.ResourceCost;
import org.fogofwarlabs.sc4j.engine.content.economy.ResourceKind;
import org.fogofwarlabs.sc4j.engine.content.faction.Faction;
import org.fogofwarlabs.sc4j.engine.game.*;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the engine default economy: per-player amounts, costs paid all or nothing, refunds and bounds.
 */
final class StandardEconomySystemTest {
    private static final @NotNull ResourceKind ORE = new ResourceKind("ore");
    private static final @NotNull ResourceKind WOOD = new ResourceKind("wood");

    @Test
    void spend_should_take_every_amount_or_nothing() {
        Game game = createGame();
        EconomySystem economy = game.getEconomy();
        Player first = game.getPlayer(0);
        Player second = game.getPlayer(1);
        ResourceCost cost = new ResourceCost.Builder().add(ORE, 50).add(WOOD, 20).build();

        economy.setAmount(first, ORE, 60);
        economy.setAmount(first, WOOD, 10);

        Assertions.assertFalse(economy.canAfford(first, cost));
        Assertions.assertFalse(economy.spend(first, cost));
        Assertions.assertEquals(60, economy.getAmount(first, ORE));

        economy.setAmount(first, WOOD, 25);

        Assertions.assertTrue(economy.spend(first, cost));
        Assertions.assertEquals(10, economy.getAmount(first, ORE));
        Assertions.assertEquals(5, economy.getAmount(first, WOOD));
        Assertions.assertEquals(0, economy.getAmount(second, ORE));

        economy.refund(first, cost);

        Assertions.assertEquals(60, economy.getAmount(first, ORE));
        Assertions.assertEquals(25, economy.getAmount(first, WOOD));
    }

    @Test
    void amounts_change_the_state_hash() {
        Game game = createGame();
        long before = game.computeStateHash();

        game.getEconomy().setAmount(game.getPlayer(1), WOOD, 1);

        Assertions.assertNotEquals(before, game.computeStateHash());
    }

    @Test
    void get_amount_should_refuse_a_player_or_kind_the_game_does_not_have() {
        Game game = createGame();
        EconomySystem economy = game.getEconomy();
        Player first = game.getPlayer(0);

        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> economy.getAmount(new Player(2), ORE));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> economy.getAmount(first, new ResourceKind("stone")));
        Assertions.assertThrows(IndexOutOfBoundsException.class, () -> economy.getSupplyUsed(first, new Faction("none")));
    }

    private static @NotNull Game createGame() {
        GameDefinition.Builder builder = new GameDefinition.Builder().useSystem(EconomySystem.KEY, context -> new StandardEconomySystem());

        builder.getContent().getResourceKinds().register(ORE);
        builder.getContent().getResourceKinds().register(WOOD);

        GameSetup setup = new GameSetup.Builder(new MapReference("empty", new byte[0]))
                .addPlayer(new PlayerSlot(0, ControllerKind.HUMAN, 0, 0, null))
                .addPlayer(new PlayerSlot(1, ControllerKind.COMPUTER, 1, 1, null))
                .build();

        return Game.create(builder.build(), setup);
    }
}
