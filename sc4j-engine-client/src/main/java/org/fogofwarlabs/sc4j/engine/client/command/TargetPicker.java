package org.fogofwarlabs.sc4j.engine.client.command;

import org.jetbrains.annotations.Nullable;

/**
 * Finds what the player points at on the screen: the world view converts the framebuffer position to the map and finds the unit there.
 */
public interface TargetPicker {
    /**
     * Picks the target at a framebuffer position.
     *
     * @param x the framebuffer column
     * @param y the framebuffer row
     * @return the target, or {@code null} if the position shows no map (for example the HUD)
     */
    @Nullable CommandTarget pick(float x, float y);
}
