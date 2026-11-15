package org.fogofwarlabs.sc4j.engine.resource;

import org.fogofwarlabs.sc4j.engine.animation.AnimationLibrary;
import org.fogofwarlabs.sc4j.engine.resource.audio.SoundClip;
import org.fogofwarlabs.sc4j.engine.resource.dialog.DialogDefinition;
import org.fogofwarlabs.sc4j.engine.resource.graphics.*;
import org.fogofwarlabs.sc4j.engine.resource.text.FontFace;
import org.fogofwarlabs.sc4j.engine.resource.text.StringTable;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Loads the resources of a mod as engine model objects. A mod registers one provider
 * ({@code ModRegistration#registerResourceProvider} of the mod API); the engine, its renderer and the
 * mod's scenes read every picture, sound, text, dialog and animation through it.
 * <p>
 * Contract of every load method:
 * <ul>
 *     <li>It runs outside the simulation, on one thread at a time; an implementation need not be thread-safe.</li>
 *     <li>It returns an immutable model object, never {@code null}. Loading the same key twice returns equal objects;
 *     whether it returns the same instance (a cache) is up to the provider.</li>
 *     <li>A key the provider does not have, or a resource type the provider does not offer at all, throws
 *     {@link ResourceNotFoundException}. A resource that exists but cannot be read or is malformed throws another
 *     {@link IOException} that names the resource.</li>
 * </ul>
 * The simulation never calls a provider: what a game needs (the animation library) is loaded before the game is created
 * and handed to its systems.
 */
public interface ResourceProvider {
    /**
     * Loads a sprite sheet.
     *
     * @param key the key of the sprite sheet
     * @return the sprite sheet
     * @throws ResourceNotFoundException if the provider has no sprite sheet under the key
     * @throws IOException               if the sprite sheet cannot be read
     */
    @NotNull SpriteSheet loadSpriteSheet(@NotNull ResourceKey key) throws IOException;

    /**
     * Loads a texture sheet: an animated picture stored as block-compressed GPU textures.
     *
     * @param key the key of the texture sheet
     * @return the texture sheet
     * @throws ResourceNotFoundException if the provider has no texture sheet under the key
     * @throws IOException               if the texture sheet cannot be read
     */
    @NotNull TextureSheet loadTextureSheet(@NotNull ResourceKey key) throws IOException;

    /**
     * Loads a palette.
     *
     * @param key the key of the palette
     * @return the palette
     * @throws ResourceNotFoundException if the provider has no palette under the key
     * @throws IOException               if the palette cannot be read
     */
    @NotNull Palette loadPalette(@NotNull ResourceKey key) throws IOException;

    /**
     * Loads the graphics of a tileset.
     *
     * @param key the key of the tileset
     * @return the tile graphics
     * @throws ResourceNotFoundException if the provider has no tileset under the key
     * @throws IOException               if the tile graphics cannot be read
     */
    @NotNull TileGraphics loadTileGraphics(@NotNull ResourceKey key) throws IOException;

    /**
     * Loads the animation library: every animation set and program of the provider.
     *
     * @return the animation library
     * @throws ResourceNotFoundException if the provider has no animations
     * @throws IOException               if the animations cannot be read
     */
    @NotNull AnimationLibrary loadAnimations() throws IOException;

    /**
     * Loads a sound clip.
     *
     * @param key the key of the sound
     * @return the sound clip
     * @throws ResourceNotFoundException if the provider has no sound under the key
     * @throws IOException               if the sound cannot be read
     */
    @NotNull SoundClip loadSound(@NotNull ResourceKey key) throws IOException;

    /**
     * Loads a string table.
     *
     * @param key the key of the string table
     * @return the string table
     * @throws ResourceNotFoundException if the provider has no string table under the key
     * @throws IOException               if the string table cannot be read
     */
    @NotNull StringTable loadStringTable(@NotNull ResourceKey key) throws IOException;

    /**
     * Loads a font face.
     *
     * @param key the key of the font
     * @return the font face
     * @throws ResourceNotFoundException if the provider has no font under the key
     * @throws IOException               if the font cannot be read
     */
    @NotNull FontFace loadFont(@NotNull ResourceKey key) throws IOException;

    /**
     * Loads a dialog definition.
     *
     * @param key the key of the dialog
     * @return the dialog definition
     * @throws ResourceNotFoundException if the provider has no dialog under the key
     * @throws IOException               if the dialog cannot be read
     */
    @NotNull DialogDefinition loadDialog(@NotNull ResourceKey key) throws IOException;

    /**
     * Loads a still picture of the interface: a screen, an emblem, a portrait, an icon, a skin piece or a cursor.
     * <p>
     * The default offers no pictures: every key throws {@link ResourceNotFoundException}.
     *
     * @param key the key of the picture
     * @return the picture
     * @throws ResourceNotFoundException if the provider has no picture under the key
     * @throws IOException               if the picture cannot be read
     */
    default @NotNull Picture loadPicture(@NotNull ResourceKey key) throws IOException {
        throw ResourceNotFoundException.create(ResourceType.PICTURE, key);
    }
}
