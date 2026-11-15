package org.fogofwarlabs.sc4j.engine.computer;

import org.fogofwarlabs.sc4j.engine.content.definition.UnitTrait;
import org.fogofwarlabs.sc4j.engine.content.table.UnitTypeTable;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.economy.HarvestSystem;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.entity.pool.EntityPool;
import org.fogofwarlabs.sc4j.engine.entity.pool.SlotList;
import org.fogofwarlabs.sc4j.engine.game.*;
import org.fogofwarlabs.sc4j.engine.math.MutablePoint;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.fogofwarlabs.sc4j.engine.production.StandardProductionSystem;
import org.fogofwarlabs.sc4j.engine.state.StateHash;
import org.fogofwarlabs.sc4j.engine.terrain.ArrayTerrainGrid;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Tests the engine default computer player on the skirmish content in an engine-only game: from a town hall and four workers it gathers,
 * trains workers, builds a supply and a production building away from its gold, trains fighting units of the requirement graph and sends them
 * off as an attack wave at the enemy's town hall; the larger next wave is part of the state hash.
 */
final class StandardComputerControllerTest {
    private static final int MAP_TILES = 40;
    private static final int FRAME_LIMIT = 9000;

    private final @NotNull SkirmishScenario scenario;

    StandardComputerControllerTest() {
        this.scenario = new SkirmishScenario();
    }

    @Test
    void computer_players_build_an_economy_and_send_an_attack_wave() {
        ArrayTerrainGrid terrain = new ArrayTerrainGrid(SkirmishScenario.TILE_SIZE, SkirmishScenario.WALK_CELL_SIZE, MAP_TILES, MAP_TILES);

        for (int tileY = 0; tileY < MAP_TILES; tileY++) {
            for (int tileX = 0; tileX < MAP_TILES; tileX++) {
                terrain.setBuildable(tileX, tileY, true);
            }
        }

        for (int walkY = 0; walkY < terrain.getWalkHeight(); walkY++) {
            for (int walkX = 0; walkX < terrain.getWalkWidth(); walkX++) {
                terrain.setWalkable(walkX, walkY, true);
            }
        }

        GameSetup setup = new GameSetup.Builder(new MapReference("open field", new byte[0]))
                .addPlayer(new PlayerSlot(0, ControllerKind.COMPUTER, 0, 0, this.scenario.getWardens()))
                .addPlayer(new PlayerSlot(1, ControllerKind.NEUTRAL, 1, 1, null))
                .addPlayer(new PlayerSlot(2, ControllerKind.HUMAN, 2, 2, this.scenario.getRaiders()))
                .build();
        Game game = Game.create(this.scenario.createDefinition(terrain, List.of()).build(), setup);
        StandardProductionSystem production = (StandardProductionSystem) game.getProduction();
        Player player = game.getPlayer(0);

        this.scenario.prepare(game);

        Unit hall = production.createUnit(player, this.scenario.getKeep(), 300, 300, 0);
        Unit mine = production.createUnit(game.getPlayer(1), this.scenario.getGoldMine(), 300, 140, 0);

        Assertions.assertNotNull(hall);
        Assertions.assertNotNull(mine);

        game.getModSystem(HarvestSystem.KEY).setResources(mine, this.scenario.getGold(), 5000);
        Assertions.assertNotNull(production.createUnit(game.getPlayer(2), this.scenario.getLodge(), 1000, 1000, 0));

        for (int count = 0; count < 4; count++) {
            Assertions.assertNotNull(production.createUnit(player, this.scenario.getKeeper(), 300, 300, 0));
        }

        StandardComputerController controller = (StandardComputerController) game.getComputerPlayers().getController(player);

        Assertions.assertNotNull(controller);

        MutablePoint site = new MutablePoint();

        Assertions.assertTrue(controller.findBuildSite(this.scenario.getKeep(), hall, site));
        Assertions.assertTrue(site.getY() > hall.getPositionY());

        int firstWave = controller.getWaveSize();
        StateHash firstWaveHash = new StateHash();

        controller.contributeStateHash(firstWaveHash);

        for (int frame = 0; frame < FRAME_LIMIT && controller.getWaveSize() == firstWave; frame++) {
            game.getFrameRunner().runFrame();
        }

        UnitTypeTable table = game.getContent().getUnitTypes();
        StateHash nextWaveHash = new StateHash();

        controller.contributeStateHash(nextWaveHash);

        Assertions.assertTrue(controller.getWaveSize() > firstWave);
        Assertions.assertNotEquals(firstWaveHash.getValue(), nextWaveHash.getValue());
        Assertions.assertTrue(game.getContent().getProgress().getCompletedUnitCount(player, this.scenario.getKeeper()) > 4);
        Assertions.assertTrue(this.countWithTrait(game, player, table, UnitTrait.BUILDING) >= 3);
    }

    private int countWithTrait(@NotNull Game game, @NotNull Player player, @NotNull UnitTypeTable table, @NotNull UnitTrait trait) {
        EntityPool<Unit> units = game.getUnits();
        SlotList used = units.getUsedSlots();
        int count = 0;

        for (int index = used.getFirst(); index != SlotList.NONE; index = used.getNext(index)) {
            UnitType type = units.get(index).getType();

            if (units.get(index).getOwner() == player && type != null && table.hasTrait(type, trait)) {
                count++;
            }
        }

        return count;
    }
}
