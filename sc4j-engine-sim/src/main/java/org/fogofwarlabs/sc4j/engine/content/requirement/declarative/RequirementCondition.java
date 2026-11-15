package org.fogofwarlabs.sc4j.engine.content.requirement.declarative;

import org.fogofwarlabs.sc4j.engine.content.requirement.RequirementResult;
import org.fogofwarlabs.sc4j.engine.content.table.GameContent;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Represents one condition of a declarative {@link Requirement}. Conditions are immutable, shared by every game, and
 * read only the game content; a test allocates nothing.
 */
public sealed interface RequirementCondition permits ActorTypeCondition, CompletedUnitCondition, UpgradeLevelCondition, TechResearchedCondition,
                                                     AnyOfCondition {
    /**
     * Tests this condition.
     *
     * @param content   the content of the game
     * @param player    the player
     * @param actorType the unit type of the acting unit, or {@code null} without one
     * @return the result, met or the reason of the failure
     */
    @NotNull RequirementResult test(@NotNull GameContent content, @NotNull Player player, @Nullable UnitType actorType);
}
