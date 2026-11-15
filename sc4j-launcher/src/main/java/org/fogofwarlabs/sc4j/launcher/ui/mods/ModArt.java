package org.fogofwarlabs.sc4j.launcher.ui.mods;

import javafx.scene.image.Image;
import org.fogofwarlabs.sc4j.launcher.catalog.ModEntry;
import org.fogofwarlabs.sc4j.launcher.i18n.LauncherTexts;
import org.fogofwarlabs.sc4j.launcher.ui.ThemeUtils;
import org.fogofwarlabs.sc4j.manifest.ModArchive;
import org.fogofwarlabs.sc4j.manifest.ModManifest;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Decodes the images a mod jar provides (icon, banner, screenshots) once per size and keeps them while the mod list is
 * shown. A mod without an image, or with an image JavaFX cannot decode, gets the placeholder. Images are decoded at their logical size
 * times the image scale ({@link ThemeUtils#getImageScale}), so they stay sharp on a scaled display.
 */
public final class ModArt {
    private static final @NotNull String KEY_SEPARATOR = "|";

    /**
     * Returns the name a mod is listed with: its manifest name in the display language, or the jar file name when the
     * manifest is broken.
     *
     * @param entry the mod
     * @param texts the launcher texts of the display language
     * @return the display name
     */
    public static @NotNull String getName(@NotNull ModEntry entry, @NotNull LauncherTexts texts) {
        ModManifest manifest = entry.getManifest();

        return manifest == null ? entry.getJarName() : texts.localize(manifest.getName());
    }


    private final double imageScale;
    private final @NotNull Map<@NotNull String, @NotNull Image> images;

    /**
     * Creates a {@link ModArt} instance without decoded images.
     *
     * @param imageScale the screen pixels per logical unit the images are decoded for
     */
    public ModArt(double imageScale) {
        this.imageScale = imageScale;
        this.images     = new HashMap<>();
    }

    /**
     * Returns a mod's icon.
     *
     * @param entry the mod
     * @param size  the width and height in logical units
     * @return the icon, or {@code null} when the mod has none
     */
    public @Nullable Image getIcon(@NotNull ModEntry entry, double size) {
        ModManifest manifest = entry.getManifest();

        return manifest == null ? null : this.getImage(entry, manifest.getImages().getIcon(), size, size);
    }

    /**
     * Returns a mod's banner at the size of its image file; the detail view scales it to cover the banner card.
     *
     * @param entry the mod
     * @return the banner, or {@code null} when the mod has none
     */
    public @Nullable Image getBanner(@NotNull ModEntry entry) {
        ModManifest manifest = entry.getManifest();

        return manifest == null ? null : this.getImage(entry, manifest.getImages().getBanner(), 0, 0);
    }

    /**
     * Returns an image of a mod jar.
     *
     * @param entry    the mod
     * @param jarEntry the image's jar entry, or {@code null} for none
     * @param width    the width of the box the image fits in, in logical units, or {@code 0} for the width of the image file
     * @param height   the height of the box the image fits in, in logical units, or {@code 0} for the height of the image file
     * @return the decoded image, or {@code null} when there is no such image or it cannot be decoded
     */
    public @Nullable Image getImage(@NotNull ModEntry entry, @Nullable String jarEntry, double width, double height) {
        ModArchive archive = entry.getArchive();

        if (archive == null || jarEntry == null) {
            return null;
        }

        String key = entry.getJar() + KEY_SEPARATOR + jarEntry + KEY_SEPARATOR + width + KEY_SEPARATOR + height;
        Image image = this.images.get(key);

        if (image == null) {
            byte[] contents = archive.getImage(jarEntry);

            if (contents == null) {
                return null;
            }

            image = ThemeUtils.decodeImage(contents, width, height, this.imageScale);
            this.images.put(key, image);
        }

        return image.isError() ? null : image;
    }
}
