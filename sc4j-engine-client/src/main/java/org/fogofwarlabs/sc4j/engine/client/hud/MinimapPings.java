package org.fogofwarlabs.sc4j.engine.client.hud;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

/**
 * Keeps the pings of a minimap in a fixed table of slots: the engine's minimap pings. A ping marks a world position for a number of animation
 * frames. Adding takes the first free slot and drops the ping when every slot is in use; each {@link #advance} moves the active pings one
 * frame on and frees those already on their last frame. Positions are world positions; the colour is the mod's value (a packed colour, or a
 * player whose colour tints the ping).
 */
public final class MinimapPings {
    private final int frameCount;

    private final boolean @NotNull [] active;
    private final int @NotNull [] x;
    private final int @NotNull [] y;
    private final int @NotNull [] frame;
    private final int @NotNull [] color;
    private final boolean @NotNull [] playerColor;

    /**
     * Creates a {@link MinimapPings} instance without pings.
     *
     * @param slotCount  the number of slots
     * @param frameCount the number of frames a ping shows
     * @throws IllegalArgumentException if a count is not positive
     */
    public MinimapPings(int slotCount, int frameCount) {
        if (slotCount <= 0 || frameCount <= 0) {
            throw Exceptions.newIllegalArgumentException("Invalid ping table of {0} slots and {1} frames", slotCount, frameCount);
        }

        this.frameCount = frameCount;

        this.active      = new boolean[slotCount];
        this.x           = new int[slotCount];
        this.y           = new int[slotCount];
        this.frame       = new int[slotCount];
        this.color       = new int[slotCount];
        this.playerColor = new boolean[slotCount];
    }

    /**
     * @return the number of slots
     */
    public int getSlotCount() {
        return this.active.length;
    }

    /**
     * @return the number of frames a ping shows
     */
    public int getFrameCount() {
        return this.frameCount;
    }

    /**
     * @return {@code true} if any slot holds a ping, {@code false} otherwise
     */
    public boolean isAnyActive() {
        for (boolean slot : this.active) {
            if (slot) {
                return true;
            }
        }

        return false;
    }

    /**
     * @param slot the slot
     * @return {@code true} if the slot holds a ping, {@code false} otherwise
     */
    public boolean isActive(int slot) {
        return this.active[slot];
    }

    /**
     * @param slot the slot
     * @return the ping's world x
     */
    public int getX(int slot) {
        return this.x[slot];
    }

    /**
     * @param slot the slot
     * @return the ping's world y
     */
    public int getY(int slot) {
        return this.y[slot];
    }

    /**
     * @param slot the slot
     * @return the ping's current frame, from {@code 0} to one below {@link #getFrameCount}
     */
    public int getFrame(int slot) {
        return this.frame[slot];
    }

    /**
     * @param slot the slot
     * @return the ping's colour value, or the player when {@link #isPlayerColor}
     */
    public int getColor(int slot) {
        return this.color[slot];
    }

    /**
     * @param slot the slot
     * @return {@code true} if the ping is tinted with a player's colour, {@code false} otherwise
     */
    public boolean isPlayerColor(int slot) {
        return this.playerColor[slot];
    }


    /**
     * Adds a ping in the first free slot; without a free slot the ping is dropped.
     *
     * @param worldX      the world x
     * @param worldY      the world y
     * @param color       the colour value, or with {@code playerColor} the player whose colour tints the ping
     * @param playerColor whether a player's colour tints the ping
     */
    public void add(int worldX, int worldY, int color, boolean playerColor) {
        for (int slot = 0; slot < this.active.length; slot++) {
            if (this.active[slot]) {
                continue;
            }

            this.active[slot]      = true;
            this.x[slot]           = worldX;
            this.y[slot]           = worldY;
            this.frame[slot]       = 0;
            this.color[slot]       = color;
            this.playerColor[slot] = playerColor;

            return;
        }
    }

    /**
     * Moves every active ping one frame on; a ping on its last frame is freed.
     *
     * @return {@code true} if any ping was active, so that the minimap needs a redraw, {@code false} otherwise
     */
    public boolean advance() {
        boolean any = false;

        for (int slot = 0; slot < this.active.length; slot++) {
            if (!this.active[slot]) {
                continue;
            }

            if (this.frame[slot] == this.frameCount - 1) {
                this.active[slot] = false;
            } else {
                this.frame[slot]++;
            }

            any = true;
        }

        return any;
    }

    /**
     * Frees every slot.
     */
    public void clear() {
        Arrays.fill(this.active, false);
    }
}
