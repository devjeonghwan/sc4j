package org.fogofwarlabs.sc4j.engine.content.requirement;

/**
 * Names the outcomes of the engine's declarative requirement checks; a missing completed unit is a {@link MissingUnitResult}.
 */
public enum StandardRequirementResult implements RequirementResult {
    /**
     * The requirement is met.
     */
    MET,
    /**
     * The type is not available to the player.
     */
    UNAVAILABLE,
    /**
     * The acting unit is not of a type that may do it.
     */
    WRONG_ACTOR,
    /**
     * The player lacks a required upgrade level.
     */
    MISSING_UPGRADE_LEVEL,
    /**
     * The player has not researched a required tech.
     */
    MISSING_TECH,
    /**
     * The upgrade is at its maximum level for the player.
     */
    MAXIMUM_LEVEL_REACHED,
    /**
     * The tech is already researched.
     */
    ALREADY_RESEARCHED;

    /**
     * @return {@code true} if the requirement is met, {@code false} otherwise
     */
    @Override
    public boolean isMet() {
        return this == MET;
    }
}
