/**
 * Defines the engine graphics model: {@link org.fogofwarlabs.sc4j.engine.resource.graphics.SpriteSheet} (the frames of
 * an animated picture with optional team colour mask and shadow layers), {@link org.fogofwarlabs.sc4j.engine.resource.graphics.Palette}
 * and {@link org.fogofwarlabs.sc4j.engine.resource.graphics.TileGraphics} (tiles of one size and
 * {@link org.fogofwarlabs.sc4j.engine.resource.graphics.TileShape}); and for pictures stored as GPU textures
 * {@link org.fogofwarlabs.sc4j.engine.resource.graphics.TextureSheet}, whose named layers are
 * {@link org.fogofwarlabs.sc4j.engine.resource.graphics.CompressedTexture} blocks
 * ({@link org.fogofwarlabs.sc4j.engine.resource.graphics.CompressionFormat}) that a renderer uploads without decoding.
 * <p>
 * Pixels are stored in {@link org.fogofwarlabs.sc4j.engine.resource.graphics.PixelLayer} objects: one primitive array per
 * layer, either palette indices, 8-bit alpha values or packed RGBA colours. Every object is immutable after loading; a
 * renderer copies the pixels it uploads.
 */
package org.fogofwarlabs.sc4j.engine.resource.graphics;
