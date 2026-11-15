package org.fogofwarlabs.sc4j.engine.animation.instruction;

import org.fogofwarlabs.sc4j.engine.animation.AnimationContext;
import org.fogofwarlabs.sc4j.engine.animation.AnimationInstruction;
import org.fogofwarlabs.sc4j.engine.animation.AnimationStep;
import org.fogofwarlabs.sc4j.engine.content.type.ImageType;
import org.jetbrains.annotations.NotNull;

/**
 * Creates an overlay image at an offset from the animated object
 * ({@link org.fogofwarlabs.sc4j.engine.animation.AnimationEffects#spawnOverlay}) and goes on.
 */
public final class SpawnOverlay extends AnimationInstruction {
    private final @NotNull ImageType imageType;
    private final int offsetX;
    private final int offsetY;

    /**
     * Creates a {@link SpawnOverlay} instance.
     *
     * @param imageType the image type of the overlay
     * @param offsetX   the horizontal offset from the animated object, in pixels
     * @param offsetY   the vertical offset from the animated object, in pixels
     */
    public SpawnOverlay(@NotNull ImageType imageType, int offsetX, int offsetY) {
        this.imageType = imageType;
        this.offsetX   = offsetX;
        this.offsetY   = offsetY;
    }

    /**
     * @return the image type of the overlay
     */
    public @NotNull ImageType getImageType() {
        return this.imageType;
    }

    /**
     * @return the horizontal offset from the animated object, in pixels
     */
    public int getOffsetX() {
        return this.offsetX;
    }

    /**
     * @return the vertical offset from the animated object, in pixels
     */
    public int getOffsetY() {
        return this.offsetY;
    }


    /**
     * Runs this instruction for the animated object of the context.
     * <p>
     * Creates the overlay and continues at the next position.
     *
     * @param context the running animation of one object
     * @return how the program continues
     */
    @Override
    public @NotNull AnimationStep execute(@NotNull AnimationContext context) {
        context.getEffects().spawnOverlay(context.getImage(), this.imageType, this.offsetX, this.offsetY);

        return AnimationStep.CONTINUE;
    }
}
