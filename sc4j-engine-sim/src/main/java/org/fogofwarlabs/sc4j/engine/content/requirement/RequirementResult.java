package org.fogofwarlabs.sc4j.engine.content.requirement;

/**
 * Represents the outcome of a requirement check: met, or the reason it failed. The engine reasons are
 * {@link StandardRequirementResult}; a mod with its own {@link RequirementSystem} implements its own reasons, for
 * example the message a failure shows. Results are shared constants, so a check allocates nothing.
 */
public interface RequirementResult {
    /**
     * @return {@code true} if the requirement is met, {@code false} otherwise
     */
    boolean isMet();
}
