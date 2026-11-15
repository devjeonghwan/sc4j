package org.fogofwarlabs.sc4j.engine.production;

import org.fogofwarlabs.sc4j.engine.command.CommandInput;
import org.fogofwarlabs.sc4j.engine.command.CommandOutput;
import org.fogofwarlabs.sc4j.engine.content.definition.MotionProfileDefinition;
import org.fogofwarlabs.sc4j.engine.content.definition.UnitDefinition;
import org.fogofwarlabs.sc4j.engine.content.definition.UnitTrait;
import org.fogofwarlabs.sc4j.engine.content.type.MotionProfile;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.*;
import org.fogofwarlabs.sc4j.engine.movement.MovementSystem;
import org.fogofwarlabs.sc4j.engine.movement.StandardMovementSystem;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * Tests the engine training command: its payload reads back into an equal command, and it queues the type at the issuing player's producer
 * only and ignores an unknown unit type.
 */
final class TrainUnitCommandTypeTest {
    private static final int TRAIN_ID = 1;

    private final @NotNull MotionProfile walker;
    private final @NotNull UnitType soldier;
    private final @NotNull UnitType barracks;
    private final @NotNull TrainUnitCommandType train;

    TrainUnitCommandTypeTest() {
        this.walker   = new MotionProfile("walker");
        this.soldier  = new UnitType("soldier");
        this.barracks = new UnitType("barracks");
        this.train    = new TrainUnitCommandType(TRAIN_ID);
    }

    @Test
    void read_should_return_the_command_that_write_encoded() throws IOException {
        Game game = this.createGame();
        Unit producer = this.createUnit(game, 0, this.barracks);
        TrainUnitCommand training = this.train.create(game, producer, this.soldier);
        CommandOutput output = new CommandOutput();

        this.train.write(training, output);

        TrainUnitCommand read = this.train.read(new CommandInput(output.toByteArray()));

        Assertions.assertEquals(TrainUnitCommandType.PAYLOAD_LENGTH, output.toByteArray().length);
        Assertions.assertEquals(TRAIN_ID, this.train.getId());
        Assertions.assertEquals(training.getProducerHandle(), read.getProducerHandle());
        Assertions.assertEquals(this.soldier.getIndex(), read.getUnitTypeIndex());
    }

    @Test
    void execute_should_queue_only_at_producers_of_the_issuing_player() {
        Game game = this.createGame();
        Unit producer = this.createUnit(game, 0, this.barracks);

        this.train.execute(game, game.getPlayer(1), this.train.create(game, producer, this.soldier));

        Assertions.assertEquals(0, game.getProduction().getQueueLength(producer));

        this.train.execute(game, game.getPlayer(0), this.train.create(game, producer, this.soldier));

        Assertions.assertEquals(1, game.getProduction().getQueueLength(producer));
        Assertions.assertSame(this.soldier, game.getProduction().getQueuedType(producer, 0));

        this.train.execute(game, game.getPlayer(0), new TrainUnitCommand(game.getUnits().createHandle(producer), 99));

        Assertions.assertEquals(1, game.getProduction().getQueueLength(producer));
    }

    private @NotNull Unit createUnit(@NotNull Game game, int player, @NotNull UnitType type) {
        Unit unit = ((StandardProductionSystem) game.getProduction()).createUnit(game.getPlayer(player), type, 20, 20, 0);

        Assertions.assertNotNull(unit);

        return unit;
    }

    private @NotNull Game createGame() {
        GameDefinition.Builder builder = new GameDefinition.Builder()
                .setLimits(new EngineLimits.Builder().setUnits(4).setOrders(4).build())
                .useSystem(MovementSystem.KEY, context -> new StandardMovementSystem(SkirmishScenario.MOTION_MODEL))
                .useSystem(ProductionSystem.KEY, context -> new StandardProductionSystem.Builder(SkirmishScenario.QUEUE_CAPACITY).build());

        StandardProductionSystem.addColumns(builder, SkirmishScenario.QUEUE_CAPACITY);
        builder.getContent().getMotionProfiles().register(this.walker, new MotionProfileDefinition.Builder().setTopSpeed(512).build());
        builder.getContent().getUnitTypes().register(this.soldier, new UnitDefinition.Builder().setMotionProfile(this.walker)
                                                                                               .setHitPoints(10)
                                                                                               .build());
        builder.getContent().getUnitTypes().register(this.barracks, new UnitDefinition.Builder().setMotionProfile(this.walker)
                                                                                                .setHitPoints(10)
                                                                                                .addTrait(UnitTrait.BUILDING)
                                                                                                .build());

        GameSetup setup = new GameSetup.Builder(new MapReference("empty", new byte[0])).addPlayer(new PlayerSlot(0, ControllerKind.HUMAN, 0, 0, null))
                                                                                       .addPlayer(new PlayerSlot(1, ControllerKind.HUMAN, 1, 1, null))
                                                                                       .build();

        return Game.create(builder.build(), setup);
    }
}
