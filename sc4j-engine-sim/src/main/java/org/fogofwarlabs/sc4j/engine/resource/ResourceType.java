package org.fogofwarlabs.sc4j.engine.resource;

/**
 * Names the kinds of resources a {@link ResourceProvider} loads.
 */
public enum ResourceType {
    /**
     * A {@link org.fogofwarlabs.sc4j.engine.resource.graphics.SpriteSheet}.
     */
    SPRITE_SHEET,
    /**
     * A {@link org.fogofwarlabs.sc4j.engine.resource.graphics.TextureSheet}.
     */
    TEXTURE_SHEET,
    /**
     * A {@link org.fogofwarlabs.sc4j.engine.resource.graphics.Palette}.
     */
    PALETTE,
    /**
     * A {@link org.fogofwarlabs.sc4j.engine.resource.graphics.TileGraphics}.
     */
    TILE_GRAPHICS,
    /**
     * The {@link org.fogofwarlabs.sc4j.engine.animation.AnimationLibrary} of a provider.
     */
    ANIMATION_LIBRARY,
    /**
     * A {@link org.fogofwarlabs.sc4j.engine.resource.audio.SoundClip}.
     */
    SOUND,
    /**
     * A {@link org.fogofwarlabs.sc4j.engine.resource.text.StringTable}.
     */
    STRING_TABLE,
    /**
     * A {@link org.fogofwarlabs.sc4j.engine.resource.text.FontFace}.
     */
    FONT,
    /**
     * A {@link org.fogofwarlabs.sc4j.engine.resource.dialog.DialogDefinition}.
     */
    DIALOG,
    /**
     * A {@link org.fogofwarlabs.sc4j.engine.resource.graphics.Picture}.
     */
    PICTURE
}
