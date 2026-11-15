package org.fogofwarlabs.sc4j.demo.client;

import org.fogofwarlabs.sc4j.base.exception.IgnoreException;
import org.fogofwarlabs.sc4j.base.log.Logger;
import org.fogofwarlabs.sc4j.demo.content.DemoContent;
import org.fogofwarlabs.sc4j.demo.map.DemoMaps;
import org.fogofwarlabs.sc4j.demo.net.DemoLobby;
import org.fogofwarlabs.sc4j.demo.net.DemoNetworkSettings;
import org.fogofwarlabs.sc4j.engine.client.app.ClientScene;
import org.fogofwarlabs.sc4j.engine.client.app.GameFileEntry;
import org.fogofwarlabs.sc4j.engine.client.app.GameFileListScene;
import org.fogofwarlabs.sc4j.engine.client.app.SceneClient;
import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.content.faction.Faction;
import org.fogofwarlabs.sc4j.engine.mod.ModException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Shows the demo's main menu, its first scene, with the emblems of the first two factions beside it: start a game against the computer (the game
 * setup), watch a replay (the engine's list of stored games over the demo's replays on the menu background, its players named by their factions, then
 * the replay viewer), network (games of two people on this computer: the network screen), options ({@link OptionsPopup}) and quit. The client keeps
 * the main menu open while the other screens show (the retained scene of its stack); it owns the demo's shared services and closes them when the
 * client closes it.
 */
public final class MainMenuScene extends DemoMenuScene implements GameFileListScene.Listener {
    /**
     * The pack id of the main menu's dialog.
     */
    public static final @NotNull String DIALOG = "main-menu";
    /**
     * The id of the start button.
     */
    public static final int START_ID = 10;
    /**
     * The id of the replays button.
     */
    public static final int REPLAYS_ID = 11;
    /**
     * The id of the network button.
     */
    public static final int NETWORK_ID = 12;
    /**
     * The id of the quit button.
     */
    public static final int QUIT_ID = 13;
    /**
     * The id of the options button.
     */
    public static final int OPTIONS_ID = 14;

    private static final @NotNull Logger LOGGER = Logger.of(MainMenuScene.class);

    /**
     * Creates the first scene of the demo's client: opens the demo's shared services, keeps the main menu open as the client's retained
     * scene, and starts with the main menu, with the replay viewer when the launch plays a replay, or in the room the launch joins
     * ({@code --join}, an address on this computer).
     *
     * @param client  the client
     * @param content the demo content
     * @param maps    the demo maps
     * @return the first scene
     * @throws ModException if the shared services cannot be opened, or the room to join cannot be reached
     */
    public static @NotNull ClientScene createFirstScene(@NotNull SceneClient client, @NotNull DemoContent content, @NotNull DemoMaps maps) throws ModException {
        DemoClient demo;

        try {
            demo = DemoClient.open(client, content, maps);
        } catch (IOException exception) {
            throw ModException.create(exception, "The demo client cannot be opened: {0}", exception.getMessage());
        }

        MainMenuScene menu = new MainMenuScene(demo);
        Path replay = client.getLaunch().getReplay();
        String join = client.getLaunch().getJoin();

        client.getScenes().setRetainedScene(menu);

        if (join != null) {
            try {
                DemoLobby lobby = DemoLobby.join(DemoNetworkSettings.DEFAULTS, client.getLaunch().getIdentity(), content.createSetupCodec(), maps,
                                                 demo.getTaggedPlayerName(), DemoLobby.parseAddress(join));

                return new NetworkRoomScene(demo, menu, DemoNetworkSettings.DEFAULTS, lobby);
            } catch (IOException | IllegalArgumentException exception) {
                IgnoreException.execute(
                        demo::close
                );

                throw ModException.create(exception, "The demo cannot join {0}: {1}", join, exception.getMessage());
            }
        }

        return replay != null ? new ReplayViewerScene(demo, menu, replay) : menu;
    }

    /**
     * Creates a {@link MainMenuScene} instance that owns the demo's shared services.
     *
     * @param demo the demo's shared services, closed with the scene
     */
    public MainMenuScene(@NotNull DemoClient demo) {
        super(demo, DIALOG);
    }

    /**
     * Shows the main menu again, for a screen that is left.
     */
    public void show() {
        this.getDemo().getClient().getScenes().replace(this);
    }

    /**
     * Draws over the screen after the dialog.
     * <p>
     * Draws the first faction's emblem left of the menu and the second faction's right of it.
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
        List<Faction> factions = this.getDemo().getContent().getFactions();

        this.drawEmblemsBeside(surface, left, top, width, height, factions.getFirst(), factions.size() > 1 ? factions.get(1) : null);
    }

    /**
     * Called when the user opens an entry.
     * <p>
     * Shows the replay viewer of the stored replay.
     *
     * @param entry the entry
     */
    @Override
    public void onChosen(@NotNull GameFileEntry entry) {
        this.getDemo().getClient().getScenes().replace(new ReplayViewerScene(this.getDemo(), this, entry.getPath()));
    }

    /**
     * Called when the user leaves the list without choosing.
     * <p>
     * Shows the main menu again.
     */
    @Override
    public void onCancelled() {
        this.show();
    }

    /**
     * Called when a control of the dialog is activated.
     * <p>
     * Starts the screen of the pressed button, or asks the client to end.
     *
     * @param id the id of the activated control
     */
    @Override
    protected void onActivated(int id) {
        DemoClient demo = this.getDemo();

        switch (id) {
            case START_ID -> demo.getClient().getScenes().replace(new GameSetupScene(demo, this));
            case REPLAYS_ID -> {
                GameFileListScene replays = GameFileListScene.createReplayList("Replays", demo.getTheme(), DemoClient.REPLAY_FOLDER,
                                                                               DemoGameScene.LOOP_MILLISECONDS, this);

                replays.setBackdrop(surface -> demo.getSkin().drawScreen(surface, DemoSkin.MENU_BACKGROUND));
                replays.setPlayerNames(DemoClient::getSlotName);
                demo.getClient().getScenes().replace(replays);
            }
            case NETWORK_ID -> demo.getClient().getScenes().replace(new NetworkScene(demo, this, DemoNetworkSettings.DEFAULTS));
            case OPTIONS_ID -> {
                try {
                    OptionsPopup.open(demo, () -> {
                        // The menu shows nothing that the options change.
                    });
                } catch (IOException exception) {
                    LOGGER.warning("OPTIONS: {0}", exception.getMessage());
                }
            }
            case QUIT_ID -> demo.getClient().requestQuit();
            default -> {
                // Labels and the panel do nothing.
            }
        }
    }

    /**
     * Closes the scene: releases what it holds. The client closes the main menu when it ends; the demo's shared services close with it.
     */
    @Override
    public void close() {
        super.close();
        IgnoreException.execute(
                this.getDemo()::close
        );
    }
}
