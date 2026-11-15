package org.fogofwarlabs.sc4j.engine.production;

/**
 * Holds the decoded payload of one command of a {@link CancelProductionCommandType}: the handle of the producing unit and the queue slot to
 * cancel. A command carries the encoded identity, because it is read without a game; {@link CancelProductionCommandType#create} encodes it from
 * the unit and {@link CancelProductionCommandType#execute} decodes it against the game.
 */
public final class CancelProductionCommand {
    private final int producerHandle;
    private final int slot;

    /**
     * Creates a {@link CancelProductionCommand} instance.
     *
     * @param producerHandle the handle of the producing unit in the game's unit pool
     * @param slot           the queue slot, {@code 0} up to {@link CancelProductionCommandType#MAXIMUM_SLOT}
     */
    public CancelProductionCommand(int producerHandle, int slot) {
        this.producerHandle = producerHandle;
        this.slot           = slot;
    }

    /**
     * @return the handle of the producing unit in the game's unit pool
     */
    public int getProducerHandle() {
        return this.producerHandle;
    }

    /**
     * @return the queue slot, ignored for a building under construction
     */
    public int getSlot() {
        return this.slot;
    }
}
