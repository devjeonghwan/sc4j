package org.fogofwarlabs.sc4j.demo.rules;

import org.fogofwarlabs.sc4j.demo.content.DemoContent;
import org.fogofwarlabs.sc4j.demo.map.DemoMaps;
import org.fogofwarlabs.sc4j.demo.order.DemoOrders;
import org.fogofwarlabs.sc4j.engine.collision.CollisionSystem;
import org.fogofwarlabs.sc4j.engine.collision.StandardCollisionSystem;
import org.fogofwarlabs.sc4j.engine.combat.CombatSystem;
import org.fogofwarlabs.sc4j.engine.combat.PatrolBehavior;
import org.fogofwarlabs.sc4j.engine.combat.StandardCombatSystem;
import org.fogofwarlabs.sc4j.engine.computer.ComputerPlayerSystem;
import org.fogofwarlabs.sc4j.engine.computer.ComputerSettings;
import org.fogofwarlabs.sc4j.engine.computer.StandardComputerController;
import org.fogofwarlabs.sc4j.engine.computer.StandardComputerPlayerSystem;
import org.fogofwarlabs.sc4j.engine.content.requirement.RequirementSystem;
import org.fogofwarlabs.sc4j.engine.content.requirement.declarative.DeclarativeRequirementSystem;
import org.fogofwarlabs.sc4j.engine.economy.EconomySystem;
import org.fogofwarlabs.sc4j.engine.economy.HarvestSystem;
import org.fogofwarlabs.sc4j.engine.economy.StandardEconomySystem;
import org.fogofwarlabs.sc4j.engine.economy.StandardHarvestSystem;
import org.fogofwarlabs.sc4j.engine.game.EngineLimits;
import org.fogofwarlabs.sc4j.engine.game.GameDefinition;
import org.fogofwarlabs.sc4j.engine.game.system.SystemContext;
import org.fogofwarlabs.sc4j.engine.map.StandardMapData;
import org.fogofwarlabs.sc4j.engine.movement.MotionModel;
import org.fogofwarlabs.sc4j.engine.movement.MovementSystem;
import org.fogofwarlabs.sc4j.engine.movement.StandardMovementSystem;
import org.fogofwarlabs.sc4j.engine.order.OrderSystem;
import org.fogofwarlabs.sc4j.engine.order.StandardOrderSystem;
import org.fogofwarlabs.sc4j.engine.path.PathfindingSystem;
import org.fogofwarlabs.sc4j.engine.path.StandardPathfindingSystem;
import org.fogofwarlabs.sc4j.engine.production.ProductionSystem;
import org.fogofwarlabs.sc4j.engine.production.StandardProductionSystem;
import org.fogofwarlabs.sc4j.engine.projectile.ProjectileSystem;
import org.fogofwarlabs.sc4j.engine.projectile.StandardProjectileSystem;
import org.fogofwarlabs.sc4j.engine.spatial.SpatialIndex;
import org.fogofwarlabs.sc4j.engine.spatial.StandardSpatialIndex;
import org.fogofwarlabs.sc4j.engine.spatial.TableUnitBounds;
import org.fogofwarlabs.sc4j.engine.terrain.TerrainGrid;
import org.fogofwarlabs.sc4j.engine.trigger.StandardTriggerSystem;
import org.fogofwarlabs.sc4j.engine.trigger.TriggerSystem;
import org.fogofwarlabs.sc4j.engine.visibility.StandardVisibilitySystem;
import org.fogofwarlabs.sc4j.engine.visibility.VisibilitySystem;
import org.jetbrains.annotations.NotNull;

/**
 * Defines the rules of the demo's games on the engine defaults only: every system is the engine default, the requirements are the declarative
 * requirements of the pack content, the victory is the map's (annihilation on the built-in maps) and the computer players are the engine
 * default controller. One definition serves every game of the demo: each system is created per game, over the terrain of the game's map.
 * <p>
 * Production: a queue of {@value #QUEUE_CAPACITY} entries per producer, the cost spent at the enqueue and the supply held from the start of an
 * entry; a cancelled entry or construction refunds its whole cost; a building starts its construction with
 * {@value #CONSTRUCTION_START_PERCENT} percent of its hit points.
 * <p>
 * Motion: {@value #DIRECTION_COUNT} directions per turn; a unit slower than {@value #INSTANT_STOP_SPEED} (fixed-point) stops at once at its
 * destination; a unit within {@value #BRAKING_CHECK_DISTANCE} pixels of its waypoint brakes for a turn that takes longer than
 * {@value #BRAKING_MARGIN_PERCENT} percent of the way left.
 * <p>
 * Combat: a splash hits a unit within its inner radius with {@value #INNER_SPLASH_PERCENT}, its middle radius with
 * {@value #MIDDLE_SPLASH_PERCENT} and its outer radius with {@value #OUTER_SPLASH_PERCENT} percent of the damage; a hit deals at least half a
 * point.
 * <p>
 * Frame phases in order: computer players, orders, production, movement, collision, projectiles, visibility, triggers.
 */
public final class DemoRules {
    /**
     * The number of entries a producer's queue holds.
     */
    public static final int QUEUE_CAPACITY = 5;
    /**
     * The share in percent of its hit points a building starts its construction with.
     */
    public static final int CONSTRUCTION_START_PERCENT = 10;
    /**
     * The number of directions of a full turn of the units and projectiles.
     */
    public static final int DIRECTION_COUNT = 256;
    /**
     * The speed, fixed-point, below which a unit stops at once at its destination.
     */
    public static final int INSTANT_STOP_SPEED = 192;
    /**
     * The distance in pixels to its waypoint within which a unit brakes for a turn.
     */
    public static final int BRAKING_CHECK_DISTANCE = 32;
    /**
     * The share in percent of the distance covered while turning that the braking before a turn compares with the way left.
     */
    public static final int BRAKING_MARGIN_PERCENT = 150;
    /**
     * The share in percent of a hit's damage a unit within the inner splash radius takes.
     */
    public static final int INNER_SPLASH_PERCENT = 100;
    /**
     * The share in percent of a hit's damage a unit within the middle splash radius takes.
     */
    public static final int MIDDLE_SPLASH_PERCENT = 50;
    /**
     * The share in percent of a hit's damage a unit within the outer splash radius takes.
     */
    public static final int OUTER_SPLASH_PERCENT = 25;
    /**
     * The share in percent of its cost a cancelled queue entry or construction refunds.
     */
    public static final int REFUND_PERCENT = 100;

    /**
     * The size of the unit pool.
     */
    private static final int UNIT_LIMIT = 400;
    /**
     * The size of the order pool.
     */
    private static final int ORDER_LIMIT = 800;
    /**
     * The size of the projectile pool.
     */
    private static final int PROJECTILE_LIMIT = 200;
    /**
     * The frames a projectile flies at most.
     */
    private static final int MAXIMUM_FLIGHT_TIME = 120;
    /**
     * The width and height in pixels of the largest unit, a town hall of three tiles.
     */
    private static final int LARGEST_UNIT_SIZE = 96;
    /**
     * The distance in pixels a free position for a new or blocked unit is searched at.
     */
    private static final int FREE_POSITION_DISTANCE = 128;
    /**
     * The largest sight range in tiles.
     */
    private static final int MAXIMUM_SIGHT_RANGE = 10;
    /**
     * The gold of one gathering trip.
     */
    private static final int GATHER_AMOUNT = 8;
    /**
     * The frames of one gathering trip at the mine.
     */
    private static final int GATHER_TIME = 24;
    /**
     * The gap in pixels within which a worker gathers and delivers.
     */
    private static final int HARVEST_REACH = 6;
    /**
     * The smallest damage of a hit, half a point.
     */
    private static final int MINIMUM_DAMAGE = 1 << (StandardCombatSystem.FRACTION_BITS - 1);

    private final @NotNull DemoContent content;
    private final @NotNull DemoMaps maps;
    private final @NotNull ComputerSettings computerSettings;
    private final @NotNull MotionModel motionModel;

    /**
     * Creates a {@link DemoRules} instance over the demo's content and maps.
     *
     * @param content the demo's content
     * @param maps    the demo's maps, which the game setups name
     */
    public DemoRules(@NotNull DemoContent content, @NotNull DemoMaps maps) {
        DemoOrders orders = content.getOrders();

        this.content          = content;
        this.maps             = maps;
        this.computerSettings = new ComputerSettings.Builder(orders.getGather(), orders.getAttackMove()).build();
        this.motionModel      = new MotionModel.Builder(DIRECTION_COUNT).setInstantStopSpeed(INSTANT_STOP_SPEED)
                                                                        .setBrakingCheckDistance(BRAKING_CHECK_DISTANCE)
                                                                        .setBrakingMarginPercent(BRAKING_MARGIN_PERCENT)
                                                                        .build();
    }

    /**
     * Defines the demo's games on an open game definition: limits, the extension columns of the engine defaults, every system, the harvest
     * blocking rule, the frame phases and the content.
     *
     * @param builder the open game definition builder of the demo's games
     */
    public void define(GameDefinition.@NotNull Builder builder) {
        DemoOrders orders = this.content.getOrders();

        builder.setLimits(new EngineLimits.Builder().setUnits(UNIT_LIMIT).setOrders(ORDER_LIMIT).setProjectiles(PROJECTILE_LIMIT).build())
               .useSystem(TableUnitBounds.KEY, context -> new TableUnitBounds())
               .useSystem(SpatialIndex.KEY, context -> new StandardSpatialIndex(context.require(TableUnitBounds.KEY),
                                                                                LARGEST_UNIT_SIZE, LARGEST_UNIT_SIZE))
               .useSystem(PathfindingSystem.KEY, context -> new StandardPathfindingSystem(this.requireTerrain(context),
                                                                                          context.require(TableUnitBounds.KEY)))
               .useSystem(MovementSystem.KEY, context -> new StandardMovementSystem(this.motionModel))
               .useSystem(CollisionSystem.KEY, context -> new StandardCollisionSystem(context.require(TableUnitBounds.KEY), FREE_POSITION_DISTANCE,
                                                                                      this.requireTerrain(context)))
               .useSystem(VisibilitySystem.KEY, context -> new StandardVisibilitySystem(this.requireTerrain(context), MAXIMUM_SIGHT_RANGE))
               .useSystem(EconomySystem.KEY, context -> new StandardEconomySystem())
               .useSystem(RequirementSystem.KEY, context -> new DeclarativeRequirementSystem(this.content.getRequirements()))
               .useSystem(CombatSystem.KEY, context -> new StandardCombatSystem.Builder()
                       .setSplashShares(INNER_SPLASH_PERCENT, MIDDLE_SPLASH_PERCENT, OUTER_SPLASH_PERCENT)
                       .setMinimumDamage(MINIMUM_DAMAGE)
                       .build())
               .useSystem(ProjectileSystem.KEY, context -> new StandardProjectileSystem(MAXIMUM_FLIGHT_TIME, this.motionModel))
               .useSystem(ProductionSystem.KEY, context -> new StandardProductionSystem.Builder(QUEUE_CAPACITY)
                       .setTerrain(this.requireTerrain(context))
                       .setRallyOrder(orders.getMove())
                       .setBuildOrder(orders.getBuild())
                       .setQueueRefundPercent(REFUND_PERCENT)
                       .setConstructionRefundPercent(REFUND_PERCENT)
                       .setConstructionStartPercent(CONSTRUCTION_START_PERCENT)
                       .build())
               .useSystem(HarvestSystem.KEY, context -> new StandardHarvestSystem(context.require(TableUnitBounds.KEY),
                                                                                  GATHER_AMOUNT, GATHER_TIME, HARVEST_REACH))
               .useSystem(OrderSystem.KEY, context -> new StandardOrderSystem(orders::registerBehaviors))
               .useSystem(TriggerSystem.KEY, this::createTriggers)
               .useSystem(ComputerPlayerSystem.KEY,
                          context -> new StandardComputerPlayerSystem(StandardComputerController.createFactory(this.computerSettings)))
               .addHook(CollisionSystem.BLOCKING, context -> (StandardHarvestSystem) context.require(HarvestSystem.KEY))
               .addFramePhase(ComputerPlayerSystem.KEY)
               .addFramePhase(OrderSystem.KEY, system -> (StandardOrderSystem) system)
               .addFramePhase(ProductionSystem.KEY, system -> (StandardProductionSystem) system)
               .addFramePhase(MovementSystem.KEY, system -> (StandardMovementSystem) system)
               .addFramePhase(CollisionSystem.KEY, system -> (StandardCollisionSystem) system)
               .addFramePhase(ProjectileSystem.KEY, system -> (StandardProjectileSystem) system)
               .addFramePhase(VisibilitySystem.KEY, system -> (StandardVisibilitySystem) system)
               .addFramePhase(TriggerSystem.KEY);

        StandardCombatSystem.addColumns(builder);
        StandardProductionSystem.addColumns(builder, QUEUE_CAPACITY);
        StandardHarvestSystem.addColumns(builder);
        PatrolBehavior.addColumns(builder);
        this.content.register(builder.getContent());
    }

    private @NotNull StandardTriggerSystem createTriggers(@NotNull SystemContext context) {
        StandardMapData data = this.maps.require(context.getSetup()).requireModData(StandardMapData.class);

        return new StandardTriggerSystem.Builder().addTriggers(data.getTriggers()).build();
    }

    private @NotNull TerrainGrid requireTerrain(@NotNull SystemContext context) {
        return this.maps.require(context.getSetup()).getTerrain();
    }
}
