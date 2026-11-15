package org.fogofwarlabs.sc4j.engine.animation;

/**
 * Says how a program continues after one instruction.
 */
public enum AnimationStep {
    /**
     * The program goes on in the same update: at the next position, or where the instruction jumped
     * ({@link AnimationContext#jump}).
     */
    CONTINUE,
    /**
     * The program pauses for the ticks the instruction set ({@link AnimationContext#setWait}; one tick if it set none) and
     * then goes on at the next position, or where the instruction jumped.
     */
    WAIT,
    /**
     * The program ends: the animated object keeps what it shows, and no later update runs an instruction until an
     * animation is started again.
     */
    END
}
