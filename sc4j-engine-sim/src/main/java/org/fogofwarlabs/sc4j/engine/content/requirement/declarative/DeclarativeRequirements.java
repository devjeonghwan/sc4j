package org.fogofwarlabs.sc4j.engine.content.requirement.declarative;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.content.requirement.RequirementAction;
import org.fogofwarlabs.sc4j.engine.content.type.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the declarative requirements of a mod: at most one {@link Requirement} per action and type. A type without a
 * requirement has only the built-in checks of {@link DeclarativeRequirementSystem}. The set is immutable and shared by
 * every game of a definition.
 */
public final class DeclarativeRequirements {
    /**
     * Builds {@link DeclarativeRequirements} instances.
     */
    public static final class Builder {
        private final @NotNull List<@NotNull List<@NotNull ContentType>> types;
        private final @NotNull List<@NotNull List<@NotNull Requirement>> requirements;

        /**
         * Creates a {@link Builder} instance without requirements.
         */
        public Builder() {
            RequirementAction[] actions = RequirementAction.values();

            this.types        = new ArrayList<>(actions.length);
            this.requirements = new ArrayList<>(actions.length);

            for (RequirementAction ignored : actions) {
                this.types.add(new ArrayList<>());
                this.requirements.add(new ArrayList<>());
            }
        }

        /**
         * Sets the requirement of building a unit type.
         *
         * @param unitType    the unit type
         * @param requirement the requirement
         * @return this instance for chaining
         * @throws IllegalArgumentException if the unit type already has a build requirement
         */
        public @NotNull Builder requireBuild(@NotNull UnitType unitType, @NotNull Requirement requirement) {
            return this.add(RequirementAction.BUILD_UNIT, unitType, requirement);
        }

        /**
         * Sets the requirement of researching the next level of an upgrade.
         *
         * @param upgrade     the upgrade
         * @param requirement the requirement
         * @return this instance for chaining
         * @throws IllegalArgumentException if the upgrade already has a requirement
         */
        public @NotNull Builder requireUpgrade(@NotNull UpgradeType upgrade, @NotNull Requirement requirement) {
            return this.add(RequirementAction.RESEARCH_UPGRADE, upgrade, requirement);
        }

        /**
         * Sets the requirement of researching a tech.
         *
         * @param tech        the tech
         * @param requirement the requirement
         * @return this instance for chaining
         * @throws IllegalArgumentException if the tech already has a research requirement
         */
        public @NotNull Builder requireResearch(@NotNull TechType tech, @NotNull Requirement requirement) {
            return this.add(RequirementAction.RESEARCH_TECH, tech, requirement);
        }

        /**
         * Sets the requirement of using a tech.
         *
         * @param tech        the tech
         * @param requirement the requirement
         * @return this instance for chaining
         * @throws IllegalArgumentException if the tech already has a use requirement
         */
        public @NotNull Builder requireTechUse(@NotNull TechType tech, @NotNull Requirement requirement) {
            return this.add(RequirementAction.USE_TECH, tech, requirement);
        }

        /**
         * Sets the requirement of issuing an order.
         *
         * @param order       the order type
         * @param requirement the requirement
         * @return this instance for chaining
         * @throws IllegalArgumentException if the order type already has a requirement
         */
        public @NotNull Builder requireOrder(@NotNull OrderType order, @NotNull Requirement requirement) {
            return this.add(RequirementAction.ISSUE_ORDER, order, requirement);
        }


        /**
         * Builds a {@link DeclarativeRequirements} instance from the requirements set so far.
         *
         * @return the built {@link DeclarativeRequirements} instance
         */
        public @NotNull DeclarativeRequirements build() {
            ContentType[][] builtTypes = new ContentType[this.types.size()][];
            Requirement[][] builtRequirements = new Requirement[this.requirements.size()][];

            for (int action = 0; action < builtTypes.length; action++) {
                builtTypes[action]        = this.types.get(action).toArray(new ContentType[0]);
                builtRequirements[action] = this.requirements.get(action).toArray(new Requirement[0]);
            }

            return new DeclarativeRequirements(builtTypes, builtRequirements);
        }

        private @NotNull Builder add(@NotNull RequirementAction action, @NotNull ContentType type, @NotNull Requirement requirement) {
            List<ContentType> actionTypes = this.types.get(action.ordinal());

            for (ContentType added : actionTypes) {
                if (added == type) {
                    throw Exceptions.newIllegalArgumentException("The type {0} already has a requirement for {1}.", type, action);
                }
            }

            actionTypes.add(type);
            this.requirements.get(action.ordinal()).add(requirement);

            return this;
        }
    }

    /**
     * Checks whether a type is registered in one content kind.
     */
    @FunctionalInterface
    interface TypeCheck {
        /**
         * Checks a type.
         *
         * @param type the type
         * @return {@code true} if the type is registered, {@code false} otherwise
         */
        boolean contains(@NotNull ContentType type);
    }

    private final @NotNull ContentType @NotNull [] @NotNull [] types;
    private final @NotNull Requirement @NotNull [] @NotNull [] requirements;

    private DeclarativeRequirements(@NotNull ContentType @NotNull [] @NotNull [] types, @NotNull Requirement @NotNull [] @NotNull [] requirements) {
        this.types        = types;
        this.requirements = requirements;
    }

    /**
     * Returns the requirements of an action by type index, for a game whose types are registered.
     *
     * @param action     the action
     * @param typeCount  the number of registered types of the action's content kind
     * @param registered checks that a type is registered in the content kind of the action
     * @return one entry per type index, {@code null} for a type without a requirement
     * @throws IllegalArgumentException if a requirement names a type that is not registered
     */
    @Nullable Requirement @NotNull [] resolve(@NotNull RequirementAction action, int typeCount, @NotNull TypeCheck registered) {
        ContentType[] actionTypes = this.types[action.ordinal()];
        Requirement[] actionRequirements = this.requirements[action.ordinal()];
        Requirement[] resolved = new Requirement[typeCount];

        for (int entry = 0; entry < actionTypes.length; entry++) {
            if (!registered.contains(actionTypes[entry])) {
                throw Exceptions.newIllegalArgumentException("The requirement of {0} for {1} names a type that is not registered.",
                                                             actionTypes[entry], action);
            }

            resolved[actionTypes[entry].getIndex()] = actionRequirements[entry];
        }

        return resolved;
    }
}
