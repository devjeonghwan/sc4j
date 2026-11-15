package org.fogofwarlabs.sc4j.engine.production;

/**
 * Holds the decoded payload of one command of a {@link SetRallyPointCommandType}: the handle of the producing unit and the rally point. A
 * command carries the encoded identity of the producer, because it is read without a game; {@link SetRallyPointCommandType#create} encodes it
 * from the unit and {@link SetRallyPointCommandType#execute} decodes it against the game.
 */
public final class SetRallyPointCommand {
    private final int producerHandle;
    private final int x;
    private final int y;

    /**
     * Creates a {@link SetRallyPointCommand} instance.
     *
     * @param producerHandle the handle of the producing unit in the game's unit pool
     * @param x              the x coordinate of the rally point in pixels
     * @param y              the y coordinate of the rally point in pixels
     */
    public SetRallyPointCommand(int producerHandle, int x, int y) {
        this.producerHandle = producerHandle;
        this.x              = x;
        this.y              = y;
    }

    /**
     * @return the handle of the producing unit in the game's unit pool
     */
    public int getProducerHandle() {
        return this.producerHandle;
    }

    /**
     * @return the x coordinate of the rally point in pixels
     */
    public int getX() {
        return this.x;
    }

    /**
     * @return the y coordinate of the rally point in pixels
     */
    public int getY() {
        return this.y;
    }
}
