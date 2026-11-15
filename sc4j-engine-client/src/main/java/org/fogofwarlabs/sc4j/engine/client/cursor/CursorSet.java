package org.fogofwarlabs.sc4j.engine.client.cursor;

import org.fogofwarlabs.sc4j.engine.client.display.DetailSet;
import org.fogofwarlabs.sc4j.engine.content.type.OrderType;
import org.fogofwarlabs.sc4j.engine.resource.graphics.Picture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

/**
 * Holds the cursor pictures of a look: one per {@link CursorContext}, and per order type the picture shown while a targeted action of that
 * order waits ({@link CursorContext#TARGET}). Each picture comes with its detail pictures ({@link DetailSet}) and its anchor is its hot spot. A
 * context without picture shows its fallback's picture; without any, the platform's own arrow is shown.
 */
public final class CursorSet {
    private final @NotNull Map<@NotNull CursorContext, @NotNull DetailSet<Picture>> cursors;
    private final @NotNull Map<@NotNull OrderType, @NotNull DetailSet<Picture>> orderCursors;

    /**
     * Creates a {@link CursorSet} instance without pictures.
     */
    public CursorSet() {
        this.cursors      = new EnumMap<>(CursorContext.class);
        this.orderCursors = new HashMap<>();
    }

    /**
     * Sets the picture of a context.
     *
     * @param context  the context
     * @param pictures the picture with its detail pictures, its anchor the hot spot
     */
    public void setCursor(@NotNull CursorContext context, @NotNull DetailSet<Picture> pictures) {
        this.cursors.put(context, pictures);
    }

    /**
     * Sets the picture shown while a targeted action of an order type waits for its target.
     *
     * @param orderType the order type
     * @param pictures  the picture with its detail pictures, its anchor the hot spot
     */
    public void setOrderCursor(@NotNull OrderType orderType, @NotNull DetailSet<Picture> pictures) {
        this.orderCursors.put(orderType, pictures);
    }


    /**
     * Finds the picture of a context: for {@link CursorContext#TARGET} the waiting order type's picture first, then the context's picture
     * and those of its fallbacks.
     *
     * @param context   the context
     * @param orderType the order type of the waiting targeted action, or {@code null} for none
     * @return the picture with its detail pictures, or {@code null} for the platform's own arrow
     */
    public @Nullable DetailSet<Picture> findCursor(@NotNull CursorContext context, @Nullable OrderType orderType) {
        if (context == CursorContext.TARGET && orderType != null) {
            DetailSet<Picture> pictures = this.orderCursors.get(orderType);

            if (pictures != null) {
                return pictures;
            }
        }

        for (CursorContext current = context; current != null; current = current.getFallback()) {
            DetailSet<Picture> pictures = this.cursors.get(current);

            if (pictures != null) {
                return pictures;
            }
        }

        return null;
    }
}
