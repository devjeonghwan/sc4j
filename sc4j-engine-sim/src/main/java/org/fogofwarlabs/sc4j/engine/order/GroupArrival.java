package org.fogofwarlabs.sc4j.engine.order;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.content.definition.UnitTrait;
import org.fogofwarlabs.sc4j.engine.content.table.UnitTypeTable;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.math.IntegerMathUtils;
import org.fogofwarlabs.sc4j.engine.spatial.UnitBounds;
import org.fogofwarlabs.sc4j.engine.spatial.UnitFilter;
import org.fogofwarlabs.sc4j.engine.spatial.UnitList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Decides when a unit of a group sent to one point has arrived although it does not stand on the point: the move orders' rule that keeps a
 * group from pushing forever for the one point. A mod passes it to its move behaviors ({@link MoveBehavior},
 * {@link org.fogofwarlabs.sc4j.engine.combat.AttackMoveBehavior}) with the bounds of its units and the group distance.
 * {@snippet :
 * GroupArrival arrival = new GroupArrival(bounds, 96);
 * behaviors.register(move, new MoveBehavior(16, arrival));
 *}
 * <p>
 * A unit within the group distance of the point has arrived when its bounds touch (overlap or share an edge with) the bounds of a unit that
 * already stands there: a unit of its owner that is nearer to the point and stands still at its destination
 * ({@link org.fogofwarlabs.sc4j.engine.movement.MovementSystem#isAtDestination}), or a structure (a {@link UnitTrait#BUILDING} or a
 * {@link UnitTrait#RESOURCE_CONTAINER} of any owner) whose bounds hold the point. So the units that come later stop at the edge of the group,
 * and a unit sent onto a structure stops at it.
 * <p>
 * Determinism contract: the test reads the game state only, draws no random value and allocates nothing; the touching units are searched in
 * spatial index order.
 */
public final class GroupArrival {
    private final @NotNull UnitBounds<Unit> bounds;
    private final int distance;

    private final @NotNull UnitFilter standingFilter;
    private final @NotNull UnitList touching;

    private @Nullable Game searchedGame;
    private @Nullable Unit searchedUnit;
    private int targetX;
    private int targetY;
    private long unitDistanceSquared;

    /**
     * Creates a {@link GroupArrival} instance.
     *
     * @param bounds   reads the bounds of a unit
     * @param distance the group distance in pixels from the point within which a touching unit counts, at least {@code 0}
     * @throws IllegalArgumentException if the distance is negative
     */
    public GroupArrival(@NotNull UnitBounds<Unit> bounds, int distance) {
        if (distance < 0) {
            throw Exceptions.newIllegalArgumentException("The group arrival distance {0} is negative.", distance);
        }

        this.bounds   = bounds;
        this.distance = distance;

        this.standingFilter = this::isStanding;
        this.touching       = new UnitList(1);

        this.searchedGame        = null;
        this.searchedUnit        = null;
        this.targetX             = 0;
        this.targetY             = 0;
        this.unitDistanceSquared = 0;
    }

    /**
     * @return the group distance in pixels
     */
    public int getDistance() {
        return this.distance;
    }


    /**
     * Checks whether a unit sent to a point has arrived with its group, as the class description says.
     *
     * @param game    the game
     * @param unit    the unit
     * @param targetX the x coordinate of the point
     * @param targetY the y coordinate of the point
     * @return {@code true} if the unit is within the group distance of the point and touches a unit that stands there, {@code false} otherwise
     */
    public boolean hasArrived(@NotNull Game game, @NotNull Unit unit, int targetX, int targetY) {
        if (!IntegerMathUtils.isWithinDistance(unit.getPositionX(), unit.getPositionY(), targetX, targetY, this.distance)) {
            return false;
        }

        long horizontal = (long) targetX - unit.getPositionX();
        long vertical = (long) targetY - unit.getPositionY();

        this.searchedGame        = game;
        this.searchedUnit        = unit;
        this.targetX             = targetX;
        this.targetY             = targetY;
        this.unitDistanceSquared = horizontal * horizontal + vertical * vertical;

        try {
            game.getSpatialIndex().findInRectangle(this.bounds.getLeft(unit) - 1, this.bounds.getTop(unit) - 1, this.bounds.getRight(unit) + 1,
                                                   this.bounds.getBottom(unit) + 1, this.standingFilter, this.touching);
        } finally {
            this.searchedGame = null;
            this.searchedUnit = null;
        }

        return !this.touching.isEmpty();
    }

    private boolean isStanding(@NotNull Unit otherUnit) {
        Game game = this.searchedGame;
        Unit unit = this.searchedUnit;

        if (game == null || unit == null || otherUnit == unit || !this.bounds.isPlaced(otherUnit)) {
            return false;
        }

        UnitType type = otherUnit.getType();
        UnitTypeTable table = game.getContent().getUnitTypes();

        if (type != null && (table.hasTrait(type, UnitTrait.BUILDING) || table.hasTrait(type, UnitTrait.RESOURCE_CONTAINER))) {
            return this.bounds.getLeft(otherUnit) <= this.targetX && this.targetX <= this.bounds.getRight(otherUnit) &&
                   this.bounds.getTop(otherUnit) <= this.targetY && this.targetY <= this.bounds.getBottom(otherUnit);
        }

        long horizontal = (long) this.targetX - otherUnit.getPositionX();
        long vertical = (long) this.targetY - otherUnit.getPositionY();

        return otherUnit.getOwner() == unit.getOwner() && horizontal * horizontal + vertical * vertical < this.unitDistanceSquared &&
               game.getMovement().isAtDestination(otherUnit);
    }
}
