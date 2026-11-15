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
import org.fogofwarlabs.sc4j.engine.math.MutablePoint;
import org.fogofwarlabs.sc4j.engine.movement.MovementSystem;
import org.fogofwarlabs.sc4j.engine.movement.StandardMovementSystem;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;

/**
 * Tests the engine rally point command: its payload reads back into an equal command, and it sets the rally point of the issuing player's
 * producer only.
 */
final class SetRallyPointCommandTypeTest {
    private static final int RALLY_ID = 4;
    private static final int RALLY_X = 100;
    private static final int RALLY_Y = 220;
    private static final int UNIT_LIMIT = 4;

    private final @NotNull MotionProfile walker;
    private final @NotNull UnitType barracks;
    private final @NotNull SetRallyPointCommandType rally;

    SetRallyPointCommandTypeTest() {
        this.walker   = new MotionProfile("walker");
        this.barracks = new UnitType("barracks");
        this.rally    = new SetRallyPointCommandType(RALLY_ID);
    }

    @Test
    void read_should_return_the_command_that_write_encoded() throws IOException {
        Game game = this.createGame();
        Unit producer = this.createProducer(game);
        SetRallyPointCommand command = this.rally.create(game, producer, RALLY_X, RALLY_Y);
        CommandOutput output = new CommandOutput();

        this.rally.write(command, output);

        SetRallyPointCommand read = this.rally.read(new CommandInput(output.toByteArray()));

        Assertions.assertEquals(SetRallyPointCommandType.PAYLOAD_LENGTH, output.toByteArray().length);
        Assertions.assertEquals(RALLY_ID, this.rally.getId());
        Assertions.assertEquals(command.getProducerHandle(), read.getProducerHandle());
        Assertions.assertEquals(RALLY_X, read.getX());
        Assertions.assertEquals(RALLY_Y, read.getY());
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SetRallyPointCommandType(-1));
    }

    @Test
    void execute_should_set_the_rally_point_of_the_issuing_players_producer_only() {
        Game game = this.createGame();
        StandardProductionSystem production = (StandardProductionSystem) game.getProduction();
        Unit producer = this.createProducer(game);
        MutablePoint point = new MutablePoint();

        this.rally.execute(game, game.getPlayer(1), this.rally.create(game, producer, RALLY_X, RALLY_Y));

        Assertions.assertFalse(production.getRallyPoint(producer, point));

        this.rally.execute(game, game.getPlayer(0), this.rally.create(game, producer, RALLY_X, RALLY_Y));

        Assertions.assertTrue(production.getRallyPoint(producer, point));
        Assertions.assertEquals(RALLY_X, point.getX());
        Assertions.assertEquals(RALLY_Y, point.getY());
    }

    private @NotNull Unit createProducer(@NotNull Game game) {
        Unit unit = ((StandardProductionSystem) game.getProduction()).createUnit(game.getPlayer(0), this.barracks, 20, 20, 0);

        Assertions.assertNotNull(unit);

        return unit;
    }

    private @NotNull Game createGame() {
        GameDefinition.Builder builder = new GameDefinition.Builder()
                .setLimits(new EngineLimits.Builder().setUnits(UNIT_LIMIT).setOrders(UNIT_LIMIT).build())
                .useSystem(MovementSystem.KEY, context -> new StandardMovementSystem(SkirmishScenario.MOTION_MODEL))
                .useSystem(ProductionSystem.KEY, context -> new StandardProductionSystem.Builder(SkirmishScenario.QUEUE_CAPACITY).build());

        StandardProductionSystem.addColumns(builder, SkirmishScenario.QUEUE_CAPACITY);
        builder.getContent().getMotionProfiles().register(this.walker, new MotionProfileDefinition.Builder().setTopSpeed(512).build());
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
