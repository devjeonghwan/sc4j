package org.fogofwarlabs.sc4j.engine.production;

import org.fogofwarlabs.sc4j.engine.command.CommandInput;
import org.fogofwarlabs.sc4j.engine.command.CommandOutput;
import org.fogofwarlabs.sc4j.engine.content.definition.MotionProfileDefinition;
import org.fogofwarlabs.sc4j.engine.content.definition.UnitDefinition;
import org.fogofwarlabs.sc4j.engine.content.definition.UnitTrait;
import org.fogofwarlabs.sc4j.engine.content.economy.ResourceCost;
import org.fogofwarlabs.sc4j.engine.content.economy.ResourceKind;
import org.fogofwarlabs.sc4j.engine.content.type.MotionProfile;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.economy.EconomySystem;
import org.fogofwarlabs.sc4j.engine.economy.StandardEconomySystem;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.*;
import org.fogofwarlabs.sc4j.engine.movement.MovementSystem;
import org.fogofwarlabs.sc4j.engine.movement.StandardMovementSystem;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * Tests the engine cancel command: its payload reads back into an equal command, it cancels a queue slot of the issuing player's producer only
 * with the mod's refund, and it cancels the construction of a building under construction with the mod's construction refund.
 */
final class CancelProductionCommandTypeTest {
    private static final int CANCEL_ID = 3;
    private static final int SOLDIER_COST = 50;
    private static final int TOWER_COST = 80;
    private static final int TOWER_TIME = 30;
    private static final int GOLD = 1000;
    private static final int SITE_X = 200;
    private static final int SITE_Y = 200;
    private static final int UNIT_LIMIT = 8;
    private static final int FULL_REFUND_PERCENT = 100;

    private final @NotNull ResourceKind gold;
    private final @NotNull MotionProfile walker;
    private final @NotNull UnitType soldier;
    private final @NotNull UnitType knight;
    private final @NotNull UnitType barracks;
    private final @NotNull UnitType tower;
    private final @NotNull CancelProductionCommandType cancel;

    CancelProductionCommandTypeTest() {
        this.gold     = new ResourceKind("gold");
        this.walker   = new MotionProfile("walker");
        this.soldier  = new UnitType("soldier");
        this.knight   = new UnitType("knight");
        this.barracks = new UnitType("barracks");
        this.tower    = new UnitType("tower");
        this.cancel   = new CancelProductionCommandType(CANCEL_ID, 0);
    }

    @Test
    void read_should_return_the_command_that_write_encoded() throws IOException {
        Game game = this.createGame();
        Unit producer = this.createUnit(game, 0, this.barracks);
        CancelProductionCommand command = this.cancel.create(game, producer, CancelProductionCommandType.MAXIMUM_SLOT);
        CommandOutput output = new CommandOutput();

        this.cancel.write(command, output);

        CancelProductionCommand read = this.cancel.read(new CommandInput(output.toByteArray()));

        Assertions.assertEquals(CancelProductionCommandType.PAYLOAD_LENGTH, output.toByteArray().length);
        Assertions.assertEquals(CANCEL_ID, this.cancel.getId());
        Assertions.assertEquals(command.getProducerHandle(), read.getProducerHandle());
        Assertions.assertEquals(CancelProductionCommandType.MAXIMUM_SLOT, read.getSlot());
        Assertions.assertThrows(IllegalArgumentException.class,
                                () -> this.cancel.create(game, producer, CancelProductionCommandType.MAXIMUM_SLOT + 1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> this.cancel.create(game, producer, -1));
    }

    @Test
    void execute_should_cancel_a_slot_of_the_issuing_players_producer_only() {
        Game game = this.createGame();
        Player owner = game.getPlayer(0);
        ProductionSystem production = game.getProduction();
        Unit producer = this.createUnit(game, 0, this.barracks);

        production.enqueueUnit(producer, this.soldier);
        production.enqueueUnit(producer, this.knight);
        production.enqueueUnit(producer, this.soldier);

        this.cancel.execute(game, game.getPlayer(1), this.cancel.create(game, producer, 1));

        Assertions.assertEquals(3, production.getQueueLength(producer));

        this.cancel.execute(game, owner, this.cancel.create(game, producer, 1));

        Assertions.assertEquals(2, production.getQueueLength(producer));
        Assertions.assertSame(this.soldier, production.getQueuedType(producer, 1));
        Assertions.assertEquals(GOLD - 2 * SOLDIER_COST, game.getEconomy().getAmount(owner, this.gold));

        this.cancel.execute(game, owner, this.cancel.create(game, producer, 2));

        Assertions.assertEquals(2, production.getQueueLength(producer), "an empty slot cancels nothing");
    }

    @Test
    void execute_should_cancel_the_construction_of_a_building_with_the_construction_refund() {
        Game game = this.createGame();
        Player owner = game.getPlayer(0);
        StandardProductionSystem production = (StandardProductionSystem) game.getProduction();
        Unit worker = this.createUnit(game, 0, this.soldier);
        Unit building = production.placeBuilding(worker, this.tower, SITE_X, SITE_Y, 0);

        Assertions.assertNotNull(building);
        Assertions.assertTrue(production.isUnderConstruction(building));
        Assertions.assertEquals(GOLD - TOWER_COST, game.getEconomy().getAmount(owner, this.gold));

        this.cancel.execute(game, game.getPlayer(1), this.cancel.create(game, building, 0));

        Assertions.assertTrue(game.getUnits().isAllocated(building.getIndex()));

        this.cancel.execute(game, owner, this.cancel.create(game, building, 0));

        Assertions.assertFalse(game.getUnits().isAllocated(building.getIndex()));
        Assertions.assertEquals(GOLD, game.getEconomy().getAmount(owner, this.gold), "the full refund of the mod");
        Assertions.assertEquals(0, game.getContent().getProgress().getUnitCount(owner, this.tower));
    }

    private @NotNull Unit createUnit(@NotNull Game game, int player, @NotNull UnitType type) {
        Unit unit = ((StandardProductionSystem) game.getProduction()).createUnit(game.getPlayer(player), type, 20, 20, 0);

        Assertions.assertNotNull(unit);

        return unit;
    }

    private @NotNull Game createGame() {
        GameDefinition.Builder builder = new GameDefinition.Builder()
                .setLimits(new EngineLimits.Builder().setUnits(UNIT_LIMIT).setOrders(UNIT_LIMIT).build())
                .useSystem(MovementSystem.KEY, context -> new StandardMovementSystem(SkirmishScenario.MOTION_MODEL))
                .useSystem(EconomySystem.KEY, context -> new StandardEconomySystem())
                .useSystem(ProductionSystem.KEY, context -> new StandardProductionSystem.Builder(SkirmishScenario.QUEUE_CAPACITY)
                        .setConstructionRefundPercent(FULL_REFUND_PERCENT)
                        .build());

        StandardProductionSystem.addColumns(builder, SkirmishScenario.QUEUE_CAPACITY);
        builder.getContent().getResourceKinds().register(this.gold);
        builder.getContent().getMotionProfiles().register(this.walker, new MotionProfileDefinition.Builder().setTopSpeed(512).build());
        builder.getContent().getUnitTypes().register(this.soldier, this.createSoldier().build());
        builder.getContent().getUnitTypes().register(this.knight, this.createSoldier().build());
        builder.getContent().getUnitTypes().register(this.barracks, new UnitDefinition.Builder().setMotionProfile(this.walker)
                                                                                                .setHitPoints(10)
                                                                                                .addTrait(UnitTrait.BUILDING)
                                                                                                .build());
        builder.getContent().getUnitTypes().register(this.tower, new UnitDefinition.Builder().setMotionProfile(this.walker)
                                                                                             .setHitPoints(10)
                                                                                             .setCost(ResourceCost.of(this.gold, TOWER_COST))
                                                                                             .setBuildTime(TOWER_TIME)
                                                                                             .addTrait(UnitTrait.BUILDING)
                                                                                             .build());

        GameSetup setup = new GameSetup.Builder(new MapReference("empty", new byte[0])).addPlayer(new PlayerSlot(0, ControllerKind.HUMAN, 0, 0, null))
                                                                                       .addPlayer(new PlayerSlot(1, ControllerKind.HUMAN, 1, 1, null))
                                                                                       .build();
        Game game = Game.create(builder.build(), setup);

        game.getEconomy().setAmount(game.getPlayer(0), this.gold, GOLD);

        return game;
    }

    private @NotNull UnitDefinition.Builder createSoldier() {
        return new UnitDefinition.Builder().setMotionProfile(this.walker).setHitPoints(10).setCost(ResourceCost.of(this.gold, SOLDIER_COST));
    }
}
