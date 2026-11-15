package org.fogofwarlabs.sc4j.engine.game;

import org.fogofwarlabs.sc4j.engine.collision.CollisionSystem;
import org.fogofwarlabs.sc4j.engine.collision.StandardCollisionSystem;
import org.fogofwarlabs.sc4j.engine.combat.*;
import org.fogofwarlabs.sc4j.engine.computer.ComputerPlayerSystem;
import org.fogofwarlabs.sc4j.engine.computer.ComputerSettings;
import org.fogofwarlabs.sc4j.engine.computer.StandardComputerController;
import org.fogofwarlabs.sc4j.engine.computer.StandardComputerPlayerSystem;
import org.fogofwarlabs.sc4j.engine.content.definition.*;
import org.fogofwarlabs.sc4j.engine.content.economy.ResourceCost;
import org.fogofwarlabs.sc4j.engine.content.economy.ResourceKind;
import org.fogofwarlabs.sc4j.engine.content.faction.Faction;
import org.fogofwarlabs.sc4j.engine.content.faction.FactionDefinition;
import org.fogofwarlabs.sc4j.engine.content.registry.ContentRegistry;
import org.fogofwarlabs.sc4j.engine.content.requirement.RequirementSystem;
import org.fogofwarlabs.sc4j.engine.content.requirement.declarative.*;
import org.fogofwarlabs.sc4j.engine.content.type.MotionProfile;
import org.fogofwarlabs.sc4j.engine.content.type.OrderType;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.content.type.WeaponType;
import org.fogofwarlabs.sc4j.engine.economy.*;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.movement.MotionModel;
import org.fogofwarlabs.sc4j.engine.movement.MovementSystem;
import org.fogofwarlabs.sc4j.engine.movement.StandardMovementSystem;
import org.fogofwarlabs.sc4j.engine.order.*;
import org.fogofwarlabs.sc4j.engine.path.PathfindingSystem;
import org.fogofwarlabs.sc4j.engine.path.StandardPathfindingSystem;
import org.fogofwarlabs.sc4j.engine.production.BuildBehavior;
import org.fogofwarlabs.sc4j.engine.production.ProductionSystem;
import org.fogofwarlabs.sc4j.engine.production.StandardProductionSystem;
import org.fogofwarlabs.sc4j.engine.projectile.ProjectileSystem;
import org.fogofwarlabs.sc4j.engine.projectile.StandardProjectileSystem;
import org.fogofwarlabs.sc4j.engine.spatial.SpatialIndex;
import org.fogofwarlabs.sc4j.engine.spatial.StandardSpatialIndex;
import org.fogofwarlabs.sc4j.engine.spatial.TableUnitBounds;
import org.fogofwarlabs.sc4j.engine.terrain.TerrainGrid;
import org.fogofwarlabs.sc4j.engine.trigger.StandardTriggerSystem;
import org.fogofwarlabs.sc4j.engine.trigger.TriggerDefinition;
import org.fogofwarlabs.sc4j.engine.trigger.TriggerSystem;
import org.fogofwarlabs.sc4j.engine.visibility.StandardVisibilitySystem;
import org.fogofwarlabs.sc4j.engine.visibility.VisibilitySystem;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Builds engine-only skirmish games over the engine default systems with the small content of two factions, the wardens and the raiders: each
 * has a worker, a town hall that is the drop-off and provides supply, a supply building, a production building, a melee and a ranged fighting
 * unit; both gather gold from neutral gold mines. The requirement graph: the production building needs a completed town hall, a building needs
 * a worker as its builder, a worker needs the town hall and a fighting unit the production building of its faction as its producer. A scout
 * without faction or weapon serves the commands of a person.
 */
public final class SkirmishScenario {
    /**
     * The tile size of the maps in pixels.
     */
    public static final int TILE_SIZE = 32;
    /**
     * The walk cell size of the maps in pixels.
     */
    public static final int WALK_CELL_SIZE = 8;
    /**
     * The gold a player starts with.
     */
    public static final int START_GOLD = 50;
    /**
     * The motion model of the units and projectiles: 256 directions, an instant stop below three quarters of a pixel per frame, braking for a
     * turn within 32 pixels of a waypoint with a margin of one and a half.
     */
    public static final @NotNull MotionModel MOTION_MODEL;
    /**
     * The number of entries a producer's queue holds.
     */
    public static final int QUEUE_CAPACITY = 5;

    private static final int UNIT_LIMIT = 400;
    private static final int ORDER_LIMIT = 800;
    private static final int PROJECTILE_LIMIT = 200;
    private static final int MAXIMUM_FLIGHT_TIME = 120;
    private static final int DIRECTION_COUNT = 256;
    private static final int INSTANT_STOP_SPEED = 192;
    private static final int BRAKING_CHECK_DISTANCE = 32;
    private static final int BRAKING_MARGIN_PERCENT = 150;
    private static final int INNER_SPLASH_PERCENT = 100;
    private static final int MIDDLE_SPLASH_PERCENT = 50;
    private static final int OUTER_SPLASH_PERCENT = 25;
    private static final int MINIMUM_DAMAGE = 1 << (StandardCombatSystem.FRACTION_BITS - 1);
    private static final int CONSTRUCTION_REFUND_PERCENT = 75;
    private static final int CONSTRUCTION_START_PERCENT = 10;
    private static final int LARGEST_UNIT_SIZE = 3 * TILE_SIZE;
    private static final int FREE_POSITION_DISTANCE = 128;
    private static final int MAXIMUM_SIGHT_RANGE = 10;
    private static final int GATHER_AMOUNT = 8;
    private static final int GATHER_TIME = 24;
    private static final int REACH = 6;
    private static final int RESOURCE_SEARCH_RANGE = 640;
    private static final int ARRIVAL_DISTANCE = 24;

    static {
        MOTION_MODEL = new MotionModel.Builder(DIRECTION_COUNT).setInstantStopSpeed(INSTANT_STOP_SPEED)
                                                               .setBrakingCheckDistance(BRAKING_CHECK_DISTANCE)
                                                               .setBrakingMarginPercent(BRAKING_MARGIN_PERCENT)
                                                               .build();
    }

    private final @NotNull ResourceKind gold;
    private final @NotNull Faction wardens;
    private final @NotNull Faction raiders;
    private final @NotNull MotionProfile walker;
    private final @NotNull MotionProfile runner;
    private final @NotNull MotionProfile arrow;
    private final @NotNull WeaponType spear;
    private final @NotNull WeaponType bow;
    private final @NotNull WeaponType club;
    private final @NotNull WeaponType sling;
    private final @NotNull OrderType idle;
    private final @NotNull OrderType guard;
    private final @NotNull OrderType move;
    private final @NotNull OrderType stop;
    private final @NotNull OrderType attack;
    private final @NotNull OrderType attackMove;
    private final @NotNull OrderType patrol;
    private final @NotNull OrderType holdPosition;
    private final @NotNull OrderType gather;
    private final @NotNull OrderType returnResources;
    private final @NotNull OrderType build;
    private final @NotNull UnitType goldMine;
    private final @NotNull UnitType keeper;
    private final @NotNull UnitType keep;
    private final @NotNull UnitType granary;
    private final @NotNull UnitType barracks;
    private final @NotNull UnitType spearman;
    private final @NotNull UnitType archer;
    private final @NotNull UnitType thrall;
    private final @NotNull UnitType lodge;
    private final @NotNull UnitType pen;
    private final @NotNull UnitType warcamp;
    private final @NotNull UnitType brute;
    private final @NotNull UnitType slinger;
    private final @NotNull UnitType scout;
    private final @NotNull TableUnitBounds bounds;

    /**
     * Creates a {@link SkirmishScenario} instance with its content types.
     */
    public SkirmishScenario() {
        this.gold            = new ResourceKind("gold");
        this.wardens         = new Faction("wardens");
        this.raiders         = new Faction("raiders");
        this.walker          = new MotionProfile("walker");
        this.runner          = new MotionProfile("runner");
        this.arrow           = new MotionProfile("arrow");
        this.spear           = new WeaponType("spear");
        this.bow             = new WeaponType("bow");
        this.club            = new WeaponType("club");
        this.sling           = new WeaponType("sling");
        this.idle            = new OrderType("idle");
        this.guard           = new OrderType("guard");
        this.move            = new OrderType("move");
        this.stop            = new OrderType("stop");
        this.attack          = new OrderType("attack");
        this.attackMove      = new OrderType("attack move");
        this.patrol          = new OrderType("patrol");
        this.holdPosition    = new OrderType("hold position");
        this.gather          = new OrderType("gather");
        this.returnResources = new OrderType("return resources");
        this.build           = new OrderType("build");
        this.goldMine        = new UnitType("gold mine");
        this.keeper          = new UnitType("keeper");
        this.keep            = new UnitType("keep");
        this.granary         = new UnitType("granary");
        this.barracks        = new UnitType("barracks");
        this.spearman        = new UnitType("spearman");
        this.archer          = new UnitType("archer");
        this.thrall          = new UnitType("thrall");
        this.lodge           = new UnitType("lodge");
        this.pen             = new UnitType("pen");
        this.warcamp         = new UnitType("warcamp");
        this.brute           = new UnitType("brute");
        this.slinger         = new UnitType("slinger");
        this.scout           = new UnitType("scout");
        this.bounds          = new TableUnitBounds();
    }

    /**
     * @return the unit types of the content in registration order
     */
    public @NotNull List<@NotNull UnitType> getUnitTypes() {
        return List.of(this.goldMine, this.keeper, this.keep, this.granary, this.barracks, this.spearman, this.archer, this.thrall, this.lodge,
                       this.pen, this.warcamp, this.brute, this.slinger, this.scout);
    }

    /**
     * @return the resource kinds of the content in registration order
     */
    public @NotNull List<@NotNull ResourceKind> getResourceKinds() {
        return List.of(this.gold);
    }

    /**
     * @return the gold resource kind
     */
    public @NotNull ResourceKind getGold() {
        return this.gold;
    }

    /**
     * @return the wardens faction
     */
    public @NotNull Faction getWardens() {
        return this.wardens;
    }

    /**
     * @return the raiders faction
     */
    public @NotNull Faction getRaiders() {
        return this.raiders;
    }

    /**
     * @return the unit bounds every system of the scenario's games uses
     */
    public @NotNull TableUnitBounds getBounds() {
        return this.bounds;
    }

    /**
     * @return the move order type
     */
    public @NotNull OrderType getMove() {
        return this.move;
    }

    /**
     * @return the patrol order type
     */
    public @NotNull OrderType getPatrol() {
        return this.patrol;
    }

    /**
     * @return the gold mine unit type
     */
    public @NotNull UnitType getGoldMine() {
        return this.goldMine;
    }

    /**
     * @return the town hall of the wardens
     */
    public @NotNull UnitType getKeep() {
        return this.keep;
    }

    /**
     * @return the town hall of the raiders
     */
    public @NotNull UnitType getLodge() {
        return this.lodge;
    }

    /**
     * @return the worker of the wardens
     */
    public @NotNull UnitType getKeeper() {
        return this.keeper;
    }

    /**
     * Creates the game definition builder of a skirmish on a terrain: the content, every engine default system and the frame phases. The
     * definition serves one game, since its systems are created with it.
     *
     * @param terrain  the terrain of the map
     * @param triggers the triggers of the standard trigger system
     * @return the builder, ready to build
     */
    public @NotNull GameDefinition.Builder createDefinition(@NotNull TerrainGrid terrain, @NotNull List<@NotNull TriggerDefinition> triggers) {
        ComputerSettings settings = new ComputerSettings.Builder(this.gather, this.attackMove).build();
        DeclarativeRequirements requirements = this.createRequirements();
        StandardHarvestSystem harvest = new StandardHarvestSystem(this.bounds, GATHER_AMOUNT, GATHER_TIME, REACH);
        GameDefinition.Builder builder = new GameDefinition.Builder()
                .setLimits(new EngineLimits.Builder().setUnits(UNIT_LIMIT).setOrders(ORDER_LIMIT).setProjectiles(PROJECTILE_LIMIT).build())
                .useSystem(TableUnitBounds.KEY, context -> this.bounds)
                .useSystem(SpatialIndex.KEY, context -> new StandardSpatialIndex(this.bounds, LARGEST_UNIT_SIZE, LARGEST_UNIT_SIZE))
                .useSystem(PathfindingSystem.KEY, context -> new StandardPathfindingSystem(terrain, this.bounds))
                .useSystem(MovementSystem.KEY, context -> new StandardMovementSystem(MOTION_MODEL))
                .useSystem(CollisionSystem.KEY, context -> new StandardCollisionSystem(this.bounds, FREE_POSITION_DISTANCE, terrain))
                .useSystem(VisibilitySystem.KEY, context -> new StandardVisibilitySystem(terrain, MAXIMUM_SIGHT_RANGE))
                .useSystem(EconomySystem.KEY, context -> new StandardEconomySystem())
                .useSystem(RequirementSystem.KEY, context -> new DeclarativeRequirementSystem(requirements))
                .useSystem(CombatSystem.KEY, context -> new StandardCombatSystem.Builder().setSplashShares(INNER_SPLASH_PERCENT,
                                                                                                           MIDDLE_SPLASH_PERCENT,
                                                                                                           OUTER_SPLASH_PERCENT)
                                                                                          .setMinimumDamage(MINIMUM_DAMAGE)
                                                                                          .build())
                .useSystem(ProjectileSystem.KEY, context -> new StandardProjectileSystem(MAXIMUM_FLIGHT_TIME, MOTION_MODEL))
                .useSystem(ProductionSystem.KEY, context -> new StandardProductionSystem.Builder(QUEUE_CAPACITY)
                        .setTerrain(terrain)
                        .setRallyOrder(this.move)
                        .setBuildOrder(this.build)
                        .setConstructionRefundPercent(CONSTRUCTION_REFUND_PERCENT)
                        .setConstructionStartPercent(CONSTRUCTION_START_PERCENT)
                        .build())
                .useSystem(HarvestSystem.KEY, context -> harvest)
                .useSystem(OrderSystem.KEY, context -> new StandardOrderSystem(this::registerBehaviors))
                .useSystem(TriggerSystem.KEY, context -> new StandardTriggerSystem.Builder().addTriggers(triggers).build())
                .useSystem(ComputerPlayerSystem.KEY, context -> new StandardComputerPlayerSystem(StandardComputerController.createFactory(settings)))
                .addHook(CollisionSystem.BLOCKING, context -> harvest)
                .addFramePhase(ComputerPlayerSystem.KEY)
                .addFramePhase(OrderSystem.KEY, orders -> (StandardOrderSystem) orders)
                .addFramePhase(ProductionSystem.KEY, production -> (StandardProductionSystem) production)
                .addFramePhase(MovementSystem.KEY, movement -> (StandardMovementSystem) movement)
                .addFramePhase(CollisionSystem.KEY, collision -> (StandardCollisionSystem) collision)
                .addFramePhase(ProjectileSystem.KEY, projectiles -> (StandardProjectileSystem) projectiles)
                .addFramePhase(VisibilitySystem.KEY, visibility -> (StandardVisibilitySystem) visibility)
                .addFramePhase(TriggerSystem.KEY);

        StandardCombatSystem.addColumns(builder);
        StandardProductionSystem.addColumns(builder, QUEUE_CAPACITY);
        StandardHarvestSystem.addColumns(builder);
        PatrolBehavior.addColumns(builder);
        this.registerContent(builder.getContent());

        return builder;
    }

    /**
     * Prepares a created game: every unit type of the factions is available to every player and every player starts with the start gold.
     *
     * @param game the game
     */
    public void prepare(@NotNull Game game) {
        for (int index = 0; index < game.getPlayerCount(); index++) {
            for (UnitType type : game.getContent().getDefinition().getUnitTypes().getTypes().getTypes()) {
                game.getContent().getProgress().setUnitAvailable(game.getPlayer(index), type, true);
            }

            game.getEconomy().setAmount(game.getPlayer(index), this.gold, START_GOLD);
        }
    }

    private @NotNull DeclarativeRequirements createRequirements() {
        DeclarativeRequirements.Builder builder = new DeclarativeRequirements.Builder();

        builder.requireBuild(this.keeper, Requirement.allOf(new ActorTypeCondition(this.keep)));
        builder.requireBuild(this.thrall, Requirement.allOf(new ActorTypeCondition(this.lodge)));
        builder.requireBuild(this.keep, Requirement.allOf(new ActorTypeCondition(this.keeper)));
        builder.requireBuild(this.lodge, Requirement.allOf(new ActorTypeCondition(this.thrall)));
        builder.requireBuild(this.granary, Requirement.allOf(new ActorTypeCondition(this.keeper)));
        builder.requireBuild(this.pen, Requirement.allOf(new ActorTypeCondition(this.thrall)));
        builder.requireBuild(this.barracks, Requirement.allOf(new ActorTypeCondition(this.keeper), new CompletedUnitCondition(this.keep)));
        builder.requireBuild(this.warcamp, Requirement.allOf(new ActorTypeCondition(this.thrall), new CompletedUnitCondition(this.lodge)));
        builder.requireBuild(this.spearman, Requirement.allOf(new ActorTypeCondition(this.barracks)));
        builder.requireBuild(this.archer, Requirement.allOf(new ActorTypeCondition(this.barracks)));
        builder.requireBuild(this.brute, Requirement.allOf(new ActorTypeCondition(this.warcamp)));
        builder.requireBuild(this.slinger, Requirement.allOf(new ActorTypeCondition(this.warcamp)));

        return builder.build();
    }

    private void registerContent(@NotNull ContentRegistry content) {
        content.getResourceKinds().register(this.gold);

        for (OrderType order : List.of(this.idle, this.guard, this.move, this.stop, this.attack, this.attackMove, this.patrol, this.holdPosition,
                                       this.gather, this.returnResources, this.build)) {
            content.getOrderTypes().register(order, new OrderDefinition.Builder().build());
        }

        content.getMotionProfiles().register(this.walker, new MotionProfileDefinition.Builder().setTopSpeed(320)
                                                                                               .setAcceleration(64)
                                                                                               .setStoppingDistance(1024)
                                                                                               .setTurnRate(40)
                                                                                               .build());
        content.getMotionProfiles().register(this.runner, new MotionProfileDefinition.Builder().setTopSpeed(384)
                                                                                               .setAcceleration(64)
                                                                                               .setStoppingDistance(1280)
                                                                                               .setTurnRate(40)
                                                                                               .build());
        content.getMotionProfiles().register(this.arrow, new MotionProfileDefinition.Builder().setTopSpeed(1536)
                                                                                              .setAcceleration(1536)
                                                                                              .setStoppingDistance(0)
                                                                                              .setTurnRate(127)
                                                                                              .build());
        content.getWeaponTypes().register(this.spear, this.createWeapon(7, 22, 20).build());
        content.getWeaponTypes().register(this.bow, this.createWeapon(5, 28, 96).setProjectile(this.arrow).build());
        content.getWeaponTypes().register(this.club, this.createWeapon(9, 26, 20).build());
        content.getWeaponTypes().register(this.sling, this.createWeapon(4, 30, 88).setProjectile(this.arrow)
                                                          .setHoming(false)
                                                          .setSplashInnerRadius(6)
                                                          .setSplashMiddleRadius(12)
                                                          .setSplashOuterRadius(18)
                                                          .build());
        content.getFactions().register(this.wardens, new FactionDefinition.Builder().setWorker(this.keeper)
                                                                                    .setTownHall(this.keep)
                                                                                    .setSupplyProvider(this.granary)
                                                                                    .addResourceKind(this.gold)
                                                                                    .addStartUnit(this.keeper)
                                                                                    .addStartUnit(this.keeper)
                                                                                    .addStartUnit(this.keeper)
                                                                                    .addStartUnit(this.keeper)
                                                                                    .build());
        content.getFactions().register(this.raiders, new FactionDefinition.Builder().setWorker(this.thrall)
                                                                                    .setTownHall(this.lodge)
                                                                                    .setSupplyProvider(this.pen)
                                                                                    .addResourceKind(this.gold)
                                                                                    .addStartUnit(this.thrall)
                                                                                    .addStartUnit(this.thrall)
                                                                                    .addStartUnit(this.thrall)
                                                                                    .addStartUnit(this.thrall)
                                                                                    .build());
        content.getUnitTypes().register(this.goldMine, this.createBox(TILE_SIZE - 1).addTrait(UnitTrait.RESOURCE_CONTAINER)
                                                           .setPlacementWidth(2 * TILE_SIZE)
                                                           .setPlacementHeight(2 * TILE_SIZE)
                                                           .build());
        this.registerFaction(content, this.wardens, this.keeper, this.keep, this.granary, this.barracks, this.spearman, this.archer, this.walker,
                             this.spear, this.bow);
        this.registerFaction(content, this.raiders, this.thrall, this.lodge, this.pen, this.warcamp, this.brute, this.slinger, this.runner, this.club,
                             this.sling);
        content.getUnitTypes().register(this.scout, this.createBox(5).setMotionProfile(this.runner)
                                                        .setHitPoints(30)
                                                        .setSightRange(9)
                                                        .setIdleOrder(ControllerKind.HUMAN, this.idle)
                                                        .build());
    }

    private void registerFaction(@NotNull ContentRegistry content, @NotNull Faction faction, @NotNull UnitType worker, @NotNull UnitType hall,
                                 @NotNull UnitType supply, @NotNull UnitType producer, @NotNull UnitType melee, @NotNull UnitType ranged,
                                 @NotNull MotionProfile motionProfile, @NotNull WeaponType meleeWeapon, @NotNull WeaponType rangedWeapon) {
        content.getUnitTypes().register(worker, this.createUnit(faction, motionProfile, 6, 40, 50, 60).addTrait(UnitTrait.WORKER)
                                                    .setIdleOrder(ControllerKind.HUMAN, this.idle)
                                                    .setIdleOrder(ControllerKind.COMPUTER, this.idle)
                                                    .build());
        content.getUnitTypes().register(hall, this.createBuilding(faction, 3, 600, 400, 600).addTrait(UnitTrait.RESOURCE_DEPOT)
                                                  .setSupplyProvided(10)
                                                  .build());
        content.getUnitTypes().register(supply, this.createBuilding(faction, 2, 200, 100, 240).setSupplyProvided(8).build());
        content.getUnitTypes().register(producer, this.createBuilding(faction, 2, 400, 150, 360).build());
        content.getUnitTypes().register(melee, this.createFighter(faction, motionProfile, 70, 50, 150).setGroundWeapon(meleeWeapon)
                                                   .setArmor(1)
                                                   .build());
        content.getUnitTypes().register(ranged, this.createFighter(faction, motionProfile, 45, 60, 180).setGroundWeapon(rangedWeapon).build());
    }

    private @NotNull UnitDefinition.Builder createBox(int extent) {
        return new UnitDefinition.Builder().setCollisionLeft(extent).setCollisionUp(extent).setCollisionRight(extent).setCollisionDown(extent);
    }

    private @NotNull UnitDefinition.Builder createUnit(@NotNull Faction faction, @NotNull MotionProfile motionProfile, int extent, int hitPoints,
                                                       int cost, int buildTime) {
        return this.createBox(extent).setFaction(faction)
                   .setMotionProfile(motionProfile)
                   .setHitPoints(hitPoints)
                   .setSightRange(7)
                   .setCost(ResourceCost.of(this.gold, cost))
                   .setBuildTime(buildTime)
                   .setSupplyRequired(1);
    }

    private @NotNull UnitDefinition.Builder createFighter(@NotNull Faction faction, @NotNull MotionProfile motionProfile, int hitPoints,
                                                          int cost, int buildTime) {
        return this.createUnit(faction, motionProfile, 7, hitPoints, cost, buildTime).setSupplyRequired(2)
                   .setTargetAcquisitionRange(160)
                   .setIdleOrder(ControllerKind.HUMAN, this.guard)
                   .setIdleOrder(ControllerKind.COMPUTER, this.guard);
    }

    private @NotNull UnitDefinition.Builder createBuilding(@NotNull Faction faction, int tiles, int hitPoints, int cost, int buildTime) {
        return this.createBox(tiles * TILE_SIZE / 2 - 1).setFaction(faction)
                   .addTrait(UnitTrait.BUILDING)
                   .setHitPoints(hitPoints)
                   .setArmor(1)
                   .setSightRange(8)
                   .setCost(ResourceCost.of(this.gold, cost))
                   .setBuildTime(buildTime)
                   .setPlacementWidth(tiles * TILE_SIZE)
                   .setPlacementHeight(tiles * TILE_SIZE);
    }

    private @NotNull WeaponDefinition.Builder createWeapon(int damage, int cooldown, int range) {
        return new WeaponDefinition.Builder().setDamage(damage)
                                             .setDamageFactor(1)
                                             .setCooldown(cooldown)
                                             .setMaximumRange(range)
                                             .addTarget(WeaponTarget.GROUND);
    }

    private void registerBehaviors(@NotNull Game game, @NotNull OrderRegistry<OrderBehavior<OrderContext, Unit>> behaviors) {
        StandardCombatSystem combat = (StandardCombatSystem) game.getCombat();
        StandardHarvestSystem harvest = (StandardHarvestSystem) game.getModSystem(HarvestSystem.KEY);

        behaviors.register(this.guard, new GuardBehavior(combat, this.attack));
        behaviors.register(this.attack, new AttackBehavior(combat));
        behaviors.register(this.move, new MoveBehavior(ARRIVAL_DISTANCE));
        behaviors.register(this.stop, new StopBehavior());
        behaviors.register(this.attackMove, new AttackMoveBehavior(combat, ARRIVAL_DISTANCE));
        behaviors.register(this.patrol, new PatrolBehavior(game, combat, ARRIVAL_DISTANCE));
        behaviors.register(this.holdPosition, new HoldPositionBehavior(combat));
        behaviors.register(this.gather, new GatherBehavior(harvest, this.returnResources, RESOURCE_SEARCH_RANGE));
        behaviors.register(this.returnResources, new ReturnResourcesBehavior(harvest, this.gather));
        behaviors.register(this.build, new BuildBehavior((StandardProductionSystem) game.getProduction(), REACH, true));
    }
}
