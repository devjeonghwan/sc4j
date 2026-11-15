package org.fogofwarlabs.sc4j.demo.order;

import org.fogofwarlabs.sc4j.engine.combat.*;
import org.fogofwarlabs.sc4j.engine.command.CommandRegistry;
import org.fogofwarlabs.sc4j.engine.command.CommandType;
import org.fogofwarlabs.sc4j.engine.content.definition.OrderDefinition;
import org.fogofwarlabs.sc4j.engine.content.registry.ContentRegistry;
import org.fogofwarlabs.sc4j.engine.content.type.OrderType;
import org.fogofwarlabs.sc4j.engine.economy.GatherBehavior;
import org.fogofwarlabs.sc4j.engine.economy.HarvestSystem;
import org.fogofwarlabs.sc4j.engine.economy.ReturnResourcesBehavior;
import org.fogofwarlabs.sc4j.engine.economy.StandardHarvestSystem;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.order.*;
import org.fogofwarlabs.sc4j.engine.production.*;
import org.fogofwarlabs.sc4j.engine.spatial.TableUnitBounds;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Holds the order types of the demo, registers them in its content and their engine behaviors in each game's order system, and owns the
 * demo's command stream: the engine order, training, construction, cancel and rally commands, the commands a player sends.
 * <p>
 * The orders: idle, guard (the idle order of fighting units, attacks what comes near), move, stop, attack, attack-move, patrol, hold
 * position, gather, return resources and build. Each order type's debug name is the id the pack content names it by.
 */
public final class DemoOrders {
    /**
     * The id of the order command in the demo's command stream.
     */
    public static final int ORDER_COMMAND_ID = 0;
    /**
     * The id of the training command in the demo's command stream.
     */
    public static final int TRAIN_COMMAND_ID = 1;
    /**
     * The id of the construction command in the demo's command stream.
     */
    public static final int CONSTRUCT_COMMAND_ID = 2;
    /**
     * The id of the cancel command in the demo's command stream: a queue entry or a construction.
     */
    public static final int CANCEL_COMMAND_ID = 3;
    /**
     * The id of the rally command in the demo's command stream: a producer's rally point.
     */
    public static final int RALLY_COMMAND_ID = 4;
    /**
     * The instruction limit of each animation program an issued order starts.
     */
    public static final int INSTRUCTION_LIMIT = 100000;

    /**
     * The distance in pixels at which a move ends.
     */
    private static final int ARRIVAL_DISTANCE = 24;
    /**
     * The distance in pixels from the target point within which a unit that touches its standing group arrives.
     */
    private static final int GROUP_ARRIVAL_DISTANCE = 96;
    /**
     * The range in pixels a worker searches the next resource container in.
     */
    private static final int RESOURCE_SEARCH_RANGE = 640;
    /**
     * The gap in pixels within which a worker builds.
     */
    private static final int BUILD_REACH = 6;

    private final @NotNull OrderType idle;
    private final @NotNull OrderType guard;
    private final @NotNull OrderType move;
    private final @NotNull OrderType stop;
    private final @NotNull OrderType attack;
    private final @NotNull OrderType attackMove;
    private final @NotNull OrderType patrol;
    private final @NotNull OrderType holdPosition;
    private final @NotNull OrderType gather;
    private final @NotNull OrderType returnResources;
    private final @NotNull OrderType build;

    private final @NotNull IssueOrderCommandType orderCommand;
    private final @NotNull TrainUnitCommandType trainCommand;
    private final @NotNull ConstructBuildingCommandType constructCommand;
    private final @NotNull CancelProductionCommandType cancelCommand;
    private final @NotNull SetRallyPointCommandType rallyCommand;
    private final @NotNull CommandRegistry<CommandType<?>> commands;

    /**
     * Creates a {@link DemoOrders} instance with new order types and the frozen command registry.
     */
    public DemoOrders() {
        this.idle            = new OrderType("idle");
        this.guard           = new OrderType("guard");
        this.move            = new OrderType("move");
        this.stop            = new OrderType("stop");
        this.attack          = new OrderType("attack");
        this.attackMove      = new OrderType("attack-move");
        this.patrol          = new OrderType("patrol");
        this.holdPosition    = new OrderType("hold-position");
        this.gather          = new OrderType("gather");
        this.returnResources = new OrderType("return-resources");
        this.build           = new OrderType("build");

        this.orderCommand     = new IssueOrderCommandType(ORDER_COMMAND_ID, INSTRUCTION_LIMIT);
        this.trainCommand     = new TrainUnitCommandType(TRAIN_COMMAND_ID);
        this.constructCommand = new ConstructBuildingCommandType(CONSTRUCT_COMMAND_ID, INSTRUCTION_LIMIT);
        this.cancelCommand    = new CancelProductionCommandType(CANCEL_COMMAND_ID, INSTRUCTION_LIMIT);
        this.rallyCommand     = new SetRallyPointCommandType(RALLY_COMMAND_ID);
        this.commands         = new CommandRegistry<>(RALLY_COMMAND_ID + 1);

        this.commands.register(this.orderCommand);
        this.commands.register(this.trainCommand);
        this.commands.register(this.constructCommand);
        this.commands.register(this.cancelCommand);
        this.commands.register(this.rallyCommand);
        this.commands.freeze();
    }

    /**
     * @return the order types in registration order
     */
    public @NotNull List<@NotNull OrderType> getOrderTypes() {
        return List.of(this.idle, this.guard, this.move, this.stop, this.attack, this.attackMove, this.patrol, this.holdPosition, this.gather,
                       this.returnResources, this.build);
    }

    /**
     * @return the move order type, also the order of rally points
     */
    public @NotNull OrderType getMove() {
        return this.move;
    }

    /**
     * @return the stop order type
     */
    public @NotNull OrderType getStop() {
        return this.stop;
    }

    /**
     * @return the attack order type, the order of a unit that attacks one target
     */
    public @NotNull OrderType getAttack() {
        return this.attack;
    }

    /**
     * @return the patrol order type
     */
    public @NotNull OrderType getPatrol() {
        return this.patrol;
    }

    /**
     * @return the hold position order type
     */
    public @NotNull OrderType getHoldPosition() {
        return this.holdPosition;
    }

    /**
     * @return the attack-move order type, the order of the computer players' attack waves
     */
    public @NotNull OrderType getAttackMove() {
        return this.attackMove;
    }

    /**
     * @return the gather order type
     */
    public @NotNull OrderType getGather() {
        return this.gather;
    }

    /**
     * @return the return resources order type, the order of a worker that carries its load to a drop-off
     */
    public @NotNull OrderType getReturnResources() {
        return this.returnResources;
    }

    /**
     * @return the build order type
     */
    public @NotNull OrderType getBuild() {
        return this.build;
    }

    /**
     * @return the order command type of the demo's command stream
     */
    public @NotNull IssueOrderCommandType getOrderCommand() {
        return this.orderCommand;
    }

    /**
     * @return the training command type of the demo's command stream
     */
    public @NotNull TrainUnitCommandType getTrainCommand() {
        return this.trainCommand;
    }

    /**
     * @return the construction command type of the demo's command stream
     */
    public @NotNull ConstructBuildingCommandType getConstructCommand() {
        return this.constructCommand;
    }

    /**
     * @return the rally command type of the demo's command stream
     */
    public @NotNull SetRallyPointCommandType getRallyCommand() {
        return this.rallyCommand;
    }

    /**
     * @return the cancel command type of the demo's command stream
     */
    public @NotNull CancelProductionCommandType getCancelCommand() {
        return this.cancelCommand;
    }

    /**
     * @return the frozen command types of the demo's command stream by id
     */
    public @NotNull CommandRegistry<CommandType<?>> getCommands() {
        return this.commands;
    }


    /**
     * Registers the order types in the content of the demo's games, in registration order.
     *
     * @param registry the content registry
     */
    public void register(@NotNull ContentRegistry registry) {
        for (OrderType order : this.getOrderTypes()) {
            registry.getOrderTypes().register(order, new OrderDefinition.Builder().build());
        }
    }

    /**
     * Registers the engine behavior of every order type in the order system of one game; the idle order has none. The move and attack-move
     * orders end for a unit that arrives with its group ({@link GroupArrival}). The game needs the engine default combat, harvest and production
     * systems and the table unit bounds.
     *
     * @param game      the game
     * @param behaviors the behavior registry of the game's order system
     */
    public void registerBehaviors(@NotNull Game game, @NotNull OrderRegistry<OrderBehavior<OrderContext, Unit>> behaviors) {
        StandardCombatSystem combat = (StandardCombatSystem) game.getCombat();
        StandardHarvestSystem harvest = (StandardHarvestSystem) game.getModSystem(HarvestSystem.KEY);
        GroupArrival groupArrival = new GroupArrival(game.getModSystem(TableUnitBounds.KEY), GROUP_ARRIVAL_DISTANCE);

        behaviors.register(this.guard, new GuardBehavior(combat, this.attack));
        behaviors.register(this.move, new MoveBehavior(ARRIVAL_DISTANCE, groupArrival));
        behaviors.register(this.stop, new StopBehavior());
        behaviors.register(this.attack, new AttackBehavior(combat));
        behaviors.register(this.attackMove, new AttackMoveBehavior(combat, ARRIVAL_DISTANCE, groupArrival));
        behaviors.register(this.patrol, new PatrolBehavior(game, combat, ARRIVAL_DISTANCE));
        behaviors.register(this.holdPosition, new HoldPositionBehavior(combat));
        behaviors.register(this.gather, new GatherBehavior(harvest, this.returnResources, RESOURCE_SEARCH_RANGE));
        behaviors.register(this.returnResources, new ReturnResourcesBehavior(harvest, this.gather));
        behaviors.register(this.build, new BuildBehavior((StandardProductionSystem) game.getProduction(), BUILD_REACH, true));
    }
}
