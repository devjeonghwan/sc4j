package org.fogofwarlabs.sc4j.engine.order;

/**
 * Holds the decoded payload of one order command of an {@link IssueOrderCommandType}: the handle of the ordered unit, the index of the order
 * type in the game content, the target point, the handle of the target unit and whether the order is queued. A command carries the encoded
 * identities, because it is read without a game; {@link IssueOrderCommandType#create} encodes them from the objects and
 * {@link IssueOrderCommandType#execute} decodes them against the game.
 */
public final class IssueOrderCommand {
    private final int unitHandle;
    private final int orderTypeIndex;
    private final int targetX;
    private final int targetY;
    private final int targetUnitHandle;
    private final boolean queued;

    /**
     * Creates an {@link IssueOrderCommand} instance.
     *
     * @param unitHandle       the handle of the ordered unit in the game's unit pool
     * @param orderTypeIndex   the index of the order type in the game content
     * @param targetX          the x coordinate of the target point
     * @param targetY          the y coordinate of the target point
     * @param targetUnitHandle the handle of the target unit, or {@link IssueOrderCommandType#NO_UNIT} for none
     * @param queued           whether the order is queued after the unit's orders instead of replacing them
     */
    public IssueOrderCommand(int unitHandle, int orderTypeIndex, int targetX, int targetY, int targetUnitHandle, boolean queued) {
        this.unitHandle       = unitHandle;
        this.orderTypeIndex   = orderTypeIndex;
        this.targetX          = targetX;
        this.targetY          = targetY;
        this.targetUnitHandle = targetUnitHandle;
        this.queued           = queued;
    }

    /**
     * @return the handle of the ordered unit in the game's unit pool
     */
    public int getUnitHandle() {
        return this.unitHandle;
    }

    /**
     * @return the index of the order type in the game content
     */
    public int getOrderTypeIndex() {
        return this.orderTypeIndex;
    }

    /**
     * @return the x coordinate of the target point
     */
    public int getTargetX() {
        return this.targetX;
    }

    /**
     * @return the y coordinate of the target point
     */
    public int getTargetY() {
        return this.targetY;
    }

    /**
     * @return the handle of the target unit, or {@link IssueOrderCommandType#NO_UNIT} for none
     */
    public int getTargetUnitHandle() {
        return this.targetUnitHandle;
    }

    /**
     * @return {@code true} if the order is queued after the unit's orders, {@code false} if it replaces them
     */
    public boolean isQueued() {
        return this.queued;
    }
}
