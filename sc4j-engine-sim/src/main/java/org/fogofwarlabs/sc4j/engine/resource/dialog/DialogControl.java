package org.fogofwarlabs.sc4j.engine.resource.dialog;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.resource.ResourceKey;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Describes one control of a dialog: its id, kind, bounds relative to its parent, text, picture, the attributes the mod
 * defines for it (kept in their given order) and its child controls.
 */
public final class DialogControl {
    private final int id;
    private final @NotNull ControlKind kind;

    private final int x;
    private final int y;
    private final int width;
    private final int height;

    private final @Nullable String text;
    private final @NotNull TextAlignment textAlignment;
    private final @Nullable ResourceKey image;

    private final @NotNull Map<@NotNull String, @NotNull String> attributes;
    private final @NotNull List<@NotNull DialogControl> children;

    /**
     * Creates a {@link DialogControl} instance.
     *
     * @param id            the control id, unique in its dialog
     * @param kind          the control kind
     * @param x             the left edge relative to the parent, in pixels
     * @param y             the top edge relative to the parent, in pixels
     * @param width         the width in pixels, at least {@code 0}
     * @param height        the height in pixels, at least {@code 0}
     * @param text          the text, or {@code null}
     * @param textAlignment the horizontal alignment of the text
     * @param image         the key of the picture, or {@code null}
     * @param attributes    the mod's attributes in their order, copied
     * @param children      the child controls in drawing order, copied
     * @throws IllegalArgumentException if a size is negative
     */
    public DialogControl(int id, @NotNull ControlKind kind,
                         int x, int y, int width, int height,
                         @Nullable String text, @NotNull TextAlignment textAlignment, @Nullable ResourceKey image,
                         @NotNull Map<@NotNull String, @NotNull String> attributes, @NotNull List<@NotNull DialogControl> children) {
        if (width < 0 || height < 0) {
            throw Exceptions.newIllegalArgumentException("A control has no negative size: {0}x{1}", width, height);
        }

        this.id   = id;
        this.kind = kind;

        this.x      = x;
        this.y      = y;
        this.width  = width;
        this.height = height;

        this.text          = text;
        this.textAlignment = textAlignment;
        this.image         = image;

        this.attributes = Collections.unmodifiableMap(new LinkedHashMap<>(attributes));
        this.children   = List.copyOf(children);
    }

    /**
     * @return the control id, unique in its dialog
     */
    public int getId() {
        return this.id;
    }

    /**
     * @return the control kind
     */
    public @NotNull ControlKind getKind() {
        return this.kind;
    }

    /**
     * @return the left edge relative to the parent, in pixels
     */
    public int getX() {
        return this.x;
    }

    /**
     * @return the top edge relative to the parent, in pixels
     */
    public int getY() {
        return this.y;
    }

    /**
     * @return the width in pixels
     */
    public int getWidth() {
        return this.width;
    }

    /**
     * @return the height in pixels
     */
    public int getHeight() {
        return this.height;
    }

    /**
     * @return the text, or {@code null} if the control has none
     */
    public @Nullable String getText() {
        return this.text;
    }

    /**
     * @return the horizontal alignment of the text
     */
    public @NotNull TextAlignment getTextAlignment() {
        return this.textAlignment;
    }

    /**
     * @return the key of the picture, or {@code null} if the control has none
     */
    public @Nullable ResourceKey getImage() {
        return this.image;
    }

    /**
     * @return the mod's attributes in their given order, unmodifiable
     */
    public @NotNull Map<@NotNull String, @NotNull String> getAttributes() {
        return this.attributes;
    }

    /**
     * @return the child controls in drawing order, unmodifiable
     */
    public @NotNull List<@NotNull DialogControl> getChildren() {
        return this.children;
    }
}
