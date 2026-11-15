package org.fogofwarlabs.sc4j.base.exception;

import org.fogofwarlabs.sc4j.base.log.LogLevel;
import org.fogofwarlabs.sc4j.base.log.Logger;
import org.jetbrains.annotations.NotNull;

/**
 * Runs cleanup callbacks so that a failing callback is logged and the remaining ones still run.
 * <p>
 * Callbacks are listed in the reverse order of acquisition:
 * {@snippet :
 * IgnoreException.execute(
 *         this.bindings::dispose,
 *         this.input::dispose,
 *         this.context::dispose
 * );
 *}
 */
public final class IgnoreException {
    private static final @NotNull Logger LOGGER = Logger.of(IgnoreException.class);

    /**
     * A cleanup callback that may throw.
     */
    @FunctionalInterface
    public interface Callback {
        /**
         * Runs the callback.
         *
         * @throws Exception if the callback fails
         */
        void run() throws Exception;
    }

    /**
     * Runs every callback in order, logging and ignoring what each one throws.
     *
     * @param callbacks the callbacks to run
     */
    public static void execute(@NotNull Callback @NotNull ... callbacks) {
        for (Callback callback : callbacks) {
            try {
                callback.run();
            } catch (Throwable throwable) {
                LOGGER.exception(LogLevel.WARNING, throwable, "Ignored a cleanup failure");
            }
        }
    }

    private IgnoreException() {
    }
}
