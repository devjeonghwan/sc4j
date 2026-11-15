package org.fogofwarlabs.sc4j.engine.client.cursor;

import org.jetbrains.annotations.Nullable;

/**
 * Names what the pointer is over or about to do, which chooses the cursor picture ({@link CursorSet}). A context without its own picture
 * shows the picture of its fallback: a diagonal scroll falls back to its vertical scroll, an invalid site to a site, every other context to
 * {@link #NORMAL}.
 */
public enum CursorContext {
    /**
     * Nothing in particular: the arrow.
     */
    NORMAL(null),
    /**
     * Over a unit of the local player or of a player on its team.
     */
    HOVER_FRIEND(NORMAL),
    /**
     * Over a unit of a player on another team.
     */
    HOVER_ENEMY(NORMAL),
    /**
     * Over a unit of a neutral player, for example a resource.
     */
    HOVER_NEUTRAL(NORMAL),
    /**
     * A targeted action waits for its target; a cursor set may give the waiting order type its own picture, for example move or attack.
     */
    TARGET(NORMAL),
    /**
     * A building waits for its site, and the pointed site is buildable.
     */
    PLACE(NORMAL),
    /**
     * A building waits for its site, and the pointed site is not buildable.
     */
    PLACE_INVALID(PLACE),
    /**
     * The pointer at the top edge scrolls the view up.
     */
    SCROLL_UP(NORMAL),
    /**
     * The pointer at the bottom edge scrolls the view down.
     */
    SCROLL_DOWN(NORMAL),
    /**
     * The pointer at the left edge scrolls the view left.
     */
    SCROLL_LEFT(NORMAL),
    /**
     * The pointer at the right edge scrolls the view right.
     */
    SCROLL_RIGHT(NORMAL),
    /**
     * The pointer in the top-left corner scrolls the view up and left.
     */
    SCROLL_UP_LEFT(SCROLL_UP),
    /**
     * The pointer in the top-right corner scrolls the view up and right.
     */
    SCROLL_UP_RIGHT(SCROLL_UP),
    /**
     * The pointer in the bottom-left corner scrolls the view down and left.
     */
    SCROLL_DOWN_LEFT(SCROLL_DOWN),
    /**
     * The pointer in the bottom-right corner scrolls the view down and right.
     */
    SCROLL_DOWN_RIGHT(SCROLL_DOWN);

    private static final int CENTERED = 0;

    /**
     * Returns the scroll context of an edge scroll direction.
     *
     * @param horizontal {@code -1} for left, {@code 1} for right, {@code 0} for neither
     * @param vertical   {@code -1} for up, {@code 1} for down, {@code 0} for neither
     * @return the scroll context, or {@code null} if both directions are {@code 0}
     */
    public static @Nullable CursorContext ofScroll(int horizontal, int vertical) {
        if (vertical < CENTERED) {
            return horizontal < CENTERED ? SCROLL_UP_LEFT : horizontal > CENTERED ? SCROLL_UP_RIGHT : SCROLL_UP;
        }

        if (vertical > CENTERED) {
            return horizontal < CENTERED ? SCROLL_DOWN_LEFT : horizontal > CENTERED ? SCROLL_DOWN_RIGHT : SCROLL_DOWN;
        }

        return horizontal < CENTERED ? SCROLL_LEFT : horizontal > CENTERED ? SCROLL_RIGHT : null;
    }


    private final @Nullable CursorContext fallback;

    CursorContext(@Nullable CursorContext fallback) {
        this.fallback = fallback;
    }

    /**
     * @return the context whose picture is shown when this context has none, or {@code null} for {@link #NORMAL}
     */
    public @Nullable CursorContext getFallback() {
        return this.fallback;
    }
}
