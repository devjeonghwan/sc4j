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
 * Tests the engine construction command: its payload reads back into an equal command, and it refuses another player's worker and a type
 * that is no building.
 */
final class ConstructBuildingCommandTypeTest {
    private static final int CONSTRUCT_ID = 2;

    private final @NotNull MotionProfile walker;
    private final @NotNull UnitType soldier;
    private final @NotNull UnitType barracks;
    private final @NotNull ConstructBuildingCommandType construct;

    ConstructBuildingCommandTypeTest() {
        this.walker    = new MotionProfile("walker");
        this.soldier   = new UnitType("soldier");
        this.barracks  = new UnitType("barracks");
        this.construct = new ConstructBuildingCommandType(CONSTRUCT_ID, 0);
    }

    @Test
    void read_should_return_the_command_that_write_encoded() throws IOException {
        Game game = this.createGame();
        Unit worker = this.createUnit(game, 0, this.soldier);
        ConstructBuildingCommand construction = this.construct.create(game, worker, this.barracks, -16, 640);
        CommandOutput output = new CommandOutput();

        this.construct.write(construction, output);

        ConstructBuildingCommand read = this.construct.read(new CommandInput(output.toByteArray()));

        Assertions.assertEquals(ConstructBuildingCommandType.PAYLOAD_LENGTH, output.toByteArray().length);
        Assertions.assertEquals(CONSTRUCT_ID, this.construct.getId());
        Assertions.assertEquals(construction.getWorkerHandle(), read.getWorkerHandle());
        Assertions.assertEquals(this.barracks.getIndex(), read.getBuildingTypeIndex());
        Assertions.assertEquals(-16, read.getSiteX());
        Assertions.assertEquals(640, read.getSiteY());
    }

    @Test
    void execute_should_refuse_another_players_worker_and_a_type_that_is_no_building() {
        Game game = this.createGame();
        Unit worker = this.createUnit(game, 1, this.soldier);

        this.construct.execute(game, game.getPlayer(0), this.construct.create(game, worker, this.barracks, 100, 100));
        this.construct.execute(game, game.getPlayer(1), this.construct.create(game, worker, this.soldier, 100, 100));

        Assertions.assertNull(((StandardProductionSystem) game.getProduction()).getConstructionType(worker));
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
