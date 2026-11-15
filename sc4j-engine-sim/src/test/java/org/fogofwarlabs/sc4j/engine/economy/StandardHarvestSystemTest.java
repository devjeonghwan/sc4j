package org.fogofwarlabs.sc4j.engine.economy;

import org.fogofwarlabs.sc4j.engine.content.definition.MotionProfileDefinition;
import org.fogofwarlabs.sc4j.engine.content.definition.OrderDefinition;
import org.fogofwarlabs.sc4j.engine.content.definition.UnitDefinition;
import org.fogofwarlabs.sc4j.engine.content.definition.UnitTrait;
import org.fogofwarlabs.sc4j.engine.content.economy.ResourceKind;
import org.fogofwarlabs.sc4j.engine.content.type.MotionProfile;
import org.fogofwarlabs.sc4j.engine.content.type.OrderType;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.entity.pool.SlotList;
import org.fogofwarlabs.sc4j.engine.game.*;
import org.fogofwarlabs.sc4j.engine.movement.MovementSystem;
import org.fogofwarlabs.sc4j.engine.movement.StandardMovementSystem;
import org.fogofwarlabs.sc4j.engine.order.*;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.fogofwarlabs.sc4j.engine.production.ProductionSystem;
import org.fogofwarlabs.sc4j.engine.production.StandardProductionSystem;
import org.fogofwarlabs.sc4j.engine.spatial.TableUnitBounds;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the engine default harvest system with the gather and return behaviors in an engine-only game: a worker carries trips from a container
 * to the drop-off until the container is empty and leaves the game, then takes the next container near it, and idles when none is left.
 */
final class StandardHarvestSystemTest {
    private static final int UNIT_LIMIT = 8;
    private static final int ORDER_LIMIT = 8;
    private static final int GATHER_AMOUNT = 8;
    private static final int GATHER_TIME = 10;
    private static final int REACH = 4;
    private static final int SEARCH_RANGE = 400;
    private static final int FRAME_LIMIT = 3000;

    private final @NotNull ResourceKind gold;
    private final @NotNull MotionProfile walker;
    private final @NotNull OrderType idle;
    private final @NotNull OrderType gather;
    private final @NotNull OrderType returnResources;
    private final @NotNull UnitType hall;
    private final @NotNull UnitType mine;
    private final @NotNull UnitType peasant;
    private final @NotNull TableUnitBounds bounds;

    StandardHarvestSystemTest() {
        this.gold            = new ResourceKind("gold");
        this.walker          = new MotionProfile("walker");
        this.idle            = new OrderType("idle");
        this.gather          = new OrderType("gather");
        this.returnResources = new OrderType("return");
        this.hall            = new UnitType("hall");
        this.mine            = new UnitType("mine");
        this.peasant         = new UnitType("peasant");
        this.bounds          = new TableUnitBounds();
    }

    @Test
    void workers_carry_trips_until_the_containers_are_empty() {
        Game game = this.createGame();
        Player player = game.getPlayer(0);
        StandardHarvestSystem harvest = (StandardHarvestSystem) game.getModSystem(HarvestSystem.KEY);
        Unit hallUnit = this.createUnit(game, 0, this.hall, 100, 100);
        Unit first = this.createUnit(game, 1, this.mine, 220, 100);
        Unit second = this.createUnit(game, 1, this.mine, 100, 220);
        Unit worker = this.createUnit(game, 0, this.peasant, 140, 100);

        harvest.setResources(first, this.gold, 20);
        harvest.setResources(second, this.gold, GATHER_AMOUNT);

        Assertions.assertSame(this.gold, harvest.getResourceKind(first));
        Assertions.assertSame(first, harvest.findResourceContainer(this.gold, 200, 100, SEARCH_RANGE));
        Assertions.assertSame(hallUnit, harvest.findDropOff(worker));

        game.getOrders().issue(worker, this.gather, first.getPositionX(), first.getPositionY(), first, 0);

        int carriedMaximum = 0;

        for (int frame = 0; frame < FRAME_LIMIT && worker.getOrderType() != this.idle; frame++) {
            game.getFrameRunner().runFrame();

            carriedMaximum = Math.max(carriedMaximum, harvest.getCarriedAmount(worker));
        }

        Assertions.assertEquals(GATHER_AMOUNT, carriedMaximum);
        Assertions.assertEquals(20 + GATHER_AMOUNT, game.getEconomy().getAmount(player, this.gold));
        Assertions.assertFalse(game.getUnits().isAllocated(first.getIndex()));
        Assertions.assertFalse(game.getUnits().isAllocated(second.getIndex()));
        Assertions.assertSame(this.idle, worker.getOrderType());
        Assertions.assertEquals(0, harvest.getCarriedAmount(worker));
        Assertions.assertNull(harvest.findResourceContainer(null, 100, 100, SEARCH_RANGE));
        Assertions.assertEquals(0, game.getContent().getProgress().getUnitCount(game.getPlayer(1), this.mine));
    }

    private @NotNull Unit createUnit(@NotNull Game game, int player, @NotNull UnitType type, int x, int y) {
        Unit unit = ((StandardProductionSystem) game.getProduction()).createUnit(game.getPlayer(player), type, x, y, 0);

        Assertions.assertNotNull(unit);

        return unit;
    }

    private @NotNull Game createGame() {
        GameDefinition.Builder builder = new GameDefinition.Builder()
                .setLimits(new EngineLimits.Builder().setUnits(UNIT_LIMIT).setOrders(ORDER_LIMIT).build())
                .useSystem(TableUnitBounds.KEY, context -> this.bounds)
                .useSystem(MovementSystem.KEY, context -> new StandardMovementSystem(SkirmishScenario.MOTION_MODEL))
                .useSystem(EconomySystem.KEY, context -> new StandardEconomySystem())
                .useSystem(ProductionSystem.KEY, context -> new StandardProductionSystem.Builder(SkirmishScenario.QUEUE_CAPACITY).build())
                .useSystem(HarvestSystem.KEY, context -> new StandardHarvestSystem(this.bounds, GATHER_AMOUNT, GATHER_TIME, REACH))
                .useSystem(OrderSystem.KEY, context -> new StandardOrderSystem(this::registerBehaviors))
                .addFramePhase(StandardHarvestSystemTest::updateOrders)
                .addFramePhase(MovementSystem.KEY, movement -> (StandardMovementSystem) movement);

        StandardProductionSystem.addColumns(builder, SkirmishScenario.QUEUE_CAPACITY);
        StandardHarvestSystem.addColumns(builder);
        builder.getContent().getResourceKinds().register(this.gold);
        builder.getContent().getOrderTypes().register(this.idle, new OrderDefinition.Builder().build());
        builder.getContent().getOrderTypes().register(this.gather, new OrderDefinition.Builder().build());
        builder.getContent().getOrderTypes().register(this.returnResources, new OrderDefinition.Builder().build());
        builder.getContent().getMotionProfiles().register(this.walker, new MotionProfileDefinition.Builder().setTopSpeed(512)
                                                                                                            .setAcceleration(64)
                                                                                                            .setStoppingDistance(2048)
                                                                                                            .setTurnRate(40)
                                                                                                            .build());
        builder.getContent().getUnitTypes().register(this.hall, this.createBox(16).addTrait(UnitTrait.BUILDING)
                                                                    .addTrait(UnitTrait.RESOURCE_DEPOT)
                                                                    .setHitPoints(100)
                                                                    .build());
        builder.getContent().getUnitTypes().register(this.mine, this.createBox(12).addTrait(UnitTrait.RESOURCE_CONTAINER).build());
        builder.getContent().getUnitTypes().register(this.peasant, this.createBox(4).addTrait(UnitTrait.WORKER)
                                                                       .setMotionProfile(this.walker)
                                                                       .setHitPoints(10)
                                                                       .setIdleOrder(ControllerKind.HUMAN, this.idle)
                                                                       .build());

        GameSetup setup = new GameSetup.Builder(new MapReference("empty", new byte[0]))
                .addPlayer(new PlayerSlot(0, ControllerKind.HUMAN, 0, 0, null))
                .addPlayer(new PlayerSlot(1, ControllerKind.NEUTRAL, 1, 1, null))
                .build();

        return Game.create(builder.build(), setup);
    }

    private @NotNull UnitDefinition.Builder createBox(int extent) {
        return new UnitDefinition.Builder().setCollisionLeft(extent).setCollisionUp(extent).setCollisionRight(extent).setCollisionDown(extent);
    }

    private void registerBehaviors(@NotNull Game game, @NotNull OrderRegistry<OrderBehavior<OrderContext, Unit>> behaviors) {
        StandardHarvestSystem harvest = (StandardHarvestSystem) game.getModSystem(HarvestSystem.KEY);

        behaviors.register(this.gather, new GatherBehavior(harvest, this.returnResources, SEARCH_RANGE));
        behaviors.register(this.returnResources, new ReturnResourcesBehavior(harvest, this.gather));
    }

    private static void updateOrders(@NotNull Game game) {
        SlotList used = game.getUnits().getUsedSlots();
        int index = used.getFirst();

        while (index != SlotList.NONE) {
            int next = used.getNext(index);

            game.getOrders().update(game.getUnits().get(index), 0);

            index = next;
        }
    }
}
