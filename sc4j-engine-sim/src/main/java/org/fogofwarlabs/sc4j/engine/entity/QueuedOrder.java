package org.fogofwarlabs.sc4j.engine.entity;

import org.fogofwarlabs.sc4j.engine.content.type.OrderType;
import org.jetbrains.annotations.Nullable;

/**
 * Holds one order waiting in a unit's order queue: its order type, its target point and target unit, and the links of the intrusive list
 * it is a member of. A unit names the ends of its queue ({@link Unit#getFirstOrder()}, {@link Unit#getLastOrder()}); the order system
 * that owns the queued orders pools them, links a free one into a queue when an order is queued and back into its free list when the
 * order leaves the queue, so queuing allocates nothing.
 * <p>
 * A mod that pools its queued orders itself extends this class, so the engine queue of a unit holds its orders.
 */
public class QueuedOrder {
    private @Nullable QueuedOrder previous;
    private @Nullable QueuedOrder next;

    private @Nullable OrderType type;
    private int targetX;
    private int targetY;
    private @Nullable Unit targetUnit;

    /**
     * Creates a {@link QueuedOrder} instance outside any list, without order type, target point {@code (0, 0)} and without target unit.
     */
    public QueuedOrder() {
        this.previous = null;
        this.next     = null;

        this.type       = null;
        this.targetX    = 0;
        this.targetY    = 0;
        this.targetUnit = null;
    }

    /**
     * @return the preceding member of the list, or {@code null} for the first one
     */
    public @Nullable QueuedOrder getPrevious() {
        return this.previous;
    }

    /**
     * Sets the preceding member of the list.
     *
     * @param previous the preceding member, or {@code null} for the first one
     */
    public void setPrevious(@Nullable QueuedOrder previous) {
        this.previous = previous;
    }

    /**
     * @return the following member of the list, or {@code null} for the last one
     */
    public @Nullable QueuedOrder getNext() {
        return this.next;
    }

    /**
     * Sets the following member of the list.
     *
     * @param next the following member, or {@code null} for the last one
     */
    public void setNext(@Nullable QueuedOrder next) {
        this.next = next;
    }

    /**
     * @return the order type, or {@code null} if the order names no order type of the content
     */
    public @Nullable OrderType getType() {
        return this.type;
    }

    /**
     * Sets the order type.
     *
     * @param type the order type, or {@code null} if the order names no order type of the content
     */
    public void setType(@Nullable OrderType type) {
        this.type = type;
    }

    /**
     * @return the horizontal map position of the target
     */
    public int getTargetX() {
        return this.targetX;
    }

    /**
     * Sets the horizontal map position of the target.
     *
     * @param targetX the position
     */
    public void setTargetX(int targetX) {
        this.targetX = targetX;
    }

    /**
     * @return the vertical map position of the target
     */
    public int getTargetY() {
        return this.targetY;
    }

    /**
     * Sets the vertical map position of the target.
     *
     * @param targetY the position
     */
    public void setTargetY(int targetY) {
        this.targetY = targetY;
    }

    /**
     * @return the target unit, or {@code null} for a point target
     */
    public @Nullable Unit getTargetUnit() {
        return this.targetUnit;
    }

    /**
     * Sets the target unit.
     *
     * @param targetUnit the target unit, or {@code null} for a point target
     */
    public void setTargetUnit(@Nullable Unit targetUnit) {
        this.targetUnit = targetUnit;
    }

    /**
     * Sets the order type and the target of the order.
     *
     * @param type       the order type, or {@code null} if the order names no order type of the content
     * @param targetX    the horizontal map position of the target
     * @param targetY    the vertical map position of the target
     * @param targetUnit the target unit, or {@code null} for a point target
     */
    public void set(@Nullable OrderType type, int targetX, int targetY, @Nullable Unit targetUnit) {
        this.type       = type;
        this.targetX    = targetX;
        this.targetY    = targetY;
        this.targetUnit = targetUnit;
    }
}
