package org.fogofwarlabs.sc4j.engine.content.modifier;

import org.fogofwarlabs.sc4j.engine.content.type.ContentType;
import org.jetbrains.annotations.NotNull;

import java.util.List;

/**
 * Identifies one value that {@link StatModifier}s change: the effective value a system computes from a table value, the
 * player and the type. Every content registers the engine stats first, in the order of {@link #getEngineStats()}; a mod
 * registers its own stats after them.
 * <p>
 * A unit stat is computed for a unit type (the weapon type is {@code null}); a weapon stat for a weapon type used by a
 * unit type.
 */
public final class Stat extends ContentType {
    /**
     * The maximum hit points of a unit type.
     */
    public static final @NotNull Stat HIT_POINTS = new Stat("hit points");
    /**
     * The armor of a unit type.
     */
    public static final @NotNull Stat ARMOR = new Stat("armor");
    /**
     * The sight range of a unit type.
     */
    public static final @NotNull Stat SIGHT_RANGE = new Stat("sight range");
    /**
     * The top speed of a unit type.
     */
    public static final @NotNull Stat TOP_SPEED = new Stat("top speed");
    /**
     * The acceleration of a unit type.
     */
    public static final @NotNull Stat ACCELERATION = new Stat("acceleration");
    /**
     * The maximum energy of a unit type.
     */
    public static final @NotNull Stat MAXIMUM_ENERGY = new Stat("maximum energy");
    /**
     * The damage of one hit of a weapon type.
     */
    public static final @NotNull Stat WEAPON_DAMAGE = new Stat("weapon damage");
    /**
     * The frames between two attacks with a weapon type.
     */
    public static final @NotNull Stat WEAPON_COOLDOWN = new Stat("weapon cooldown");
    /**
     * The maximum range of a weapon type.
     */
    public static final @NotNull Stat WEAPON_RANGE = new Stat("weapon range");
    /**
     * The range within which a unit type acquires targets on its own.
     */
    public static final @NotNull Stat TARGET_ACQUISITION_RANGE = new Stat("target acquisition range");

    private static final @NotNull List<@NotNull Stat> ENGINE_STATS = List.of(HIT_POINTS, ARMOR, SIGHT_RANGE, TOP_SPEED, ACCELERATION, MAXIMUM_ENERGY,
                                                                             WEAPON_DAMAGE, WEAPON_COOLDOWN, WEAPON_RANGE, TARGET_ACQUISITION_RANGE);

    /**
     * @return the engine stats in the order every content registers them, so they have the same index in every content
     */
    public static @NotNull List<@NotNull Stat> getEngineStats() {
        return ENGINE_STATS;
    }

    /**
     * Creates a {@link Stat} instance that is not registered yet, for a stat of a mod.
     *
     * @param debugName the name, for diagnostics only; the simulation never reads it
     */
    public Stat(@NotNull String debugName) {
        super(debugName);
    }
}
