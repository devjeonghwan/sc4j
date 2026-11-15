package org.fogofwarlabs.sc4j.engine.client.cursor;

import org.fogofwarlabs.sc4j.engine.client.display.DetailSet;
import org.fogofwarlabs.sc4j.engine.resource.graphics.Picture;
import org.jetbrains.annotations.NotNull;

/**
 * Shows the pointer of a client window as a picture with a hot spot (a platform cursor), so the cursor follows the mouse at the platform's
 * rate instead of the frame rate. {@link org.fogofwarlabs.sc4j.engine.client.host.HostWindow#createCursorSurface} serves a window of the engine's
 * host; {@link HeadlessCursorSurface} serves a client without window, for example an offscreen run or a test.
 * <p>
 * A picture comes with its detail pictures ({@link DetailSet}): at a scale of {@code n} platform pixels per picture pixel the pointer is built
 * from the detail of {@code n} ({@link DetailSet#find}), so it keeps its size and its hot spot and shows the detail's pixels.
 * <p>
 * A platform may refuse cursor pictures; from then on {@link #isAvailable} is {@code false}, the platform's own arrow is shown and a client draws
 * its cursor into the frame instead.
 */
public interface CursorSurface extends AutoCloseable {
    /**
     * @return {@code true} if the platform shows cursor pictures (it has not refused one), {@code false} otherwise
     */
    boolean isAvailable();

    /**
     * Sets how many platform pixels each picture pixel covers, so that the pointer keeps its size on a scaled display; a picture shown already
     * is shown again at the new scale.
     *
     * @param scale the whole scale, at least {@code 1}
     * @throws IllegalArgumentException if the scale is less than {@code 1}
     */
    void setScale(int scale);


    /**
     * Shows a picture as the pointer, from the detail of the scale; the picture's anchor is the hot spot. Showing the same set again builds
     * nothing new. A refused picture makes the surface unavailable ({@link #isAvailable}) and shows the platform's own arrow.
     *
     * @param pictures the picture with its detail pictures
     */
    void show(@NotNull DetailSet<Picture> pictures);

    /**
     * Shows the platform's own arrow.
     */
    void showDefault();

    /**
     * Shows no pointer over the window (a blank picture), for frames whose cursor the client draws itself or that show none. Nothing changes
     * while the surface is unavailable.
     */
    void hide();


    /**
     * Disposes resources: shows the platform's own arrow again and releases the built cursors. Closing again does nothing.
     */
    @Override
    void close();
}
