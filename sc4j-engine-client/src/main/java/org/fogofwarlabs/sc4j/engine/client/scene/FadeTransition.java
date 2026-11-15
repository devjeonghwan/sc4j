package org.fogofwarlabs.sc4j.engine.client.scene;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.jetbrains.annotations.NotNull;

/**
 * Fades the outgoing scene to a colour over a duration, then lets the stack switch: the engine default
 * {@link SceneTransition} of scenes that draw on a {@link Canvas}.
 */
public final class FadeTransition implements SceneTransition<Canvas> {
    /**
     * The shift of a packed {@code 0xRRGGBB} colour to the colour channels of {@code 0xRRGGBBAA}.
     */
    private static final int ALPHA_BITS = 8;
    /**
     * The largest alpha value.
     */
    private static final int OPAQUE_ALPHA = 0xFF;

    private final long duration;
    private final int color;

    private long startTime;
    private long time;

    /**
     * Creates a {@link FadeTransition} instance.
     *
     * @param duration the fade duration in milliseconds
     * @param color    the colour faded to, packed {@code 0xRRGGBB}
     * @throws IllegalArgumentException if the duration is not positive
     */
    public FadeTransition(long duration, int color) {
        if (duration <= 0) {
            throw Exceptions.newIllegalArgumentException("A fade needs a positive duration, not {0}.", duration);
        }

        this.duration = duration;
        this.color    = color;

        this.startTime = 0;
        this.time      = 0;
    }

    /**
     * Starts the transition at the supplied time; called once, on the first frame the stack applies it.
     *
     * @param time the client time in milliseconds
     */
    @Override
    public void start(long time) {
        this.startTime = time;
        this.time      = time;
    }

    /**
     * Advances the transition.
     *
     * @param time the client time in milliseconds
     * @return {@code true} if the transition is complete and the stack switches scenes, {@code false} otherwise
     */
    @Override
    public boolean update(long time) {
        this.time = time;

        return time - this.startTime >= this.duration;
    }

    /**
     * Draws the transition over the scenes: the colour over the whole canvas with an alpha that grows with the elapsed time.
     *
     * @param surface the surface
     */
    @Override
    public void draw(@NotNull Canvas surface) {
        long elapsed = Math.clamp(this.time - this.startTime, 0, this.duration);
        int alpha = (int) (elapsed * OPAQUE_ALPHA / this.duration);

        surface.fillRectangle(0, 0, surface.getWidth(), surface.getHeight(), this.color << ALPHA_BITS | alpha);
    }
}
