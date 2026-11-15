package org.fogofwarlabs.sc4j.demo.client;

import org.fogofwarlabs.sc4j.base.exception.IgnoreException;
import org.fogofwarlabs.sc4j.demo.content.DemoContent;
import org.fogofwarlabs.sc4j.demo.order.DemoOrders;
import org.fogofwarlabs.sc4j.engine.client.app.SceneClient;
import org.fogofwarlabs.sc4j.engine.client.cursor.CursorContext;
import org.fogofwarlabs.sc4j.engine.client.cursor.CursorController;
import org.fogofwarlabs.sc4j.engine.client.cursor.CursorSet;
import org.fogofwarlabs.sc4j.engine.client.dialog.DialogTheme;
import org.fogofwarlabs.sc4j.engine.client.display.DetailSet;
import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.client.render.PictureRenderer;
import org.fogofwarlabs.sc4j.engine.client.render.PictureScaling;
import org.fogofwarlabs.sc4j.engine.client.render.SliceFill;
import org.fogofwarlabs.sc4j.engine.client.skin.*;
import org.fogofwarlabs.sc4j.engine.client.text.Font;
import org.fogofwarlabs.sc4j.engine.content.economy.ResourceKind;
import org.fogofwarlabs.sc4j.engine.content.faction.Faction;
import org.fogofwarlabs.sc4j.engine.content.type.OrderType;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.resource.ResourceKey;
import org.fogofwarlabs.sc4j.engine.resource.graphics.Picture;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

/**
 * Holds the look of the demo's client made of the pictures of its pack: the screen pictures (the menu background, the loading picture of each
 * faction, the victory and defeat backdrops), the emblem of each faction, the dialog look of the menus (the first faction's panel and buttons), the
 * HUD skin of each faction, the game's backdrop beyond the map's edge ({@value #BACKDROP}, a dark seamless texture apart from the black of unexplored
 * ground) and the cursors. The pack names the pictures by these rules: {@code loading-<faction>} and {@code emblem-<faction>}; per faction
 * {@code <faction>-panel}, {@code <faction>-field} (the recessed frame of the menus' text fields, drop-down lists, lists and tables),
 * {@code <faction>-selection} (the lit band under the selected row of a list or table), {@code <faction>-button-<state>} (normal, hover, pressed,
 * disabled, and selected: the lit frame of a chosen tab), {@code <faction>-topbar}, the console pieces {@code <faction>-console-plate} (the seamless
 * texture of the console's frame, with its nails), {@code <faction>-console-well} (the same texture without nails or rivets, a little darker: the
 * quiet ground of everything that shows texts or icons), {@code <faction>-console-frame} (the console's frame), {@code <faction>-console-minimap},
 * {@code <faction>-console-selection}, {@code <faction>-console-queue} and {@code <faction>-console-command} (the wells of the console's sections),
 * {@code <faction>-slot}, {@code <faction>-supply}, {@code <faction>-order-<order type>}, {@code <faction>-cancel} (the cancel button) and
 * {@code <faction>-rally} (the rally button); per unit type {@code icon-<unit type>} (its train or build button) and {@code portrait-<unit type>};
 * per resource kind {@code resource-<resource kind>}; and {@code cursor-<name>}. An order type, unit type or resource kind without a picture, and a
 * cancel or rally button without one, keeps the engine's text.
 * <p>
 * The top bar, the console, the buttons and the menu panel are laid out from their pieces at the pieces' own resolution: the plate repeats
 * under the console's frame and the quiet texture under the top bar, the console's wells, the buttons and the menu panel, both from the canvas
 * origin, so neighbouring pieces show one continuous texture and each button its own part of it; the frames, wells and button frames repeat
 * their edges and centres ({@link SliceFill#TILE}), so no piece is scaled at any window size. A HUD tooltip is the faction's slot frame over its
 * quiet texture, a HUD dialog (the network waiting dialog) the faction's panel over its quiet texture, each padded by its frame's border.
 */
public final class DemoSkin implements AutoCloseable {
    /**
     * The pack id of the menu background.
     */
    public static final @NotNull String MENU_BACKGROUND = "menu-background";
    /**
     * The pack id of the backdrop of a won game.
     */
    public static final @NotNull String VICTORY_BACKDROP = "victory-backdrop";
    /**
     * The pack id of the backdrop of a lost game.
     */
    public static final @NotNull String DEFEAT_BACKDROP = "defeat-backdrop";
    /**
     * The pack id of the game's backdrop beyond the map's edge.
     */
    public static final @NotNull String BACKDROP = "backdrop";
    /**
     * The side of a selected unit's slot in the selection panel, in framebuffer pixels.
     */
    public static final float SLOT_SIZE = 48;
    /**
     * The padding of a HUD tooltip's text inside the slot frame, its border in logical units.
     */
    public static final float TOOLTIP_PADDING = 8;
    /**
     * The padding of a HUD dialog's content inside the panel frame, its border in logical units.
     */
    public static final float DIALOG_PADDING = 16;
    /**
     * The distance of a menu field's text from the field's edges in logical units: the field frame's border and a gap.
     */
    public static final float FIELD_PADDING = 6;

    private static final char SEPARATOR = '-';
    private static final float HALF = 2;
    private static final @NotNull String LOADING_PREFIX = "loading-";
    private static final @NotNull String EMBLEM_PREFIX = "emblem-";
    private static final @NotNull String ICON_PREFIX = "icon-";
    private static final @NotNull String PORTRAIT_PREFIX = "portrait-";
    private static final @NotNull String RESOURCE_PREFIX = "resource-";
    private static final @NotNull String CURSOR_PREFIX = "cursor-";
    private static final @NotNull String ORDER_PREFIX = "order-";
    private static final @NotNull String PANEL = "panel";
    private static final @NotNull String FIELD = "field";
    private static final @NotNull String SELECTION = "selection";
    private static final @NotNull String NORMAL_BUTTON = "button-normal";
    private static final @NotNull String HOVERED_BUTTON = "button-hover";
    private static final @NotNull String PRESSED_BUTTON = "button-pressed";
    private static final @NotNull String DISABLED_BUTTON = "button-disabled";
    private static final @NotNull String SELECTED_BUTTON = "button-selected";
    private static final @NotNull String TOP_BAR = "topbar";
    private static final @NotNull String CONSOLE_PLATE = "console-plate";
    private static final @NotNull String CONSOLE_WELL = "console-well";
    private static final @NotNull String CONSOLE_FRAME = "console-frame";
    private static final @NotNull String MINIMAP_WELL = "console-minimap";
    private static final @NotNull String SELECTION_WELL = "console-selection";
    private static final @NotNull String QUEUE_WELL = "console-queue";
    private static final @NotNull String COMMAND_WELL = "console-command";
    private static final @NotNull String SLOT = "slot";
    private static final @NotNull String SUPPLY = "supply";
    private static final @NotNull String CANCEL = "cancel";
    private static final @NotNull String RALLY = "rally";
    private static final @NotNull String ARROW_CURSOR = "arrow";
    private static final @NotNull String ATTACK_CURSOR = "attack";
    private static final @NotNull String MOVE_CURSOR = "move";
    private static final @NotNull String BUILD_CURSOR = "build";
    private static final @NotNull String INVALID_CURSOR = "invalid";

    /**
     * Opens the look of the demo's client: reads the pictures of the dialog look, the HUD skins and the cursors; the screen pictures and the
     * emblems are read when they are drawn first.
     *
     * @param client  the client, whose canvas the pictures are drawn on and whose cursor surface shows the cursors
     * @param content the demo content with its pack
     * @param font    the font of the dialogs
     * @return the created {@link DemoSkin} instance, closed by the caller
     * @throws IOException if a picture cannot be read
     */
    public static @NotNull DemoSkin open(@NotNull SceneClient client, @NotNull DemoContent content, @NotNull Font font) throws IOException {
        PictureRenderer pictures = new PictureRenderer(client.getCanvas(), content.getPack());

        try {
            List<Faction> factions = content.getFactions();
            Faction menuFaction = factions.getFirst();
            Picture menuWell = pictures.getPicture(createFactionKey(menuFaction, CONSOLE_WELL));
            DialogTheme theme = DialogTheme.createDefault(font)
                                           .withPanelStyle(createTiledBox(pictures, menuWell, menuFaction, PANEL))
                                           .withFieldStyle(createTiledBox(pictures, menuWell, menuFaction, FIELD), FIELD_PADDING)
                                           .withSelectionStyle(createTiledBox(pictures, null, menuFaction, SELECTION))
                                           .withButtonStyle(createButtonStyle(pictures, menuFaction));
            HudSkinTable hudSkins = new HudSkinTable(createHudSkin(pictures, content, menuFaction));
            CursorController cursor = new CursorController(client.getCursor());

            for (Faction faction : factions) {
                hudSkins.setFactionSkin(faction, faction == menuFaction ? hudSkins.getDefaultSkin() : createHudSkin(pictures, content, faction));
            }

            cursor.setCursors(createCursors(pictures, content.getOrders()));

            BoxStyle backdrop = new PictureBoxStyle(pictures, null, pictures.getPicture(ResourceKey.of(BACKDROP)), SliceFill.TILE,
                                                    PictureRenderer.OPAQUE_WHITE);

            return new DemoSkin(pictures, theme, hudSkins, backdrop, cursor);
        } catch (IOException | RuntimeException exception) {
            IgnoreException.execute(
                    pictures::close
            );

            throw exception;
        }
    }

    private static @NotNull HudSkin createHudSkin(@NotNull PictureRenderer pictures, @NotNull DemoContent content, @NotNull Faction faction) throws IOException {
        Picture plate = pictures.getPicture(createFactionKey(faction, CONSOLE_PLATE));
        Picture well = pictures.getPicture(createFactionKey(faction, CONSOLE_WELL));
        HudSkin.Builder builder = HudSkin.builder(pictures)
                                         .setResourceBarBackground(createTiledBox(pictures, well, faction, TOP_BAR))
                                         .setConsoleBackground(createTiledBox(pictures, plate, faction, CONSOLE_FRAME))
                                         .setMinimapBackground(createTiledBox(pictures, well, faction, MINIMAP_WELL))
                                         .setMinimapBorder(ColorBoxStyle.NONE)
                                         .setSelectionPanelBackground(createTiledBox(pictures, well, faction, SELECTION_WELL))
                                         .setSelectionSlot(createTiledBox(pictures, null, faction, SLOT), SLOT_SIZE)
                                         .setQueuePanelBackground(createTiledBox(pictures, well, faction, QUEUE_WELL))
                                         .setCommandCardBackground(createTiledBox(pictures, well, faction, COMMAND_WELL))
                                         .setCommandButton(createButtonStyle(pictures, faction))
                                         .setTooltip(createTiledBox(pictures, well, faction, SLOT), TOOLTIP_PADDING)
                                         .setDialog(createTiledBox(pictures, well, faction, PANEL), DIALOG_PADDING)
                                         .setSupplyIcon(faction, pictures.getPicture(createFactionKey(faction, SUPPLY)));

        for (OrderType orderType : content.getOrders().getOrderTypes()) {
            Picture icon = pictures.findPicture(createFactionKey(faction, ORDER_PREFIX + orderType.getDebugName()));

            if (icon != null) {
                builder.setOrderIcon(orderType, icon);
            }
        }

        for (UnitType unitType : content.getUnitTypes()) {
            Picture icon = pictures.findPicture(ResourceKey.of(ICON_PREFIX + unitType.getDebugName()));
            Picture portrait = pictures.findPicture(ResourceKey.of(PORTRAIT_PREFIX + unitType.getDebugName()));

            if (icon != null) {
                builder.setUnitIcon(unitType, icon);
            }

            if (portrait != null) {
                builder.setPortrait(unitType, portrait);
            }
        }

        for (ResourceKind kind : content.getResourceKinds()) {
            Picture icon = pictures.findPicture(ResourceKey.of(RESOURCE_PREFIX + kind.getDebugName()));

            if (icon != null) {
                builder.setResourceIcon(kind, icon);
            }
        }

        Picture cancelIcon = pictures.findPicture(createFactionKey(faction, CANCEL));

        if (cancelIcon != null) {
            builder.setCancelIcon(cancelIcon);
        }

        Picture rallyIcon = pictures.findPicture(createFactionKey(faction, RALLY));

        if (rallyIcon != null) {
            builder.setRallyIcon(rallyIcon);
        }

        return builder.build();
    }

    /**
     * Creates the button style of a faction: each state's frame repeated at its own size over the faction's quiet texture.
     */
    private static @NotNull ButtonStyle createButtonStyle(@NotNull PictureRenderer pictures, @NotNull Faction faction) throws IOException {
        Picture well = pictures.getPicture(createFactionKey(faction, CONSOLE_WELL));

        return new ButtonStyle(createTiledBox(pictures, well, faction, NORMAL_BUTTON), createTiledBox(pictures, well, faction, HOVERED_BUTTON),
                               createTiledBox(pictures, well, faction, PRESSED_BUTTON), createTiledBox(pictures, well, faction, DISABLED_BUTTON),
                               createTiledBox(pictures, well, faction, SELECTED_BUTTON));
    }

    /**
     * Creates the box of a console piece: the piece repeated at its own size over its parts, over a seamless texture (the plate or the quiet
     * texture) when one is given.
     */
    private static @NotNull BoxStyle createTiledBox(@NotNull PictureRenderer pictures, @Nullable Picture plate, @NotNull Faction faction,
                                                    @NotNull String piece) throws IOException {
        return new PictureBoxStyle(pictures, plate, pictures.getPicture(createFactionKey(faction, piece)), SliceFill.TILE,
                                   PictureRenderer.OPAQUE_WHITE);
    }

    /**
     * Creates the cursors: the arrow, the attack cursor over an enemy and while an action waits for its target, the move cursor while a move,
     * patrol or gather order waits, and the build and invalid cursors while a building waits for its site.
     */
    private static @NotNull CursorSet createCursors(@NotNull PictureRenderer pictures, @NotNull DemoOrders orders) throws IOException {
        CursorSet cursors = new CursorSet();
        DetailSet<Picture> move = pictures.getDetails(ResourceKey.of(CURSOR_PREFIX + MOVE_CURSOR));
        DetailSet<Picture> attack = pictures.getDetails(ResourceKey.of(CURSOR_PREFIX + ATTACK_CURSOR));

        cursors.setCursor(CursorContext.NORMAL, pictures.getDetails(ResourceKey.of(CURSOR_PREFIX + ARROW_CURSOR)));
        cursors.setCursor(CursorContext.HOVER_ENEMY, attack);
        cursors.setCursor(CursorContext.TARGET, attack);
        cursors.setCursor(CursorContext.PLACE, pictures.getDetails(ResourceKey.of(CURSOR_PREFIX + BUILD_CURSOR)));
        cursors.setCursor(CursorContext.PLACE_INVALID, pictures.getDetails(ResourceKey.of(CURSOR_PREFIX + INVALID_CURSOR)));
        cursors.setOrderCursor(orders.getMove(), move);
        cursors.setOrderCursor(orders.getPatrol(), move);
        cursors.setOrderCursor(orders.getGather(), move);

        return cursors;
    }

    private static @NotNull ResourceKey createFactionKey(@NotNull Faction faction, @NotNull String piece) {
        return ResourceKey.of(faction.getDebugName() + SEPARATOR + piece);
    }

    private final @NotNull PictureRenderer pictures;
    private final @NotNull DialogTheme theme;
    private final @NotNull HudSkinTable hudSkins;
    private final @NotNull BoxStyle backdrop;
    private final @NotNull CursorController cursor;

    private DemoSkin(@NotNull PictureRenderer pictures, @NotNull DialogTheme theme, @NotNull HudSkinTable hudSkins, @NotNull BoxStyle backdrop,
                     @NotNull CursorController cursor) {
        this.pictures = pictures;
        this.theme    = theme;
        this.hudSkins = hudSkins;
        this.backdrop = backdrop;
        this.cursor   = cursor;
    }

    /**
     * @return the look of the demo's dialogs: the first faction's panel and buttons
     */
    public @NotNull DialogTheme getTheme() {
        return this.theme;
    }

    /**
     * @return the HUD skins by the local player's faction; a game without local player shows the skin of the viewed player's faction
     */
    public @NotNull HudSkinTable getHudSkins() {
        return this.hudSkins;
    }

    /**
     * @return the game's backdrop beyond the map's edge: the dark seamless texture repeated over the screen
     */
    public @NotNull BoxStyle getBackdrop() {
        return this.backdrop;
    }

    /**
     * @return the controller of the window's pointer with the demo's cursors
     */
    public @NotNull CursorController getCursor() {
        return this.cursor;
    }

    /**
     * Draws a screen picture over the whole canvas, filling it and cut at the longer side.
     *
     * @param canvas the client's canvas, between its begin and end
     * @param id     the pack id of the picture
     * @throws IOException if the picture cannot be read
     */
    public void drawScreen(@NotNull Canvas canvas, @NotNull String id) throws IOException {
        Picture picture = this.pictures.getPicture(ResourceKey.of(id));

        this.pictures.draw(canvas, picture, 0, 0, canvas.getWidth(), canvas.getHeight(), PictureScaling.FILL, PictureRenderer.NO_TEAM_COLOR,
                           PictureRenderer.OPAQUE_WHITE);
    }

    /**
     * Draws the loading picture of a faction over the whole canvas.
     *
     * @param canvas  the client's canvas, between its begin and end
     * @param faction the faction
     * @throws IOException if the picture cannot be read
     */
    public void drawLoadingScreen(@NotNull Canvas canvas, @NotNull Faction faction) throws IOException {
        this.drawScreen(canvas, LOADING_PREFIX + faction.getDebugName());
    }

    /**
     * Draws the emblem of a faction fitted into a square.
     *
     * @param canvas  the client's canvas, between its begin and end
     * @param faction the faction, or {@code null} to draw nothing
     * @param centerX the centre column of the square
     * @param centerY the centre row of the square
     * @param size    the side of the square
     * @throws IOException if the picture cannot be read
     */
    public void drawEmblem(@NotNull Canvas canvas, @Nullable Faction faction, float centerX, float centerY, float size) throws IOException {
        if (faction == null || size <= 0) {
            return;
        }

        Picture picture = this.pictures.getPicture(ResourceKey.of(EMBLEM_PREFIX + faction.getDebugName()));
        float half = size / HALF;

        this.pictures.draw(canvas, picture, centerX - half, centerY - half, size, size, PictureScaling.FIT, PictureRenderer.NO_TEAM_COLOR,
                           PictureRenderer.OPAQUE_WHITE);
    }

    /**
     * Disposes resources: releases the uploaded pictures.
     */
    @Override
    public void close() {
        IgnoreException.execute(
                this.pictures::close
        );
    }
}
