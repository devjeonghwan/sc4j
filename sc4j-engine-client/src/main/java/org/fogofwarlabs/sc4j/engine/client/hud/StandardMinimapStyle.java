package org.fogofwarlabs.sc4j.engine.client.hud;

import org.fogofwarlabs.sc4j.engine.client.view.GameView;
import org.fogofwarlabs.sc4j.engine.client.world.FogLayer;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Draws the minimap in the engine default look: the default {@link MinimapStyle}. Unit dots are in their owner's team colour, the fog is the
 * world view's fog ({@link FogLayer}), the camera outline is white, and a ping is a square that shrinks from {@link #PING_START_SIZE} to
 * {@link #PING_END_SIZE} pixels over its frames.
 */
public final class StandardMinimapStyle implements MinimapStyle {
    /**
     * The side length of a ping's square at its first frame, in minimap pixels.
     */
    public static final float PING_START_SIZE = 24;
    /**
     * The side length of a ping's square at its last frame, in minimap pixels.
     */
    public static final float PING_END_SIZE = 4;
    /**
     * The colour of the camera's outline.
     */
    public static final int CAMERA_COLOR = 0xFFFFFFFF;

    private static final int OPAQUE_ALPHA = 0xFF;

    /**
     * Creates a {@link StandardMinimapStyle} instance.
     */
    public StandardMinimapStyle() {
    }

    /**
     * Reads the colour of a unit's dot: the owner's team colour, opaque.
     *
     * @param view  the game view
     * @param unit  the unit
     * @param owner the unit's owner
     * @return the packed colour
     */
    @Override
    public int getUnitColor(@NotNull GameView view, @NotNull Unit unit, @NotNull Player owner) {
        return view.getTeamColor(owner) << Byte.SIZE | OPAQUE_ALPHA;
    }

    /**
     * Reads the colour of a tile the local player does not see: the world view's fog or unexplored colour.
     *
     * @param explored whether the player has seen the tile before
     * @return the packed colour
     */
    @Override
    public int getFogColor(boolean explored) {
        return explored ? FogLayer.DEFAULT_FOG_COLOR : FogLayer.DEFAULT_UNEXPLORED_COLOR;
    }

    /**
     * @return the packed colour of the camera's outline, {@link #CAMERA_COLOR}
     */
    @Override
    public int getCameraColor() {
        return CAMERA_COLOR;
    }

    /**
     * Reads the side length of a ping's square at a frame: from {@link #PING_START_SIZE} at the first frame down to {@link #PING_END_SIZE} at
     * the last, linearly.
     *
     * @param frame      the ping's frame from {@code 0}
     * @param frameCount the number of frames a ping shows
     * @return the side length in minimap pixels
     */
    @Override
    public float getPingSize(int frame, int frameCount) {
        if (frameCount <= 1) {
            return PING_END_SIZE;
        }

        return PING_START_SIZE - (PING_START_SIZE - PING_END_SIZE) * frame / (frameCount - 1);
    }
}
