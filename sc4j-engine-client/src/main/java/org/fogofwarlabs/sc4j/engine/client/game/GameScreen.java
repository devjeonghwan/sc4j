package org.fogofwarlabs.sc4j.engine.client.game;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.base.exception.IgnoreException;
import org.fogofwarlabs.sc4j.engine.client.command.*;
import org.fogofwarlabs.sc4j.engine.client.console.ConsoleCommandRegistry;
import org.fogofwarlabs.sc4j.engine.client.cursor.CursorContext;
import org.fogofwarlabs.sc4j.engine.client.cursor.CursorController;
import org.fogofwarlabs.sc4j.engine.client.display.UiSurface;
import org.fogofwarlabs.sc4j.engine.client.hud.*;
import org.fogofwarlabs.sc4j.engine.client.input.InputEvent;
import org.fogofwarlabs.sc4j.engine.client.input.KeyBindingTable;
import org.fogofwarlabs.sc4j.engine.client.input.PointerButton;
import org.fogofwarlabs.sc4j.engine.client.projection.WorldProjection;
import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.client.render.WorldLayer;
import org.fogofwarlabs.sc4j.engine.client.render.WorldView;
import org.fogofwarlabs.sc4j.engine.client.render.WorldViewport;
import org.fogofwarlabs.sc4j.engine.client.skin.BoxStyle;
import org.fogofwarlabs.sc4j.engine.client.skin.HudSkin;
import org.fogofwarlabs.sc4j.engine.client.skin.HudSkinTable;
import org.fogofwarlabs.sc4j.engine.client.text.Font;
import org.fogofwarlabs.sc4j.engine.client.view.GameView;
import org.fogofwarlabs.sc4j.engine.client.view.UnitView;
import org.fogofwarlabs.sc4j.engine.client.world.*;
import org.fogofwarlabs.sc4j.engine.command.CommandSink;
import org.fogofwarlabs.sc4j.engine.command.CommandType;
import org.fogofwarlabs.sc4j.engine.content.economy.ResourceKind;
import org.fogofwarlabs.sc4j.engine.content.type.OrderType;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.ControllerKind;
import org.fogofwarlabs.sc4j.engine.game.PlayerSlot;
import org.fogofwarlabs.sc4j.engine.lockstep.LockstepSession;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Shows a {@link GameSession} as the engine default in-game screen of an RTS client, built only from engine defaults: the world view (terrain, units,
 * selection, rally points of the selection ({@link RallyLayer}), placement footprint, fog) of a {@link Camera} in the mod's {@link WorldProjection}
 * over the whole framebuffer, over the screen's backdrop where no map lies ({@link #setBackdrop}, by default black, so a mod can set the ground
 * beyond the map's edge apart from unexplored ground), and the HUD over it: the {@link ResourceBar} along the top, and along the bottom the
 * console: its plate ({@link ConsolePlate}) and on it, in sections apart by a gap, the {@link Minimap}, the {@link SelectionPanel}, a queue panel the
 * mod hosts ({@link #setQueuePanel}, for example a {@link ProductionQueuePanel}) and the {@link CommandCard}. The console spans the whole width at
 * {@link #PANEL_HEIGHT}, or at {@link #CONSOLE_HEIGHT_SHARE} of a lower screen ({@link #getConsoleHeight}); the minimap is as high as the console's
 * inside and as wide as the map's projected aspect asks (a square from above), at most {@link #MINIMAP_WIDTH_SHARE} of the width, so the map shows
 * undistorted; the command card (none without local player, a replay) keeps its width (a narrow screen gives the card at most a third of the room
 * right of the minimap) and the selection panel and the queue panel share the room between them while the queue panel shows something
 * ({@link HudBlock#isShown}); otherwise the selection panel takes the whole room and no queue region is drawn. The minimap's camera outline shows the
 * world between the resource bar and the console. The terrain and unit layers are the mod's choice (for example a
 * {@link org.fogofwarlabs.sc4j.engine.client.world.TilesetLayer} and a {@link org.fogofwarlabs.sc4j.engine.client.world.UnitSpriteLayer}), by default
 * the colour {@link TerrainLayer} and the footprint {@link UnitLayer}. The command card shows the buttons the mod sets, or those its
 * {@link CommandCardSource} creates for the current selection; while a {@link ConstructAction} waits for its site, the building's footprint follows
 * the pointer with the building's ghost ({@link PlacementLayer#setGhost}) and a press on buildable ground points at the snapped site. A replay or an
 * observer without local player shows the resources of the viewed player ({@link #setViewedPlayer}) in the resource bar. Input goes, in this order,
 * to the command translator (hotkeys, and pointer presses while an action waits for its target or the right button), the HUD blocks (the one drawn
 * last first), and the {@link SelectionBox}; the arrow keys and the pointer at a screen edge scroll the camera through its {@link CameraScroll} at a
 * {@link ContinuousScrollRate}. Camera extras are the slash commands of {@link CameraCommandUtils} ({@link #registerCommands}). The HUD blocks draw
 * in one {@link HudSkin} ({@link #setSkin}) or in the skin a {@link HudSkinTable} chooses by the local (or viewed) player's faction
 * ({@link #setSkins}), by default the engine default look. With a {@link CursorController} ({@link #setCursor}) the screen shows the cursor of its
 * {@link CursorContext} at every update ({@link #findCursorContext}). A {@link HudListener} ({@link #setListener}) hears each press that runs a
 * command card button or a queue slot, each change of the selection by the player's input, each refused action ({@link CommandRefusal}) with its
 * message line, and each producer of the local player whose front entry starts waiting for supply ({@link UnitView#isWaitingForSupply}), once per
 * wait. A network session over a {@link LockstepSession} ({@link GameSession#getLockstepSession()}) shows the {@link WaitingDialog} over the world's
 * centre while the session waits for overdue participants ({@link #getWaitingDialog}); it takes the input first.
 * <p>
 * A replay session has no local player: its screen shows everything and issues nothing.
 * <p>
 * Sizes, positions and pointer events are in the logical units of the canvas ({@link UiSurface}): {@link #draw} begins the canvas on the
 * frame's surface, and the caller hands {@link #handleInput} pointer positions in the same units ({@link UiSurface#toLogicalX},
 * {@link UiSurface#toLogicalY}), as the engine's scene client does for its scenes.
 */
public final class GameScreen implements AutoCloseable {
    /**
     * The height of the resource bar in logical units.
     */
    public static final int RESOURCE_BAR_HEIGHT = 24;
    /**
     * The height of the bottom console in logical units on a screen of at least {@code PANEL_HEIGHT / CONSOLE_HEIGHT_SHARE} logical rows.
     */
    public static final int PANEL_HEIGHT = 160;
    /**
     * The largest part of the screen's logical height the console takes, so that the world view keeps the rest on a low screen.
     */
    public static final float CONSOLE_HEIGHT_SHARE = 0.3f;
    /**
     * The gap between the console's sections and between a section and the console's edge, in logical units.
     */
    public static final int SECTION_GAP = 6;
    /**
     * The largest share of the console's inner width the minimap takes.
     */
    public static final float MINIMAP_WIDTH_SHARE = 0.25f;
    /**
     * The number of selected units the screen's selection holds at most.
     */
    public static final int SELECTION_CAPACITY = 24;

    private static final int COMMAND_COLUMNS = 3;
    private static final int COMMAND_ROWS = 3;
    private static final float COMMAND_CARD_WIDTH = 300;
    /**
     * The command card takes at most this part of the room right of the minimap.
     */
    private static final float COMMAND_CARD_SHARE = 3;
    /**
     * The gaps across the console with a command card: at both edges and between the minimap, the selection panel and the card.
     */
    private static final int GAPS_WITH_CARD = 4;
    /**
     * The gaps across the console without a command card: at both edges and between the minimap and the selection panel.
     */
    private static final int GAPS_WITHOUT_CARD = 3;
    private static final @NotNull String MESSAGE_END = ".";
    private static final int CLEAR_COLOR = 0x000000FF;
    private static final float HALF = 2;

    /**
     * Drops the commands of a session without local player.
     */
    private static final class DroppingSink implements CommandSink {
        /**
         * Submits one command of a player: drops it.
         *
         * @param <C>     the command class
         * @param player  the issuing player
         * @param type    the command type
         * @param command the command
         */
        @Override
        public <C> void submit(@NotNull Player player, @NotNull CommandType<C> type, @NotNull C command) {
            // A replay viewer issues nothing.
        }
    }

    /**
     * Returns the height of the bottom console on a screen: {@link #PANEL_HEIGHT}, at most {@link #CONSOLE_HEIGHT_SHARE} of the screen's
     * height, rounded to whole units.
     *
     * @param height the logical height of the screen
     * @return the console height in logical units
     */
    public static int getConsoleHeight(int height) {
        return Math.min(PANEL_HEIGHT, Math.round(height * CONSOLE_HEIGHT_SHARE));
    }


    private final @NotNull GameSession session;
    private final @NotNull Canvas canvas;
    private final @NotNull Function<ResourceKind, String> resourceNames;
    private final @NotNull String supplyName;
    private final @NotNull Function<UnitType, String> typeNames;

    private final @NotNull Camera camera;
    private final @NotNull ContinuousScrollRate scrollRate;
    private final @NotNull CameraScroll cameraScroll;
    private final @NotNull WorldViewport viewport;
    private final @NotNull OrderedSelection selection;
    private final @NotNull WorldPicker picker;
    private final @NotNull SelectionBox selectionBox;
    private final @NotNull PlacementLayer placement;
    private final @NotNull WorldView worldView;

    private final @NotNull ResourceBar resourceBar;
    private final @NotNull ConsolePlate consolePlate;
    private final @NotNull Minimap minimap;
    private final @NotNull SelectionPanel selectionPanel;
    private final @NotNull CommandTranslator translator;
    private final @NotNull CommandCard commandCard;
    private final @Nullable WaitingDialog waitingDialog;
    private final @NotNull List<@NotNull HudBlock> blocks;

    private @Nullable HudBlock queuePanel;
    private float queuePanelWidth;
    private @Nullable BoxStyle backdrop;
    private @Nullable HudSkinTable skins;
    private @Nullable Player viewedPlayer;
    private @Nullable CursorController cursor;
    private @Nullable HudListener listener;

    private boolean refusedInEvent;
    private @NotNull List<@NotNull Unit> waitingProducers;
    private @NotNull List<@NotNull Unit> nextWaitingProducers;

    private @Nullable CommandCardSource commandCardSource;
    private final @NotNull List<@NotNull Unit> commandCardSelection;
    private final @NotNull List<@NotNull String> commandCardLabels;

    private float pointerX;
    private float pointerY;

    private boolean leftKeyHeld;
    private boolean rightKeyHeld;
    private boolean upKeyHeld;
    private boolean downKeyHeld;

    /**
     * Creates a {@link GameScreen} instance with the colour terrain layer and the footprint unit layer; it uploads nothing until it draws.
     *
     * @param session       the session it shows
     * @param canvas        the canvas it draws on
     * @param font          the font of the HUD texts
     * @param projection    the projection of the world view and the minimap
     * @param colors        the terrain colours of the world view and the minimap
     * @param resourceNames the shown name of each resource kind
     * @param supplyName    the shown name of the supply
     * @param typeNames     the shown name of each unit type
     */
    public GameScreen(@NotNull GameSession session,
                      @NotNull Canvas canvas,
                      @NotNull Font font,
                      @NotNull WorldProjection projection,
                      @NotNull TerrainColors colors,
                      @NotNull Function<ResourceKind, String> resourceNames,
                      @NotNull String supplyName,
                      @NotNull Function<UnitType, String> typeNames) {
        this(session, canvas, font, projection, colors, new TerrainLayer(canvas, session.getView(), colors), new UnitLayer(canvas, session.getView()),
             resourceNames, supplyName, typeNames);
    }

    /**
     * Creates a {@link GameScreen} instance with the mod's terrain and unit layers; it uploads nothing until it draws. The caller closes
     * layers that hold resources.
     *
     * @param session       the session it shows
     * @param canvas        the canvas it draws on
     * @param font          the font of the HUD texts
     * @param projection    the projection of the world view and the minimap
     * @param colors        the terrain colours of the minimap
     * @param terrainLayer  the terrain layer of the world view, drawn first
     * @param unitLayer     the unit layer of the world view, drawn over the terrain
     * @param resourceNames the shown name of each resource kind
     * @param supplyName    the shown name of the supply
     * @param typeNames     the shown name of each unit type
     */
    public GameScreen(@NotNull GameSession session,
                      @NotNull Canvas canvas,
                      @NotNull Font font,
                      @NotNull WorldProjection projection,
                      @NotNull TerrainColors colors,
                      @NotNull WorldLayer terrainLayer,
                      @NotNull WorldLayer unitLayer,
                      @NotNull Function<ResourceKind, String> resourceNames,
                      @NotNull String supplyName,
                      @NotNull Function<UnitType, String> typeNames) {
        GameView view = session.getView();
        Player player = session.getLocalPlayer();
        CommandSink sink = session.getCommandSink();
        LockstepSession lockstep = session.getLockstepSession();

        this.session       = session;
        this.canvas        = canvas;
        this.resourceNames = resourceNames;
        this.supplyName    = supplyName;
        this.typeNames     = typeNames;

        this.camera       = new Camera(projection, view.getMapWidth(), view.getMapHeight());
        this.scrollRate   = new ContinuousScrollRate();
        this.cameraScroll = new CameraScroll(CameraScroll.DEFAULT_EDGE_WIDTH, this.scrollRate);
        this.viewport     = new WorldViewport(projection);
        this.selection    = new OrderedSelection(SELECTION_CAPACITY);
        this.picker       = new WorldPicker(view, this.camera);
        this.selectionBox = new SelectionBox(view, this.picker, this.selection);
        this.placement    = new PlacementLayer(canvas, view, this.picker);
        this.worldView    = new WorldView(List.of(terrainLayer,
                                                  unitLayer,
                                                  new SelectionLayer(canvas, view, this.selection, this.selectionBox),
                                                  new RallyLayer(canvas, view, this.selection),
                                                  this.placement,
                                                  new FogLayer(canvas, view)));

        this.resourceBar    = new ResourceBar(view, font, resourceNames, supplyName);
        this.consolePlate   = new ConsolePlate();
        this.minimap        = new Minimap(view, this.camera, colors);
        this.selectionPanel = new SelectionPanel(view, this.selection, font, typeNames, resourceNames);
        this.translator     = new CommandTranslator(player != null ? player : view.getPlayers().getFirst(),
                                                    sink != null ? sink : new DroppingSink(),
                                                    this.selection,
                                                    new KeyBindingTable<>(),
                                                    this::pickTarget);
        this.commandCard    = new CommandCard(this.translator, font, typeNames, resourceNames, supplyName, COMMAND_COLUMNS, COMMAND_ROWS);
        this.waitingDialog  = lockstep != null ? new WaitingDialog(lockstep, font) : null;
        this.blocks         = new ArrayList<>(List.of(this.resourceBar, this.minimap, this.selectionPanel, this.commandCard));

        this.queuePanel      = null;
        this.queuePanelWidth = 0;
        this.backdrop        = null;
        this.skins           = null;
        this.viewedPlayer    = null;
        this.cursor          = null;
        this.listener        = null;

        this.refusedInEvent       = false;
        this.waitingProducers     = new ArrayList<>();
        this.nextWaitingProducers = new ArrayList<>();

        this.commandCardSource    = null;
        this.commandCardSelection = new ArrayList<>(SELECTION_CAPACITY);
        this.commandCardLabels    = new ArrayList<>();

        this.pointerX = -1;
        this.pointerY = -1;

        this.leftKeyHeld  = false;
        this.rightKeyHeld = false;
        this.upKeyHeld    = false;
        this.downKeyHeld  = false;

        this.translator.setRefusalListener(this::reportRefusal);

        if (this.waitingDialog != null) {
            this.blocks.add(this.waitingDialog);
        }
    }

    /**
     * @return the camera of the world view
     */
    public @NotNull Camera getCamera() {
        return this.camera;
    }

    /**
     * @return the rate of the camera scroll by the arrow keys and the screen edges, for example for the mod's scroll speed option
     */
    public @NotNull ContinuousScrollRate getScrollRate() {
        return this.scrollRate;
    }

    /**
     * @return the scroll of the camera by the arrow keys and the screen edges
     */
    public @NotNull CameraScroll getCameraScroll() {
        return this.cameraScroll;
    }

    /**
     * @return the local player's selection
     */
    public @NotNull OrderedSelection getSelection() {
        return this.selection;
    }

    /**
     * @return the translator of the local player's input into commands
     */
    public @NotNull CommandTranslator getTranslator() {
        return this.translator;
    }

    /**
     * @return the command card
     */
    public @NotNull CommandCard getCommandCard() {
        return this.commandCard;
    }

    /**
     * @return the plate of the bottom console
     */
    public @NotNull ConsolePlate getConsolePlate() {
        return this.consolePlate;
    }

    /**
     * @return the resource bar
     */
    public @NotNull ResourceBar getResourceBar() {
        return this.resourceBar;
    }

    /**
     * @return the minimap
     */
    public @NotNull Minimap getMinimap() {
        return this.minimap;
    }

    /**
     * @return the selection panel
     */
    public @NotNull SelectionPanel getSelectionPanel() {
        return this.selectionPanel;
    }

    /**
     * @return the network waiting dialog of a session over a lockstep session, or {@code null} for a local game or a replay
     */
    public @Nullable WaitingDialog getWaitingDialog() {
        return this.waitingDialog;
    }

    /**
     * @return the queue panel the console hosts, or {@code null} for none
     */
    public @Nullable HudBlock getQueuePanel() {
        return this.queuePanel;
    }

    /**
     * Hosts a queue panel in the console, for example the queue of the selected producer: the console lays out a region of the given width
     * between the selection panel and the command card, at most half the room the minimap and the command card leave, and draws the skin's
     * queue panel box under the panel, both only while the panel shows something ({@link HudBlock#isShown}); the panel is laid out in the
     * region and draws, takes input and wears the skin like the other HUD blocks.
     *
     * @param panel the panel, or {@code null} to give its region back to the selection panel
     * @param width the width of the panel's region in logical units
     * @throws IllegalArgumentException if the width is not positive
     */
    public void setQueuePanel(@Nullable HudBlock panel, float width) {
        if (!(width > 0)) {
            throw Exceptions.newIllegalArgumentException("Invalid queue panel width {0}", width);
        }

        HudBlock hosted = this.queuePanel;

        if (hosted != null) {
            this.blocks.remove(hosted);
        }

        this.queuePanel      = panel;
        this.queuePanelWidth = width;

        if (panel != null) {
            panel.setSkin(this.consolePlate.getSkin());
            this.blocks.add(this.waitingDialog != null ? this.blocks.size() - 1 : this.blocks.size(), panel);
        }
    }

    /**
     * @return the box drawn over the whole screen under the world view, or {@code null} for black
     */
    public @Nullable BoxStyle getBackdrop() {
        return this.backdrop;
    }

    /**
     * Sets the box drawn over the whole screen under the world view: what shows beyond the map's edge, for example a dark seamless texture.
     *
     * @param backdrop the box, or {@code null} for black
     */
    public void setBackdrop(@Nullable BoxStyle backdrop) {
        this.backdrop = backdrop;
    }

    /**
     * @return the placement footprint of the world view, which follows the pointer while it is shown
     */
    public @NotNull PlacementLayer getPlacement() {
        return this.placement;
    }

    /**
     * Sets the player whose resources and supply the resource bar and whose forces the selection panel show while the session has no local
     * player (a replay or an observer); with a skin table ({@link #setSkins}) the HUD wears the skin of the viewed player's faction.
     *
     * @param player the player, or {@code null} for an empty resource bar
     * @param name   the player's shown name, drawn at the bar's left edge
     */
    public void setViewedPlayer(@Nullable Player player, @NotNull String name) {
        HudSkinTable table = this.skins;

        this.viewedPlayer = player;
        this.resourceBar.setViewedPlayer(player, name);
        this.selectionPanel.setOverviewPlayer(player);

        if (table != null) {
            this.applySkin(this.findSkin(table));
        }
    }

    /**
     * Sets the command card's buttons and the action of a right press on the map, for example move.
     *
     * @param buttons       the buttons in grid order
     * @param defaultAction the action of a right press on the map, or {@code null} for none
     */
    public void setCommands(@NotNull List<@NotNull CommandButton> buttons, @Nullable CommandAction defaultAction) {
        this.commandCard.setButtons(buttons);
        this.translator.setDefaultAction(defaultAction);
    }

    /**
     * Sets the source of the command card's buttons and the action of a right press on the map: from then on the screen asks the source for
     * the buttons of the local player's selection at every update and shows them when they changed.
     *
     * @param source        the source, or {@code null} to keep the buttons set with {@link #setCommands}
     * @param defaultAction the action of a right press on the map, or {@code null} for none
     */
    public void setCommandCardSource(@Nullable CommandCardSource source, @Nullable CommandAction defaultAction) {
        this.commandCardSource = source;
        this.commandCardSelection.clear();
        this.commandCardLabels.clear();
        this.translator.setDefaultAction(defaultAction);
        this.commandCard.setButtons(List.of());
    }

    /**
     * Sets the skin of every HUD block, from then on regardless of the local player's faction.
     *
     * @param skin the skin
     */
    public void setSkin(@NotNull HudSkin skin) {
        this.skins = null;
        this.applySkin(skin);
    }

    /**
     * Sets the table that chooses the HUD skin by the local player's faction; the screen applies its choice now and at every update, so a replay
     * or an observer without local player shows the skin of the viewed player's faction ({@link #setViewedPlayer}), or the table's default skin
     * while no player is viewed.
     *
     * @param skins the skin table
     */
    public void setSkins(@NotNull HudSkinTable skins) {
        this.skins = skins;
        this.applySkin(this.findSkin(skins));
    }

    private @NotNull HudSkin findSkin(@NotNull HudSkinTable skins) {
        GameView view = this.session.getView();
        Player local = view.getLocalPlayer();
        Player player = local != null ? local : this.viewedPlayer;

        return skins.findSkin(player != null ? view.getFaction(player) : null);
    }

    private void applySkin(@NotNull HudSkin skin) {
        this.consolePlate.setSkin(skin);

        for (int index = 0; index < this.blocks.size(); index++) {
            this.blocks.get(index).setSkin(skin);
        }
    }

    /**
     * Sets the controller of the window's pointer: from then on every update shows the cursor of the screen's cursor context.
     *
     * @param cursor the controller, or {@code null} to leave the pointer alone
     */
    public void setCursor(@Nullable CursorController cursor) {
        this.cursor = cursor;
    }

    /**
     * @return the listener of the HUD's button presses and refused actions, or {@code null} for none
     */
    public @Nullable HudListener getListener() {
        return this.listener;
    }

    /**
     * Sets the listener of the HUD's button presses and refused actions, for example the mod's sounds and messages.
     *
     * @param listener the listener, or {@code null} for none
     */
    public void setListener(@Nullable HudListener listener) {
        this.listener = listener;
    }


    /**
     * Registers the screen's slash commands: the camera and camera scroll commands of {@link CameraCommandUtils}.
     *
     * @param registry the registry of the client's commands
     */
    public void registerCommands(@NotNull ConsoleCommandRegistry registry) {
        CameraCommandUtils.register(registry, this.camera, this.cameraScroll, this.scrollRate);
    }

    /**
     * Updates the screen for the time since the last frame: drops selected units that left the game or are no longer seen, scrolls the
     * camera while arrow keys are held or the pointer rests at a screen edge, moves the minimap's pings on, refreshes the command card from its
     * source, tells the listener of a producer that starts waiting for supply and shows the placement footprint while a construction waits for
     * its site; then it applies the skin of its skin table and shows the cursor of the cursor context.
     *
     * @param milliseconds the time since the last frame
     */
    public void update(long milliseconds) {
        GameView view = this.session.getView();
        Player player = view.getLocalPlayer();

        this.selection.retain(unit -> {
            UnitView unitView = view.getUnitView(unit);

            return unitView.getOwner() != null && unitView.isPlaced() && (player == null || view.isUnitVisible(player, unit));
        });

        int horizontalKeys = (this.rightKeyHeld ? 1 : 0) - (this.leftKeyHeld ? 1 : 0);
        int verticalKeys = (this.downKeyHeld ? 1 : 0) - (this.upKeyHeld ? 1 : 0);
        boolean dragging = this.selectionBox.isDragging();

        if (dragging) {
            this.cameraScroll.clearEdge();
        }

        this.cameraScroll.update(this.camera,
                                 horizontalKeys, verticalKeys,
                                 this.pointerX, this.pointerY,
                                 this.pointerX >= 0 && !dragging,
                                 this.camera.getScreenWidth(), this.camera.getScreenHeight(),
                                 milliseconds);

        this.minimap.update(milliseconds);
        this.updateCommandCard(player);
        this.updateWaitingProducers(player);

        HudSkinTable table = this.skins;

        if (table != null) {
            this.applySkin(this.findSkin(table));
        }

        if (this.translator.getPendingAction() instanceof ConstructAction construct) {
            this.placement.show(construct.getBuildingType(), construct.getFootprintWidth(), construct.getFootprintHeight());
        } else if (this.placement.isShown()) {
            this.placement.hide();
        }

        CursorController controller = this.cursor;

        if (controller != null) {
            OrderType orderType = this.translator.getPendingAction() instanceof OrderAction order ? order.getOrderType() : null;

            controller.update(this.findCursorContext(), orderType);
        }
    }

    private void updateCommandCard(@Nullable Player player) {
        CommandCardSource source = this.commandCardSource;

        if (source == null || player == null) {
            return;
        }

        List<Unit> units = this.selection.getUnits();
        List<CommandButton> buttons = units.isEmpty() ? List.of() : source.createButtons(player, units);
        List<String> labels = new ArrayList<>(buttons.size());

        for (CommandButton button : buttons) {
            labels.add(button.getLabel());
        }

        if (units.equals(this.commandCardSelection) && labels.equals(this.commandCardLabels)) {
            return;
        }

        this.commandCardSelection.clear();
        this.commandCardSelection.addAll(units);
        this.commandCardLabels.clear();
        this.commandCardLabels.addAll(labels);
        this.commandCard.setButtons(buttons);
    }

    /**
     * Finds the local player's producers whose front entry waits for supply and tells the listener of the first one that started waiting since
     * the last update; the others that started meanwhile count as told.
     *
     * @param player the local player, or {@code null} for none
     */
    private void updateWaitingProducers(@Nullable Player player) {
        GameView view = this.session.getView();
        List<Unit> waiting = this.nextWaitingProducers;
        Unit started = null;

        waiting.clear();

        if (player != null) {
            List<Unit> units = view.getUnits();

            for (int index = 0; index < units.size(); index++) {
                Unit unit = units.get(index);
                UnitView unitView = view.getUnitView(unit);

                if (unitView.getOwner() != player || !unitView.isWaitingForSupply()) {
                    continue;
                }

                waiting.add(unit);

                if (started == null && !this.waitingProducers.contains(unit)) {
                    started = unit;
                }
            }
        }

        this.nextWaitingProducers = this.waitingProducers;
        this.waitingProducers     = waiting;

        HudListener current = this.listener;

        if (started != null && current != null) {
            current.onProductionWaiting(started, CommandRefusal.MISSING_SUPPLY,
                                        CommandRefusal.MISSING_SUPPLY.describe(this.typeNames, this.resourceNames, this.supplyName) + MESSAGE_END);
        }
    }

    /**
     * Finds the cursor context of the pointer: the scroll direction of the camera scroll's edge at the last update (not during a selection drag);
     * over the minimap, the target cursor while an action waits, else the normal one; over the other HUD blocks the normal one; while a building
     * waits for its site, whether the pointed site is buildable; while another action waits, the target cursor; else the hovered unit seen by the
     * local player as friend (the player's team), enemy or neutral, or the normal cursor. A screen without pointer position or without local player
     * (a replay) hovers nothing.
     *
     * @return the cursor context
     */
    public @NotNull CursorContext findCursorContext() {
        float x = this.pointerX;
        float y = this.pointerY;
        CommandAction pending = this.translator.getPendingAction();

        if (x < 0) {
            return CursorContext.NORMAL;
        }

        if (!this.selectionBox.isDragging()) {
            CursorContext scroll = CursorContext.ofScroll(this.cameraScroll.getEdgeX(), this.cameraScroll.getEdgeY());

            if (scroll != null) {
                return scroll;
            }
        }

        if (this.minimap.contains(x, y)) {
            return pending != null ? CursorContext.TARGET : CursorContext.NORMAL;
        }

        if (this.isOverHud(x, y)) {
            return CursorContext.NORMAL;
        }

        if (this.placement.isShown()) {
            this.placement.moveTo(x, y);

            return this.placement.isBuildable() ? CursorContext.PLACE : CursorContext.PLACE_INVALID;
        }

        if (pending != null) {
            return CursorContext.TARGET;
        }

        return this.findHoverContext(this.picker.pickUnit(x, y));
    }

    private @NotNull CursorContext findHoverContext(@Nullable Unit unit) {
        GameView view = this.session.getView();
        Player player = view.getLocalPlayer();
        Player owner = unit != null ? view.getUnitView(unit).getOwner() : null;

        if (player == null || owner == null) {
            return CursorContext.NORMAL;
        }

        PlayerSlot ownerSlot = this.findSlot(owner);
        PlayerSlot playerSlot = this.findSlot(player);

        if (ownerSlot != null && ownerSlot.getControllerKind() == ControllerKind.NEUTRAL) {
            return CursorContext.HOVER_NEUTRAL;
        }

        boolean friend = owner == player || ownerSlot != null && playerSlot != null && ownerSlot.getTeam() == playerSlot.getTeam();

        return friend ? CursorContext.HOVER_FRIEND : CursorContext.HOVER_ENEMY;
    }

    private @Nullable PlayerSlot findSlot(@NotNull Player player) {
        List<PlayerSlot> slots = this.session.getGame().getPlayers();

        for (int index = 0; index < slots.size(); index++) {
            if (slots.get(index).getSlotIndex() == player.getIndex()) {
                return slots.get(index);
            }
        }

        return null;
    }

    /**
     * Draws the screen into the framebuffer in the logical units of a surface: begins the canvas on the surface, draws the backdrop over the
     * whole framebuffer, the world view over all of it and the HUD over the world.
     *
     * @param surface the interface surface of the frame
     * @throws IOException if a world layer cannot read a resource it draws
     */
    public void draw(@NotNull UiSurface surface) throws IOException {
        this.canvas.begin(surface);

        try {
            this.drawContent(surface.getWidth(), surface.getHeight());
        } finally {
            this.canvas.end();
        }
    }

    /**
     * Draws the screen on a canvas that has begun, for a scene that draws more over it: draws the backdrop over the whole canvas, the world
     * view over all of it and the HUD over the world.
     *
     * @param width  the width of the canvas in logical units
     * @param height the height of the canvas in logical units
     * @throws IOException if a world layer cannot read a resource it draws
     */
    public void drawContent(int width, int height) throws IOException {
        this.layout(width, height);
        this.camera.applyTo(this.viewport);

        if (this.placement.isShown()) {
            this.placement.moveTo(this.pointerX, this.pointerY);
        }

        BoxStyle shownBackdrop = this.backdrop;

        if (shownBackdrop != null) {
            shownBackdrop.draw(this.canvas, 0, 0, width, height);
        } else {
            this.canvas.fillRectangle(0, 0, width, height, CLEAR_COLOR);
        }

        this.worldView.draw(this.viewport);
        this.consolePlate.draw(this.canvas);

        HudBlock panel = this.queuePanel;

        if (panel != null && panel.isShown()) {
            this.consolePlate.getSkin().getQueuePanelBackground().draw(this.canvas, panel.getX(), panel.getY(), panel.getWidth(), panel.getHeight());
        }

        for (int index = 0; index < this.blocks.size(); index++) {
            this.blocks.get(index).draw(this.canvas);
        }
    }

    /**
     * Handles an input event: the arrow keys scroll the camera, any other event goes to the translator first, then the HUD blocks from the one
     * drawn last and the console plate, then the selection box. A left press that the command card or the queue panel takes and that runs no
     * refused action tells the listener that a button was pressed; an event that changed the selection, while units are selected, tells it
     * that units were selected.
     *
     * @param event the event
     * @return {@code true} if a part of the screen took the event, {@code false} otherwise
     */
    public boolean handleInput(@NotNull InputEvent event) {
        int changes = this.selection.getChangeCount();
        boolean taken = this.routeInput(event);
        HudListener current = this.listener;

        if (current != null && this.selection.getChangeCount() != changes && !this.selection.isEmpty()) {
            current.onUnitsSelected(this.selection.getUnits());
        }

        return taken;
    }

    /**
     * Hands an input event to the first part that takes it: the command translator, the HUD blocks (the one drawn last first), the console
     * plate, then the selection box.
     *
     * @param event the event
     * @return {@code true} if a part of the screen took the event, {@code false} otherwise
     */
    private boolean routeInput(@NotNull InputEvent event) {
        if (event.getType() == InputEvent.Type.POINTER_MOVED) {
            this.pointerX = event.getX();
            this.pointerY = event.getY();
        }

        if (event.isKeyEvent() && this.holdScrollKey(event)) {
            return true;
        }

        boolean pointerPress = event.getType() == InputEvent.Type.BUTTON_PRESSED;

        this.refusedInEvent = false;

        if ((event.isKeyEvent() || pointerPress && this.isTranslatorPress(event)) && this.translator.handleInput(event)) {
            this.selectionBox.cancel();

            return true;
        }

        for (int index = this.blocks.size() - 1; index >= 0; index--) {
            HudBlock block = this.blocks.get(index);

            if (block.handleInput(event)) {
                this.reportPress(event, block);

                return true;
            }
        }

        return this.consolePlate.handleInput(event) || this.selectionBox.handleInput(event);
    }

    /**
     * Tells the listener that a left press on a command card button, on a queue slot (any press the hosted queue panel takes, if it is no
     * {@link ProductionQueuePanel}) or on the waiting dialog's drop button ran it, unless the button's action was refused.
     *
     * @param event the event a HUD block took
     * @param block the HUD block that took it
     */
    private void reportPress(@NotNull InputEvent event, @NotNull HudBlock block) {
        HudListener current = this.listener;

        if (current == null || event.getType() != InputEvent.Type.BUTTON_PRESSED || event.getButton() != PointerButton.LEFT || this.refusedInEvent) {
            return;
        }

        float x = event.getX();
        float y = event.getY();
        WaitingDialog dialog = this.waitingDialog;
        boolean button = block == this.commandCard ? this.commandCard.findButtonAt(x, y) != null
                                                   : block == dialog ? dialog.isDropButtonAt(x, y)
                                                                     : block == this.queuePanel && (!(block instanceof ProductionQueuePanel queue) || queue.findSlotAt(x, y) >= 0);

        if (button) {
            current.onButtonPressed();
        }
    }

    /**
     * Tells the listener of an action the translator refused, with the refusal as a message line.
     *
     * @param refusal the refusal
     */
    private void reportRefusal(@NotNull CommandRefusal refusal) {
        HudListener current = this.listener;

        this.refusedInEvent = true;

        if (current != null) {
            current.onCommandRefused(refusal, refusal.describe(this.typeNames, this.resourceNames, this.supplyName) + MESSAGE_END);
        }
    }

    /**
     * Keeps whether each arrow key is held, from its press and release events (a repeat holds it too).
     *
     * @param event the event
     * @return {@code true} if the event is one of an arrow key, {@code false} otherwise
     */
    private boolean holdScrollKey(@NotNull InputEvent event) {
        InputEvent.Type type = event.getType();

        if (type != InputEvent.Type.KEY_PRESSED && type != InputEvent.Type.KEY_REPEATED && type != InputEvent.Type.KEY_RELEASED) {
            return false;
        }

        boolean held = type != InputEvent.Type.KEY_RELEASED;

        switch (event.getKey()) {
            case LEFT -> this.leftKeyHeld = held;
            case RIGHT -> this.rightKeyHeld = held;
            case UP -> this.upKeyHeld = held;
            case DOWN -> this.downKeyHeld = held;
            default -> {
                return false;
            }
        }

        return true;
    }

    private boolean isTranslatorPress(@NotNull InputEvent event) {
        return this.translator.getPendingAction() != null || event.getButton() != PointerButton.LEFT;
    }

    /**
     * Finds the target of a pointer position for the translator: the minimap's map position, nothing on the other HUD blocks, the snapped
     * site while the placement footprint is shown (nothing where it is not buildable), else the world view's target.
     *
     * @param x the logical column
     * @param y the logical row
     * @return the target, or {@code null} if the position shows no map
     */
    private @Nullable CommandTarget pickTarget(float x, float y) {
        if (this.minimap.contains(x, y)) {
            return this.minimap.pick(x, y);
        }

        if (this.isOverHud(x, y)) {
            return null;
        }

        if (this.placement.isShown()) {
            this.placement.moveTo(x, y);

            return this.placement.isBuildable() ? CommandTarget.at(this.placement.getCenterX(), this.placement.getCenterY()) : null;
        }

        return this.picker.pick(x, y);
    }

    private boolean isOverHud(float x, float y) {
        for (int index = 0; index < this.blocks.size(); index++) {
            HudBlock block = this.blocks.get(index);

            if (block.isShown() && block.contains(x, y)) {
                return true;
            }
        }

        return this.consolePlate.contains(x, y);
    }

    /**
     * Lays the HUD out: the resource bar along the top; the console plate along the bottom at its height for the screen
     * ({@link #getConsoleHeight}) and on it, a gap apart, the minimap at the left (as high as the console's inside, as wide as the map's
     * projected aspect asks, at most {@link #MINIMAP_WIDTH_SHARE} of the inner width), the command card at the right (at most a third of the room
     * right of the minimap; none without local player, a replay), and between them the selection panel and the hosted queue panel while it shows
     * something, else the selection panel alone (the hidden panel's region has no width); the minimap's camera outline leaves out the rows of the
     * resource bar and the console; the waiting dialog stands at the centre of the world between them.
     *
     * @param width  the logical width of the screen
     * @param height the logical height of the screen
     */
    private void layout(int width, int height) {
        int consoleHeight = getConsoleHeight(height);
        float consoleTop = height - consoleHeight;
        float sectionTop = consoleTop + SECTION_GAP;
        float sectionHeight = consoleHeight - 2 * SECTION_GAP;
        float innerWidth = Math.max(width - 2 * SECTION_GAP, 0);
        float mapAspect = (this.camera.getBoundsRight() - this.camera.getBoundsLeft()) / (this.camera.getBoundsBottom() - this.camera.getBoundsTop());
        float minimapHeight = Math.min(sectionHeight, innerWidth);
        float minimapWidth = Math.min(Math.max(minimapHeight * mapAspect, minimapHeight), Math.max(innerWidth * MINIMAP_WIDTH_SHARE, minimapHeight));
        // A session without local player (a replay) issues nothing: the command card takes no room and the gap before it goes too.
        boolean commanding = this.session.getLocalPlayer() != null;
        float cardWidth = commanding
                          ? Math.min(COMMAND_CARD_WIDTH, Math.max(width - minimapWidth - GAPS_WITH_CARD * SECTION_GAP, 0) / COMMAND_CARD_SHARE)
                          : 0;
        float room = Math.max(width - minimapWidth - cardWidth - (commanding ? GAPS_WITH_CARD : GAPS_WITHOUT_CARD) * SECTION_GAP, 0);
        float selectionLeft = minimapWidth + 2 * SECTION_GAP;
        HudBlock panel = this.queuePanel;
        WaitingDialog dialog = this.waitingDialog;

        this.camera.setScreenSize(width, height);
        this.resourceBar.setBounds(0, 0, width, RESOURCE_BAR_HEIGHT);
        this.consolePlate.setBounds(0, consoleTop, width, consoleHeight);
        this.minimap.setBounds(SECTION_GAP, sectionTop, minimapWidth, minimapHeight);
        this.minimap.setViewInsets(RESOURCE_BAR_HEIGHT, consoleHeight);
        this.commandCard.setBounds(width - SECTION_GAP - cardWidth, sectionTop, cardWidth, commanding ? sectionHeight : 0);

        if (dialog != null) {
            dialog.placeAt(width / HALF, (RESOURCE_BAR_HEIGHT + consoleTop) / HALF);
        }

        if (panel == null || !panel.isShown()) {
            this.selectionPanel.setBounds(selectionLeft, sectionTop, room, sectionHeight);

            if (panel != null) {
                panel.setBounds(selectionLeft + room, sectionTop, 0, sectionHeight);
            }

            return;
        }

        float queueWidth = Math.min(this.queuePanelWidth, Math.max(room - SECTION_GAP, 0) / 2);
        float selectionWidth = Math.max(room - SECTION_GAP - queueWidth, 0);

        this.selectionPanel.setBounds(selectionLeft, sectionTop, selectionWidth, sectionHeight);
        panel.setBounds(selectionLeft + selectionWidth + SECTION_GAP, sectionTop, queueWidth, sectionHeight);
    }

    /**
     * Disposes resources: releases the minimap's uploaded image.
     */
    @Override
    public void close() {
        IgnoreException.execute(
                this.minimap::close
        );
    }
}
