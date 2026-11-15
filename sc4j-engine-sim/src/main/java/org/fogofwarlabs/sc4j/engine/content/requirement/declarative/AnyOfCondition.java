package org.fogofwarlabs.sc4j.engine.content.requirement.declarative;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.content.requirement.RequirementResult;
import org.fogofwarlabs.sc4j.engine.content.table.GameContent;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Requires at least one of its alternatives, tested in the listed order.
 */
public final class AnyOfCondition implements RequirementCondition {
    private final @NotNull RequirementCondition @NotNull [] alternatives;

    /**
     * Creates an {@link AnyOfCondition} instance.
     *
     * @param alternatives the alternatives, copied, at least one
     * @throws IllegalArgumentException if no alternative is given
     */
    public AnyOfCondition(@NotNull RequirementCondition @NotNull ... alternatives) {
        if (alternatives.length == 0) {
            throw Exceptions.newIllegalArgumentException("An any-of condition needs at least one alternative.");
        }

        this.alternatives = alternatives.clone();
    }

    /**
     * Tests the alternatives in order until one is met.
     *
     * @param content   the content of the game
     * @param player    the player
     * @param actorType the unit type of the acting unit, or {@code null} without one
     * @return the first met result, or the failure of the first alternative if none is met
     */
    @Override
    public @NotNull RequirementResult test(@NotNull GameContent content, @NotNull Player player, @Nullable UnitType actorType) {
        RequirementResult firstFailure = this.alternatives[0].test(content, player, actorType);

        if (firstFailure.isMet()) {
            return firstFailure;
        }

        for (int index = 1; index < this.alternatives.length; index++) {
            RequirementResult result = this.alternatives[index].test(content, player, actorType);

            if (result.isMet()) {
                return result;
            }
        }

        return firstFailure;
    }
}
