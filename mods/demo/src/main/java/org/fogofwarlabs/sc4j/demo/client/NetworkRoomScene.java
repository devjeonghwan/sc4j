package org.fogofwarlabs.sc4j.demo.client;

import org.fogofwarlabs.sc4j.demo.net.DemoLobby;
import org.fogofwarlabs.sc4j.demo.net.DemoNetworkGame;
import org.fogofwarlabs.sc4j.demo.net.DemoNetworkSettings;
import org.fogofwarlabs.sc4j.engine.client.community.TableBox;
import org.fogofwarlabs.sc4j.engine.client.community.TableRow;
import org.fogofwarlabs.sc4j.engine.client.dialog.ComboBox;
import org.fogofwarlabs.sc4j.engine.client.dialog.Control;
import org.fogofwarlabs.sc4j.engine.content.faction.Faction;
import org.fogofwarlabs.sc4j.engine.game.ControllerKind;
import org.fogofwarlabs.sc4j.engine.game.MapReference;
import org.fogofwarlabs.sc4j.engine.net.NetworkClock;
import org.fogofwarlabs.sc4j.engine.net.lobby.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Shows a room of a demo network game before the start: the room's map, its turn rate and a table of its members (player, slot, faction, and whether
 * they are ready or still wait for the map); the faction of this machine's slot, Ready (toggles this machine's ready flag), Start (the host, once
 * every member is ready) and Leave (back to the network screen). When the game starts, every machine opens its game scene over its
 * {@link DemoNetworkGame}; a refused or closed room goes back to the network screen with the reason ({@value #WRONG_PASSWORD_TEXT} for a wrong
 * password).
 */
public final class NetworkRoomScene extends DemoMenuScene {
    /**
     * The pack id of the room screen's dialog.
     */
    public static final @NotNull String DIALOG = "network-room";
    /**
     * The id of the title, which names the room's map.
     */
    public static final int TITLE_ID = 2;
    /**
     * The id of the turn rate label.
     */
    public static final int TURN_RATE_ID = 5;
    /**
     * The id of the member table.
     */
    public static final int MEMBERS_ID = 10;
    /**
     * The id of the faction list.
     */
    public static final int FACTION_ID = 11;
    /**
     * The id of the status line.
     */
    public static final int STATUS_ID = 13;
    /**
     * The id of the ready button.
     */
    public static final int READY_ID = 20;
    /**
     * The id of the start button.
     */
    public static final int START_ID = 21;
    /**
     * The id of the leave button.
     */
    public static final int LEAVE_ID = 22;
    /**
     * The reason shown when the host refused the password.
     */
    public static final @NotNull String WRONG_PASSWORD_TEXT = "Wrong password.";

    private static final @NotNull String TURN_RATE_PREFIX = "Turn rate ";
    /**
     * The member table's columns: player, slot, faction, status.
     */
    private static final float @NotNull [] MEMBER_COLUMNS = {0.34f, 0.18f, 0.22f, 0.26f};
    private static final @NotNull List<@NotNull String> MEMBER_HEADERS = List.of("Player", "Slot", "Faction", "Status");
    private static final @NotNull String SLOT_PREFIX = "Slot ";
    private static final @NotNull String OBSERVER = "Observer";
    private static final @NotNull String ANY_FACTION = "Any";
    private static final @NotNull String READY = "Ready";
    private static final @NotNull String NOT_READY = "Not ready";
    private static final @NotNull String RECEIVING_MAP = "Receiving map";
    private static final @NotNull String MISSING_MAP = "No map";
    private static final @NotNull String CHECKING_MAP = "Checking map";
    private static final int READY_COLOR = 0xE0E0E0FF;
    private static final int WAITING_COLOR = 0x909090FF;

    private final @NotNull MainMenuScene menu;
    private final @NotNull DemoNetworkSettings settings;
    private final @NotNull DemoLobby lobby;

    private int shownFaction;
    private boolean started;

    /**
     * Creates a {@link NetworkRoomScene} instance; the scene closes the room unless its game starts.
     *
     * @param demo     the demo's shared services
     * @param menu     the main menu
     * @param settings where the network games run
     * @param lobby    this machine's room
     */
    public NetworkRoomScene(@NotNull DemoClient demo, @NotNull MainMenuScene menu, @NotNull DemoNetworkSettings settings, @NotNull DemoLobby lobby) {
        super(demo, DIALOG);

        this.menu     = menu;
        this.settings = settings;
        this.lobby    = lobby;

        this.shownFaction = ComboBox.NO_SELECTION;
        this.started      = false;
    }

    /**
     * @return this machine's room
     */
    public @NotNull DemoLobby getLobby() {
        return this.lobby;
    }

    /**
     * Called when the scene opened and its dialog is built: fills the dialog's controls. The default fills nothing.
     * <p>
     * Makes the member table, lists the factions and shows the room as it is.
     */
    @Override
    protected void onOpened() {
        Control members = this.requireControl(MEMBERS_ID);
        List<String> factions = new ArrayList<>();

        this.requireDialog().replaceControl(members, new TableBox(members.getDefinition(), this.getDemo().getFont().getLineHeight(),
                                                                  MEMBER_COLUMNS, MEMBER_HEADERS, false));

        for (Faction faction : this.getDemo().getContent().getFactions()) {
            factions.add(DemoClient.getName(faction));
        }

        ComboBox factionBox = (ComboBox) this.requireControl(FACTION_ID);

        factionBox.setEntries(factions);
        factionBox.setSelection(ComboBox.NO_SELECTION);
        this.requireControl(TURN_RATE_ID).setText(TURN_RATE_PREFIX + this.lobby.getTurnRate());
        this.showRoom();
    }

    /**
     * Updates the scene at the supplied time.
     * <p>
     * Polls the room, sends a changed faction choice, shows the room and opens the game once it started.
     *
     * @param time the client time in milliseconds
     */
    @Override
    public void update(long time) {
        NetworkLobby room = this.lobby.getLobby();

        try {
            this.lobby.poll();

            int faction = ((ComboBox) this.requireControl(FACTION_ID)).getSelection();

            if (faction != this.shownFaction && room.getLobbyState() == LobbyState.OPEN && room.getLocalMember() != null) {
                this.shownFaction = faction;
                room.setFaction(faction < 0 ? null : this.getDemo().getContent().getFactions().get(faction));
            }
        } catch (IOException exception) {
            this.showStatus("The room failed: " + exception.getMessage());

            return;
        }

        NetworkGameStart start = room.getStart();

        if (start != null) {
            this.openGame(start);
        } else if (room.getLobbyState() == LobbyState.REFUSED || room.getLobbyState() == LobbyState.CLOSED) {
            this.leave(room.getRefusal() == JoinRefusal.WRONG_PASSWORD ? WRONG_PASSWORD_TEXT : room.getRefusalText());
        } else {
            this.showRoom();
        }
    }

    /**
     * Called when a control of the dialog is activated.
     * <p>
     * Ready toggles this machine's ready flag, Start starts the game at the host, Leave returns to the network screen.
     *
     * @param id the id of the activated control
     */
    @Override
    protected void onActivated(int id) {
        NetworkLobby room = this.lobby.getLobby();
        LobbyMember local = room.getLocalMember();

        try {
            if (id == READY_ID && local != null && room.getLobbyState() == LobbyState.OPEN) {
                room.setReady(!local.isReady());
            } else if (id == START_ID && room.isReadyToStart()) {
                this.openGame(this.lobby.start(this.getDemo().getGames(), ControllerKind.HUMAN, this.getDemo().getClient().getTime()));
            } else if (id == LEAVE_ID) {
                this.leave("");
            }
        } catch (IOException | IllegalStateException | IllegalArgumentException exception) {
            this.showStatus(exception.getMessage() != null ? exception.getMessage() : exception.toString());
        }
    }

    /**
     * Returns to the network screen the room came from: the Beacon.Link tab for a room over Beacon.Link, with the reason a refused or closed
     * room ended.
     */
    private void leave(@NotNull String reason) {
        DemoClient demo = this.getDemo();

        demo.getClient().getScenes().replace(this.lobby.isOverBeaconLink() ? new BeaconLinkScene(demo, this.menu, this.settings, reason)
                                                                           : new NetworkScene(demo, this.menu, this.settings));
    }

    private void openGame(@NotNull NetworkGameStart start) {
        if (this.started) {
            return;
        }

        DemoClient demo = this.getDemo();
        DemoNetworkGame network = new DemoNetworkGame(start, this.lobby, NetworkClock.SYSTEM, this.settings, demo.getContent(),
                                                      demo.getMaps(), demo.getGames(), demo.getReplayFormat(), DemoGameScene.CHECKPOINT_INTERVAL);

        this.started = true;
        demo.getClient().getScenes().replace(new DemoGameScene(demo, this.menu, network));
    }

    private void showRoom() {
        NetworkLobby room = this.lobby.getLobby();
        MapReference map = room.getMap();
        List<TableRow> rows = new ArrayList<>();
        LobbyMember local = room.getLocalMember();

        for (LobbyMember member : room.getMembers()) {
            Faction faction = member.getFaction();
            String status = switch (member.getMapState()) {
                case PRESENT -> member.isReady() ? READY : NOT_READY;
                case TRANSFERRING -> RECEIVING_MAP;
                case MISSING -> MISSING_MAP;
                case UNKNOWN -> CHECKING_MAP;
            };

            rows.add(new TableRow(List.of(member.getName(), member.isObserver() ? OBSERVER : SLOT_PREFIX + (member.getSlotIndex() + 1),
                                          faction == null ? ANY_FACTION : DemoClient.getName(faction), status),
                                  member.isReady() ? READY_COLOR : WAITING_COLOR, null));
        }

        TableBox members = (TableBox) this.requireControl(MEMBERS_ID);

        if (!rows.equals(members.getRows())) {
            members.setRows(rows);
        }

        this.requireControl(TITLE_ID).setText(map == null ? "Joining the room" : "Room on " + map.getName());
        this.requireControl(READY_ID).setText(local != null && local.isReady() ? "Not ready" : "Ready");
        this.requireControl(START_ID).setEnabled(room.isReadyToStart());
        this.requireControl(START_ID).setVisible(room.isHost());
        this.showStatus(room.isHost() ? room.isReadyToStart() ? "Everyone is ready: start the game." : "Waiting until everyone is ready."
                                      : "Waiting for the host to start.");
    }

    private void showStatus(@NotNull String status) {
        this.requireControl(STATUS_ID).setText(status);
    }

    /**
     * Closes the scene: releases what it holds. The room closes unless its game started; the network game took the room.
     */
    @Override
    public void close() {
        if (!this.started) {
            this.lobby.close();
        }

        super.close();
    }
}
