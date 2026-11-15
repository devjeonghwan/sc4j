package org.fogofwarlabs.sc4j.engine.pack;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.json.JsonObjectReader;
import org.fogofwarlabs.sc4j.engine.resource.ResourceKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Holds the parsed {@value #FILE_NAME} of a pack: the format, the animation kinds, per resource type the resource ids in file order with
 * the pack path of each (sprite sheets, detail sprite sheets, sounds, palettes, string tables, tilesets, fonts, dialogs, pictures), and the
 * pack path of the content definitions file.
 */
final class PackManifest {
    /**
     * The pack path of the manifest.
     */
    static final @NotNull String FILE_NAME = "pack.json";
    /**
     * The format number this reader understands.
     */
    static final int FORMAT = 1;

    /**
     * The field of the format number.
     */
    private static final @NotNull String FORMAT_FIELD = "format";
    /**
     * The field of the animation kinds.
     */
    private static final @NotNull String ANIMATION_KINDS_FIELD = "animationKinds";
    /**
     * The field of the sprite sheets.
     */
    private static final @NotNull String SPRITE_SHEETS_FIELD = "spriteSheets";
    /**
     * The field of the detail sprite sheets.
     */
    private static final @NotNull String SPRITE_SHEET_DETAILS_FIELD = "spriteSheetDetails";
    /**
     * The field of the sounds.
     */
    private static final @NotNull String SOUNDS_FIELD = "sounds";
    /**
     * The field of the palettes.
     */
    private static final @NotNull String PALETTES_FIELD = "palettes";
    /**
     * The field of the string tables.
     */
    private static final @NotNull String STRING_TABLES_FIELD = "stringTables";
    /**
     * The field of the tilesets.
     */
    private static final @NotNull String TILESETS_FIELD = "tilesets";
    /**
     * The field of the fonts.
     */
    private static final @NotNull String FONTS_FIELD = "fonts";
    /**
     * The field of the dialogs.
     */
    private static final @NotNull String DIALOGS_FIELD = "dialogs";
    /**
     * The field of the pictures.
     */
    private static final @NotNull String PICTURES_FIELD = "pictures";
    /**
     * The field of the content definitions file.
     */
    private static final @NotNull String CONTENT_FIELD = "content";

    /**
     * Reads the manifest of a pack.
     *
     * @param source the pack files
     * @return the manifest
     * @throws IOException if the manifest is missing or invalid, or names an invalid id or path
     */
    static @NotNull PackManifest read(@NotNull PackSource source) throws IOException {
        String location = source.getLocation() + "!" + FILE_NAME;
        JsonObjectReader manifest = JsonObjectReader.parse(source.read(FILE_NAME), location);
        int format = manifest.getInt(FORMAT_FIELD);

        if (format != FORMAT) {
            throw Exceptions.newIOException("{0}: unsupported pack format {1}; this engine reads format {2}.", location, format, FORMAT);
        }

        List<String> kinds = manifest.getTextList(ANIMATION_KINDS_FIELD);

        for (int index = 0; index < kinds.size(); index++) {
            if (kinds.get(index).isEmpty() || kinds.indexOf(kinds.get(index)) != index) {
                throw Exceptions.newIOException("{0}: the animation kind {1} is empty or listed twice.", location, kinds.get(index));
            }
        }

        String content = manifest.findText(CONTENT_FIELD);

        if (content != null) {
            PackSource.checkPath(content);
        }

        Map<String, String> spriteSheets = checkEntries(manifest.getTextMap(SPRITE_SHEETS_FIELD), location);
        Map<String, String> spriteSheetDetails = checkEntries(manifest.getTextMap(SPRITE_SHEET_DETAILS_FIELD), location);

        for (String id : spriteSheetDetails.keySet()) {
            if (spriteSheets.containsKey(id)) {
                throw Exceptions.newIOException("{0}: the sprite sheet {1} is also listed as a detail sprite sheet.", location, id);
            }
        }

        return new PackManifest(List.copyOf(kinds),
                                spriteSheets, spriteSheetDetails,
                                checkEntries(manifest.getTextMap(SOUNDS_FIELD), location),
                                checkEntries(manifest.getTextMap(PALETTES_FIELD), location),
                                checkEntries(manifest.getTextMap(STRING_TABLES_FIELD), location),
                                checkEntries(manifest.getTextMap(TILESETS_FIELD), location),
                                checkEntries(manifest.getTextMap(FONTS_FIELD), location),
                                checkEntries(manifest.getTextMap(DIALOGS_FIELD), location),
                                checkEntries(manifest.getTextMap(PICTURES_FIELD), location),
                                content);
    }

    private static @NotNull Map<@NotNull String, @NotNull String> checkEntries(@NotNull Map<@NotNull String, @NotNull String> entries,
                                                                               @NotNull String location) throws IOException {
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            try {
                ResourceKey.of(entry.getKey());
            } catch (IllegalArgumentException exception) {
                throw Exceptions.newIOException(exception, "{0}: invalid resource id {1}.", location, entry.getKey());
            }

            PackSource.checkPath(entry.getValue());
        }

        return entries;
    }


    private final @NotNull List<@NotNull String> animationKinds;

    private final @NotNull Map<@NotNull String, @NotNull String> spriteSheets;
    private final @NotNull List<@NotNull String> spriteSheetIds;
    private final @NotNull Map<@NotNull String, @NotNull String> spriteSheetDetails;
    private final @NotNull Map<@NotNull String, @NotNull String> sounds;
    private final @NotNull List<@NotNull String> soundIds;
    private final @NotNull Map<@NotNull String, @NotNull String> palettes;
    private final @NotNull Map<@NotNull String, @NotNull String> stringTables;
    private final @NotNull Map<@NotNull String, @NotNull String> tilesets;
    private final @NotNull Map<@NotNull String, @NotNull String> fonts;
    private final @NotNull Map<@NotNull String, @NotNull String> dialogs;
    private final @NotNull Map<@NotNull String, @NotNull String> pictures;
    private final @Nullable String content;

    private PackManifest(@NotNull List<@NotNull String> animationKinds,
                         @NotNull Map<@NotNull String, @NotNull String> spriteSheets, @NotNull Map<@NotNull String, @NotNull String> spriteSheetDetails,
                         @NotNull Map<@NotNull String, @NotNull String> sounds,
                         @NotNull Map<@NotNull String, @NotNull String> palettes, @NotNull Map<@NotNull String, @NotNull String> stringTables,
                         @NotNull Map<@NotNull String, @NotNull String> tilesets, @NotNull Map<@NotNull String, @NotNull String> fonts,
                         @NotNull Map<@NotNull String, @NotNull String> dialogs, @NotNull Map<@NotNull String, @NotNull String> pictures,
                         @Nullable String content) {
        this.animationKinds = animationKinds;

        this.spriteSheets       = spriteSheets;
        this.spriteSheetIds     = List.copyOf(spriteSheets.keySet());
        this.spriteSheetDetails = spriteSheetDetails;
        this.sounds             = sounds;
        this.soundIds           = List.copyOf(sounds.keySet());
        this.palettes           = palettes;
        this.stringTables       = stringTables;
        this.tilesets           = tilesets;
        this.fonts              = fonts;
        this.dialogs            = dialogs;
        this.pictures           = pictures;
        this.content            = content;
    }

    /**
     * @return the animation kind names in index order
     */
    @NotNull List<@NotNull String> getAnimationKinds() {
        return this.animationKinds;
    }

    /**
     * @return the sprite sheet ids in index order
     */
    @NotNull List<@NotNull String> getSpriteSheetIds() {
        return this.spriteSheetIds;
    }

    /**
     * @return the sound ids in index order
     */
    @NotNull List<@NotNull String> getSoundIds() {
        return this.soundIds;
    }

    /**
     * @return the pack path of the content definitions file, or {@code null} if the pack has none
     */
    @Nullable String getContent() {
        return this.content;
    }

    /**
     * Returns the pack path of a listed sprite sheet definition.
     *
     * @param id the sprite sheet id
     * @return the pack path
     * @throws IllegalArgumentException if the id is not listed
     */
    @NotNull String getSpriteSheetPath(@NotNull String id) {
        String path = this.spriteSheets.get(id);

        if (path == null) {
            throw Exceptions.newIllegalArgumentException("The sprite sheet {0} is not listed.", id);
        }

        return path;
    }

    /**
     * Finds the pack path of a sprite sheet or detail sprite sheet definition.
     *
     * @param key the resource key
     * @return the pack path, or {@code null} if the pack lists no such sprite sheet
     */
    @Nullable String findSpriteSheet(@NotNull ResourceKey key) {
        String path = this.spriteSheets.get(key.getName());

        return path != null ? path : this.spriteSheetDetails.get(key.getName());
    }

    /**
     * Finds the pack path of a sound.
     *
     * @param key the resource key
     * @return the pack path, or {@code null} if the pack lists no such sound
     */
    @Nullable String findSound(@NotNull ResourceKey key) {
        return this.sounds.get(key.getName());
    }

    /**
     * Finds the pack path of a palette.
     *
     * @param key the resource key
     * @return the pack path, or {@code null} if the pack lists no such palette
     */
    @Nullable String findPalette(@NotNull ResourceKey key) {
        return this.palettes.get(key.getName());
    }

    /**
     * Finds the pack path of a string table.
     *
     * @param key the resource key
     * @return the pack path, or {@code null} if the pack lists no such string table
     */
    @Nullable String findStringTable(@NotNull ResourceKey key) {
        return this.stringTables.get(key.getName());
    }

    /**
     * Finds the pack path of a tileset definition.
     *
     * @param key the resource key
     * @return the pack path, or {@code null} if the pack lists no such tileset
     */
    @Nullable String findTileset(@NotNull ResourceKey key) {
        return this.tilesets.get(key.getName());
    }

    /**
     * Finds the pack path of a font definition.
     *
     * @param key the resource key
     * @return the pack path, or {@code null} if the pack lists no such font
     */
    @Nullable String findFont(@NotNull ResourceKey key) {
        return this.fonts.get(key.getName());
    }

    /**
     * Finds the pack path of a dialog definition.
     *
     * @param key the resource key
     * @return the pack path, or {@code null} if the pack lists no such dialog
     */
    @Nullable String findDialog(@NotNull ResourceKey key) {
        return this.dialogs.get(key.getName());
    }

    /**
     * Finds the pack path of a picture or picture definition.
     *
     * @param key the resource key
     * @return the pack path, or {@code null} if the pack lists no such picture
     */
    @Nullable String findPicture(@NotNull ResourceKey key) {
        return this.pictures.get(key.getName());
    }
}
