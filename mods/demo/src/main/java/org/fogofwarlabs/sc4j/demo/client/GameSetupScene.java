package org.fogofwarlabs.sc4j.demo.client;

import org.fogofwarlabs.sc4j.engine.client.dialog.ComboBox;
import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.content.faction.Faction;
import org.fogofwarlabs.sc4j.engine.game.GameSetup;
import org.fogofwarlabs.sc4j.engine.game.MapReference;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Shows the demo's game setup: the map, the person's faction, the second slot (a computer player) and its faction, with the chosen factions' emblems
 * beside it. Start begins the game behind the loading screen of the person's faction, back returns to the main menu. The game's seed is the
 * client time of the start.
 */
public final class GameSetupScene extends DemoMenuScene {
    /**
     * The pack id of the setup's dialog.
     */
    public static final @NotNull String DIALOG = "game-setup";
    /**
     * The id of the map list.
     */
    public static final int MAP_ID = 10;
    /**
     * The id of the person's faction list.
     */
    public static final int FACTION_ID = 11;
    /**
     * The id of the second slot's player list.
     */
    public static final int SLOT_ID = 12;
    /**
     * The id of the second slot's faction list.
     */
    public static final int OPPONENT_FACTION_ID = 13;
    /**
     * The id of the start button.
     */
    public static final int START_ID = 20;
    /**
     * The id of the back button.
     */
    public static final int BACK_ID = 21;

    private static final @NotNull String COMPUTER = "Computer";

    private final @NotNull MainMenuScene menu;

    /**
     * Creates a {@link GameSetupScene} instance.
     *
     * @param demo the demo's shared services
     * @param menu the main menu the back button returns to
     */
    public GameSetupScene(@NotNull DemoClient demo, @NotNull MainMenuScene menu) {
        super(demo, DIALOG);

        this.menu = menu;
    }

    /**
     * Creates the setup the dialog shows.
     *
     * @return the created {@link GameSetup} instance
     */
    public @NotNull GameSetup createSetup() {
        DemoClient demo = this.getDemo();
        List<MapReference> maps = demo.getMaps().getReferences();
        MapReference map = maps.get(Math.max(0, this.requireComboBox(MAP_ID).getSelection()));

        return demo.getGames().createPersonSetup(map, this.findFaction(FACTION_ID), this.findFaction(OPPONENT_FACTION_ID),
                                                 demo.getClient().getTime());
    }

    /**
     * Called when the scene opened and its dialog is built: fills the dialog's controls. The default fills nothing.
     * <p>
     * Lists the maps, the factions and the computer slot; the person plays the first faction, the computer the second.
     */
    @Override
    protected void onOpened() {
        List<String> maps = new ArrayList<>();
        List<String> factions = new ArrayList<>();

        for (MapReference map : this.getDemo().getMaps().getReferences()) {
            maps.add(map.getName());
        }

        for (Faction faction : this.getDemo().getContent().getFactions()) {
            factions.add(DemoClient.getName(faction));
        }

        this.fill(MAP_ID, maps, 0);
        this.fill(FACTION_ID, factions, 0);
        this.fill(SLOT_ID, List.of(COMPUTER), 0);
        this.fill(OPPONENT_FACTION_ID, factions, factions.size() > 1 ? 1 : 0);
    }

    /**
     * Draws over the screen after the dialog.
     * <p>
     * Draws the person's faction's emblem left of the setup and the second slot's faction's right of it.
     *
     * @param surface the surface
     * @param left    the left coordinate of the dialog
     * @param top     the top coordinate of the dialog
     * @param width   the width of the dialog
     * @param height  the height of the dialog
     * @throws IOException if a picture cannot be read
     */
    @Override
    protected void drawOver(@NotNull Canvas surface, float left, float top, float width, float height) throws IOException {
        this.drawEmblemsBeside(surface, left, top, width, height, this.findFaction(FACTION_ID), this.findFaction(OPPONENT_FACTION_ID));
    }

    /**
     * Called when a control of the dialog is activated.
     * <p>
     * Start shows the loading screen of the person's faction, then the game of the shown setup; back returns to the main menu.
     *
     * @param id the id of the activated control
     */
    @Override
    protected void onActivated(int id) {
        if (id == START_ID) {
            DemoClient demo = this.getDemo();
            GameSetup setup = this.createSetup();

            demo.getClient().getScenes().replace(new LoadingScene(demo, this.findFaction(FACTION_ID), demo.describe(setup),
                                                                  new DemoGameScene(demo, this.menu, setup)));
        } else if (id == BACK_ID) {
            this.menu.show();
        }
    }

    private @NotNull Faction findFaction(int id) {
        return this.getDemo().getContent().getFactions().get(Math.max(0, this.requireComboBox(id).getSelection()));
    }

    private void fill(int id, @NotNull List<@NotNull String> entries, int selection) {
        ComboBox box = this.requireComboBox(id);

        box.setEntries(entries);
        box.setSelection(selection);
    }

    private @NotNull ComboBox requireComboBox(int id) {
        return (ComboBox) this.requireControl(id);
    }
}
