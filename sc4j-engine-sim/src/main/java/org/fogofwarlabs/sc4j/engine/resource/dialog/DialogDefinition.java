package org.fogofwarlabs.sc4j.engine.resource.dialog;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Describes one dialog: its name, its bounds on the screen and its top-level controls in drawing order.
 */
public final class DialogDefinition {
    private static @Nullable DialogControl findControl(@NotNull List<@NotNull DialogControl> controls, int id) {
        for (DialogControl control : controls) {
            if (control.getId() == id) {
                return control;
            }

            DialogControl child = findControl(control.getChildren(), id);

            if (child != null) {
                return child;
            }
        }

        return null;
    }


    private final @NotNull String name;

    private final int x;
    private final int y;
    private final int width;
    private final int height;

    private final @NotNull List<@NotNull DialogControl> controls;

    /**
     * Creates a {@link DialogDefinition} instance.
     *
     * @param name     the dialog name, for diagnostics
     * @param x        the left edge on the screen, in pixels
     * @param y        the top edge on the screen, in pixels
     * @param width    the width in pixels, at least {@code 0}
     * @param height   the height in pixels, at least {@code 0}
     * @param controls the top-level controls in drawing order, copied
     * @throws IllegalArgumentException if a size is negative
     */
    public DialogDefinition(@NotNull String name, int x, int y, int width, int height, @NotNull List<@NotNull DialogControl> controls) {
        if (width < 0 || height < 0) {
            throw Exceptions.newIllegalArgumentException("A dialog has no negative size: {0}x{1}", width, height);
        }

        this.name = name;

        this.x      = x;
        this.y      = y;
        this.width  = width;
        this.height = height;

        this.controls = List.copyOf(controls);
    }

    /**
     * @return the dialog name, for diagnostics
     */
    public @NotNull String getName() {
        return this.name;
    }

    /**
     * @return the left edge on the screen, in pixels
     */
    public int getX() {
        return this.x;
    }

    /**
     * @return the top edge on the screen, in pixels
     */
    public int getY() {
        return this.y;
    }

    /**
     * @return the width in pixels
     */
    public int getWidth() {
        return this.width;
    }

    /**
     * @return the height in pixels
     */
    public int getHeight() {
        return this.height;
    }

    /**
     * @return the top-level controls in drawing order, unmodifiable
     */
    public @NotNull List<@NotNull DialogControl> getControls() {
        return this.controls;
    }

    /**
     * Finds a control by its id, depth first in drawing order.
     *
     * @param id the control id
     * @return the first control with the id, or {@code null} if the dialog has none
     */
    public @Nullable DialogControl findControl(int id) {
        return findControl(this.controls, id);
    }
}
