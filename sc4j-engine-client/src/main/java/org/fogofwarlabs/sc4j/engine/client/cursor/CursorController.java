package org.fogofwarlabs.sc4j.engine.client.cursor;

import org.fogofwarlabs.sc4j.engine.client.display.DetailSet;
import org.fogofwarlabs.sc4j.engine.content.type.OrderType;
import org.fogofwarlabs.sc4j.engine.resource.graphics.Picture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Shows the cursor picture of the current {@link CursorContext} on a {@link CursorSurface}: a screen tells it the context every frame, and it
 * passes the picture of its {@link CursorSet} to the surface only when the picture changes. Without cursor set, or where the set has no
 * picture, the platform's own arrow is shown.
 */
public final class CursorController {
    private final @NotNull CursorSurface surface;

    private @Nullable CursorSet cursors;

    private @NotNull CursorContext context;
    private @Nullable DetailSet<Picture> shown;
    private boolean started;

    /**
     * Creates a {@link CursorController} instance without cursor set.
     *
     * @param surface the surface that shows the pointer
     */
    public CursorController(@NotNull CursorSurface surface) {
        this.surface = surface;

        this.cursors = null;

        this.context = CursorContext.NORMAL;
        this.shown   = null;
        this.started = false;
    }

    /**
     * @return the cursor set, or {@code null} for the platform's own arrow everywhere
     */
    public @Nullable CursorSet getCursors() {
        return this.cursors;
    }

    /**
     * Sets the cursor set; the next {@link #update} shows its picture.
     *
     * @param cursors the cursor set, or {@code null} for the platform's own arrow everywhere
     */
    public void setCursors(@Nullable CursorSet cursors) {
        this.cursors = cursors;
    }

    /**
     * @return the context of the last update
     */
    public @NotNull CursorContext getContext() {
        return this.context;
    }

    /**
     * @return the shown picture with its detail pictures, or {@code null} while the platform's own arrow is shown
     */
    public @Nullable DetailSet<Picture> getShown() {
        return this.shown;
    }


    /**
     * Shows the picture of a context, when it differs from the shown one.
     *
     * @param context   the context
     * @param orderType the order type of the waiting targeted action, or {@code null} for none
     */
    public void update(@NotNull CursorContext context, @Nullable OrderType orderType) {
        CursorSet set = this.cursors;
        DetailSet<Picture> pictures = set != null ? set.findCursor(context, orderType) : null;

        this.context = context;

        if (this.started && pictures == this.shown) {
            return;
        }

        this.started = true;
        this.shown   = pictures;

        if (pictures != null) {
            this.surface.show(pictures);
        } else {
            this.surface.showDefault();
        }
    }
}
