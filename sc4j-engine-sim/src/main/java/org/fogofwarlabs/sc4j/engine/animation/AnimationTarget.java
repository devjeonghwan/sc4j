package org.fogofwarlabs.sc4j.engine.animation;

import org.fogofwarlabs.sc4j.engine.entity.Image;
import org.jetbrains.annotations.NotNull;

/**
 * Represents the owner of the animated images: it applies what a program shows to an image. The engine implements it
 * for its image pool; a mod that pools its own images (subclasses of {@link Image}) implements it for them, and its own
 * instructions reach the rest of its image state through it.
 * <p>
 * Determinism contract: the target is called inside the simulation frame, changes only the given image and draws no
 * random value.
 */
@FunctionalInterface
public interface AnimationTarget {
    /**
     * Shows a frame on an animated image.
     *
     * @param image the animated image
     * @param frame the frame number of the animation; how it maps to a picture (direction, mirroring) is up to the owner
     */
    void showFrame(@NotNull Image image, int frame);
}
