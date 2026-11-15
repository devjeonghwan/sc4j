package org.fogofwarlabs.sc4j.engine.trigger;

/**
 * Compares a counted or measured value with the amount of a standard trigger condition.
 */
public enum Comparison {
    /**
     * The value is at least the amount.
     */
    AT_LEAST,
    /**
     * The value is at most the amount.
     */
    AT_MOST,
    /**
     * The value is the amount.
     */
    EXACTLY;

    /**
     * Compares a value with an amount.
     *
     * @param value  the counted or measured value
     * @param amount the amount of the condition
     * @return {@code true} if the value compares to the amount as this comparison says, {@code false} otherwise
     */
    public boolean test(int value, int amount) {
        return switch (this) {
            case AT_LEAST -> value >= amount;
            case AT_MOST -> value <= amount;
            case EXACTLY -> value == amount;
        };
    }
}
