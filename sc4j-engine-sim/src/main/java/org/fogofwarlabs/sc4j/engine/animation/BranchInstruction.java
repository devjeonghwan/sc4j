package org.fogofwarlabs.sc4j.engine.animation;

/**
 * Marks an {@link AnimationInstruction} that may continue at a fixed position of its program, such as a jump or a call. A
 * program checks every branch target when it is created, so a running program never branches out of its instructions.
 */
public interface BranchInstruction {
    /**
     * @return the position the instruction may continue at, {@code 0} up to the program length minus one
     */
    int getTargetPosition();
}
