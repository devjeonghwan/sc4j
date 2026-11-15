package org.fogofwarlabs.sc4j.engine.trigger;

import org.fogofwarlabs.sc4j.engine.content.definition.UnitTrait;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Creates the trigger sets of the usual skirmish victory rules for every player that is not neutral, from the standard conditions and actions:
 * a player without units, or without buildings, loses; a player whose opponents all lost wins.
 */
public final class StandardTriggerUtils {
    /**
     * Creates the triggers of an annihilation game: a player who has no unit left loses, and a player whose opponents all lost wins.
     *
     * @return the triggers, in run order
     */
    public static @NotNull List<@NotNull TriggerDefinition> createAnnihilation() {
        return create(null);
    }

    /**
     * Creates the triggers of an elimination game: a player who has no {@link UnitTrait#BUILDING} left loses, and a player whose opponents all
     * lost wins.
     *
     * @return the triggers, in run order
     */
    public static @NotNull List<@NotNull TriggerDefinition> createElimination() {
        return create(UnitTrait.BUILDING);
    }

    private static @NotNull List<@NotNull TriggerDefinition> create(@Nullable UnitTrait counted) {
        TriggerDefinition defeat = new TriggerDefinition.Builder()
                .forAllPlayers()
                .addCondition(new ConditionRecord.Builder(StandardTriggerConditionUtils.UNIT_COUNT).setUnitTrait(counted)
                                                                                                   .setComparison(Comparison.AT_MOST)
                                                                                                   .setAmount(0)
                                                                                                   .build())
                .addAction(new ActionRecord.Builder(StandardTriggerActionUtils.DEFEAT).build())
                .build();
        TriggerDefinition victory = new TriggerDefinition.Builder()
                .forAllPlayers()
                .addCondition(new ConditionRecord.Builder(StandardTriggerConditionUtils.OPPONENTS_DEFEATED).build())
                .addAction(new ActionRecord.Builder(StandardTriggerActionUtils.VICTORY).build())
                .build();

        return List.of(defeat, victory);
    }

    private StandardTriggerUtils() {
    }
}
