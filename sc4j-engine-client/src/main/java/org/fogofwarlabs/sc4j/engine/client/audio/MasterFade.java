package org.fogofwarlabs.sc4j.engine.client.audio;

/**
 * Fades the master sound gain when the window loses or regains the focus: out in the background unless sound plays in the background, back in
 * when the window is active again. The level progresses linearly over {@link #FADE_MILLISECONDS} of host time and is squared before it is
 * applied to the master bus ({@link AudioBus#setGain}).
 */
public final class MasterFade {
    /**
     * The master sound fade duration in milliseconds.
     */
    public static final long FADE_MILLISECONDS = 300;

    private static final float FULL_LEVEL = 1.0f;
    private static final float SILENT_LEVEL = 0.0f;

    private float level;
    private float target;
    private long fadeStartMilliseconds;

    private boolean fading;
    private boolean active;

    /**
     * Creates a {@link MasterFade} instance for an active window at full level.
     */
    public MasterFade() {
        this.level                 = FULL_LEVEL;
        this.target                = FULL_LEVEL;
        this.fadeStartMilliseconds = 0;

        this.fading = false;
        this.active = true;
    }

    /**
     * Follows the window activation and advances the fade.
     *
     * @param nowMilliseconds   the host clock in milliseconds
     * @param active            whether the window is active
     * @param soundInBackground whether sound keeps playing while the window is in the background
     * @return the master bus gain ({@code level * level})
     */
    public float update(long nowMilliseconds, boolean active, boolean soundInBackground) {
        if (active != this.active) {
            this.active = active;

            if (active) {
                this.startFade(nowMilliseconds, FULL_LEVEL);
            } else if (!soundInBackground) {
                this.startFade(nowMilliseconds, SILENT_LEVEL);
            }
        }

        if (this.fading) {
            float elapsed = (nowMilliseconds - this.fadeStartMilliseconds) / (float) FADE_MILLISECONDS;

            this.level = Math.clamp((2 * this.target - 1) * elapsed + (1 - this.target), SILENT_LEVEL, FULL_LEVEL);

            if (this.level == this.target) {
                this.fading = false;
            }
        }

        return this.level * this.level;
    }

    /**
     * Starts a fade toward a target, timed so that it continues from the current level.
     *
     * @param nowMilliseconds the host clock in milliseconds
     * @param fadeTarget      the target master level
     */
    private void startFade(long nowMilliseconds, float fadeTarget) {
        this.target = fadeTarget;

        // The level is t toward 1 and 1 - t toward 0: the fade start lies that share of the fade time back.
        float completedFraction = fadeTarget == FULL_LEVEL ? this.level : FULL_LEVEL - this.level;

        this.fadeStartMilliseconds = nowMilliseconds - (long) (completedFraction * FADE_MILLISECONDS);
        this.fading                = true;
    }
}
