package org.fogofwarlabs.sc4j.engine.production;

import org.fogofwarlabs.sc4j.engine.combat.StandardCombatSystem;
import org.fogofwarlabs.sc4j.engine.content.definition.MotionProfileDefinition;
import org.fogofwarlabs.sc4j.engine.content.definition.OrderDefinition;
import org.fogofwarlabs.sc4j.engine.content.definition.UnitDefinition;
import org.fogofwarlabs.sc4j.engine.content.definition.UnitTrait;
import org.fogofwarlabs.sc4j.engine.content.economy.ResourceCost;
import org.fogofwarlabs.sc4j.engine.content.economy.ResourceKind;
import org.fogofwarlabs.sc4j.engine.content.faction.Faction;
import org.fogofwarlabs.sc4j.engine.content.faction.FactionDefinition;
import org.fogofwarlabs.sc4j.engine.content.table.PlayerProgressTable;
import org.fogofwarlabs.sc4j.engine.content.type.MotionProfile;
import org.fogofwarlabs.sc4j.engine.content.type.OrderType;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.economy.EconomySystem;
import org.fogofwarlabs.sc4j.engine.economy.StandardEconomySystem;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.*;
import org.fogofwarlabs.sc4j.engine.lifecycle.UnitAccountingUtils;
import org.fogofwarlabs.sc4j.engine.movement.MovementSystem;
import org.fogofwarlabs.sc4j.engine.movement.StandardMovementSystem;
import org.fogofwarlabs.sc4j.engine.order.*;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.fogofwarlabs.sc4j.engine.spatial.SpatialIndex;
import org.fogofwarlabs.sc4j.engine.spatial.StandardSpatialIndex;
import org.fogofwarlabs.sc4j.engine.spatial.TableUnitBounds;
import org.fogofwarlabs.sc4j.engine.terrain.ArrayTerrainGrid;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the construction of the engine default production system with the build behavior in an engine-only game: the placement check on the
 * terrain and the units, a worker that walks to the site and places the building, the construction time with growing hit points and the
 * completed counts and supply, a cancelled construction refunding its share, and units changing their owner.
 */
final class BuildBehaviorTest {
    private static final int UNIT_LIMIT = 8;
    private static final int ORDER_LIMIT = 8;
    private static final int TILE_SIZE = 32;
    private static final int WALK_CELL_SIZE = 8;
    private static final int MAP_TILES = 16;
    private static final int FARM_COST = 100;
    private static final int FARM_TIME = 30;
    private static final int FARM_SUPPLY = 8;
    private static final int FARM_HIT_POINTS = 100;
    private static final int REFUND_PERCENT = 75;
    private static final int START_PERCENT = 10;
    private static final int FRAME_LIMIT = 600;

    private final @NotNull ResourceKind gold;
    private final @NotNull Faction humans;
    private final @NotNull MotionProfile walker;
    private final @NotNull OrderType idle;
    private final @NotNull OrderType build;
    private final @NotNull UnitType hall;
    private final @NotNull UnitType farm;
    private final @NotNull UnitType peasant;
    private final @NotNull TableUnitBounds bounds;
    private final @NotNull ArrayTerrainGrid terrain;

    BuildBehaviorTest() {
        this.gold    = new ResourceKind("gold");
        this.humans  = new Faction("humans");
        this.walker  = new MotionProfile("walker");
        this.idle    = new OrderType("idle");
        this.build   = new OrderType("build");
        this.hall    = new UnitType("hall");
        this.farm    = new UnitType("farm");
        this.peasant = new UnitType("peasant");
        this.bounds  = new TableUnitBounds();
        this.terrain = new ArrayTerrainGrid(TILE_SIZE, WALK_CELL_SIZE, MAP_TILES, MAP_TILES);

        for (int tileY = 0; tileY < MAP_TILES; tileY++) {
            for (int tileX = 0; tileX < MAP_TILES; tileX++) {
                this.terrain.setBuildable(tileX, tileY, tileX < MAP_TILES / 2);
            }
        }

        for (int walkY = 0; walkY < this.terrain.getWalkHeight(); walkY++) {
            for (int walkX = 0; walkX < this.terrain.getWalkWidth(); walkX++) {
                this.terrain.setWalkable(walkX, walkY, true);
            }
        }
    }

    @Test
    void can_place_should_check_the_terrain_and_the_buildings() {
        Game game = this.createGame();
        StandardProductionSystem production = (StandardProductionSystem) game.getProduction();

        this.createUnit(game, this.hall, 100, 100);
        this.createUnit(game, this.peasant, 200, 200);

        Assertions.assertEquals(2 * TILE_SIZE, production.getFootprintWidth(this.farm));
        Assertions.assertTrue(production.canPlace(this.farm, 200, 200));
        Assertions.assertFalse(production.canPlace(this.farm, 110, 110));
        Assertions.assertFalse(production.canPlace(this.farm, MAP_TILES / 2 * TILE_SIZE, 200));
        Assertions.assertFalse(production.canPlace(this.farm, 10, 200));
    }

    @Test
    void workers_place_buildings_that_complete_after_their_build_time() {
        Game game = this.createGame();
        Player player = game.getPlayer(0);
        StandardProductionSystem production = (StandardProductionSystem) game.getProduction();
        PlayerProgressTable progress = game.getContent().getProgress();
        Unit worker = this.createUnit(game, this.peasant, 60, 60);

        game.getEconomy().setAmount(player, this.gold, FARM_COST);

        Assertions.assertTrue(production.orderConstruction(worker, this.farm, 160, 200, 0));
        Assertions.assertSame(this.farm, production.getConstructionType(worker));

        Unit building = null;

        for (int frame = 0; frame < FRAME_LIMIT && building == null; frame++) {
            game.getFrameRunner().runFrame();

            building = production.getConstructedBuilding(worker);
        }

        Assertions.assertNotNull(building);
        Assertions.assertTrue(production.isUnderConstruction(building));
        Assertions.assertEquals(0, game.getEconomy().getAmount(player, this.gold));
        Assertions.assertEquals(1, progress.getUnitCount(player, this.farm));
        Assertions.assertEquals(0, progress.getCompletedUnitCount(player, this.farm));
        Assertions.assertEquals(0, game.getEconomy().getSupplyProvided(player, this.humans));
        Assertions.assertTrue(building.getHitPoints() < FARM_HIT_POINTS << StandardCombatSystem.FRACTION_BITS);
        Assertions.assertEquals(-1, production.enqueueUnit(building, this.peasant));
        Assertions.assertSame(this.build, worker.getOrderType());

        int hitPoints = building.getHitPoints();

        this.runFrames(game, FARM_TIME);

        Assertions.assertFalse(production.isUnderConstruction(building));
        Assertions.assertTrue(building.getHitPoints() > hitPoints);
        Assertions.assertEquals(1, progress.getCompletedUnitCount(player, this.farm));
        Assertions.assertEquals(FARM_SUPPLY, game.getEconomy().getSupplyProvided(player, this.humans));
        Assertions.assertSame(this.idle, worker.getOrderType());
        Assertions.assertNull(production.getConstructionType(worker));
    }

    @Test
    void cancelled_constructions_refund_their_share() {
        Game game = this.createGame();
        Player player = game.getPlayer(0);
        StandardProductionSystem production = (StandardProductionSystem) game.getProduction();
        Unit worker = this.createUnit(game, this.peasant, 160, 160);

        game.getEconomy().setAmount(player, this.gold, FARM_COST);

        Assertions.assertFalse(production.isNearSite(worker, this.farm, 160, 240, 4));
        Assertions.assertTrue(production.isNearSite(worker, this.farm, 160, 192, 4));

        Unit building = production.placeBuilding(worker, this.farm, 160, 192, 0);

        Assertions.assertNotNull(building);
        Assertions.assertNull(production.placeBuilding(worker, this.farm, 160, 192, 0));
        Assertions.assertTrue(production.cancelConstruction(building, 0));
        Assertions.assertFalse(game.getUnits().isAllocated(building.getIndex()));
        Assertions.assertEquals(FARM_COST * REFUND_PERCENT / 100, game.getEconomy().getAmount(player, this.gold));
        Assertions.assertEquals(0, game.getContent().getProgress().getUnitCount(player, this.farm));
    }

    @Test
    void transferred_units_move_their_counts_and_supply() {
        Game game = this.createGame();
        Player first = game.getPlayer(0);
        Player second = game.getPlayer(1);
        PlayerProgressTable progress = game.getContent().getProgress();
        Unit hallUnit = this.createUnit(game, this.hall, 100, 100);

        UnitAccountingUtils.transferUnit(game, hallUnit, second, 0);

        Assertions.assertSame(second, hallUnit.getOwner());
        Assertions.assertEquals(0, progress.getUnitCount(first, this.hall));
        Assertions.assertEquals(1, progress.getCompletedUnitCount(second, this.hall));
        Assertions.assertEquals(0, game.getEconomy().getSupplyProvided(first, this.humans));
        Assertions.assertEquals(FARM_SUPPLY, game.getEconomy().getSupplyProvided(second, this.humans));

        UnitAccountingUtils.removeUnit(game, hallUnit, false);

        Assertions.assertEquals(0, progress.getUnitCount(second, this.hall));
        Assertions.assertEquals(0, game.getEconomy().getSupplyProvided(second, this.humans));
    }

    private void runFrames(@NotNull Game game, int count) {
        for (int frame = 0; frame < count; frame++) {
            game.getFrameRunner().runFrame();
        }
    }

    private @NotNull Unit createUnit(@NotNull Game game, @NotNull UnitType type, int x, int y) {
        Unit unit = ((StandardProductionSystem) game.getProduction()).createUnit(game.getPlayer(0), type, x, y, 0);

        Assertions.assertNotNull(unit);

        return unit;
    }

    private @NotNull Game createGame() {
        GameDefinition.Builder builder = new GameDefinition.Builder()
                .setLimits(new EngineLimits.Builder().setUnits(UNIT_LIMIT).setOrders(ORDER_LIMIT).build())
                .useSystem(TableUnitBounds.KEY, context -> this.bounds)
                .useSystem(SpatialIndex.KEY, context -> new StandardSpatialIndex(this.bounds, 2 * TILE_SIZE + 1, 2 * TILE_SIZE + 1))
                .useSystem(MovementSystem.KEY, context -> new StandardMovementSystem(SkirmishScenario.MOTION_MODEL))
                .useSystem(EconomySystem.KEY, context -> new StandardEconomySystem())
                .useSystem(ProductionSystem.KEY, context -> new StandardProductionSystem.Builder(SkirmishScenario.QUEUE_CAPACITY)
                        .setTerrain(this.terrain)
                        .setBuildOrder(this.build)
                        .setConstructionRefundPercent(REFUND_PERCENT)
                        .setConstructionStartPercent(START_PERCENT)
                        .build())
                .useSystem(OrderSystem.KEY, context -> new StandardOrderSystem(this::registerBehaviors))
                .addFramePhase(OrderSystem.KEY, orders -> (StandardOrderSystem) orders)
                .addFramePhase(ProductionSystem.KEY, production -> (StandardProductionSystem) production)
                .addFramePhase(MovementSystem.KEY, movement -> (StandardMovementSystem) movement);

        StandardProductionSystem.addColumns(builder, SkirmishScenario.QUEUE_CAPACITY);
        builder.getContent().getResourceKinds().register(this.gold);
        builder.getContent().getFactions().register(this.humans, new FactionDefinition.Builder().build());
        builder.getContent().getOrderTypes().register(this.idle, new OrderDefinition.Builder().build());
        builder.getContent().getOrderTypes().register(this.build, new OrderDefinition.Builder().build());
        builder.getContent().getMotionProfiles().register(this.walker, new MotionProfileDefinition.Builder().setTopSpeed(512)
                                                                                                            .setAcceleration(64)
                                                                                                            .setStoppingDistance(2048)
                                                                                                            .setTurnRate(40)
                                                                                                            .build());
        builder.getContent().getUnitTypes().register(this.hall, this.createBox(16).setFaction(this.humans)
                                                                    .addTrait(UnitTrait.BUILDING)
                                                                    .setHitPoints(500)
                                                                    .setSupplyProvided(FARM_SUPPLY)
                                                                    .build());
        builder.getContent().getUnitTypes().register(this.farm, this.createBox(TILE_SIZE - 1).setFaction(this.humans)
                                                                    .addTrait(UnitTrait.BUILDING)
                                                                    .setHitPoints(FARM_HIT_POINTS)
                                                                    .setCost(ResourceCost.of(this.gold, FARM_COST))
                                                                    .setBuildTime(FARM_TIME)
                                                                    .setSupplyProvided(FARM_SUPPLY)
                                                                    .setPlacementWidth(2 * TILE_SIZE)
                                                                    .setPlacementHeight(2 * TILE_SIZE)
                                                                    .build());
        builder.getContent().getUnitTypes().register(this.peasant, this.createBox(4).setFaction(this.humans)
                                                                       .addTrait(UnitTrait.WORKER)
                                                                       .setMotionProfile(this.walker)
                                                                       .setHitPoints(10)
                                                                       .setIdleOrder(ControllerKind.HUMAN, this.idle)
                                                                       .build());

        GameSetup setup = new GameSetup.Builder(new MapReference("empty", new byte[0])).addPlayer(new PlayerSlot(0, ControllerKind.HUMAN, 0, 0, null))
                                                                                       .addPlayer(new PlayerSlot(1, ControllerKind.HUMAN, 1, 1, null))
                                                                                       .build();

        return Game.create(builder.build(), setup);
    }

    private @NotNull UnitDefinition.Builder createBox(int extent) {
        return new UnitDefinition.Builder().setCollisionLeft(extent).setCollisionUp(extent).setCollisionRight(extent).setCollisionDown(extent);
    }

    private void registerBehaviors(@NotNull Game game, @NotNull OrderRegistry<OrderBehavior<OrderContext, Unit>> behaviors) {
        behaviors.register(this.build, new BuildBehavior((StandardProductionSystem) game.getProduction(), 4, true));
    }
}
