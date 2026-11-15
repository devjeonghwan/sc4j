package org.fogofwarlabs.sc4j.engine.animation;

import org.fogofwarlabs.sc4j.engine.content.type.ImageType;
import org.fogofwarlabs.sc4j.engine.content.type.SoundType;
import org.fogofwarlabs.sc4j.engine.entity.Image;
import org.jetbrains.annotations.NotNull;

/**
 * Carries out what an animation asks of the rest of the game: sounds, overlays, weapon fire and movement steps. The game
 * creates one per game; it forwards each request to the system that owns the concern.
 * <p>
 * Determinism contract: the effects are called inside the simulation frame, in program order. Whatever they change and
 * whatever random values they draw is part of the simulation, so an implementation follows the contracts of the systems
 * it calls.
 */
public interface AnimationEffects {
    /**
     * Plays a sound at an animated image.
     *
     * @param image the animated image
     * @param sound the sound type
     */
    void playSound(@NotNull Image image, @NotNull SoundType sound);

    /**
     * Creates an overlay image on the object of an animated image.
     *
     * @param image     the animated image
     * @param imageType the image type of the overlay
     * @param offsetX   the horizontal offset from the animated image, in pixels
     * @param offsetY   the vertical offset from the animated image, in pixels
     */
    void spawnOverlay(@NotNull Image image, @NotNull ImageType imageType, int offsetX, int offsetY);

    /**
     * Fires a weapon of the unit of an animated image: the melee hit, or the launch of a projectile.
     *
     * @param image            the animated image
     * @param weaponSlot       the weapon slot of the unit, {@code 0} for its first weapon
     * @param instructionLimit the instruction limit of each animation program run the weapon fire starts (its projectile)
     */
    void fireWeapon(@NotNull Image image, int weaponSlot, int instructionLimit);

    /**
     * Moves the unit of an animated image forward.
     *
     * @param image    the animated image
     * @param distance the distance in pixels
     */
    void move(@NotNull Image image, int distance);
}
