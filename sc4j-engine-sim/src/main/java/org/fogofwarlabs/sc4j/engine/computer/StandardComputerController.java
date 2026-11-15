package org.fogofwarlabs.sc4j.engine.computer;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.combat.CombatSystem;
import org.fogofwarlabs.sc4j.engine.content.definition.UnitTrait;
import org.fogofwarlabs.sc4j.engine.content.economy.ResourceKind;
import org.fogofwarlabs.sc4j.engine.content.faction.Faction;
import org.fogofwarlabs.sc4j.engine.content.faction.FactionDefinition;
import org.fogofwarlabs.sc4j.engine.content.requirement.RequirementAction;
import org.fogofwarlabs.sc4j.engine.content.requirement.RequirementSystem;
import org.fogofwarlabs.sc4j.engine.content.table.UnitTypeTable;
import org.fogofwarlabs.sc4j.engine.content.type.OrderType;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.economy.EconomySystem;
import org.fogofwarlabs.sc4j.engine.economy.HarvestSystem;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.entity.pool.EntityPool;
import org.fogofwarlabs.sc4j.engine.entity.pool.SlotList;
import org.fogofwarlabs.sc4j.engine.game.ControllerKind;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.game.PlayerSlot;
import org.fogofwarlabs.sc4j.engine.math.MutablePoint;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.fogofwarlabs.sc4j.engine.production.StandardProductionSystem;
import org.fogofwarlabs.sc4j.engine.state.StateHash;
import org.fogofwarlabs.sc4j.engine.state.StateHashContributor;
import org.fogofwarlabs.sc4j.engine.state.snapshot.SnapshotContributor;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotReader;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotWriter;
import org.fogofwarlabs.sc4j.engine.terrain.TerrainGrid;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Plays one computer player by simple, deterministic rules over the engine defaults: the engine default {@link ComputerController}. A mod
 * registers the engine computer player system with its factory ({@link #createFactory}) and the settings of its orders and numbers.
 * {@snippet :
 * ComputerSettings settings = new ComputerSettings.Builder(gather, attackMove).build();
 * builder.useSystem(ComputerPlayerSystem.KEY, context -> new StandardComputerPlayerSystem(StandardComputerController.createFactory(settings)))
 *        .addFramePhase(ComputerPlayerSystem.KEY);
 *}
 * <p>
 * The controller knows nothing of the content beyond the engine model: the faction of its player slot names the worker, the town hall and the supply
 * provider ({@link FactionDefinition}); every other building type of the faction is a production building, and every other unit type of the faction
 * with a weapon a fighting unit. What a building trains and what a worker builds come from the requirement graph of the content
 * ({@link RequirementSystem#check} with {@link RequirementAction#BUILD_UNIT} and the producing unit as actor; without a requirement system,
 * everything is allowed). It needs the engine production default ({@link StandardProductionSystem}) for construction and the harvest system
 * ({@link HarvestSystem}) for resources; it sees the whole map.
 * <p>
 * Every decision interval it decides in this order, from its base, the first completed town hall of its player in slot order (without one it
 * only sends attack waves): every idle worker (its order is its type's idle order for {@link ControllerKind#COMPUTER}) gathers from the resource
 * container nearest to the base within the search range; a supply provider is built when the free supply of the faction is below the margin and
 * none is being built; workers are trained at the base up to the target; production building types are built in content order, one of each,
 * until the target number of production buildings is reached; every completed production building with an empty queue trains the allowed
 * fighting unit type the player has fewest of (content order among equal counts), when the player can pay it and has the supply; and when the
 * idle fighting units reach the wave size, they all get the attack-move order to the enemy building nearest to the base (or the nearest enemy
 * unit without enemy buildings), and the next wave is larger by the growth, up to the largest wave size. Without a base of its own, or when the
 * enemies have no building left, every idle fighting unit goes at once. A building is ordered from the worker of the lowest slot that is
 * not building, at the first free site of rings of growing size around the base ({@link #findBuildSite}).
 * <p>
 * Determinism contract: the controller acts only in the frames whose number is a multiple of the decision interval, visits units in slot order,
 * draws no random value and allocates nothing after it is created.
 */
public final class StandardComputerController implements ComputerController, StateHashContributor, SnapshotContributor {
    /**
     * The instruction limit of the animation programs the orders of the controller start.
     */
    private static final int INSTRUCTION_LIMIT = 0;
    /**
     * The distance in rings from the base at which the building search starts, so that buildings keep the base's surroundings free.
     */
    private static final int FIRST_BUILD_RING = 2;
    /**
     * The divisor that gives the half of a size.
     */
    private static final int HALF = 2;
    /**
     * The queue length below which a production building trains another unit.
     */
    private static final int QUEUE_TARGET = 1;
    /**
     * The number of positions on a ring of the building search per ring distance: the ring of distance {@code r} has {@code 8 * r} positions.
     */
    private static final int RING_POSITIONS_PER_DISTANCE = 8;

    /**
     * Creates the factory of the controllers of a computer player system.
     *
     * @param settings the settings of every created controller
     * @return the created {@link ComputerControllerFactory} instance
     */
    public static @NotNull ComputerControllerFactory createFactory(@NotNull ComputerSettings settings) {
        return (game, player) -> new StandardComputerController(game, player, settings);
    }

    private static long squareDistance(long horizontal, long vertical) {
        return horizontal * horizontal + vertical * vertical;
    }


    private final @NotNull Game game;
    private final @NotNull Player player;
    private final @NotNull ComputerSettings settings;

    private final @Nullable Faction faction;
    private final @Nullable UnitType worker;
    private final @Nullable UnitType townHall;
    private final @Nullable UnitType supplyProvider;
    private final @NotNull UnitType @NotNull [] productionTypes;
    private final @NotNull UnitType @NotNull [] fightingTypes;

    private final int @NotNull [] counts;
    private final @NotNull MutablePoint site;

    private int waveSize;

    /**
     * Creates a {@link StandardComputerController} instance for a player of a game; it reads the faction of the player's setup slot and sorts the
     * unit types of that faction.
     *
     * @param game     the game
     * @param player   the player
     * @param settings the settings
     */
    public StandardComputerController(@NotNull Game game, @NotNull Player player, @NotNull ComputerSettings settings) {
        Faction slotFaction = null;

        for (PlayerSlot slot : game.getPlayers()) {
            if (slot.getSlotIndex() == player.getIndex()) {
                slotFaction = slot.getFaction();
            }
        }

        FactionDefinition definition = slotFaction != null ? game.getContent().getDefinition().getFactions().getDefinition(slotFaction) : null;
        List<UnitType> types = game.getContent().getDefinition().getUnitTypes().getTypes().getTypes();
        UnitTypeTable table = game.getContent().getUnitTypes();

        this.game     = game;
        this.player   = player;
        this.settings = settings;

        this.faction         = slotFaction;
        this.worker          = definition != null ? definition.getWorker() : null;
        this.townHall        = definition != null ? definition.getTownHall() : null;
        this.supplyProvider  = definition != null ? definition.getSupplyProvider() : null;
        this.productionTypes = types.stream()
                                    .filter(type -> table.getFaction(type) == this.faction && table.hasTrait(type, UnitTrait.BUILDING) &&
                                                    type != this.townHall && type != this.supplyProvider)
                                    .toArray(UnitType[]::new);
        this.fightingTypes   = types.stream()
                                    .filter(type -> table.getFaction(type) == this.faction && !table.hasTrait(type, UnitTrait.BUILDING) &&
                                                    type != this.worker && (table.getGroundWeapon(type) != null || table.getAirWeapon(type) != null))
                                    .toArray(UnitType[]::new);

        this.counts = new int[types.size()];
        this.site   = new MutablePoint();

        this.waveSize = settings.getFirstWaveSize();
    }

    /**
     * @return the player the controller plays
     */
    @Override
    public @NotNull Player getPlayer() {
        return this.player;
    }

    /**
     * @return the size of the next attack wave
     */
    public int getWaveSize() {
        return this.waveSize;
    }

    /**
     * Mixes this part of the simulation state into the hash: the size of the next attack wave. The other fields are the settings and
     * per-decision scratch.
     *
     * @param hash the hash being computed
     */
    @Override
    public void contributeStateHash(@NotNull StateHash hash) {
        hash.mix(this.waveSize);
    }

    /**
     * Writes this part of the simulation state: the size of the next attack wave. The other fields are the settings and per-decision scratch.
     *
     * @param writer the writer of the snapshot
     */
    @Override
    public void writeSnapshot(@NotNull StateSnapshotWriter writer) {
        writer.writeInt(this.waveSize);
    }

    /**
     * Reads this part of the simulation state back, replacing the current values.
     *
     * @param reader the reader of the snapshot
     * @throws IOException if the snapshot ends early
     */
    @Override
    public void readSnapshot(@NotNull StateSnapshotReader reader) throws IOException {
        this.waveSize = reader.readInt();
    }

    /**
     * Runs the decisions of this computer player at the point of the frame where its system's schedule reaches it: the player issues its
     * orders ({@link org.fogofwarlabs.sc4j.engine.order.OrderSystem}) and uses the other systems of the game.
     * <p>
     * Decides as the class description says, in the frames of the decision interval.
     *
     * @param game the game
     * @throws IllegalStateException if the game has no standard production system or no harvest system
     */
    @Override
    public void update(@NotNull Game game) {
        if (game.getClock().getFrame() % this.settings.getDecisionInterval() != 0) {
            return;
        }

        StandardProductionSystem production = this.requireProduction();
        Unit base = this.countUnits(production);

        if (base != null) {
            this.gatherWithIdleWorkers(base);
            this.buildSupply(production, base);
            this.trainWorker(production, base);
            this.buildProduction(production, base);
        }

        this.trainFighters(production);
        this.launchWave(base);
    }

    /**
     * Finds a free site for a building around a base: the positions on square rings around the base, from ring {@value #FIRST_BUILD_RING} up to
     * the build radius in tiles, one tile apart; each ring top row left to right, bottom row, then the left and right columns top to bottom.
     * A site is free when the building can be placed there ({@link StandardProductionSystem#canPlace}), keeps the building spacing from every
     * other building and the resource clearance from every resource container, and lies farther from the container nearest to the base than the
     * base itself, so that buildings stay out of the workers' way.
     *
     * @param type the building type
     * @param base the base
     * @param site receives the site
     * @return {@code true} if a site was found and written, {@code false} otherwise
     * @throws IllegalStateException if the game has no standard production system or no harvest system
     */
    public boolean findBuildSite(@NotNull UnitType type, @NotNull Unit base, @NotNull MutablePoint site) {
        StandardProductionSystem production = this.requireProduction();
        TerrainGrid terrain = production.getTerrain();
        int step = terrain != null ? terrain.getTileSize() : production.getFootprintWidth(type);
        Unit container = this.game.getModSystem(HarvestSystem.KEY).findResourceContainer(null, base.getPositionX(), base.getPositionY(),
                                                                                         this.settings.getResourceSearchRange());

        for (int ring = FIRST_BUILD_RING; ring <= this.settings.getBuildRadius(); ring++) {
            for (int position = 0; position < ring * RING_POSITIONS_PER_DISTANCE; position++) {
                int offsetX;
                int offsetY;
                int side = ring * 2 + 1;

                if (position < side) {
                    offsetX = position - ring;
                    offsetY = -ring;
                } else if (position < side * 2) {
                    offsetX = position - side - ring;
                    offsetY = ring;
                } else {
                    int row = (position - side * 2) / 2 + 1 - ring;

                    offsetX = (position - side * 2) % 2 == 0 ? -ring : ring;
                    offsetY = row;
                }

                int x = base.getPositionX() + offsetX * step;
                int y = base.getPositionY() + offsetY * step;

                if (this.isGoodSite(production, type, base, container, x, y)) {
                    site.set(x, y);

                    return true;
                }
            }
        }

        return false;
    }

    private boolean isGoodSite(@NotNull StandardProductionSystem production, @NotNull UnitType type, @NotNull Unit base, @Nullable Unit container,
                               int x, int y) {
        if (!production.canPlace(type, x, y)) {
            return false;
        }

        if (container != null && squareDistance(x - container.getPositionX(), y - container.getPositionY()) <=
                                 squareDistance(base.getPositionX() - container.getPositionX(), base.getPositionY() - container.getPositionY())) {
            return false;
        }

        EntityPool<Unit> units = this.game.getUnits();
        UnitTypeTable table = this.game.getContent().getUnitTypes();
        SlotList used = units.getUsedSlots();
        long clearance = this.settings.getResourceClearance();

        for (int index = used.getFirst(); index != SlotList.NONE; index = used.getNext(index)) {
            Unit unit = units.get(index);
            UnitType unitType = unit.getType();

            if (unitType == null) {
                continue;
            }

            if (table.hasTrait(unitType, UnitTrait.RESOURCE_CONTAINER) &&
                squareDistance(x - unit.getPositionX(), y - unit.getPositionY()) <= clearance * clearance) {
                return false;
            }

            if (table.hasTrait(unitType, UnitTrait.BUILDING) &&
                Math.abs(x - unit.getPositionX()) < (production.getFootprintWidth(type) + production.getFootprintWidth(unitType)) / HALF +
                                                    this.settings.getBuildingSpacing() &&
                Math.abs(y - unit.getPositionY()) < (production.getFootprintHeight(type) + production.getFootprintHeight(unitType)) / HALF +
                                                    this.settings.getBuildingSpacing()) {
                return false;
            }
        }

        return true;
    }

    /**
     * Counts the player's units by type, counting buildings under construction and buildings a worker is ordered to build as well, and finds
     * the base.
     *
     * @return the base, or {@code null} if the player has no completed town hall
     */
    private @Nullable Unit countUnits(@NotNull StandardProductionSystem production) {
        EntityPool<Unit> units = this.game.getUnits();
        SlotList used = units.getUsedSlots();
        Unit base = null;

        Arrays.fill(this.counts, 0);

        for (int index = used.getFirst(); index != SlotList.NONE; index = used.getNext(index)) {
            Unit unit = units.get(index);
            UnitType type = unit.getType();

            if (unit.getOwner() != this.player || type == null) {
                continue;
            }

            this.counts[type.getIndex()]++;

            UnitType planned = production.getConstructionType(unit);

            if (planned != null && production.getConstructedBuilding(unit) == null) {
                this.counts[planned.getIndex()]++;
            }

            if (base == null && type == this.townHall && !production.isUnderConstruction(unit)) {
                base = unit;
            }
        }

        return base;
    }

    private void gatherWithIdleWorkers(@NotNull Unit base) {
        EntityPool<Unit> units = this.game.getUnits();
        SlotList used = units.getUsedSlots();
        HarvestSystem harvest = this.game.getModSystem(HarvestSystem.KEY);

        for (int index = used.getFirst(); index != SlotList.NONE; index = used.getNext(index)) {
            Unit unit = units.get(index);

            if (unit.getOwner() != this.player || unit.getType() != this.worker || !this.isIdle(unit)) {
                continue;
            }

            Unit container = harvest.findResourceContainer(null, base.getPositionX(), base.getPositionY(), this.settings.getResourceSearchRange());

            if (container == null) {
                return;
            }

            this.game.getOrders().issue(unit, this.settings.getGatherOrder(), container.getPositionX(), container.getPositionY(), container,
                                        INSTRUCTION_LIMIT);
        }
    }

    private void buildSupply(@NotNull StandardProductionSystem production, @NotNull Unit base) {
        UnitType provider = this.supplyProvider;
        Faction playerFaction = this.faction;

        if (provider == null || playerFaction == null || !this.game.hasSystem(EconomySystem.KEY) || this.isPlanned(production, provider)) {
            return;
        }

        EconomySystem economy = this.game.getEconomy();

        if (economy.getSupplyProvided(this.player, playerFaction) - economy.getSupplyUsed(this.player, playerFaction) <
            this.settings.getSupplyMargin()) {
            this.build(production, provider, base);
        }
    }

    private void trainWorker(@NotNull StandardProductionSystem production, @NotNull Unit base) {
        UnitType workerType = this.worker;

        if (workerType != null && this.counts[workerType.getIndex()] < this.settings.getWorkerTarget() &&
            production.getQueueLength(base) < QUEUE_TARGET && this.canTrain(workerType, base)) {
            production.enqueueUnit(base, workerType);
        }
    }

    private void buildProduction(@NotNull StandardProductionSystem production, @NotNull Unit base) {
        int built = 0;

        for (UnitType type : this.productionTypes) {
            built += this.counts[type.getIndex()];
        }

        for (UnitType type : this.productionTypes) {
            if (built >= this.settings.getProductionBuildingTarget()) {
                return;
            }

            if (this.counts[type.getIndex()] == 0 && this.build(production, type, base)) {
                built++;
            }
        }
    }

    private void trainFighters(@NotNull StandardProductionSystem production) {
        EntityPool<Unit> units = this.game.getUnits();
        SlotList used = units.getUsedSlots();
        UnitTypeTable table = this.game.getContent().getUnitTypes();

        for (int index = used.getFirst(); index != SlotList.NONE; index = used.getNext(index)) {
            Unit producer = units.get(index);
            UnitType type = producer.getType();

            if (producer.getOwner() != this.player || type == null || !table.hasTrait(type, UnitTrait.BUILDING) ||
                production.isUnderConstruction(producer) || production.getQueueLength(producer) >= QUEUE_TARGET) {
                continue;
            }

            UnitType chosen = null;

            for (UnitType fighter : this.fightingTypes) {
                if (this.canTrain(fighter, producer) &&
                    (chosen == null || this.counts[fighter.getIndex()] < this.counts[chosen.getIndex()])) {
                    chosen = fighter;
                }
            }

            if (chosen != null && production.enqueueUnit(producer, chosen) >= 0) {
                this.counts[chosen.getIndex()]++;
            }
        }
    }

    private void launchWave(@Nullable Unit base) {
        EntityPool<Unit> units = this.game.getUnits();
        SlotList used = units.getUsedSlots();
        int idle = 0;
        Unit reference = base;

        for (int index = used.getFirst(); index != SlotList.NONE; index = used.getNext(index)) {
            Unit unit = units.get(index);

            if (unit.getOwner() == this.player && this.isFighter(unit) && this.isIdle(unit)) {
                idle++;
                reference = reference != null ? reference : unit;
            }
        }

        if (idle == 0) {
            return;
        }

        Unit target = this.findEnemy(reference);
        UnitType targetType = target != null ? target.getType() : null;

        if (target == null || targetType == null) {
            return;
        }

        boolean finalBlow = base == null || !this.game.getContent().getUnitTypes().hasTrait(targetType, UnitTrait.BUILDING);

        if (idle < this.waveSize && !finalBlow) {
            return;
        }

        for (int index = used.getFirst(); index != SlotList.NONE; index = used.getNext(index)) {
            Unit unit = units.get(index);

            if (unit.getOwner() == this.player && this.isFighter(unit) && this.isIdle(unit)) {
                this.game.getOrders().issue(unit, this.settings.getAttackMoveOrder(), target.getPositionX(), target.getPositionY(), null,
                                            INSTRUCTION_LIMIT);
            }
        }

        this.waveSize = Math.min(this.waveSize + this.settings.getWaveGrowth(), this.settings.getMaximumWaveSize());
    }

    /**
     * Finds the enemy building nearest to a unit of the player, or the nearest enemy unit when the enemies have no building.
     */
    private @Nullable Unit findEnemy(@NotNull Unit reference) {
        EntityPool<Unit> units = this.game.getUnits();
        SlotList used = units.getUsedSlots();
        UnitTypeTable table = this.game.getContent().getUnitTypes();
        CombatSystem combat = this.game.getCombat();
        Unit nearestBuilding = null;
        Unit nearestUnit = null;
        long buildingDistance = Long.MAX_VALUE;
        long unitDistance = Long.MAX_VALUE;

        for (int index = used.getFirst(); index != SlotList.NONE; index = used.getNext(index)) {
            Unit unit = units.get(index);
            UnitType type = unit.getType();

            if (type == null || !combat.isEnemy(reference, unit)) {
                continue;
            }

            long distance = squareDistance(unit.getPositionX() - reference.getPositionX(), unit.getPositionY() - reference.getPositionY());

            if (table.hasTrait(type, UnitTrait.BUILDING) && distance < buildingDistance) {
                nearestBuilding  = unit;
                buildingDistance = distance;
            }

            if (distance < unitDistance) {
                nearestUnit  = unit;
                unitDistance = distance;
            }
        }

        return nearestBuilding != null ? nearestBuilding : nearestUnit;
    }

    /**
     * Orders a building from the player's first worker in slot order that is not building, at the first free site around the base.
     *
     * @return {@code true} if a worker got the order, {@code false} otherwise
     */
    private boolean build(@NotNull StandardProductionSystem production, @NotNull UnitType type, @NotNull Unit base) {
        if (!this.canPay(type)) {
            return false;
        }

        EntityPool<Unit> units = this.game.getUnits();
        SlotList used = units.getUsedSlots();
        MutablePoint buildSite = this.site;

        for (int index = used.getFirst(); index != SlotList.NONE; index = used.getNext(index)) {
            Unit unit = units.get(index);

            if (unit.getOwner() != this.player || unit.getType() != this.worker || production.getConstructionType(unit) != null ||
                !this.isAllowed(type, unit)) {
                continue;
            }

            if (!this.findBuildSite(type, base, buildSite)) {
                return false;
            }

            production.orderConstruction(unit, type, buildSite.getX(), buildSite.getY(), INSTRUCTION_LIMIT);
            this.counts[type.getIndex()]++;

            return true;
        }

        return false;
    }

    private boolean isPlanned(@NotNull StandardProductionSystem production, @NotNull UnitType type) {
        EntityPool<Unit> units = this.game.getUnits();
        SlotList used = units.getUsedSlots();

        for (int index = used.getFirst(); index != SlotList.NONE; index = used.getNext(index)) {
            Unit unit = units.get(index);

            if (unit.getOwner() != this.player) {
                continue;
            }

            if (unit.getType() == type && production.isUnderConstruction(unit) || production.getConstructionType(unit) == type) {
                return true;
            }
        }

        return false;
    }

    private boolean canTrain(@NotNull UnitType type, @NotNull Unit producer) {
        Faction typeFaction = this.game.getContent().getUnitTypes().getFaction(type);
        int required = this.game.getContent().getUnitTypes().getSupplyRequired(type);

        if (!this.isAllowed(type, producer) || !this.canPay(type)) {
            return false;
        }

        if (typeFaction == null || required <= 0 || !this.game.hasSystem(EconomySystem.KEY)) {
            return true;
        }

        EconomySystem economy = this.game.getEconomy();

        return economy.getSupplyUsed(this.player, typeFaction) + required <= economy.getSupplyProvided(this.player, typeFaction);
    }

    private boolean isAllowed(@NotNull UnitType type, @NotNull Unit actor) {
        return !this.game.hasSystem(RequirementSystem.KEY) ||
               this.game.getRequirements().check(RequirementAction.BUILD_UNIT, type, this.player, actor).isMet();
    }

    private boolean canPay(@NotNull UnitType type) {
        if (!this.game.hasSystem(EconomySystem.KEY)) {
            return true;
        }

        EconomySystem economy = this.game.getEconomy();
        UnitTypeTable table = this.game.getContent().getUnitTypes();
        List<ResourceKind> kinds = this.game.getContent().getDefinition().getResourceKinds().getTypes();

        for (int index = 0; index < kinds.size(); index++) {
            if (economy.getAmount(this.player, kinds.get(index)) < table.getCost(type, kinds.get(index))) {
                return false;
            }
        }

        return true;
    }

    private boolean isFighter(@NotNull Unit unit) {
        UnitType type = unit.getType();

        if (type == null) {
            return false;
        }

        for (UnitType fighter : this.fightingTypes) {
            if (fighter == type) {
                return true;
            }
        }

        return false;
    }

    private boolean isIdle(@NotNull Unit unit) {
        UnitType type = unit.getType();
        OrderType order = unit.getOrderType();

        return type != null && (order == null || order == this.game.getContent().getUnitTypes().getIdleOrder(type, ControllerKind.COMPUTER));
    }

    private @NotNull StandardProductionSystem requireProduction() {
        if (!(this.game.getProduction() instanceof StandardProductionSystem production) || !this.game.hasSystem(HarvestSystem.KEY)) {
            throw Exceptions.newIllegalStateException("The standard computer player needs the standard production system and a harvest system.");
        }

        return production;
    }
}
