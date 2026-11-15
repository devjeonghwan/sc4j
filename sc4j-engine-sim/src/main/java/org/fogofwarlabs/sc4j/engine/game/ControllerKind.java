package org.fogofwarlabs.sc4j.engine.game;

/**
 * Says who controls a player slot. Where its commands come from (this machine, the network, a replay) is the command
 * source's concern, not the slot's.
 */
public enum ControllerKind {
    /**
     * A person.
     */
    HUMAN,
    /**
     * A computer player.
     */
    COMPUTER,
    /**
     * Nobody: the slot owns map objects and issues no commands.
     */
    NEUTRAL,
    /**
     * A person who watches and owns nothing.
     */
    OBSERVER
}
