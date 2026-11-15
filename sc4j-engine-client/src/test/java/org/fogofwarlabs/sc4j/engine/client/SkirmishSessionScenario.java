package org.fogofwarlabs.sc4j.engine.client;

import org.fogofwarlabs.sc4j.engine.client.game.GameSession;
import org.fogofwarlabs.sc4j.engine.client.game.GameSessionFactory;
import org.fogofwarlabs.sc4j.engine.combat.StandardCombatSystem;
import org.fogofwarlabs.sc4j.engine.game.*;
import org.fogofwarlabs.sc4j.engine.map.*;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;

/**
 * Starts an engine-only client session of the skirmish map without installed game data: two engine default computer players (wardens and raiders), a
 * neutral player and the person of {@link #LOCAL_SLOT}, whose commands go through the session's local command queue.
 */
public final class SkirmishSessionScenario {
    /**
     * The slot of the local player, a person on the wardens' team.
     */
    public static final int LOCAL_SLOT = 3;
    /**
     * The team colours by colour index: blue, red, grey and teal.
     */
    public static final int @NotNull [] TEAM_COLORS = {0x2050E0, 0xE03020, 0xA0A0A0, 0x20C0C0};

    private static final long SEED = 0x5C4A_2026_1105L;

    private final @NotNull SkirmishScenario scenario;
    private final @NotNull MapModel map;
    private final @NotNull GameSession session;

    /**
     * Creates a {@link SkirmishSessionScenario} instance: loads the map, creates the game, places the map's units and opens a local session.
     *
     * @throws IOException if the map cannot be read
     */
    public SkirmishSessionScenario() throws IOException {
        byte[] contents = MapTestResources.read(MapTestResources.TWIN_ROCKS);

        this.scenario = new SkirmishScenario();
        this.map      = new JsonMapLoader(this.scenario.getUnitTypes(), this.scenario.getResourceKinds()).read(contents, MapTestResources.TWIN_ROCKS);

        StandardMapData data = this.map.requireModData(StandardMapData.class);
        GameSetup setup = new GameSetup.Builder(new MapReference(data.getName(), contents))
                .addPlayer(new PlayerSlot(0, ControllerKind.COMPUTER, 0, 0, this.scenario.getWardens()))
                .addPlayer(new PlayerSlot(1, ControllerKind.COMPUTER, 1, 1, this.scenario.getRaiders()))
                .addPlayer(new PlayerSlot(2, ControllerKind.NEUTRAL, 2, 2, null))
                .addPlayer(new PlayerSlot(LOCAL_SLOT, ControllerKind.HUMAN, 0, LOCAL_SLOT, this.scenario.getWardens()))
                .setSeed(SEED)
                .build();
        Game game = Game.create(this.scenario.createDefinition(this.map.getTerrain(), data.getTriggers()).build(), setup);
        GameSessionFactory factory = new GameSessionFactory(this.map.getTerrain(), this.scenario.getBounds(), StandardCombatSystem.FRACTION_BITS,
                                                            this.scenario.getResourceKinds(), TEAM_COLORS);

        this.scenario.prepare(game);
        MapPlacementUtils.placeUnits(game, this.map);
        this.session = factory.createLocal(game, game.getPlayer(LOCAL_SLOT), List.of());
    }

    /**
     * @return the skirmish content
     */
    public @NotNull SkirmishScenario getScenario() {
        return this.scenario;
    }

    /**
     * @return the loaded map
     */
    public @NotNull MapModel getMap() {
        return this.map;
    }

    /**
     * @return the local session
     */
    public @NotNull GameSession getSession() {
        return this.session;
    }

    /**
     * Runs frames of the session's game.
     *
     * @param frames the number of frames
     */
    public void runFrames(int frames) {
        for (int frame = 0; frame < frames; frame++) {
            this.session.runFrame();
        }
    }
}
