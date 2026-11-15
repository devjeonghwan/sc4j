package org.fogofwarlabs.sc4j.engine.production;

/**
 * Holds the decoded payload of one command of a {@link ConstructBuildingCommandType}: the handle of the worker, the index of the building type
 * in the game content and the site's center. A command carries the encoded identities, because it is read without a game;
 * {@link ConstructBuildingCommandType#create} encodes them from the objects and {@link ConstructBuildingCommandType#execute} decodes them
 * against the game.
 */
public final class ConstructBuildingCommand {
    private final int workerHandle;
    private final int buildingTypeIndex;
    private final int siteX;
    private final int siteY;

    /**
     * Creates a {@link ConstructBuildingCommand} instance.
     *
     * @param workerHandle      the handle of the worker in the game's unit pool
     * @param buildingTypeIndex the index of the building type in the game content
     * @param siteX             the x coordinate of the site's center in pixels
     * @param siteY             the y coordinate of the site's center in pixels
     */
    public ConstructBuildingCommand(int workerHandle, int buildingTypeIndex, int siteX, int siteY) {
        this.workerHandle      = workerHandle;
        this.buildingTypeIndex = buildingTypeIndex;
        this.siteX             = siteX;
        this.siteY             = siteY;
    }

    /**
     * @return the handle of the worker in the game's unit pool
     */
    public int getWorkerHandle() {
        return this.workerHandle;
    }

    /**
     * @return the index of the building type in the game content
     */
    public int getBuildingTypeIndex() {
        return this.buildingTypeIndex;
    }

    /**
     * @return the x coordinate of the site's center in pixels
     */
    public int getSiteX() {
        return this.siteX;
    }

    /**
     * @return the y coordinate of the site's center in pixels
     */
    public int getSiteY() {
        return this.siteY;
    }
}
