package org.fogofwarlabs.sc4j.engine.replay;

import org.fogofwarlabs.sc4j.engine.client.game.GameSession;
import org.fogofwarlabs.sc4j.engine.client.game.GameSessionFactory;
import org.fogofwarlabs.sc4j.engine.combat.StandardCombatSystem;
import org.fogofwarlabs.sc4j.engine.command.CommandRegistry;
import org.fogofwarlabs.sc4j.engine.command.CommandSink;
import org.fogofwarlabs.sc4j.engine.command.CommandType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.entity.pool.EntityPool;
import org.fogofwarlabs.sc4j.engine.entity.pool.SlotList;
import org.fogofwarlabs.sc4j.engine.game.*;
import org.fogofwarlabs.sc4j.engine.map.*;
import org.fogofwarlabs.sc4j.engine.mod.identity.ModIdentity;
import org.fogofwarlabs.sc4j.engine.order.IssueOrderCommandType;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.fogofwarlabs.sc4j.manifest.ManifestException;
import org.fogofwarlabs.sc4j.manifest.ModManifest;
import org.fogofwarlabs.sc4j.manifest.ModManifestReader;
import org.fogofwarlabs.sc4j.manifest.version.EngineVersion;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Records and replays engine-only skirmish games through the engine replay format: a local client session of the skirmish map whose person
 * orders the scout a few times while the two computer players play, recorded at the game's command source, and replay sessions that play the
 * recordings back.
 */
public final class ReplayGameScenario {
    /**
     * The seed of the recorded games.
     */
    public static final long SEED = 0x5C4A_2026_1107L;
    /**
     * The game loops between two state hash checkpoints of the recordings.
     */
    public static final int CHECKPOINT_INTERVAL = 100;
    /**
     * The slot of the person's scout.
     */
    public static final int SCOUT_SLOT = 3;
    /**
     * The game loops at which the person orders the scout.
     */
    public static final int @NotNull [] ORDER_LOOPS = {40, 300, 301, 650};

    private static final int COMMAND_ID = 0;
    private static final int INSTRUCTION_LIMIT = 100000;
    private static final int DIGEST_LENGTH = 32;
    private static final int @NotNull [] TEAM_COLORS = {0x2050E0, 0xE03020, 0xA0A0A0, 0x20C0C0};
    private static final int @NotNull [] TARGETS = {600, 140, 200, 420, 420, 120, 120, 420};

    /**
     * Creates a {@link ModIdentity} instance of a run of the duel mod.
     *
     * @param version the mod version
     * @param content the first byte of the content digest
     * @return the created {@link ModIdentity} instance
     * @throws ManifestException if the manifest is invalid
     */
    public static @NotNull ModIdentity createIdentity(@NotNull String version, int content) throws ManifestException {
        String text = "{\"format\": 1, \"id\": \"duel\", \"name\": {\"en\": \"Duel\"}, \"version\": \"" + version +
                      "\", \"engine\": \"0.1\", \"entry\": \"org.example.DuelMod\"}";
        ModManifest manifest = ModManifestReader.parse(text.getBytes(StandardCharsets.UTF_8));
        byte[] digest = new byte[DIGEST_LENGTH];

        digest[0] = (byte) content;

        return ModIdentity.create(EngineVersion.CURRENT, manifest, digest, List.of());
    }

    private static @Nullable Unit findUnit(@NotNull Game game, @NotNull Player owner) {
        EntityPool<Unit> units = game.getUnits();
        SlotList used = units.getUsedSlots();

        for (int index = used.getFirst(); index != SlotList.NONE; index = used.getNext(index)) {
            if (units.get(index).getOwner() == owner) {
                return units.get(index);
            }
        }

        return null;
    }


    private final @NotNull SkirmishScenario scenario;
    private final byte @NotNull [] mapContents;
    private final @NotNull MapModel map;
    private final @NotNull IssueOrderCommandType orders;
    private final @NotNull CommandRegistry<CommandType<?>> commands;
    private final @NotNull GameSetupCodec setupCodec;
    private final @NotNull GameSessionFactory factory;

    /**
     * Creates a {@link ReplayGameScenario} instance with the skirmish content, its map and its scout order command.
     *
     * @throws IOException if the map cannot be read
     */
    public ReplayGameScenario() throws IOException {
        this.scenario    = new SkirmishScenario();
        this.mapContents = MapTestResources.read(MapTestResources.TWIN_ROCKS);
        this.map         = new JsonMapLoader(this.scenario.getUnitTypes(), this.scenario.getResourceKinds()).read(this.mapContents,
                                                                                                                  MapTestResources.TWIN_ROCKS);
        this.orders      = new IssueOrderCommandType(COMMAND_ID, INSTRUCTION_LIMIT);
        this.commands    = new CommandRegistry<>(1);
        this.setupCodec  = new GameSetupCodec(List.of(this.scenario.getWardens(), this.scenario.getRaiders()), List.of());
        this.factory     = new GameSessionFactory(this.map.getTerrain(), this.scenario.getBounds(), StandardCombatSystem.FRACTION_BITS,
                                                  this.scenario.getResourceKinds(), TEAM_COLORS);

        this.commands.register(this.orders);
        this.commands.freeze();
    }

    /**
     * @return the command types of the games by id
     */
    public @NotNull CommandRegistry<CommandType<?>> getCommands() {
        return this.commands;
    }

    /**
     * @return the codec of the games' setups
     */
    public @NotNull GameSetupCodec getSetupCodec() {
        return this.setupCodec;
    }


    /**
     * Creates a {@link EngineReplayFormat} instance of a run of the duel mod.
     *
     * @param identity the identity of the run
     * @return the created {@link EngineReplayFormat} instance
     */
    public @NotNull EngineReplayFormat createFormat(@NotNull ModIdentity identity) {
        return new EngineReplayFormat(identity, this.commands, this.setupCodec);
    }

    /**
     * Creates a {@link GameSetup} instance of the skirmish game: two computer players, a neutral player and the scout's person.
     *
     * @param seed the seed
     * @return the created {@link GameSetup} instance
     */
    public @NotNull GameSetup createSetup(long seed) {
        String name = this.map.requireModData(StandardMapData.class).getName();

        return new GameSetup.Builder(new MapReference(name, this.mapContents))
                .addPlayer(new PlayerSlot(0, ControllerKind.COMPUTER, 0, 0, this.scenario.getWardens()))
                .addPlayer(new PlayerSlot(1, ControllerKind.COMPUTER, 1, 1, this.scenario.getRaiders()))
                .addPlayer(new PlayerSlot(2, ControllerKind.NEUTRAL, 2, 2, null))
                .addPlayer(new PlayerSlot(SCOUT_SLOT, ControllerKind.HUMAN, 0, SCOUT_SLOT, null))
                .setSeed(seed)
                .build();
    }

    /**
     * Creates a {@link Game} instance of the skirmish game of a setup with the map's units placed.
     *
     * @param setup the setup
     * @return the created {@link Game} instance
     */
    public @NotNull Game createGame(@NotNull GameSetup setup) {
        StandardMapData data = this.map.requireModData(StandardMapData.class);
        Game game = Game.create(this.scenario.createDefinition(this.map.getTerrain(), data.getTriggers()).build(), setup);

        this.scenario.prepare(game);
        MapPlacementUtils.placeUnits(game, this.map);

        return game;
    }

    /**
     * Plays a local game of the skirmish setup with the {@link #SEED} and records it: the person orders the scout through the session's command
     * sink at the {@link #ORDER_LOOPS}, and the recorder at the game's command source records every command.
     *
     * @param format      the replay format
     * @param loopCount   the game loops to play
     * @param frameHashes receives the state hash after every game loop
     * @return the recorded replay
     */
    public @NotNull EngineReplay record(@NotNull EngineReplayFormat format, int loopCount, @NotNull List<@NotNull Long> frameHashes) {
        GameSetup setup = this.createSetup(SEED);
        Game game = this.createGame(setup);
        Player person = game.getPlayer(SCOUT_SLOT);
        GameSession session = this.factory.createLocal(game, person, List.of());
        ReplayRecorder recorder = format.createRecorder(setup, CHECKPOINT_INTERVAL);
        CommandSink sink = session.getCommandSink();
        int order = 0;

        Assertions.assertNotNull(sink);

        session.getCommandSource().setRecorder(recorder);

        for (int loop = 0; loop < loopCount; loop++) {
            Unit scout = findUnit(game, person);

            if (order < ORDER_LOOPS.length && ORDER_LOOPS[order] == loop && scout != null) {
                sink.submit(person, this.orders, this.orders.create(game, scout, order % 2 == 0 ? this.scenario.getPatrol() : this.scenario.getMove(),
                                                                    TARGETS[order * 2], TARGETS[order * 2 + 1], null, order == 2));
                order++;
            }

            Assertions.assertTrue(session.runFrame());
            frameHashes.add(game.computeStateHash());
        }

        return recorder.createReplay();
    }

    /**
     * Creates a {@link GameSession} instance: the replay session of a game with one replay controller.
     *
     * @param game       the game
     * @param controller the controller that plays the replay back
     * @return the created {@link GameSession} instance
     */
    public @NotNull GameSession createReplaySession(@NotNull Game game, @NotNull ReplayController controller) {
        return this.factory.createReplay(game, List.of(controller));
    }

    /**
     * Plays a replay back in a replay session of a game until the replay's last game loop.
     *
     * @param replay      the replay
     * @param game        the game, created from the replay's setup or, to test a mismatch, from another one
     * @param listener    the receiver of the checkpoint mismatches, or {@code null}
     * @param frameHashes receives the state hash after every game loop
     * @return the controller that played the replay back
     */
    public @NotNull ReplayController play(@NotNull EngineReplay replay, @NotNull Game game, @Nullable CheckpointMismatchListener listener,
                                          @NotNull List<@NotNull Long> frameHashes) {
        ReplayController controller = replay.createController(listener);
        GameSession session = this.createReplaySession(game, controller);

        while (!controller.isFinished()) {
            Assertions.assertTrue(session.runFrame());
            frameHashes.add(game.computeStateHash());
        }

        return controller;
    }
}
