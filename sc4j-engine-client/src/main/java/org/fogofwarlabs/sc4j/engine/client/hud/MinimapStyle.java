package org.fogofwarlabs.sc4j.engine.client.hud;

import org.fogofwarlabs.sc4j.engine.client.view.GameView;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Decides the look of a {@link Minimap}: the hooks of the engine minimap renderer. The minimap asks its style for the colour of each unit
 * dot, of the fog and of the camera outline, and for the size of a ping at each of its frames. The engine default is
 * {@link StandardMinimapStyle}. Colours are packed {@code 0xRRGGBBAA}.
 */
public interface MinimapStyle {
    /**
     * Reads the colour of a unit's dot.
     *
     * @param view  the game view
     * @param unit  the unit
     * @param owner the unit's owner
     * @return the packed colour
     */
    int getUnitColor(@NotNull GameView view, @NotNull Unit unit, @NotNull Player owner);

    /**
     * Reads the colour of a tile the local player does not see.
     *
     * @param explored whether the player has seen the tile before
     * @return the packed colour
     */
    int getFogColor(boolean explored);

    /**
     * @return the packed colour of the camera's outline
     */
    int getCameraColor();

    /**
     * Reads the side length of a ping's square at a frame.
     *
     * @param frame      the ping's frame from {@code 0}
     * @param frameCount the number of frames a ping shows
     * @return the side length in minimap pixels
     */
    float getPingSize(int frame, int frameCount);
}
