package org.fogofwarlabs.sc4j.engine.client.world;

import org.fogofwarlabs.sc4j.engine.client.command.CommandTarget;
import org.fogofwarlabs.sc4j.engine.client.command.TargetPicker;
import org.fogofwarlabs.sc4j.engine.client.projection.WorldProjection;
import org.fogofwarlabs.sc4j.engine.client.view.GameView;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Finds what the player points at in the world view: the engine default {@link TargetPicker}. The world view shows the camera's screen area
 * from the framebuffer's top-left corner (the HUD draws over it); a framebuffer position inside it converts to the world through the
 * {@link Camera} and its projection, and the target is the unit its {@link UnitPickOrder} picks there (by default
 * {@link GameView#findUnitAt}) if the local player sees it, else the world position. An observer without local player sees every unit.
 */
public final class WorldPicker implements TargetPicker {
    private static final int POINT_VALUES = 2;

    private final @NotNull GameView view;
    private final @NotNull Camera camera;
    private final @NotNull UnitPickOrder pickOrder;

    /**
     * Creates a {@link WorldPicker} instance with the view's pick order ({@link GameView#findUnitAt}).
     *
     * @param view   the game view
     * @param camera the camera of the world view
     */
    public WorldPicker(@NotNull GameView view, @NotNull Camera camera) {
        this(view, camera, view::findUnitAt);
    }

    /**
     * Creates a {@link WorldPicker} instance.
     *
     * @param view      the game view
     * @param camera    the camera of the world view
     * @param pickOrder the order that decides which of overlapping units is picked
     */
    public WorldPicker(@NotNull GameView view, @NotNull Camera camera, @NotNull UnitPickOrder pickOrder) {
        this.view      = view;
        this.camera    = camera;
        this.pickOrder = pickOrder;
    }

    /**
     * @return the width of the world view in framebuffer pixels, the camera's screen area
     */
    public int getScreenWidth() {
        return this.camera.getScreenWidth();
    }

    /**
     * @return the height of the world view in framebuffer pixels, the camera's screen area
     */
    public int getScreenHeight() {
        return this.camera.getScreenHeight();
    }


    /**
     * Checks whether a framebuffer position lies in the world view.
     *
     * @param x the framebuffer column
     * @param y the framebuffer row
     * @return {@code true} if the position shows the world, {@code false} otherwise
     */
    public boolean contains(float x, float y) {
        return x >= 0 && y >= 0 && x < this.camera.getScreenWidth() && y < this.camera.getScreenHeight();
    }

    /**
     * Converts a framebuffer position to a world x.
     *
     * @param x the framebuffer column
     * @param y the framebuffer row
     * @return the world x
     */
    public int toWorldX(float x, float y) {
        return (int) Math.floor(this.camera.toWorldX(x, y));
    }

    /**
     * Converts a framebuffer position to a world y.
     *
     * @param x the framebuffer column
     * @param y the framebuffer row
     * @return the world y
     */
    public int toWorldY(float x, float y) {
        return (int) Math.floor(this.camera.toWorldY(x, y));
    }

    /**
     * Writes the framebuffer outline of a world rectangle through the camera's projection: the corners of the world top-left, top-right,
     * bottom-right and bottom-left corners, in this order, as x and y pairs.
     *
     * @param left    the world x of the left edge
     * @param top     the world y of the top edge
     * @param right   the world x of the right edge
     * @param bottom  the world y of the bottom edge
     * @param corners the {@value WorldProjection#OUTLINE_LENGTH} values written from index {@code 0}
     * @throws IndexOutOfBoundsException if the array is shorter than {@value WorldProjection#OUTLINE_LENGTH}
     */
    public void outlineOnScreen(float left, float top, float right, float bottom, float @NotNull [] corners) {
        this.camera.getProjection().outlineFootprint(left, top, right, bottom, corners);

        for (int index = 0; index < WorldProjection.OUTLINE_LENGTH; index += POINT_VALUES) {
            corners[index]     = (corners[index] - this.camera.getX()) * this.camera.getZoom();
            corners[index + 1] = (corners[index + 1] - this.camera.getY()) * this.camera.getZoom();
        }
    }

    /**
     * Finds the unit the local player sees at a framebuffer position.
     *
     * @param x the framebuffer column
     * @param y the framebuffer row
     * @return the unit, or {@code null} if the position shows no unit the player sees or lies outside the world view
     */
    public @Nullable Unit pickUnit(float x, float y) {
        if (!this.contains(x, y)) {
            return null;
        }

        Unit unit = this.pickOrder.findUnitAt(this.toWorldX(x, y), this.toWorldY(x, y));
        Player player = this.view.getLocalPlayer();

        return unit != null && (player == null || this.view.isUnitVisible(player, unit)) ? unit : null;
    }

    /**
     * Picks the target at a framebuffer position: the unit the local player sees there, else the world position.
     *
     * @param x the framebuffer column
     * @param y the framebuffer row
     * @return the target, or {@code null} if the position lies outside the world view
     */
    @Override
    public @Nullable CommandTarget pick(float x, float y) {
        if (!this.contains(x, y)) {
            return null;
        }

        Unit unit = this.pickUnit(x, y);

        return unit != null ? CommandTarget.of(unit) : CommandTarget.at(this.toWorldX(x, y), this.toWorldY(x, y));
    }
}
