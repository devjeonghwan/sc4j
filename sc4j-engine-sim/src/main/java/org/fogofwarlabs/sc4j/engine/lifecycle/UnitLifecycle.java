package org.fogofwarlabs.sc4j.engine.lifecycle;

import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.hook.HookComposition;
import org.fogofwarlabs.sc4j.engine.hook.HookList;
import org.fogofwarlabs.sc4j.engine.hook.HookPoint;
import org.fogofwarlabs.sc4j.engine.hook.HookTable;
import org.jetbrains.annotations.NotNull;

/**
 * Announces the places of a unit's life to the hooks registered at them: a unit is completed, leaves its owner, has a new owner, has finished
 * its owner change, dies (leaving the systems it provides for, possibly remaining as something else, and releasing the terrain around it), or a
 * building is destroyed. The unit lifecycle is enforced by the engine and not replaceable; the game creates one and resolves the hooks of its
 * points once, so a notification is an indexed loop without lookup or allocation.
 * <p>
 * The system that removes, transfers or kills a unit calls the notification at the point its rules define; with no hook registered a
 * notification does nothing.
 * <p>
 * Determinism contract: the hooks of a point run in registration order (every point is {@link HookComposition#EVERY_IN_ORDER} but
 * {@link #UNIT_REMAINS}, {@link HookComposition#FIRST_ANSWER}, and {@link #TERRAIN_RELEASE}, {@link HookComposition#CHAINED}), inside the
 * simulation frame at the caller's point; they follow the hook contract of the {@code hook} package.
 */
public final class UnitLifecycle {
    /**
     * The point a unit is completed at: its completion state and visibility are set, its first order is not yet given.
     */
    public static final @NotNull HookPoint<UnitCompletedHook> UNIT_COMPLETED = HookPoint.of("unit.completed", UnitCompletedHook.class,
                                                                                            HookComposition.EVERY_IN_ORDER);
    /**
     * The point a unit leaves its owner at, by death or by an owner change.
     */
    public static final @NotNull HookPoint<UnitRemovedHook> UNIT_REMOVED = HookPoint.of("unit.removed", UnitRemovedHook.class,
                                                                                        HookComposition.EVERY_IN_ORDER);
    /**
     * The point a unit has its new owner at.
     */
    public static final @NotNull HookPoint<UnitAdoptedHook> UNIT_ADOPTED = HookPoint.of("unit.adopted", UnitAdoptedHook.class,
                                                                                        HookComposition.EVERY_IN_ORDER);
    /**
     * The point an owner change of a unit is finished at: the unit, its subunit and its speed belong to the new owner.
     */
    public static final @NotNull HookPoint<UnitOwnerChangedHook> UNIT_OWNER_CHANGED = HookPoint.of("unit.owner-changed",
                                                                                                   UnitOwnerChangedHook.class,
                                                                                                   HookComposition.EVERY_IN_ORDER);
    /**
     * The point of a unit's death where it leaves the systems it provides for.
     */
    public static final @NotNull HookPoint<UnitDyingHook> UNIT_DYING = HookPoint.of("unit.dying", UnitDyingHook.class,
                                                                                    HookComposition.EVERY_IN_ORDER);
    /**
     * The point of a unit's death, after it left its owner, where a unit in the world may remain as something else: the hooks are asked until
     * one keeps the unit, which ends its death ({@link HookComposition#FIRST_ANSWER}; without an answer the death goes on).
     */
    public static final @NotNull HookPoint<UnitRemainsHook> UNIT_REMAINS = HookPoint.of("unit.remains", UnitRemainsHook.class,
                                                                                        HookComposition.FIRST_ANSWER);
    /**
     * The point of a unit's death where it stops changing the terrain around it: each hook receives whether the releases before it finished
     * ({@link HookComposition#CHAINED}; the first receives {@code true}); an unfinished release is retried before the unit is released.
     */
    public static final @NotNull HookPoint<TerrainReleaseHook> TERRAIN_RELEASE = HookPoint.of("unit.terrain-release", TerrainReleaseHook.class,
                                                                                              HookComposition.CHAINED);
    /**
     * The point a building is destroyed at.
     */
    public static final @NotNull HookPoint<BuildingDestroyedHook> BUILDING_DESTROYED = HookPoint.of("unit.building-destroyed",
                                                                                                    BuildingDestroyedHook.class,
                                                                                                    HookComposition.EVERY_IN_ORDER);

    private final @NotNull HookList<UnitCompletedHook> completedHooks;
    private final @NotNull HookList<UnitRemovedHook> removedHooks;
    private final @NotNull HookList<UnitAdoptedHook> adoptedHooks;
    private final @NotNull HookList<UnitOwnerChangedHook> ownerChangedHooks;
    private final @NotNull HookList<UnitDyingHook> dyingHooks;
    private final @NotNull HookList<UnitRemainsHook> remainsHooks;
    private final @NotNull HookList<TerrainReleaseHook> terrainReleaseHooks;
    private final @NotNull HookList<BuildingDestroyedHook> buildingDestroyedHooks;

    /**
     * Creates a {@link UnitLifecycle} instance over the hooks of a game.
     *
     * @param hooks the hooks of the game
     */
    public UnitLifecycle(@NotNull HookTable hooks) {
        this.completedHooks         = hooks.get(UNIT_COMPLETED);
        this.removedHooks           = hooks.get(UNIT_REMOVED);
        this.adoptedHooks           = hooks.get(UNIT_ADOPTED);
        this.ownerChangedHooks      = hooks.get(UNIT_OWNER_CHANGED);
        this.dyingHooks             = hooks.get(UNIT_DYING);
        this.remainsHooks           = hooks.get(UNIT_REMAINS);
        this.terrainReleaseHooks    = hooks.get(TERRAIN_RELEASE);
        this.buildingDestroyedHooks = hooks.get(BUILDING_DESTROYED);
    }

    /**
     * Announces that a unit is completed ({@link #UNIT_COMPLETED}).
     *
     * @param unit the completed unit
     */
    public void notifyCompleted(@NotNull Unit unit) {
        HookList<UnitCompletedHook> hooks = this.completedHooks;

        for (int index = 0; index < hooks.size(); index++) {
            hooks.get(index).onUnitCompleted(unit);
        }
    }

    /**
     * Announces that a unit leaves its owner ({@link #UNIT_REMOVED}).
     *
     * @param unit  the unit
     * @param death {@code true} if the unit dies, {@code false} if it changes owner
     */
    public void notifyRemoved(@NotNull Unit unit, boolean death) {
        HookList<UnitRemovedHook> hooks = this.removedHooks;

        for (int index = 0; index < hooks.size(); index++) {
            hooks.get(index).onUnitRemoved(unit, death);
        }
    }

    /**
     * Announces that a unit has its new owner ({@link #UNIT_ADOPTED}).
     *
     * @param unit             the unit with its new owner
     * @param instructionLimit the instruction limit of each animation program run a hook starts
     */
    public void notifyAdopted(@NotNull Unit unit, int instructionLimit) {
        HookList<UnitAdoptedHook> hooks = this.adoptedHooks;

        for (int index = 0; index < hooks.size(); index++) {
            hooks.get(index).onUnitAdopted(unit, instructionLimit);
        }
    }

    /**
     * Announces that the owner change of a unit is finished ({@link #UNIT_OWNER_CHANGED}).
     *
     * @param unit             the unit with its new owner
     * @param instructionLimit the instruction limit of each animation program run a hook starts
     */
    public void notifyOwnerChanged(@NotNull Unit unit, int instructionLimit) {
        HookList<UnitOwnerChangedHook> hooks = this.ownerChangedHooks;

        for (int index = 0; index < hooks.size(); index++) {
            hooks.get(index).onUnitOwnerChanged(unit, instructionLimit);
        }
    }

    /**
     * Announces that a unit dies and leaves the systems it provides for ({@link #UNIT_DYING}).
     *
     * @param unit the dying unit
     */
    public void notifyDying(@NotNull Unit unit) {
        HookList<UnitDyingHook> hooks = this.dyingHooks;

        for (int index = 0; index < hooks.size(); index++) {
            hooks.get(index).onUnitDying(unit);
        }
    }

    /**
     * Asks whether a dying unit in the world remains as something else ({@link #UNIT_REMAINS}): the hooks in registration order until one keeps
     * it.
     *
     * @param unit             the dying unit, after it left its owner
     * @param instructionLimit the instruction limit of each animation program run a hook starts
     * @return {@code true} if a hook kept the unit, which ends its death, {@code false} otherwise
     */
    public boolean notifyRemains(@NotNull Unit unit, int instructionLimit) {
        HookList<UnitRemainsHook> hooks = this.remainsHooks;

        for (int index = 0; index < hooks.size(); index++) {
            if (hooks.get(index).keepsUnit(unit, instructionLimit)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Announces that a dying unit stops changing the terrain around it ({@link #TERRAIN_RELEASE}): every hook in registration order, each with
     * the result of the hook before it.
     *
     * @param unit the dying unit
     * @return {@code true} if every release is finished, {@code false} if one must be retried before the unit is released
     */
    public boolean notifyTerrainRelease(@NotNull Unit unit) {
        HookList<TerrainReleaseHook> hooks = this.terrainReleaseHooks;
        boolean released = true;

        for (int index = 0; index < hooks.size(); index++) {
            released = hooks.get(index).releaseTerrain(unit, released);
        }

        return released;
    }

    /**
     * Announces that a building is destroyed ({@link #BUILDING_DESTROYED}).
     *
     * @param unit the destroyed building
     */
    public void notifyBuildingDestroyed(@NotNull Unit unit) {
        HookList<BuildingDestroyedHook> hooks = this.buildingDestroyedHooks;

        for (int index = 0; index < hooks.size(); index++) {
            hooks.get(index).onBuildingDestroyed(unit);
        }
    }
}
