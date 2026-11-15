package org.fogofwarlabs.sc4j.engine.controller;

/**
 * Says where the commands of a {@link PlayerController} come from. Who controls a player slot (a person, a computer) is the slot's
 * {@link org.fogofwarlabs.sc4j.engine.game.ControllerKind}; the source is how that controller's commands reach this machine.
 */
public enum ControllerSource {
    /**
     * A person at this machine: the input layer submits the commands.
     */
    HUMAN,
    /**
     * A remote player: the commands arrive with the turns of the network session.
     */
    NETWORK,
    /**
     * A recorded game: the commands come from the replay.
     */
    REPLAY,
    /**
     * A computer player of this machine: the commands come from its decisions about the game.
     */
    COMPUTER
}
