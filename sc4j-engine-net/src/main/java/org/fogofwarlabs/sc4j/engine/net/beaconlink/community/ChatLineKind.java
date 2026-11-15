package org.fogofwarlabs.sc4j.engine.net.beaconlink.community;

/**
 * Lists the kinds of chat lines.
 */
public enum ChatLineKind {
    /**
     * A line the author said.
     */
    SAY,
    /**
     * An emote: the author's name followed by the text, as {@code /me} writes it.
     */
    EMOTE
}
