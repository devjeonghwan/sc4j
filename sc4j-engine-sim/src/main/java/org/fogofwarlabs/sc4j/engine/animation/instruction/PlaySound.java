package org.fogofwarlabs.sc4j.engine.animation.instruction;

import org.fogofwarlabs.sc4j.engine.animation.AnimationContext;
import org.fogofwarlabs.sc4j.engine.animation.AnimationInstruction;
import org.fogofwarlabs.sc4j.engine.animation.AnimationStep;
import org.fogofwarlabs.sc4j.engine.content.type.SoundType;
import org.jetbrains.annotations.NotNull;

/**
 * Plays a sound at the animated object ({@link org.fogofwarlabs.sc4j.engine.animation.AnimationEffects#playSound}) and
 * goes on.
 */
public final class PlaySound extends AnimationInstruction {
    private final @NotNull SoundType sound;

    /**
     * Creates a {@link PlaySound} instance.
     *
     * @param sound the sound type
     */
    public PlaySound(@NotNull SoundType sound) {
        this.sound = sound;
    }

    /**
     * @return the sound type
     */
    public @NotNull SoundType getSound() {
        return this.sound;
    }


    /**
     * Runs this instruction for the animated object of the context.
     * <p>
     * Plays the sound and continues at the next position.
     *
     * @param context the running animation of one object
     * @return how the program continues
     */
    @Override
    public @NotNull AnimationStep execute(@NotNull AnimationContext context) {
        context.getEffects().playSound(context.getImage(), this.sound);

        return AnimationStep.CONTINUE;
    }
}
