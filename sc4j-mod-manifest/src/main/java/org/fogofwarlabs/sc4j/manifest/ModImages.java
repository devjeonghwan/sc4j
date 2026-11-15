package org.fogofwarlabs.sc4j.manifest;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Names the launcher images of a mod, the manifest's {@code images}: jar entries such as {@code meta/icon.png}. Every
 * image is optional; each one named must be a PNG file in the jar.
 */
public final class ModImages {
    /**
     * The images of a manifest without {@code images}.
     */
    public static final @NotNull ModImages NONE = new ModImages(null, null, List.of());

    private final @Nullable String icon;
    private final @Nullable String banner;
    private final @NotNull List<@NotNull String> screenshots;

    /**
     * Creates a {@link ModImages} instance.
     *
     * @param icon        the jar entry of the icon, or {@code null}
     * @param banner      the jar entry of the banner, or {@code null}
     * @param screenshots the jar entries of the screenshots, in display order
     */
    public ModImages(@Nullable String icon, @Nullable String banner, @NotNull List<@NotNull String> screenshots) {
        this.icon        = icon;
        this.banner      = banner;
        this.screenshots = List.copyOf(screenshots);
    }

    /**
     * @return the jar entry of the icon, or {@code null} when the mod has none
     */
    public @Nullable String getIcon() {
        return this.icon;
    }

    /**
     * @return the jar entry of the banner, or {@code null} when the mod has none
     */
    public @Nullable String getBanner() {
        return this.banner;
    }

    /**
     * @return the jar entries of the screenshots, in display order
     */
    public @NotNull List<@NotNull String> getScreenshots() {
        return this.screenshots;
    }

    /**
     * @return every named jar entry: the icon, the banner and the screenshots, in that order
     */
    public @NotNull List<@NotNull String> getEntries() {
        List<String> entries = new ArrayList<>();

        if (this.icon != null) {
            entries.add(this.icon);
        }

        if (this.banner != null) {
            entries.add(this.banner);
        }

        entries.addAll(this.screenshots);

        return entries;
    }
}
