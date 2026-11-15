package org.fogofwarlabs.sc4j.engine.client.render.gl;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.base.exception.IgnoreException;
import org.fogofwarlabs.sc4j.engine.client.render.GraphicsDevice;
import org.fogofwarlabs.sc4j.engine.client.render.SpriteBatch;
import org.fogofwarlabs.sc4j.engine.client.render.WorldViewport;
import org.fogofwarlabs.sc4j.engine.resource.graphics.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.MemoryUtil;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.IdentityHashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL33C.*;

/**
 * Draws sprite sheet frames on an OpenGL 3.3 context: the engine default {@link SpriteBatch}. Each frame's layers are uploaded
 * once, on its first draw, as textures (indices and masks as one-byte textures, colours as RGBA); the palette is a 256 by 1
 * texture. One shader colours a sprite: an indexed pixel takes its palette colour (palette alpha {@code 0} is transparent), an
 * RGBA pixel its own colour, and the team colour mask mixes the team colour in by the mask value; the shadow layer is drawn as
 * black with the shadow value as alpha before the sprite.
 */
public final class GlSpriteBatch implements SpriteBatch {
    /**
     * The renderer name of the shader failure messages.
     */
    private static final @NotNull String SHADER_PROGRAM_NAME = "Sprite";
    /**
     * The vertex layout: screen position and texture coordinates.
     */
    private static final int POSITION_ATTRIBUTE = 0;
    private static final int COORDINATES_ATTRIBUTE = 1;
    private static final int POSITION_COMPONENTS = 2;
    private static final int COORDINATES_COMPONENTS = 2;
    private static final int VERTEX_FLOATS = POSITION_COMPONENTS + COORDINATES_COMPONENTS;
    private static final int QUAD_FLOATS = GraphicsDevice.QUAD_VERTICES * VERTEX_FLOATS;
    /**
     * The texture units of the colour layer, the palette and the team colour mask.
     */
    private static final int COLOR_UNIT = 0;
    private static final int PALETTE_UNIT = 1;
    private static final int MASK_UNIT = 2;
    /**
     * The uniform names of the shaders.
     */
    private static final @NotNull String FRAMEBUFFER_SIZE_UNIFORM = "framebufferSize";
    private static final @NotNull String COLOR_TEXTURE_UNIFORM = "colorTexture";
    private static final @NotNull String PALETTE_TEXTURE_UNIFORM = "paletteTexture";
    private static final @NotNull String MASK_TEXTURE_UNIFORM = "maskTexture";
    private static final @NotNull String COLOR_MODE_UNIFORM = "colorMode";
    private static final @NotNull String MASK_PRESENT_UNIFORM = "maskPresent";
    private static final @NotNull String TEAM_COLOR_UNIFORM = "teamColor";
    /**
     * The colour modes of the fragment shader: palette indices, RGBA colours and the black shadow.
     */
    private static final int INDEXED_MODE = 0;
    private static final int RGBA_MODE = 1;
    private static final int SHADOW_MODE = 2;
    /**
     * The colours of the palette texture.
     */
    private static final int PALETTE_SIZE = 256;
    /**
     * The bytes of an RGBA texel and the offsets of its channels.
     */
    private static final int RGBA_BYTES = 4;
    private static final int RGBA_GREEN = 1;
    private static final int RGBA_BLUE = 2;
    private static final int RGBA_ALPHA = 3;
    /**
     * The bit shifts of the channels of a packed {@code 0xAARRGGBB} colour, and the mask of one channel.
     */
    private static final int ALPHA_SHIFT = 24;
    private static final int RED_SHIFT = 16;
    private static final int GREEN_SHIFT = 8;
    private static final int CHANNEL_MASK = 0xFF;
    private static final float CHANNEL_MAXIMUM = 255.0f;
    private static final @NotNull String VERTEX_SHADER = """
                                                         #version 330 core
                                                         layout(location = 0) in vec2 position;
                                                         layout(location = 1) in vec2 coordinates;
                                                         uniform vec2 framebufferSize;
                                                         out vec2 sampleCoordinates;
                                                         void main() {
                                                             gl_Position = vec4(position.x * 2.0 / framebufferSize.x - 1.0,
                                                                                1.0 - position.y * 2.0 / framebufferSize.y, 0.0, 1.0);
                                                             sampleCoordinates = coordinates;
                                                         }
                                                         """;
    private static final @NotNull String FRAGMENT_SHADER = """
                                                           #version 330 core
                                                           in vec2 sampleCoordinates;
                                                           uniform sampler2D colorTexture;
                                                           uniform sampler2D paletteTexture;
                                                           uniform sampler2D maskTexture;
                                                           uniform int colorMode;
                                                           uniform int maskPresent;
                                                           uniform vec3 teamColor;
                                                           out vec4 resultColor;
                                                           void main() {
                                                               vec4 color;
                                                               if (colorMode == 2) {
                                                                   color = vec4(0.0, 0.0, 0.0, texture(colorTexture, sampleCoordinates).r);
                                                               } else if (colorMode == 0) {
                                                                   int index = int(texture(colorTexture, sampleCoordinates).r * 255.0 + 0.5);
                                                                   color = texelFetch(paletteTexture, ivec2(index, 0), 0);
                                                               } else {
                                                                   color = texture(colorTexture, sampleCoordinates);
                                                               }
                                                               if (maskPresent != 0 && colorMode != 2) {
                                                                   color.rgb = mix(color.rgb, teamColor, texture(maskTexture, sampleCoordinates).r);
                                                               }
                                                               if (color.a <= 0.0) {
                                                                   discard;
                                                               }
                                                               resultColor = color;
                                                           }
                                                           """;

    /**
     * Holds the uploaded textures of one sprite sheet frame; a missing layer has the texture name {@code 0}.
     */
    private static final class FrameTextures {
        private final int color;
        private final int mask;
        private final int shadow;

        private FrameTextures(int color, int mask, int shadow) {
            this.color  = color;
            this.mask   = mask;
            this.shadow = shadow;
        }
    }

    private static @NotNull ByteBuffer copyBytes(@NotNull PixelLayer layer, @NotNull SpriteFrame frame) {
        int length = frame.getWidth() * frame.getHeight();
        byte[] values = new byte[length];
        ByteBuffer pixels = MemoryUtil.memAlloc(length);

        layer.copyBytes(frame.getPixelOffset(), values, 0, length);
        pixels.put(values).flip();

        return pixels;
    }

    private static @NotNull ByteBuffer copyColors(@NotNull PixelLayer layer, @NotNull SpriteFrame frame) {
        int length = frame.getWidth() * frame.getHeight();
        int[] values = new int[length];
        ByteBuffer pixels = MemoryUtil.memAlloc(length * RGBA_BYTES);

        layer.copyValues(frame.getPixelOffset(), values, 0, length);

        for (int value : values) {
            pixels.put((byte) (value >>> RED_SHIFT)).put((byte) (value >>> GREEN_SHIFT)).put((byte) value).put((byte) (value >>> ALPHA_SHIFT));
        }

        pixels.flip();

        return pixels;
    }

    private static int createTexture(int width, int height, int internalFormat, int format, @NotNull ByteBuffer pixels) {
        int texture = glGenTextures();

        glBindTexture(GL_TEXTURE_2D, texture);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        glTexImage2D(GL_TEXTURE_2D, 0, internalFormat, width, height, 0, format, GL_UNSIGNED_BYTE, pixels);

        return texture;
    }


    private final int program;
    private final int framebufferUniform;
    private final int modeUniform;
    private final int maskPresentUniform;
    private final int teamColorUniform;

    private final int vertexArray;
    private final int vertexBuffer;
    private final @NotNull FloatBuffer vertices;
    private final @NotNull ByteBuffer paletteTexels;
    private final int paletteTexture;

    private final @NotNull Map<@NotNull SpriteSheet, @Nullable FrameTextures @NotNull []> frames;
    private @Nullable WorldViewport viewport;

    private boolean closed;

    /**
     * Creates a {@link GlSpriteBatch} instance on the current OpenGL 3.3 context with a palette of transparent colours.
     */
    public GlSpriteBatch() {
        this.program            = GlShaderProgramUtils.linkProgram(SHADER_PROGRAM_NAME, VERTEX_SHADER, FRAGMENT_SHADER);
        this.framebufferUniform = glGetUniformLocation(this.program, FRAMEBUFFER_SIZE_UNIFORM);
        this.modeUniform        = glGetUniformLocation(this.program, COLOR_MODE_UNIFORM);
        this.maskPresentUniform = glGetUniformLocation(this.program, MASK_PRESENT_UNIFORM);
        this.teamColorUniform   = glGetUniformLocation(this.program, TEAM_COLOR_UNIFORM);

        glUseProgram(this.program);
        glUniform1i(glGetUniformLocation(this.program, COLOR_TEXTURE_UNIFORM), COLOR_UNIT);
        glUniform1i(glGetUniformLocation(this.program, PALETTE_TEXTURE_UNIFORM), PALETTE_UNIT);
        glUniform1i(glGetUniformLocation(this.program, MASK_TEXTURE_UNIFORM), MASK_UNIT);
        glUseProgram(0);

        this.vertexArray   = glGenVertexArrays();
        this.vertexBuffer  = glGenBuffers();
        this.vertices      = MemoryUtil.memAllocFloat(QUAD_FLOATS);
        this.paletteTexels = MemoryUtil.memCalloc(PALETTE_SIZE * RGBA_BYTES);

        glBindVertexArray(this.vertexArray);
        glBindBuffer(GL_ARRAY_BUFFER, this.vertexBuffer);
        glBufferData(GL_ARRAY_BUFFER, (long) QUAD_FLOATS * Float.BYTES, GL_STREAM_DRAW);
        glEnableVertexAttribArray(POSITION_ATTRIBUTE);
        glEnableVertexAttribArray(COORDINATES_ATTRIBUTE);
        glVertexAttribPointer(POSITION_ATTRIBUTE, POSITION_COMPONENTS, GL_FLOAT, false, VERTEX_FLOATS * Float.BYTES, 0L);
        glVertexAttribPointer(COORDINATES_ATTRIBUTE, COORDINATES_COMPONENTS, GL_FLOAT, false,
                              VERTEX_FLOATS * Float.BYTES, (long) POSITION_COMPONENTS * Float.BYTES);
        glBindVertexArray(0);

        glActiveTexture(GL_TEXTURE0 + PALETTE_UNIT);
        this.paletteTexture = createTexture(PALETTE_SIZE, 1, GL_RGBA8, GL_RGBA, this.paletteTexels);
        glActiveTexture(GL_TEXTURE0);

        this.frames   = new IdentityHashMap<>();
        this.viewport = null;

        this.closed = false;
    }

    /**
     * Begins drawing a frame into the window.
     *
     * @param viewport the frame's viewport
     * @throws IllegalStateException if the batch is closed
     */
    @Override
    public void begin(@NotNull WorldViewport viewport) {
        if (this.closed) {
            throw Exceptions.newIllegalStateException("The sprite batch is closed.");
        }

        this.viewport = viewport;

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, viewport.getWidth(), viewport.getHeight());
        glDisable(GL_DEPTH_TEST);
        glDisable(GL_SCISSOR_TEST);
        glEnable(GL_BLEND);
        glBlendFuncSeparate(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA, GL_ONE, GL_ONE_MINUS_SRC_ALPHA);
        glUseProgram(this.program);
        glUniform2f(this.framebufferUniform, viewport.getWidth(), viewport.getHeight());
        glBindVertexArray(this.vertexArray);
        glBindBuffer(GL_ARRAY_BUFFER, this.vertexBuffer);
    }

    /**
     * Sets the palette of indexed colour layers; it applies to the following draws.
     *
     * @param palette the palette, at most 256 colours
     * @throws IllegalArgumentException if the palette has more than 256 colours
     */
    @Override
    public void setPalette(@NotNull Palette palette) {
        if (palette.getSize() > PALETTE_SIZE) {
            throw Exceptions.newIllegalArgumentException("A palette has at most {0} colours, not {1}.", PALETTE_SIZE, palette.getSize());
        }

        this.paletteTexels.clear();

        for (int index = 0; index < PALETTE_SIZE; index++) {
            int color = index < palette.getSize() ? palette.getColor(index) : 0;
            int offset = index * RGBA_BYTES;

            this.paletteTexels.put(offset, (byte) (color >>> RED_SHIFT));
            this.paletteTexels.put(offset + RGBA_GREEN, (byte) (color >>> GREEN_SHIFT));
            this.paletteTexels.put(offset + RGBA_BLUE, (byte) color);
            this.paletteTexels.put(offset + RGBA_ALPHA, (byte) (color >>> ALPHA_SHIFT));
        }

        glActiveTexture(GL_TEXTURE0 + PALETTE_UNIT);
        glBindTexture(GL_TEXTURE_2D, this.paletteTexture);
        glPixelStorei(GL_UNPACK_ALIGNMENT, 1);
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, PALETTE_SIZE, 1, GL_RGBA, GL_UNSIGNED_BYTE, this.paletteTexels);
        glActiveTexture(GL_TEXTURE0);
    }

    /**
     * Draws one frame of a sprite sheet.
     *
     * @param sheet      the sprite sheet
     * @param frameIndex the frame of the sheet
     * @param x          the world x the anchor is placed on
     * @param y          the world y the anchor is placed on
     * @param flipped    whether the frame is mirrored horizontally around the anchor
     * @param teamColor  the owner's team colour, packed {@code 0xRRGGBB}
     * @throws IllegalStateException if the batch has not begun
     */
    @Override
    public void draw(@NotNull SpriteSheet sheet, int frameIndex, int x, int y, boolean flipped, int teamColor) {
        WorldViewport current = this.viewport;

        if (current == null) {
            throw Exceptions.newIllegalStateException("The sprite batch has not begun.");
        }

        SpriteFrame frame = sheet.getFrame(frameIndex);

        if (frame.getWidth() == 0 || frame.getHeight() == 0) {
            return;
        }

        FrameTextures textures = this.getTextures(sheet, frameIndex, frame);
        float anchorX = current.getProjection().toProjectedX(x, y);
        float anchorY = current.getProjection().toProjectedY(x, y);
        float left = flipped ? anchorX - (sheet.getWidth() - sheet.getAnchorX()) + sheet.getWidth() - frame.getX() - frame.getWidth()
                             : anchorX - sheet.getAnchorX() + frame.getX();
        float top = anchorY - sheet.getAnchorY() + frame.getY();
        float screenLeft = current.toScreenX(left);
        float screenTop = current.toScreenY(top);
        float screenRight = current.toScreenX(left + frame.getWidth());
        float screenBottom = current.toScreenY(top + frame.getHeight());

        this.fillQuad(screenLeft, screenTop, screenRight, screenBottom, flipped);

        if (textures.shadow != 0) {
            this.drawQuad(textures.shadow, SHADOW_MODE, 0, teamColor);
        }

        this.drawQuad(textures.color, sheet.getColor().getFormat() == PixelFormat.INDEXED_8 ? INDEXED_MODE : RGBA_MODE, textures.mask, teamColor);
    }

    private @NotNull FrameTextures getTextures(@NotNull SpriteSheet sheet, int frameIndex, @NotNull SpriteFrame frame) {
        FrameTextures[] sheetFrames = this.frames.computeIfAbsent(sheet, key -> new FrameTextures[key.getFrameCount()]);
        FrameTextures textures = sheetFrames[frameIndex];

        if (textures != null) {
            return textures;
        }

        PixelLayer color = sheet.getColor();
        PixelLayer mask = sheet.getTeamMask();
        PixelLayer shadow = sheet.getShadow();

        glActiveTexture(GL_TEXTURE0);

        int colorTexture = this.uploadLayer(color, frame);
        int maskTexture = mask == null ? 0 : this.uploadLayer(mask, frame);
        int shadowTexture = shadow == null ? 0 : this.uploadLayer(shadow, frame);

        textures                = new FrameTextures(colorTexture, maskTexture, shadowTexture);
        sheetFrames[frameIndex] = textures;

        return textures;
    }

    private int uploadLayer(@NotNull PixelLayer layer, @NotNull SpriteFrame frame) {
        boolean bytes = layer.getFormat().isByteStored();
        ByteBuffer pixels = bytes ? copyBytes(layer, frame) : copyColors(layer, frame);

        try {
            return createTexture(frame.getWidth(), frame.getHeight(), bytes ? GL_R8 : GL_RGBA8, bytes ? GL_RED : GL_RGBA, pixels);
        } finally {
            MemoryUtil.memFree(pixels);
        }
    }

    private void fillQuad(float left, float top, float right, float bottom, boolean flipped) {
        float textureLeft = flipped ? 1 : 0;
        float textureRight = flipped ? 0 : 1;

        this.vertices.clear();
        this.vertices.put(left).put(top).put(textureLeft).put(0);
        this.vertices.put(right).put(top).put(textureRight).put(0);
        this.vertices.put(right).put(bottom).put(textureRight).put(1);
        this.vertices.put(left).put(top).put(textureLeft).put(0);
        this.vertices.put(right).put(bottom).put(textureRight).put(1);
        this.vertices.put(left).put(bottom).put(textureLeft).put(1);
        this.vertices.flip();
        glBufferSubData(GL_ARRAY_BUFFER, 0, this.vertices);
    }

    private void drawQuad(int colorTexture, int mode, int maskTexture, int teamColor) {
        glActiveTexture(GL_TEXTURE0 + MASK_UNIT);
        glBindTexture(GL_TEXTURE_2D, maskTexture);
        glActiveTexture(GL_TEXTURE0 + PALETTE_UNIT);
        glBindTexture(GL_TEXTURE_2D, this.paletteTexture);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, colorTexture);
        glUniform1i(this.modeUniform, mode);
        glUniform1i(this.maskPresentUniform, maskTexture != 0 ? 1 : 0);
        glUniform3f(this.teamColorUniform,
                    (teamColor >>> RED_SHIFT & CHANNEL_MASK) / CHANNEL_MAXIMUM,
                    (teamColor >>> GREEN_SHIFT & CHANNEL_MASK) / CHANNEL_MAXIMUM,
                    (teamColor & CHANNEL_MASK) / CHANNEL_MAXIMUM);
        glDrawArrays(GL_TRIANGLES, 0, GraphicsDevice.QUAD_VERTICES);
    }

    /**
     * Submits the pending drawing.
     */
    @Override
    public void end() {
        this.viewport = null;

        glBindVertexArray(0);
        glUseProgram(0);
    }

    /**
     * Disposes resources: releases the uploaded frames and the drawing objects. Closing again does nothing.
     */
    @Override
    public void close() {
        if (this.closed) {
            return;
        }

        this.closed = true;

        for (FrameTextures[] sheetFrames : this.frames.values()) {
            for (FrameTextures textures : sheetFrames) {
                if (textures != null) {
                    IgnoreException.execute(
                            () -> glDeleteTextures(new int[]{textures.color, textures.mask, textures.shadow})
                    );
                }
            }
        }

        this.frames.clear();

        IgnoreException.execute(
                () -> glDeleteTextures(this.paletteTexture),
                () -> MemoryUtil.memFree(this.paletteTexels),
                () -> MemoryUtil.memFree(this.vertices),
                () -> glDeleteBuffers(this.vertexBuffer),
                () -> glDeleteVertexArrays(this.vertexArray),
                () -> glDeleteProgram(this.program)
        );
    }
}
