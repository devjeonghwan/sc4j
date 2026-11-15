package org.fogofwarlabs.sc4j.engine.production;

/**
 * Holds the decoded payload of one command of a {@link TrainUnitCommandType}: the handle of the producing unit and the index of the unit type
 * in the game content. A command carries the encoded identities, because it is read without a game; {@link TrainUnitCommandType#create}
 * encodes them from the objects and {@link TrainUnitCommandType#execute} decodes them against the game.
 */
public final class TrainUnitCommand {
    private final int producerHandle;
    private final int unitTypeIndex;

    /**
     * Creates a {@link TrainUnitCommand} instance.
     *
     * @param producerHandle the handle of the producing unit in the game's unit pool
     * @param unitTypeIndex  the index of the unit type in the game content
     */
    public TrainUnitCommand(int producerHandle, int unitTypeIndex) {
        this.producerHandle = producerHandle;
        this.unitTypeIndex  = unitTypeIndex;
    }

    /**
     * @return the handle of the producing unit in the game's unit pool
     */
    public int getProducerHandle() {
        return this.producerHandle;
    }

    /**
     * @return the index of the unit type in the game content
     */
    public int getUnitTypeIndex() {
        return this.unitTypeIndex;
    }
}
