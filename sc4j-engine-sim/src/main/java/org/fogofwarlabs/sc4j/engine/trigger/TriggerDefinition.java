package org.fogofwarlabs.sc4j.engine.trigger;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.game.ControllerKind;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes one trigger of the standard trigger system, as a map or a mod defines it: the players it runs for, its conditions and actions in
 * list order, and whether it repeats (runs again each time its conditions hold) or runs once per player.
 */
public final class TriggerDefinition {
    /**
     * Builds {@link TriggerDefinition} instances.
     */
    public static final class Builder {
        private final @NotNull List<@NotNull Integer> playerSlots;
        private final @NotNull List<@NotNull ConditionRecord> conditions;
        private final @NotNull List<@NotNull ActionRecord> actions;

        private boolean allPlayers;
        private boolean repeating;

        /**
         * Creates a {@link Builder} instance of a trigger that runs once, for no player yet.
         */
        public Builder() {
            this.playerSlots = new ArrayList<>();
            this.conditions  = new ArrayList<>();
            this.actions     = new ArrayList<>();

            this.allPlayers = false;
            this.repeating  = false;
        }

        /**
         * Makes the trigger run for every player slot of the setup that is not {@link ControllerKind#NEUTRAL}.
         *
         * @return this instance for chaining
         */
        public @NotNull Builder forAllPlayers() {
            this.allPlayers = true;

            return this;
        }

        /**
         * Makes the trigger run for the player of a slot.
         *
         * @param slotIndex the player slot index, at least {@code 0}
         * @return this instance for chaining
         * @throws IllegalArgumentException if the slot index is negative
         */
        public @NotNull Builder addPlayer(int slotIndex) {
            if (slotIndex < 0) {
                throw Exceptions.newIllegalArgumentException("Invalid player slot of a trigger: {0}", slotIndex);
            }

            this.playerSlots.add(slotIndex);

            return this;
        }

        /**
         * Appends a condition; every condition must hold for the actions to run.
         *
         * @param condition the condition
         * @return this instance for chaining
         */
        public @NotNull Builder addCondition(@NotNull ConditionRecord condition) {
            this.conditions.add(condition);

            return this;
        }

        /**
         * Appends an action.
         *
         * @param action the action
         * @return this instance for chaining
         */
        public @NotNull Builder addAction(@NotNull ActionRecord action) {
            this.actions.add(action);

            return this;
        }

        /**
         * Sets whether the trigger runs again each time its conditions hold.
         *
         * @param repeating {@code true} to run it again, {@code false} to run it once per player
         * @return this instance for chaining
         */
        public @NotNull Builder setRepeating(boolean repeating) {
            this.repeating = repeating;

            return this;
        }

        /**
         * Builds a {@link TriggerDefinition} instance.
         *
         * @return the built {@link TriggerDefinition} instance
         */
        public @NotNull TriggerDefinition build() {
            return new TriggerDefinition(this);
        }
    }

    private final boolean allPlayers;
    private final @NotNull List<@NotNull Integer> playerSlots;
    private final @NotNull List<@NotNull ConditionRecord> conditions;
    private final @NotNull List<@NotNull ActionRecord> actions;
    private final boolean repeating;

    private TriggerDefinition(@NotNull Builder builder) {
        this.allPlayers  = builder.allPlayers;
        this.playerSlots = List.copyOf(builder.playerSlots);
        this.conditions  = List.copyOf(builder.conditions);
        this.actions     = List.copyOf(builder.actions);
        this.repeating   = builder.repeating;
    }

    /**
     * @return {@code true} if the trigger runs for every player slot that is not neutral, {@code false} if only for its listed slots
     */
    public boolean isForAllPlayers() {
        return this.allPlayers;
    }

    /**
     * @return the listed player slot indices in definition order, unmodifiable
     */
    public @NotNull List<@NotNull Integer> getPlayerSlots() {
        return this.playerSlots;
    }

    /**
     * @return the conditions in list order, unmodifiable
     */
    public @NotNull List<@NotNull ConditionRecord> getConditions() {
        return this.conditions;
    }

    /**
     * @return the actions in list order, unmodifiable
     */
    public @NotNull List<@NotNull ActionRecord> getActions() {
        return this.actions;
    }

    /**
     * @return {@code true} if the trigger runs again each time its conditions hold, {@code false} if once per player
     */
    public boolean isRepeating() {
        return this.repeating;
    }
}
