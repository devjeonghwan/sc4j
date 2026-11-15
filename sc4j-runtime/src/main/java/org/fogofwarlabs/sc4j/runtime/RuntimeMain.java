package org.fogofwarlabs.sc4j.runtime;

import org.fogofwarlabs.sc4j.runtime.session.RuntimeSession;
import org.jetbrains.annotations.NotNull;

/**
 * Starts the SC4J runtime: {@code sc4j-runtime --mod <jar> ...} runs one mod and ends the process with the session's
 * exit code.
 */
public final class RuntimeMain {
    /**
     * Runs the runtime and exits with its exit code.
     *
     * @param arguments the command line; {@code --help} describes it
     */
    public static void main(@NotNull String @NotNull [] arguments) {
        System.exit(RuntimeSession.run(arguments).getStatus());
    }


    private RuntimeMain() {
    }
}
