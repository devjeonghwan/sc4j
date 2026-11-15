package org.fogofwarlabs.sc4j.engine.content.requirement.declarative;

import org.fogofwarlabs.sc4j.engine.content.requirement.RequirementResult;
import org.fogofwarlabs.sc4j.engine.content.requirement.StandardRequirementResult;
import org.fogofwarlabs.sc4j.engine.content.table.GameContent;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Holds the declarative requirement of one action and type: every condition must be met, tested in the listed order.
 */
public final class Requirement {
    /**
     * Creates a {@link Requirement} instance of all the given conditions.
     *
     * @param conditions the conditions, copied; none makes a requirement that is always met
     * @return the created {@link Requirement} instance
     */
    public static @NotNull Requirement allOf(@NotNull RequirementCondition @NotNull ... conditions) {
        return new Requirement(conditions.clone());
    }

    private final @NotNull RequirementCondition @NotNull [] conditions;

    private Requirement(@NotNull RequirementCondition @NotNull [] conditions) {
        this.conditions = conditions;
    }

    /**
     * Tests the conditions in order and stops at the first failure.
     *
     * @param content   the content of the game
     * @param player    the player
     * @param actorType the unit type of the acting unit, or {@code null} without one
     * @return {@link StandardRequirementResult#MET}, or the first failure
     */
    public @NotNull RequirementResult test(@NotNull GameContent content, @NotNull Player player, @Nullable UnitType actorType) {
        for (RequirementCondition condition : this.conditions) {
            RequirementResult result = condition.test(content, player, actorType);

            if (!result.isMet()) {
                return result;
            }
        }

        return StandardRequirementResult.MET;
    }
}
