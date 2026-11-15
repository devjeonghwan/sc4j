package org.fogofwarlabs.sc4j.engine.client.skin;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.client.command.*;
import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.client.render.PictureRenderer;
import org.fogofwarlabs.sc4j.engine.client.render.PictureScaling;
import org.fogofwarlabs.sc4j.engine.content.economy.ResourceKind;
import org.fogofwarlabs.sc4j.engine.content.faction.Faction;
import org.fogofwarlabs.sc4j.engine.content.type.OrderType;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.resource.graphics.Picture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds the look of the engine default HUD blocks ({@link org.fogofwarlabs.sc4j.engine.client.hud.HudBlock}): the box of each block, the plate of the
 * bottom console behind its blocks, the box of the queue panel's region in the console, the minimap's border, the slot of a selected unit and its
 * size, the command buttons in their states, the box of a tooltip and of a HUD dialog (for example the network waiting dialog) with their padding,
 * and the pictures the blocks show: an icon per order type and per unit type for the command buttons (a train or build button shows the made unit
 * type's icon, and so does a queue entry), the icon of the cancel button, a portrait per unit type with the owner's team colour for the selection
 * panel, and an icon per resource kind and per faction's supply for the resource bar. A block shows text where the skin has no picture.
 * <p>
 * The default skin ({@link #DEFAULT}) is the engine default look: translucent black blocks on a translucent black console plate, a grey minimap
 * border, team colour squares as slots, flat buttons, tooltips and dialogs with a light outline and texts only.
 */
public final class HudSkin {
    /**
     * The default size of a selected unit's slot in framebuffer pixels.
     */
    public static final float DEFAULT_SLOT_SIZE = 32;
    /**
     * The default padding between a tooltip's box and its text in logical units.
     */
    public static final float DEFAULT_TOOLTIP_PADDING = 4;
    /**
     * The default padding between a HUD dialog's box and its content in logical units.
     */
    public static final float DEFAULT_DIALOG_PADDING = 8;

    /**
     * The engine default look.
     */
    public static final @NotNull HudSkin DEFAULT = builder().build();

    private static final int BLOCK_COLOR = 0x000000A0;
    private static final int BUTTON_COLOR = 0x303848FF;
    private static final int PENDING_BUTTON_COLOR = 0x607030FF;
    private static final int BUTTON_OUTLINE_COLOR = 0x8090A0FF;
    private static final int MINIMAP_BORDER_COLOR = 0x808080FF;
    private static final int TOOLTIP_COLOR = 0x000000D0;
    private static final float OUTLINE_THICKNESS = 1;

    /**
     * Builds {@link HudSkin} instances; every part starts with the engine default look and no pictures.
     */
    public static final class Builder {
        private final @Nullable PictureRenderer renderer;

        private @NotNull BoxStyle resourceBarBackground;
        private @NotNull BoxStyle consoleBackground;
        private @NotNull BoxStyle minimapBackground;
        private @NotNull BoxStyle minimapBorder;
        private @NotNull BoxStyle selectionPanelBackground;
        private @Nullable BoxStyle selectionSlot;
        private float slotSize;
        private @NotNull BoxStyle commandCardBackground;
        private @NotNull BoxStyle queuePanelBackground;
        private @NotNull ButtonStyle commandButton;
        private @NotNull BoxStyle tooltipBackground;
        private float tooltipPadding;
        private @NotNull BoxStyle dialogBackground;
        private float dialogPadding;

        private final @NotNull Map<@NotNull OrderType, @NotNull Picture> orderIcons;
        private final @NotNull Map<@NotNull UnitType, @NotNull Picture> unitIcons;
        private final @NotNull Map<@NotNull UnitType, @NotNull Picture> portraits;
        private final @NotNull Map<@NotNull ResourceKind, @NotNull Picture> resourceIcons;
        private final @NotNull Map<@NotNull Faction, @NotNull Picture> supplyIcons;
        private @Nullable Picture cancelIcon;
        private @Nullable Picture rallyIcon;

        private Builder(@Nullable PictureRenderer renderer) {
            BoxStyle button = new ColorBoxStyle(BUTTON_COLOR, BUTTON_OUTLINE_COLOR, OUTLINE_THICKNESS);
            BoxStyle outlinedBox = new ColorBoxStyle(TOOLTIP_COLOR, BUTTON_OUTLINE_COLOR, OUTLINE_THICKNESS);

            this.renderer = renderer;

            this.resourceBarBackground    = new ColorBoxStyle(BLOCK_COLOR);
            this.consoleBackground        = new ColorBoxStyle(BLOCK_COLOR);
            this.minimapBackground        = ColorBoxStyle.NONE;
            this.minimapBorder            = new ColorBoxStyle(0, MINIMAP_BORDER_COLOR, OUTLINE_THICKNESS);
            this.selectionPanelBackground = new ColorBoxStyle(BLOCK_COLOR);
            this.selectionSlot            = null;
            this.slotSize                 = DEFAULT_SLOT_SIZE;
            this.commandCardBackground    = new ColorBoxStyle(BLOCK_COLOR);
            this.queuePanelBackground     = new ColorBoxStyle(BLOCK_COLOR);
            this.commandButton            = new ButtonStyle(button, button,
                                                            new ColorBoxStyle(PENDING_BUTTON_COLOR, BUTTON_OUTLINE_COLOR, OUTLINE_THICKNESS),
                                                            button);
            this.tooltipBackground        = outlinedBox;
            this.tooltipPadding           = DEFAULT_TOOLTIP_PADDING;
            this.dialogBackground         = outlinedBox;
            this.dialogPadding            = DEFAULT_DIALOG_PADDING;

            this.orderIcons    = new HashMap<>();
            this.unitIcons     = new HashMap<>();
            this.portraits     = new HashMap<>();
            this.resourceIcons = new HashMap<>();
            this.supplyIcons   = new HashMap<>();
            this.cancelIcon    = null;
            this.rallyIcon     = null;
        }

        /**
         * Sets the box of the resource bar.
         *
         * @param style the box
         * @return this instance for chaining
         */
        public @NotNull Builder setResourceBarBackground(@NotNull BoxStyle style) {
            this.resourceBarBackground = style;

            return this;
        }

        /**
         * Sets the plate of the bottom console: the box over the console's whole rectangle, drawn behind its blocks and seen between them.
         *
         * @param style the box
         * @return this instance for chaining
         */
        public @NotNull Builder setConsoleBackground(@NotNull BoxStyle style) {
            this.consoleBackground = style;

            return this;
        }

        /**
         * Sets the box drawn under the minimap's terrain, seen where the map does not cover the block (the corners of an isometric map).
         *
         * @param style the box
         * @return this instance for chaining
         */
        public @NotNull Builder setMinimapBackground(@NotNull BoxStyle style) {
            this.minimapBackground = style;

            return this;
        }

        /**
         * Sets the box drawn over the minimap, its border.
         *
         * @param style the box
         * @return this instance for chaining
         */
        public @NotNull Builder setMinimapBorder(@NotNull BoxStyle style) {
            this.minimapBorder = style;

            return this;
        }

        /**
         * Sets the box of the selection panel.
         *
         * @param style the box
         * @return this instance for chaining
         */
        public @NotNull Builder setSelectionPanelBackground(@NotNull BoxStyle style) {
            this.selectionPanelBackground = style;

            return this;
        }

        /**
         * Sets the slot of a selected unit, drawn under its portrait instead of the team colour square, and the slot size.
         *
         * @param style the box of the slot
         * @param size  the side of the square slot in framebuffer pixels
         * @return this instance for chaining
         * @throws IllegalArgumentException if the size is not positive
         */
        public @NotNull Builder setSelectionSlot(@NotNull BoxStyle style, float size) {
            if (!(size > 0)) {
                throw Exceptions.newIllegalArgumentException("Invalid slot size {0}", size);
            }

            this.selectionSlot = style;
            this.slotSize      = size;

            return this;
        }

        /**
         * Sets the box of the command card.
         *
         * @param style the box
         * @return this instance for chaining
         */
        public @NotNull Builder setCommandCardBackground(@NotNull BoxStyle style) {
            this.commandCardBackground = style;

            return this;
        }

        /**
         * Sets the box of the console's region for a queue panel, drawn under the panel.
         *
         * @param style the box
         * @return this instance for chaining
         */
        public @NotNull Builder setQueuePanelBackground(@NotNull BoxStyle style) {
            this.queuePanelBackground = style;

            return this;
        }

        /**
         * Sets the boxes of the command buttons; a button whose targeted action waits is drawn pressed.
         *
         * @param style the button boxes
         * @return this instance for chaining
         */
        public @NotNull Builder setCommandButton(@NotNull ButtonStyle style) {
            this.commandButton = style;

            return this;
        }

        /**
         * Sets the box of a tooltip ({@link org.fogofwarlabs.sc4j.engine.client.hud.Tooltip}) and the padding between the box and its text.
         *
         * @param style   the box
         * @param padding the padding in logical units, at least the box's border so the text stands inside the border
         * @return this instance for chaining
         * @throws IllegalArgumentException if the padding is negative
         */
        public @NotNull Builder setTooltip(@NotNull BoxStyle style, float padding) {
            if (!(padding >= 0)) {
                throw Exceptions.newIllegalArgumentException("Invalid tooltip padding {0}", padding);
            }

            this.tooltipBackground = style;
            this.tooltipPadding    = padding;

            return this;
        }

        /**
         * Sets the box of a HUD dialog, for example the network waiting dialog, and the padding between the box and its content.
         *
         * @param style   the box
         * @param padding the padding in logical units, at least the box's border so the content stands inside the border
         * @return this instance for chaining
         * @throws IllegalArgumentException if the padding is negative
         */
        public @NotNull Builder setDialog(@NotNull BoxStyle style, float padding) {
            if (!(padding >= 0)) {
                throw Exceptions.newIllegalArgumentException("Invalid dialog padding {0}", padding);
            }

            this.dialogBackground = style;
            this.dialogPadding    = padding;

            return this;
        }

        /**
         * Sets the icon of the command buttons of an order type.
         *
         * @param orderType the order type
         * @param icon      the icon
         * @return this instance for chaining
         */
        public @NotNull Builder setOrderIcon(@NotNull OrderType orderType, @NotNull Picture icon) {
            this.orderIcons.put(orderType, icon);

            return this;
        }

        /**
         * Sets the icon of a unit type: the picture of the command buttons that train or build it.
         *
         * @param unitType the unit type
         * @param icon     the icon
         * @return this instance for chaining
         */
        public @NotNull Builder setUnitIcon(@NotNull UnitType unitType, @NotNull Picture icon) {
            this.unitIcons.put(unitType, icon);

            return this;
        }

        /**
         * Sets the portrait of a unit type, shown with its owner's team colour in the selection panel.
         *
         * @param unitType the unit type
         * @param portrait the portrait, with a team colour mask for the team colour
         * @return this instance for chaining
         */
        public @NotNull Builder setPortrait(@NotNull UnitType unitType, @NotNull Picture portrait) {
            this.portraits.put(unitType, portrait);

            return this;
        }

        /**
         * Sets the icon of a resource kind in the resource bar.
         *
         * @param kind the resource kind
         * @param icon the icon
         * @return this instance for chaining
         */
        public @NotNull Builder setResourceIcon(@NotNull ResourceKind kind, @NotNull Picture icon) {
            this.resourceIcons.put(kind, icon);

            return this;
        }

        /**
         * Sets the icon of a faction's supply in the resource bar.
         *
         * @param faction the faction
         * @param icon    the icon
         * @return this instance for chaining
         */
        public @NotNull Builder setSupplyIcon(@NotNull Faction faction, @NotNull Picture icon) {
            this.supplyIcons.put(faction, icon);

            return this;
        }

        /**
         * Sets the icon of the cancel button ({@link CancelAction}).
         *
         * @param icon the icon
         * @return this instance for chaining
         */
        public @NotNull Builder setCancelIcon(@NotNull Picture icon) {
            this.cancelIcon = icon;

            return this;
        }

        /**
         * Sets the icon of the rally button ({@link RallyAction}).
         *
         * @param icon the icon
         * @return this instance for chaining
         */
        public @NotNull Builder setRallyIcon(@NotNull Picture icon) {
            this.rallyIcon = icon;

            return this;
        }

        /**
         * Builds a {@link HudSkin} instance from the builder's values.
         *
         * @return the built {@link HudSkin} instance
         * @throws IllegalStateException if the skin has pictures but the builder has no picture renderer
         */
        public @NotNull HudSkin build() {
            if (this.renderer == null && (!this.orderIcons.isEmpty() || !this.unitIcons.isEmpty() || !this.portraits.isEmpty() ||
                                          !this.resourceIcons.isEmpty() || !this.supplyIcons.isEmpty() || this.cancelIcon != null ||
                                          this.rallyIcon != null)) {
                throw Exceptions.newIllegalStateException("A skin with pictures needs a picture renderer.");
            }

            return new HudSkin(this);
        }
    }

    /**
     * Creates a {@link Builder} instance of a skin without pictures.
     *
     * @return the created {@link Builder} instance
     */
    public static @NotNull Builder builder() {
        return new Builder(null);
    }

    /**
     * Creates a {@link Builder} instance of a skin whose pictures a renderer draws.
     *
     * @param renderer the renderer that draws the skin's pictures, on the canvas of the HUD
     * @return the created {@link Builder} instance
     */
    public static @NotNull Builder builder(@NotNull PictureRenderer renderer) {
        return new Builder(renderer);
    }


    private final @Nullable PictureRenderer renderer;

    private final @NotNull BoxStyle resourceBarBackground;
    private final @NotNull BoxStyle consoleBackground;
    private final @NotNull BoxStyle minimapBackground;
    private final @NotNull BoxStyle minimapBorder;
    private final @NotNull BoxStyle selectionPanelBackground;
    private final @Nullable BoxStyle selectionSlot;
    private final float slotSize;
    private final @NotNull BoxStyle commandCardBackground;
    private final @NotNull BoxStyle queuePanelBackground;
    private final @NotNull ButtonStyle commandButton;
    private final @NotNull BoxStyle tooltipBackground;
    private final float tooltipPadding;
    private final @NotNull BoxStyle dialogBackground;
    private final float dialogPadding;

    private final @NotNull Map<@NotNull OrderType, @NotNull Picture> orderIcons;
    private final @NotNull Map<@NotNull UnitType, @NotNull Picture> unitIcons;
    private final @NotNull Map<@NotNull UnitType, @NotNull Picture> portraits;
    private final @NotNull Map<@NotNull ResourceKind, @NotNull Picture> resourceIcons;
    private final @NotNull Map<@NotNull Faction, @NotNull Picture> supplyIcons;
    private final @Nullable Picture cancelIcon;
    private final @Nullable Picture rallyIcon;

    private HudSkin(@NotNull Builder builder) {
        this.renderer = builder.renderer;

        this.resourceBarBackground    = builder.resourceBarBackground;
        this.consoleBackground        = builder.consoleBackground;
        this.minimapBackground        = builder.minimapBackground;
        this.minimapBorder            = builder.minimapBorder;
        this.selectionPanelBackground = builder.selectionPanelBackground;
        this.selectionSlot            = builder.selectionSlot;
        this.slotSize                 = builder.slotSize;
        this.commandCardBackground    = builder.commandCardBackground;
        this.queuePanelBackground     = builder.queuePanelBackground;
        this.commandButton            = builder.commandButton;
        this.tooltipBackground        = builder.tooltipBackground;
        this.tooltipPadding           = builder.tooltipPadding;
        this.dialogBackground         = builder.dialogBackground;
        this.dialogPadding            = builder.dialogPadding;

        this.orderIcons    = Map.copyOf(builder.orderIcons);
        this.unitIcons     = Map.copyOf(builder.unitIcons);
        this.portraits     = Map.copyOf(builder.portraits);
        this.resourceIcons = Map.copyOf(builder.resourceIcons);
        this.supplyIcons   = Map.copyOf(builder.supplyIcons);
        this.cancelIcon    = builder.cancelIcon;
        this.rallyIcon     = builder.rallyIcon;
    }

    /**
     * @return the box of the resource bar
     */
    public @NotNull BoxStyle getResourceBarBackground() {
        return this.resourceBarBackground;
    }

    /**
     * @return the plate of the bottom console
     */
    public @NotNull BoxStyle getConsoleBackground() {
        return this.consoleBackground;
    }

    /**
     * @return the box drawn under the minimap's terrain
     */
    public @NotNull BoxStyle getMinimapBackground() {
        return this.minimapBackground;
    }

    /**
     * @return the box drawn over the minimap, its border
     */
    public @NotNull BoxStyle getMinimapBorder() {
        return this.minimapBorder;
    }

    /**
     * @return the box of the selection panel
     */
    public @NotNull BoxStyle getSelectionPanelBackground() {
        return this.selectionPanelBackground;
    }

    /**
     * @return the slot of a selected unit, or {@code null} for the default team colour square
     */
    public @Nullable BoxStyle getSelectionSlot() {
        return this.selectionSlot;
    }

    /**
     * @return the side of a selected unit's square slot in framebuffer pixels
     */
    public float getSlotSize() {
        return this.slotSize;
    }

    /**
     * @return the box of the command card
     */
    public @NotNull BoxStyle getCommandCardBackground() {
        return this.commandCardBackground;
    }

    /**
     * @return the box of the console's region for a queue panel
     */
    public @NotNull BoxStyle getQueuePanelBackground() {
        return this.queuePanelBackground;
    }

    /**
     * @return the boxes of the command buttons
     */
    public @NotNull ButtonStyle getCommandButton() {
        return this.commandButton;
    }

    /**
     * @return the box of a tooltip
     */
    public @NotNull BoxStyle getTooltipBackground() {
        return this.tooltipBackground;
    }

    /**
     * @return the padding between a tooltip's box and its text in logical units
     */
    public float getTooltipPadding() {
        return this.tooltipPadding;
    }

    /**
     * @return the box of a HUD dialog
     */
    public @NotNull BoxStyle getDialogBackground() {
        return this.dialogBackground;
    }

    /**
     * @return the padding between a HUD dialog's box and its content in logical units
     */
    public float getDialogPadding() {
        return this.dialogPadding;
    }


    /**
     * Finds the icon of an order type.
     *
     * @param orderType the order type
     * @return the icon, or {@code null} if the skin has none
     */
    public @Nullable Picture findOrderIcon(@NotNull OrderType orderType) {
        return this.orderIcons.get(orderType);
    }

    /**
     * Finds the icon of a unit type, the picture of the buttons that train or build it.
     *
     * @param unitType the unit type
     * @return the icon, or {@code null} if the skin has none
     */
    public @Nullable Picture findUnitIcon(@NotNull UnitType unitType) {
        return this.unitIcons.get(unitType);
    }

    /**
     * Finds the portrait of a unit type.
     *
     * @param unitType the unit type
     * @return the portrait, or {@code null} if the skin has none
     */
    public @Nullable Picture findPortrait(@NotNull UnitType unitType) {
        return this.portraits.get(unitType);
    }

    /**
     * Finds the icon of a resource kind.
     *
     * @param kind the resource kind
     * @return the icon, or {@code null} if the skin has none
     */
    public @Nullable Picture findResourceIcon(@NotNull ResourceKind kind) {
        return this.resourceIcons.get(kind);
    }

    /**
     * Finds the icon of a faction's supply.
     *
     * @param faction the faction
     * @return the icon, or {@code null} if the skin has none
     */
    public @Nullable Picture findSupplyIcon(@NotNull Faction faction) {
        return this.supplyIcons.get(faction);
    }

    /**
     * Finds the icon of the cancel button.
     *
     * @return the icon, or {@code null} if the skin has none
     */
    public @Nullable Picture findCancelIcon() {
        return this.cancelIcon;
    }

    /**
     * Finds the icon of the rally button.
     *
     * @return the icon, or {@code null} if the skin has none
     */
    public @Nullable Picture findRallyIcon() {
        return this.rallyIcon;
    }

    /**
     * Finds the icon of a command button by its action: the order type's icon for an {@link OrderAction}, the made unit type's icon for a
     * {@link TrainAction} or a {@link ConstructAction}, the cancel icon for a {@link CancelAction}, the rally icon for a {@link RallyAction}.
     *
     * @param action the button's action
     * @return the icon, or {@code null} if the skin has none for the action
     */
    public @Nullable Picture findButtonIcon(@NotNull CommandAction action) {
        return switch (action) {
            case OrderAction order -> this.findOrderIcon(order.getOrderType());
            case TrainAction train -> this.findUnitIcon(train.getUnitType());
            case ConstructAction construct -> this.findUnitIcon(construct.getBuildingType());
            case CancelAction ignored -> this.cancelIcon;
            case RallyAction ignored -> this.rallyIcon;
            default -> null;
        };
    }

    /**
     * Draws one of the skin's pictures fitted into a rectangle ({@link PictureScaling#FIT}).
     *
     * @param canvas    the canvas of the skin's renderer, between its begin and end
     * @param picture   a picture of this skin
     * @param x         the left coordinate
     * @param y         the top coordinate
     * @param width     the width
     * @param height    the height
     * @param teamColor the team colour of a portrait, packed {@code 0xRRGGBB}, or {@link PictureRenderer#NO_TEAM_COLOR}
     * @param color     the packed {@code 0xRRGGBBAA} colour multiplied with the picture, {@link PictureRenderer#OPAQUE_WHITE} for none
     * @throws IllegalStateException    if the skin has no picture renderer
     * @throws IllegalArgumentException if the canvas is not the canvas of the renderer
     */
    public void drawPicture(@NotNull Canvas canvas, @NotNull Picture picture, float x, float y, float width, float height, int teamColor, int color) {
        PictureRenderer renderer = this.renderer;

        if (renderer == null) {
            throw Exceptions.newIllegalStateException("The skin has no picture renderer.");
        }

        renderer.draw(canvas, picture, x, y, width, height, PictureScaling.FIT, teamColor, color);
    }
}
