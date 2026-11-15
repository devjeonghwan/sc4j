package org.fogofwarlabs.sc4j.demo.client;

import org.fogofwarlabs.sc4j.base.exception.IgnoreException;
import org.fogofwarlabs.sc4j.demo.order.DemoOrders;
import org.fogofwarlabs.sc4j.engine.client.command.CommandRefusal;
import org.fogofwarlabs.sc4j.engine.client.console.Console;
import org.fogofwarlabs.sc4j.engine.client.console.ConsoleCommandRegistry;
import org.fogofwarlabs.sc4j.engine.client.display.DetailSet;
import org.fogofwarlabs.sc4j.engine.client.game.GameScreen;
import org.fogofwarlabs.sc4j.engine.client.game.GameSession;
import org.fogofwarlabs.sc4j.engine.client.hud.HudListener;
import org.fogofwarlabs.sc4j.engine.client.hud.OrderTexts;
import org.fogofwarlabs.sc4j.engine.client.hud.ProductionQueuePanel;
import org.fogofwarlabs.sc4j.engine.client.input.InputEvent;
import org.fogofwarlabs.sc4j.engine.client.input.Key;
import org.fogofwarlabs.sc4j.engine.client.projection.WorldProjection;
import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.client.text.Font;
import org.fogofwarlabs.sc4j.engine.client.view.GameView;
import org.fogofwarlabs.sc4j.engine.client.view.UnitView;
import org.fogofwarlabs.sc4j.engine.client.world.ProjectileSpriteLayer;
import org.fogofwarlabs.sc4j.engine.client.world.TerrainColors;
import org.fogofwarlabs.sc4j.engine.client.world.TilesetLayer;
import org.fogofwarlabs.sc4j.engine.client.world.UnitSpriteLayer;
import org.fogofwarlabs.sc4j.engine.content.definition.UnitTrait;
import org.fogofwarlabs.sc4j.engine.content.table.UnitTypeTable;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.map.MapModel;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.fogofwarlabs.sc4j.engine.resource.graphics.TileGraphics;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.List;

/**
 * Shows a demo game on the engine {@link GameScreen}: the map's diamond tileset and the units' sprites in the demo's isometric projection over the
 * demo's dark backdrop beyond the map's edge ({@link DemoSkin#getBackdrop}), the engine HUD in the skin of the local player's faction (the followed
 * player's faction in a replay) with the demo's cursors and the engine production queue panel in the console's queue region, the texts of the current
 * orders in the selection panel ({@link #createOrderTexts}), a building that waits for its site drawn as the ghost of its sprite, the camera
 * scrolling at the speed of the options, and over it the console with the latest messages and a status line. A message shows for
 * {@value #MESSAGE_SHOWN_MILLISECONDS} milliseconds and fades out over {@value #MESSAGE_FADE_MILLISECONDS}. A pressed HUD button plays the click
 * sound, units the player selects their selection sound, a refused action and a producer that starts waiting for supply the refusal sound and its
 * message. Enter opens the console's typed line; a line starting with {@code /} runs one of the screen's slash commands ({@code /help} lists them),
 * any other line shows as a message. The camera starts on the town hall of the shown player.
 */
public final class DemoGameScreen implements AutoCloseable {
    /**
     * The time a message shows fully, in milliseconds.
     */
    public static final long MESSAGE_SHOWN_MILLISECONDS = 10000;
    /**
     * The time a message then fades out over, in milliseconds.
     */
    public static final long MESSAGE_FADE_MILLISECONDS = 2000;

    private static final @NotNull String SUPPLY_NAME = "Supply";
    private static final @NotNull String MOVING = "Moving";
    private static final @NotNull String ATTACKING = "Attacking";
    private static final @NotNull String ADVANCING = "Advancing";
    private static final @NotNull String PATROLLING = "Patrolling";
    private static final @NotNull String HOLDING = "Holding";
    private static final @NotNull String GATHERING = "Gathering";
    private static final @NotNull String RETURNING = "Returning";
    private static final @NotNull String BUILDING = "Building";
    private static final int PAGE_LINES = 8;
    private static final int MESSAGE_LINES = 8;
    private static final int TEXT_COLOR = 0xF0E6C8FF;
    private static final int STATUS_COLOR = 0xC8D2DCFF;
    private static final float MARGIN = 8;
    private static final float HALF = 2;
    /**
     * The width of the console's queue region in logical units: five queue slots of 48 with the panel's padding and gaps.
     */
    private static final float QUEUE_PANEL_WIDTH = 268;

    /**
     * Plays the HUD's click, selection and refusal sounds and shows the message of a refusal or of a producer that waits for supply.
     */
    private static final class Feedback implements HudListener {
        private final @NotNull DemoSounds sounds;
        private final @NotNull Console console;
        private final @NotNull GameView view;
        private final @NotNull UnitTypeTable unitTypes;

        private Feedback(@NotNull DemoSounds sounds, @NotNull Console console, @NotNull GameView view, @NotNull UnitTypeTable unitTypes) {
            this.sounds    = sounds;
            this.console   = console;
            this.view      = view;
            this.unitTypes = unitTypes;
        }

        /**
         * Called when a press on a HUD button runs it: a command card button or a queue slot.
         * <p>
         * Plays the click sound.
         */
        @Override
        public void onButtonPressed() {
            this.sounds.play(DemoSounds.CLICK);
        }

        /**
         * Called when the player's input (a click, a selection box, a selection panel cell, a key) changed the selection and units are
         * selected; a selection the game changes (a unit that died) is not reported.
         * <p>
         * Plays the selection sound of the first unit: a building's knock or a unit's notes.
         *
         * @param units the selected units in selection order, a read-only view that changes with the selection
         */
        @Override
        public void onUnitsSelected(@NotNull List<@NotNull Unit> units) {
            UnitType type = this.view.getUnitView(units.getFirst()).getType();
            boolean building = type != null && this.unitTypes.hasTrait(type, UnitTrait.BUILDING);

            this.sounds.play(building ? DemoSounds.SELECT_BUILDING : DemoSounds.SELECT);
        }

        /**
         * Called when the player tries to run an action that is refused now, by a button or a hotkey.
         * <p>
         * Plays the refusal sound and shows the message.
         *
         * @param refusal the refusal
         * @param message the refusal as a short message line, for example {@code Not enough Gold.}
         */
        @Override
        public void onCommandRefused(@NotNull CommandRefusal refusal, @NotNull String message) {
            this.sounds.play(DemoSounds.REFUSED);
            this.console.addLine(message);
        }

        /**
         * Called when the front entry of a producer of the local player starts waiting for supply ({@link UnitView#isWaitingForSupply}); the
         * screen calls it once per producer and wait, for at most one producer per update.
         * <p>
         * Plays the refusal sound and shows the message.
         *
         * @param producer the producer
         * @param refusal  the refusal of the wait, {@link CommandRefusal#MISSING_SUPPLY}
         * @param message  the wait as a short message line, for example {@code Not enough Supply.}
         */
        @Override
        public void onProductionWaiting(@NotNull Unit producer, @NotNull CommandRefusal refusal, @NotNull String message) {
            this.sounds.play(DemoSounds.REFUSED);
            this.console.addLine(message);
        }
    }

    /**
     * Creates the texts the selection panel shows for the demo's orders; the idle, guard and stop orders show none.
     *
     * @param orders the demo's orders
     * @return the created {@link OrderTexts} instance
     */
    public static @NotNull OrderTexts createOrderTexts(@NotNull DemoOrders orders) {
        OrderTexts texts = new OrderTexts();

        texts.setText(orders.getMove(), MOVING, false);
        texts.setText(orders.getAttack(), ATTACKING, true);
        texts.setText(orders.getAttackMove(), ADVANCING, false);
        texts.setText(orders.getPatrol(), PATROLLING, false);
        texts.setText(orders.getHoldPosition(), HOLDING, false);
        texts.setText(orders.getGather(), GATHERING, true);
        texts.setText(orders.getReturnResources(), RETURNING, false);
        texts.setText(orders.getBuild(), BUILDING, false);

        return texts;
    }

    private final @NotNull GameSession session;
    private final @NotNull Font font;
    private final @Nullable Player focus;

    private final @NotNull TilesetLayer terrain;
    private final @NotNull UnitSpriteLayer units;
    private final @NotNull ProjectileSpriteLayer projectiles;
    private final @NotNull GameScreen screen;
    private final @NotNull ProductionQueuePanel queuePanel;
    private final @NotNull ConsoleCommandRegistry commands;
    private final @NotNull Console console;

    private boolean centered;

    /**
     * Creates a {@link DemoGameScreen} instance; it uploads its pictures when it draws.
     *
     * @param demo    the demo's shared services
     * @param session the session it shows
     * @param map     the game's map
     * @param focus   the player whose town hall the camera starts on, or {@code null} for the map's center
     * @throws IOException if the map's tileset cannot be read
     */
    public DemoGameScreen(@NotNull DemoClient demo, @NotNull GameSession session, @NotNull MapModel map, @Nullable Player focus) throws IOException {
        Canvas canvas = demo.getClient().getCanvas();
        GameView view = session.getView();
        DetailSet<TileGraphics> tiles = DemoWorldUtils.loadTileGraphics(demo.getContent(), map);
        WorldProjection projection = DemoWorldUtils.createProjection(map, tiles);

        this.session = session;
        this.font    = demo.getFont();
        this.focus   = focus;

        this.terrain     = DemoWorldUtils.createTerrainLayer(canvas, view, demo.getContent(), map, tiles);
        this.units       = new UnitSpriteLayer(canvas, view, demo.getSprites()::findSprite);
        this.projectiles = new ProjectileSpriteLayer(canvas, view, demo.getSprites()::findProjectileSprite);
        this.screen      = new GameScreen(session, canvas, demo.getFont(), projection, new TerrainColors(), this.terrain, viewport -> {
            this.units.draw(viewport);
            this.projectiles.draw(viewport);
        }, DemoClient::getName, SUPPLY_NAME, DemoClient::getName);
        this.queuePanel  = new ProductionQueuePanel(view, this.screen.getSelection(), this.screen.getTranslator(), demo.getFont(),
                                                    DemoClient::getName, SUPPLY_NAME);
        this.commands    = new ConsoleCommandRegistry(PAGE_LINES);
        this.console     = new Console(this.commands, this::addMessage, MESSAGE_LINES);

        this.centered = false;

        this.console.setLineTimes(MESSAGE_SHOWN_MILLISECONDS, MESSAGE_FADE_MILLISECONDS);
        this.screen.setSkins(demo.getSkin().getHudSkins());
        this.screen.setBackdrop(demo.getSkin().getBackdrop());
        this.screen.setQueuePanel(this.queuePanel, QUEUE_PANEL_WIDTH);
        this.screen.setCursor(demo.getSkin().getCursor());
        this.screen.getPlacement().setGhost(this.units);
        this.screen.getSelectionPanel().setOrderTexts(createOrderTexts(demo.getContent().getOrders()));
        demo.getOptions().applyTo(this.screen.getScrollRate());
        this.screen.setListener(new Feedback(demo.getSounds(), this.console, view, session.getGame().getContent().getUnitTypes()));
        this.screen.registerCommands(this.commands);
    }

    /**
     * @return the read-only view of the shown game
     */
    public @NotNull GameView getView() {
        return this.session.getView();
    }

    /**
     * @return the engine game screen
     */
    public @NotNull GameScreen getScreen() {
        return this.screen;
    }

    /**
     * @return the production queue panel in the console's queue region
     */
    public @NotNull ProductionQueuePanel getQueuePanel() {
        return this.queuePanel;
    }

    /**
     * @return the registry of the screen's slash commands, for the scene's own commands
     */
    public @NotNull ConsoleCommandRegistry getCommands() {
        return this.commands;
    }

    /**
     * @return the console with the messages and the typed line
     */
    public @NotNull Console getConsole() {
        return this.console;
    }

    /**
     * Shows a message line over the game, the oldest line making room.
     *
     * @param message the message
     */
    public void addMessage(@NotNull String message) {
        this.console.addLine(message);
    }

    /**
     * Runs a slash command line as if it was typed, and shows its reply as messages.
     *
     * @param line the command line, starting with {@code /}
     */
    public void runCommand(@NotNull String line) {
        this.console.submit(line);
    }

    /**
     * Updates the screen for the time since the last frame: the game screen and the messages' fading.
     *
     * @param milliseconds the time since the last frame
     */
    public void update(long milliseconds) {
        this.screen.update(milliseconds);
        this.console.update(milliseconds);
    }

    /**
     * Draws the screen on a canvas that has begun: the game screen, the status line at the top right, and the messages and the typed line
     * below it.
     *
     * @param canvas the canvas
     * @param status the status line
     * @throws IOException if a world layer cannot read a resource it draws
     */
    public void draw(@NotNull Canvas canvas, @NotNull String status) throws IOException {
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        float top = GameScreen.RESOURCE_BAR_HEIGHT + MARGIN;

        if (!this.centered) {
            this.centered = true;
            this.screen.getCamera().setScreenSize(width, height);
            this.centerOnStart();
        }

        this.screen.drawContent(width, height);
        this.console.draw(canvas, this.font, MARGIN, top + this.font.getLineHeight(), TEXT_COLOR);
        this.font.draw(canvas, status, width - MARGIN - this.font.measure(status), top, STATUS_COLOR);
    }

    /**
     * Handles an input event: the open console takes it, Enter opens the console, anything else goes to the game screen. A key released while
     * the console is open, which the console does not use, still goes to the game screen, so an arrow key held when the console opened stops
     * scrolling the camera.
     *
     * @param event the event
     * @return {@code true} if a part of the screen took the event, {@code false} otherwise
     */
    public boolean handleInput(@NotNull InputEvent event) {
        if (this.console.isOpen()) {
            return this.console.handleInput(event) || event.getType() == InputEvent.Type.KEY_RELEASED && this.screen.handleInput(event);
        }

        if (event.getType() == InputEvent.Type.KEY_PRESSED && (event.getKey() == Key.ENTER || event.getKey() == Key.KEYPAD_ENTER)) {
            this.console.open();

            return true;
        }

        return this.screen.handleInput(event);
    }

    private void centerOnStart() {
        Game game = this.session.getGame();
        GameView view = this.session.getView();
        List<Unit> shown = view.getUnits();

        for (Unit shownUnit : shown) {
            UnitView unit = view.getUnitView(shownUnit);
            UnitType type = unit.getType();

            if (type != null && unit.getOwner() != null && (this.focus == null || unit.getOwner() == this.focus) &&
                game.getContent().getUnitTypes().hasTrait(type, UnitTrait.RESOURCE_DEPOT)) {
                this.screen.getCamera().centerOn(unit.getX(), unit.getY());

                return;
            }
        }

        this.screen.getCamera().centerOn(view.getMapWidth() / HALF, view.getMapHeight() / HALF);
    }

    /**
     * Disposes resources: releases the uploaded pictures of the screen, the projectile and unit sprites and the tileset.
     */
    @Override
    public void close() {
        IgnoreException.execute(
                this.screen::close,
                this.projectiles::close,
                this.units::close,
                this.terrain::close
        );
    }
}
