package org.fogofwarlabs.sc4j.engine.client.display;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Objects;

/**
 * Holds how many physical pixels one logical unit of a client's interface covers: the content scale the operating system gives the
 * window's monitor (for example {@code 1.5} at a display scale of 150%) times the user's own interface scale of the mod.
 * <p>
 * The user scale is the mod's choice setting {@value #SETTING_KEY}, whose options are percentages of the operating system's scale
 * ({@link #USER_SCALE_OPTIONS}); a mod that declares it in its manifest gets it in the launcher form and stored with its other settings.
 */
public final class DisplayScale {
    /**
     * The key of the mod setting that holds the user's interface scale.
     */
    public static final @NotNull String SETTING_KEY = "uiScale";
    /**
     * The options of the user's interface scale setting, in percent of the operating system's scale.
     */
    public static final @NotNull List<@NotNull String> USER_SCALE_OPTIONS = List.of("100", "125", "150", "175", "200");
    /**
     * The scale of one physical pixel per logical unit, for example of an offscreen run.
     */
    public static final @NotNull DisplayScale ONE = new DisplayScale(1, 1);

    private static final float PERCENT = 100f;

    /**
     * Creates a {@link DisplayScale} instance.
     *
     * @param contentScale the operating system's scale of the window's monitor
     * @param userScale    the user's interface scale of the mod
     * @return the created {@link DisplayScale} instance
     * @throws IllegalArgumentException if a scale is not a positive finite number
     */
    public static @NotNull DisplayScale of(float contentScale, float userScale) {
        if (!(contentScale > 0) || !(userScale > 0) || !Float.isFinite(contentScale) || !Float.isFinite(userScale)) {
            throw Exceptions.newIllegalArgumentException("Invalid display scale {0} or user scale {1}", contentScale, userScale);
        }

        return new DisplayScale(contentScale, userScale);
    }

    /**
     * Reads the value of the user's interface scale setting.
     *
     * @param option one of {@link #USER_SCALE_OPTIONS}
     * @return the user scale, for example {@code 1.25} for {@code "125"}
     * @throws IllegalArgumentException if the option is not one of {@link #USER_SCALE_OPTIONS}
     */
    public static float parseUserScale(@NotNull String option) {
        if (!USER_SCALE_OPTIONS.contains(option)) {
            throw Exceptions.newIllegalArgumentException("Invalid interface scale {0}, expected one of {1}", option, USER_SCALE_OPTIONS);
        }

        return Integer.parseInt(option) / PERCENT;
    }


    private final float contentScale;
    private final float userScale;

    private DisplayScale(float contentScale, float userScale) {
        this.contentScale = contentScale;
        this.userScale    = userScale;
    }

    /**
     * @return the operating system's scale of the window's monitor
     */
    public float getContentScale() {
        return this.contentScale;
    }

    /**
     * @return the user's interface scale of the mod
     */
    public float getUserScale() {
        return this.userScale;
    }

    /**
     * @return the physical pixels per logical unit: the content scale times the user scale
     */
    public float getScale() {
        return this.contentScale * this.userScale;
    }

    /**
     * Returns the scale of pictures that the platform shows at whole pixels, such as the pointer: the whole number nearest the scale.
     *
     * @return the whole scale, at least {@code 1}
     */
    public int getWholeScale() {
        return Math.max(1, Math.round(this.getScale()));
    }

    /**
     * Compares this scale with another object.
     *
     * @param other the object
     * @return {@code true} if the other object is a {@link DisplayScale} of the same content and user scale, {@code false} otherwise
     */
    @Override
    public boolean equals(@Nullable Object other) {
        return other instanceof DisplayScale scale && scale.contentScale == this.contentScale && scale.userScale == this.userScale;
    }

    /**
     * @return the hash code of the content and user scale
     */
    @Override
    public int hashCode() {
        return Objects.hash(this.contentScale, this.userScale);
    }

    /**
     * @return the scale as text, for example {@code "1.5 x 1.25"}
     */
    @Override
    public @NotNull String toString() {
        return this.contentScale + " x " + this.userScale;
    }
}
