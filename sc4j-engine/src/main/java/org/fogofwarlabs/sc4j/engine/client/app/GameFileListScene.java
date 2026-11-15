package org.fogofwarlabs.sc4j.engine.client.app;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.client.community.TableBox;
import org.fogofwarlabs.sc4j.engine.client.community.TableRow;
import org.fogofwarlabs.sc4j.engine.client.dialog.*;
import org.fogofwarlabs.sc4j.engine.client.game.GameTimeUtils;
import org.fogofwarlabs.sc4j.engine.client.input.InputEvent;
import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.game.PlayerSlot;
import org.fogofwarlabs.sc4j.engine.resource.dialog.ControlKind;
import org.fogofwarlabs.sc4j.engine.resource.dialog.DialogControl;
import org.fogofwarlabs.sc4j.engine.resource.dialog.DialogDefinition;
import org.fogofwarlabs.sc4j.engine.resource.dialog.TextAlignment;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Shows the stored games of a mod as a table to choose from: the engine default scene of the replay list and the load screen. It lists the
 * folder of the mod storage through the mod's registered formats when it opens ({@link GameFileListUtils}), newest first, centred on the
 * screen in a dialog of the engine controls: a centred title, the table ({@link TableBox}) with one row per stored game (its map, its
 * players, its game time ({@link GameTimeUtils}), the date it was stored and its file name), a line with the number of entries, an open button
 * (Enter) and a back button (Escape). The players column names the playing slots by the mod's names ({@link #setPlayerNames}, by default
 * {@code Player 1}, {@code Player 2}), two of them as {@code A vs B}, or shows their number when the format reads only that. Opening the
 * selected entry or going back is the mod's {@link Listener}'s choice; what shows behind the dialog, for example the mod's menu picture, is
 * the mod's {@link Backdrop} ({@link #setBackdrop}).
 */
public final class GameFileListScene implements ClientScene, DialogListener {
    /**
     * Receives the choice made in a {@link GameFileListScene}.
     */
    public interface Listener {
        /**
         * Called when the user opens an entry.
         *
         * @param entry the entry
         */
        void onChosen(@NotNull GameFileEntry entry);

        /**
         * Called when the user leaves the list without choosing.
         */
        void onCancelled();
    }

    /**
     * Draws what shows behind the dialog of a {@link GameFileListScene}, for example the mod's menu picture.
     */
    @FunctionalInterface
    public interface Backdrop {
        /**
         * Draws the backdrop over the whole surface.
         *
         * @param surface the surface
         * @throws IOException if a picture cannot be read
         */
        void draw(@NotNull Canvas surface) throws IOException;
    }

    /**
     * Lists the entries of a {@link GameFileListScene} when it opens.
     */
    @FunctionalInterface
    public interface Source {
        /**
         * Lists the entries.
         *
         * @param client the client the scene runs on
         * @return the entries in their shown order
         * @throws IOException if the stored games cannot be listed
         */
        @NotNull List<@NotNull GameFileEntry> list(@NotNull SceneClient client) throws IOException;
    }

    /**
     * The id of the title label.
     */
    public static final int TITLE_ID = 1;
    /**
     * The id of the table.
     */
    public static final int LIST_ID = 2;
    /**
     * The id of the line with the number of entries.
     */
    public static final int STATUS_ID = 3;
    /**
     * The id of the open button.
     */
    public static final int OPEN_ID = 4;
    /**
     * The id of the back button.
     */
    public static final int BACK_ID = 5;
    /**
     * The header texts of the table's columns.
     */
    public static final @NotNull List<@NotNull String> HEADERS = List.of("Map", "Players", "Length", "Date", "File");

    private static final int PANEL_ID = 0;
    private static final int WIDTH = 880;
    private static final int HEIGHT = 440;
    private static final int MARGIN = 16;
    private static final int LINE_HEIGHT = 24;
    private static final int BUTTON_WIDTH = 140;
    private static final int BUTTON_HEIGHT = 32;
    private static final int HALF = 2;
    /**
     * The margins left and right of the inner width.
     */
    private static final int HORIZONTAL_MARGIN_COUNT = 2;
    /**
     * The buttons of the bottom row, each with a margin to its right.
     */
    private static final int BUTTON_COUNT = 2;
    /**
     * The share of the table's width of each column: map, players, length, date, file.
     */
    private static final float @NotNull [] COLUMN_SHARES = {0.16f, 0.30f, 0.10f, 0.23f, 0.21f};
    private static final @NotNull DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.ROOT);
    /**
     * The number of playing slots the players column shows as {@code A vs B}.
     */
    private static final int TWO_PLAYERS = 2;
    private static final @NotNull String TWO_PLAYERS_SEPARATOR = " vs ";
    private static final @NotNull String PLAYERS_SEPARATOR = ", ";
    private static final @NotNull String PLAYERS_SUFFIX = " players";
    private static final @NotNull String PLAYER_PREFIX = "Player ";
    private static final int TEXT_COLOR = 0xFFFFFFFF;

    /**
     * Creates a {@link GameFileListScene} instance of the replays in a folder of the mod storage, read through the launch's replay formats.
     *
     * @param title            the title
     * @param theme            the look of the controls
     * @param folder           the folder relative to the mod storage
     * @param loopMilliseconds the client time of one game loop at normal speed in milliseconds, for the game time
     * @param listener         the receiver of the choice
     * @return the created {@link GameFileListScene} instance
     */
    public static @NotNull GameFileListScene createReplayList(@NotNull String title, @NotNull DialogTheme theme, @NotNull String folder,
                                                              long loopMilliseconds, @NotNull Listener listener) {
        return new GameFileListScene(title, theme,
                                     client -> GameFileListUtils.listReplays(client.getStorage().resolve(folder), client.getLaunch().getReplayFormats()),
                                     loopMilliseconds, listener);
    }

    /**
     * Creates a {@link GameFileListScene} instance of the saved games in a folder of the mod storage, read through the launch's save formats.
     *
     * @param title            the title
     * @param theme            the look of the controls
     * @param folder           the folder relative to the mod storage
     * @param loopMilliseconds the client time of one game loop at normal speed in milliseconds, for the game time
     * @param listener         the receiver of the choice
     * @return the created {@link GameFileListScene} instance
     */
    public static @NotNull GameFileListScene createSaveList(@NotNull String title, @NotNull DialogTheme theme, @NotNull String folder,
                                                            long loopMilliseconds, @NotNull Listener listener) {
        return new GameFileListScene(title, theme,
                                     client -> GameFileListUtils.listSaves(client.getStorage().resolve(folder), client.getLaunch().getSaveFormats()),
                                     loopMilliseconds, listener);
    }

    /**
     * Describes an entry as the cells of its table row: the map, the players, the game time, the date and the file name.
     *
     * @param entry            the entry
     * @param loopMilliseconds the client time of one game loop at normal speed in milliseconds
     * @param playerNames      the shown name of each playing slot
     * @return the cells in column order
     */
    public static @NotNull List<@NotNull String> describe(@NotNull GameFileEntry entry, long loopMilliseconds,
                                                          @NotNull Function<@NotNull PlayerSlot, @NotNull String> playerNames) {
        List<PlayerSlot> players = entry.getPlayers();
        String shownPlayers;

        if (players.isEmpty()) {
            shownPlayers = entry.getPlayerCount() + PLAYERS_SUFFIX;
        } else {
            List<String> names = new ArrayList<>(players.size());

            for (PlayerSlot player : players) {
                names.add(playerNames.apply(player));
            }

            shownPlayers = String.join(names.size() == TWO_PLAYERS ? TWO_PLAYERS_SEPARATOR : PLAYERS_SEPARATOR, names);
        }

        return List.of(entry.getMapName(), shownPlayers, GameTimeUtils.format(entry.getFrameCount(), loopMilliseconds),
                       DATE_FORMAT.format(entry.getModifiedTime().atZone(ZoneId.systemDefault())), entry.getPath().getFileName().toString());
    }

    private static @NotNull DialogControl createControl(int id, @NotNull ControlKind kind, int x, int y, int width, int height, @Nullable String text,
                                                        @NotNull TextAlignment alignment, @NotNull Map<@NotNull String, @NotNull String> attributes) {
        return new DialogControl(id, kind, x, y, width, height, text, alignment, null, attributes, List.of());
    }

    private static @NotNull DialogDefinition createDefinition(@NotNull String title) {
        int listTop = MARGIN + LINE_HEIGHT + MARGIN;
        int buttonTop = HEIGHT - MARGIN - BUTTON_HEIGHT;
        int statusTop = buttonTop - MARGIN - LINE_HEIGHT;
        int innerWidth = WIDTH - MARGIN * HORIZONTAL_MARGIN_COUNT;
        int openLeft = WIDTH - MARGIN * BUTTON_COUNT - BUTTON_WIDTH * BUTTON_COUNT;
        int backLeft = WIDTH - MARGIN - BUTTON_WIDTH;
        List<DialogControl> children = List.of(
                createControl(TITLE_ID, ControlKind.LABEL, MARGIN, MARGIN, innerWidth, LINE_HEIGHT, title, TextAlignment.CENTER, Map.of()),
                createControl(LIST_ID, ControlKind.LIST_BOX, MARGIN, listTop, innerWidth, statusTop - MARGIN - listTop, null, TextAlignment.LEFT,
                              Map.of()),
                createControl(STATUS_ID, ControlKind.LABEL, MARGIN, statusTop, innerWidth, LINE_HEIGHT, null, TextAlignment.LEFT, Map.of()),
                createControl(OPEN_ID, ControlKind.BUTTON, openLeft, buttonTop, BUTTON_WIDTH, BUTTON_HEIGHT, "Open", TextAlignment.CENTER,
                              Map.of(Control.DEFAULT_ATTRIBUTE, Control.TRUE_VALUE, Control.HOTKEY_ATTRIBUTE, "o")),
                createControl(BACK_ID, ControlKind.BUTTON, backLeft, buttonTop, BUTTON_WIDTH, BUTTON_HEIGHT, "Back", TextAlignment.CENTER,
                              Map.of(Control.CANCEL_ATTRIBUTE, Control.TRUE_VALUE, Control.HOTKEY_ATTRIBUTE, "b"))
        );
        DialogControl panel = new DialogControl(PANEL_ID, ControlKind.PANEL, 0, 0, WIDTH, HEIGHT, null, TextAlignment.LEFT, null, Map.of(), children);

        return new DialogDefinition(title, 0, 0, WIDTH, HEIGHT, List.of(panel));
    }


    private final @NotNull DialogTheme theme;
    private final @NotNull Source source;
    private final long loopMilliseconds;
    private final @NotNull Listener listener;

    private final @NotNull Dialog dialog;
    private final @NotNull TableBox table;
    private final @NotNull List<@NotNull GameFileEntry> entries;
    private @Nullable Backdrop backdrop;
    private @NotNull Function<@NotNull PlayerSlot, @NotNull String> playerNames;

    /**
     * Creates a {@link GameFileListScene} instance; it lists nothing until it opens.
     *
     * @param title            the title
     * @param theme            the look of the controls
     * @param source           lists the entries when the scene opens
     * @param loopMilliseconds the client time of one game loop at normal speed in milliseconds, for the game time
     * @param listener         the receiver of the choice
     */
    public GameFileListScene(@NotNull String title, @NotNull DialogTheme theme, @NotNull Source source, long loopMilliseconds,
                             @NotNull Listener listener) {
        Dialog created = new Dialog(createDefinition(title), new DefaultControlFactory(theme.getFont().getLineHeight()), this, false);
        Control list = created.findControl(LIST_ID);

        if (list == null) {
            throw Exceptions.newIllegalStateException("The list dialog has no control {0}.", LIST_ID);
        }

        this.theme            = theme;
        this.source           = source;
        this.loopMilliseconds = loopMilliseconds;
        this.listener         = listener;

        this.dialog      = created;
        this.table       = created.replaceControl(list, new TableBox(list.getDefinition(), theme.getFont().getLineHeight(),
                                                                     COLUMN_SHARES, HEADERS, false));
        this.entries     = new ArrayList<>();
        this.backdrop    = null;
        this.playerNames = slot -> PLAYER_PREFIX + (slot.getSlotIndex() + 1);
    }

    /**
     * @return the listed entries in their shown order, empty before the scene opens
     */
    public @NotNull List<@NotNull GameFileEntry> getEntries() {
        return List.copyOf(this.entries);
    }

    /**
     * @return what shows behind the dialog, or {@code null} for the client's black background
     */
    public @Nullable Backdrop getBackdrop() {
        return this.backdrop;
    }

    /**
     * Sets what shows behind the dialog.
     *
     * @param backdrop the backdrop, or {@code null} for the client's black background
     */
    public void setBackdrop(@Nullable Backdrop backdrop) {
        this.backdrop = backdrop;
    }

    /**
     * Sets the shown name of each playing slot in the players column, for example the name of its faction.
     *
     * @param playerNames the shown name of each playing slot
     */
    public void setPlayerNames(@NotNull Function<@NotNull PlayerSlot, @NotNull String> playerNames) {
        this.playerNames = playerNames;
    }

    /**
     * @return the table of the stored games
     */
    public @NotNull TableBox getTable() {
        return this.table;
    }

    /**
     * @return the dialog of the scene
     */
    public @NotNull Dialog getDialog() {
        return this.dialog;
    }

    /**
     * @return the selected entry, or {@code null} for none
     */
    public @Nullable GameFileEntry getSelectedEntry() {
        int selection = this.table.getSelection();

        return selection >= 0 && selection < this.entries.size() ? this.entries.get(selection) : null;
    }

    /**
     * Opens the scene: loads what it shows. Called once, before the first {@link #update}.
     * <p>
     * Lists the stored games and selects the first, the newest.
     *
     * @param context the client's shared services
     * @throws IOException if the stored games cannot be listed
     */
    @Override
    public void open(@NotNull SceneClient context) throws IOException {
        List<TableRow> rows = new ArrayList<>();

        this.entries.clear();
        this.entries.addAll(this.source.list(context));

        for (GameFileEntry entry : this.entries) {
            rows.add(new TableRow(describe(entry, this.loopMilliseconds, this.playerNames), TEXT_COLOR, null));
        }

        this.table.setRows(rows);
        this.table.setSelection(this.entries.isEmpty() ? TableBox.NO_SELECTION : 0);
        this.requireControl(STATUS_ID).setText(this.entries.isEmpty() ? "Nothing stored yet." : this.entries.size() + " stored.");
        this.requireControl(OPEN_ID).setEnabled(!this.entries.isEmpty());
    }

    /**
     * Updates the scene at the supplied time. The list changes only on input.
     *
     * @param time the client time in milliseconds
     */
    @Override
    public void update(long time) {
    }

    /**
     * Returns the logical width the scene needs to show whole: the dialog's width with a margin on each side.
     *
     * @return the logical width the scene needs
     */
    @Override
    public int getMinimumWidth() {
        return WIDTH + 2 * MARGIN;
    }

    /**
     * Returns the logical height the scene needs to show whole: the dialog's height with a margin on each side.
     *
     * @return the logical height the scene needs
     */
    @Override
    public int getMinimumHeight() {
        return HEIGHT + 2 * MARGIN;
    }

    /**
     * Draws the scene: the backdrop, then the dialog, centred on the surface.
     *
     * @param surface the surface
     * @throws IOException if the backdrop cannot read a picture it draws
     */
    @Override
    public void draw(@NotNull Canvas surface) throws IOException {
        Backdrop current = this.backdrop;

        if (current != null) {
            current.draw(surface);
        }

        this.dialog.setPosition((surface.getWidth() - WIDTH) / (float) HALF, (surface.getHeight() - HEIGHT) / (float) HALF);
        this.dialog.draw(surface, this.theme);
    }

    /**
     * Handles an input event the scene's stack hands to it: the dialog takes it.
     *
     * @param event the event
     * @return {@code true} if the scene used the event, {@code false} otherwise
     */
    @Override
    public boolean handleInput(@NotNull InputEvent event) {
        return this.dialog.handleInput(event);
    }

    /**
     * Called when a control is activated: a released press, Enter, Escape or a hotkey.
     * <p>
     * The open button reports the selected entry, the back button the cancellation.
     *
     * @param dialog  the dialog
     * @param control the activated control
     */
    @Override
    public void onActivated(@NotNull Dialog dialog, @NotNull Control control) {
        int id = control.getDefinition().getId();

        if (id == OPEN_ID) {
            GameFileEntry entry = this.getSelectedEntry();

            if (entry != null) {
                this.listener.onChosen(entry);
            }
        } else if (id == BACK_ID) {
            this.listener.onCancelled();
        }
    }

    /**
     * Closes the scene: releases what it holds. The dialog holds nothing to release.
     */
    @Override
    public void close() {
    }

    private @NotNull Control requireControl(int id) {
        Control control = this.dialog.findControl(id);

        if (control == null) {
            throw Exceptions.newIllegalStateException("The list dialog has no control {0}.", id);
        }

        return control;
    }
}
