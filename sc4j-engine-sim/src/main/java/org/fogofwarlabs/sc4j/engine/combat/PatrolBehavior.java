package org.fogofwarlabs.sc4j.engine.combat;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.entity.EntityKind;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.entity.column.ColumnKey;
import org.fogofwarlabs.sc4j.engine.entity.column.ColumnReset;
import org.fogofwarlabs.sc4j.engine.entity.column.IntColumn;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.game.GameDefinition;
import org.fogofwarlabs.sc4j.engine.order.OrderBehavior;
import org.fogofwarlabs.sc4j.engine.order.OrderContext;
import org.jetbrains.annotations.NotNull;

/**
 * Moves a unit back and forth between where its patrol started and the target point of the current order, fighting the enemies it meets like
 * an attack-move ({@link AttackMoveBehavior}): the engine default behavior of a patrol order. A mod registers it for its patrol order type and
 * adds its unit columns to the game definition ({@link #addColumns}).
 * {@snippet :
 * PatrolBehavior.addColumns(builder);
 * behaviors.register(patrol, new PatrolBehavior(game, (StandardCombatSystem) game.getCombat(), 16));
 *}
 * <p>
 * When the order starts, the unit's position is kept as the other end of the patrol in two unit columns ({@link #PATROL_ORIGIN_X},
 * {@link #PATROL_ORIGIN_Y}). Each update fights as an attack-move does; when the unit arrives at the target point by the attack-move rules, the
 * two ends swap: the kept end becomes the order's target point and the destination, the reached point the kept end. The order does not end by
 * itself.
 * <p>
 * Determinism contract: the behavior draws no random value and allocates nothing. Its state is in its unit columns, which the unit pool mixes
 * into the state hash.
 */
public final class PatrolBehavior implements OrderBehavior<OrderContext, Unit> {
    /**
     * The unit column holding the x coordinate of the patrol end a unit started from.
     */
    public static final @NotNull ColumnKey<IntColumn> PATROL_ORIGIN_X = ColumnKey.ofInt("combat.patrol-origin-x", 0, ColumnReset.ON_ALLOCATE);
    /**
     * The unit column holding the y coordinate of the patrol end a unit started from.
     */
    public static final @NotNull ColumnKey<IntColumn> PATROL_ORIGIN_Y = ColumnKey.ofInt("combat.patrol-origin-y", 0, ColumnReset.ON_ALLOCATE);

    /**
     * Adds the unit columns of this behavior to a game definition; a game that registers this behavior needs them.
     *
     * @param builder the game definition builder
     * @throws IllegalArgumentException if a column is already added
     * @throws IllegalStateException    if the builder is closed
     */
    public static void addColumns(@NotNull GameDefinition.Builder builder) {
        builder.addColumn(EntityKind.UNIT, PATROL_ORIGIN_X);
        builder.addColumn(EntityKind.UNIT, PATROL_ORIGIN_Y);
    }

    private final @NotNull AttackMoveBehavior attackMove;
    private final @NotNull IntColumn originX;
    private final @NotNull IntColumn originY;

    /**
     * Creates a {@link PatrolBehavior} instance.
     *
     * @param game            the game, whose unit pool holds the columns of this behavior
     * @param combat          the combat system of the game
     * @param arrivalDistance the distance in pixels from a patrol end within which the unit turns back, at least {@code 0}
     * @throws IllegalArgumentException if the unit pool lacks a column of this behavior ({@link #addColumns})
     */
    public PatrolBehavior(@NotNull Game game, @NotNull StandardCombatSystem combat, int arrivalDistance) {
        if (!game.getUnits().getColumns().contains(PATROL_ORIGIN_X) || !game.getUnits().getColumns().contains(PATROL_ORIGIN_Y)) {
            throw Exceptions.newIllegalArgumentException("The unit pool lacks the patrol columns; call PatrolBehavior.addColumns.");
        }

        this.attackMove = new AttackMoveBehavior(combat, arrivalDistance);
        this.originX    = game.getUnits().getColumn(PATROL_ORIGIN_X);
        this.originY    = game.getUnits().getColumn(PATROL_ORIGIN_Y);
    }

    /**
     * Starts the order of a unit when it becomes the unit's current order: the engine
     * {@link org.fogofwarlabs.sc4j.engine.order.StandardOrderSystem} calls it right after an
     * {@link org.fogofwarlabs.sc4j.engine.order.OrderSystem#issue} or {@link org.fogofwarlabs.sc4j.engine.order.OrderSystem#finishOrder} made the
     * order current, with the order's target already on the unit. An order system of a mod may run its own start rules instead.
     * <p>
     * The unit's position becomes the end it patrols back to; the target point becomes its destination.
     *
     * @param context the context of the dispatch
     * @param unit    the unit whose order starts
     */
    @Override
    public void start(@NotNull OrderContext context, @NotNull Unit unit) {
        this.originX.set(unit.getIndex(), unit.getPositionX());
        this.originY.set(unit.getIndex(), unit.getPositionY());
        this.attackMove.start(context, unit);
    }

    /**
     * Runs the order of one unit for one frame.
     *
     * @param context the context of the dispatch
     * @param unit    the unit whose order runs
     */
    @Override
    public void update(@NotNull OrderContext context, @NotNull Unit unit) {
        if (this.attackMove.engage(context, unit) || !this.attackMove.hasArrived(context, unit)) {
            return;
        }

        int index = unit.getIndex();
        int returnX = this.originX.get(index);
        int returnY = this.originY.get(index);

        this.originX.set(index, unit.getOrderTargetX());
        this.originY.set(index, unit.getOrderTargetY());
        unit.setOrderTargetX(returnX);
        unit.setOrderTargetY(returnY);
        context.getGame().getMovement().setDestination(unit, returnX, returnY);
    }
}
