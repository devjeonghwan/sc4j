package org.fogofwarlabs.sc4j.engine.production;

import org.fogofwarlabs.sc4j.engine.content.type.TechType;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.content.type.UpgradeType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.system.GameSystem;
import org.fogofwarlabs.sc4j.engine.game.system.SystemKey;
import org.fogofwarlabs.sc4j.engine.hook.HookComposition;
import org.fogofwarlabs.sc4j.engine.hook.HookPoint;
import org.jetbrains.annotations.NotNull;

/**
 * Runs the production of producing units: the queue of units to train, build or morph, and the one research or upgrade a producer carries
 * out at a time. Commands, triggers, abilities and computer players add, start and cancel; the producer's orders advance them. The caller
 * asks the requirement system whether an entry is allowed; this system tests and spends the cost through the economy system.
 * <p>
 * Queue slots count from {@code 0}, the entry in progress first.
 * <p>
 * Determinism contract: an enqueue or start tests the cost and spends it in that order and changes nothing when the test fails;
 * {@link #cancel} and {@link #cancelConstruction} refund in the implementation's documented share; the operations draw no random value and
 * allocate nothing, except what the animation programs a start or cancel runs draw, bounded by its instruction limit.
 */
public interface ProductionSystem extends GameSystem {
    /**
     * The key of the production system.
     */
    @NotNull SystemKey<ProductionSystem> KEY = SystemKey.of("production", ProductionSystem.class);
    /**
     * The point a queue entry moves from one slot to another at, when an entry before it is cancelled ({@link HookComposition#EVERY_IN_ORDER}).
     */
    @NotNull HookPoint<QueueSlotMovedHook> QUEUE_SLOT_MOVED = HookPoint.of("production.queue-slot-moved", QueueSlotMovedHook.class,
                                                                           HookComposition.EVERY_IN_ORDER);
    /**
     * The point a queue slot becomes empty at, when its entry is made or cannot be placed ({@link HookComposition#EVERY_IN_ORDER}).
     */
    @NotNull HookPoint<QueueSlotClearedHook> QUEUE_SLOT_CLEARED = HookPoint.of("production.queue-slot-cleared", QueueSlotClearedHook.class,
                                                                               HookComposition.EVERY_IN_ORDER);

    /**
     * Adds a unit to train, build or morph at the end of a producer's queue after testing its cost; the cost is spent now or when the entry
     * starts, as the implementation documents.
     *
     * @param producer the producing unit
     * @param type     the unit type to make
     * @return the queue slot of the new entry, or {@code -1} if the queue is full or the cost cannot be paid
     */
    int enqueueUnit(@NotNull Unit producer, @NotNull UnitType type);

    /**
     * Starts researching a tech at a producer after testing and spending its cost.
     *
     * @param producer         the producing unit
     * @param type             the tech
     * @param instructionLimit the instruction limit of each animation program run the start starts
     * @return {@code true} if the research started, {@code false} otherwise
     */
    boolean startResearch(@NotNull Unit producer, @NotNull TechType type, int instructionLimit);

    /**
     * Starts the next level of an upgrade at a producer after testing and spending its cost.
     *
     * @param producer         the producing unit
     * @param type             the upgrade
     * @param instructionLimit the instruction limit of each animation program run the start starts
     * @return {@code true} if the upgrade started, {@code false} otherwise
     */
    boolean startUpgrade(@NotNull Unit producer, @NotNull UpgradeType type, int instructionLimit);

    /**
     * Cancels one entry of a producer's queue; the later entries move up.
     *
     * @param producer         the producing unit
     * @param slot             the queue slot
     * @param instructionLimit the instruction limit of each animation program run the cancellation starts
     * @return {@code true} if an entry was cancelled, {@code false} if the slot is empty
     */
    boolean cancel(@NotNull Unit producer, int slot, int instructionLimit);

    /**
     * Clears the production of a producer that leaves the game, before its counts and supply leave its owner: the queue and the research end,
     * the supply the queue entries hold is given back, and their spent cost is lost. The default has nothing to clear.
     *
     * @param producer the producing unit
     */
    default void clearProduction(@NotNull Unit producer) {
        // A production system without state per producer has nothing to clear.
    }

    /**
     * Returns the length of a producer's queue.
     *
     * @param producer the producing unit
     * @return the number of entries in the producer's queue
     */
    int getQueueLength(@NotNull Unit producer);

    /**
     * Returns the unit type of one queue entry.
     *
     * @param producer the producing unit
     * @param slot     the queue slot, below the queue length
     * @return the unit type
     */
    @NotNull UnitType getQueuedType(@NotNull Unit producer, int slot);

    /**
     * Checks whether a unit is a building under construction: it exists but is not complete, so it neither produces nor counts as completed.
     * The default has no construction.
     *
     * @param unit the unit
     * @return {@code true} if the unit is under construction, {@code false} otherwise
     */
    default boolean isUnderConstruction(@NotNull Unit unit) {
        return false;
    }

    /**
     * Cancels the construction of a building under construction: the owner gets a share of its cost back and the building leaves the game. The
     * default has no construction.
     *
     * @param building         the building
     * @param instructionLimit the instruction limit of each animation program run the cancellation starts
     * @return {@code true} if the construction was cancelled, {@code false} if the unit is not under construction
     */
    default boolean cancelConstruction(@NotNull Unit building, int instructionLimit) {
        return false;
    }

    /**
     * Orders a worker to construct a building at a site: the worker goes there and places the building. The default has no construction.
     *
     * @param worker           the worker
     * @param type             the building type
     * @param x                the x coordinate of the site's center in pixels
     * @param y                the y coordinate of the site's center in pixels
     * @param instructionLimit the instruction limit of each animation program run the order starts
     * @return {@code true} if the worker got the order, {@code false} otherwise
     */
    default boolean orderConstruction(@NotNull Unit worker, @NotNull UnitType type, int x, int y, int instructionLimit) {
        return false;
    }
}
