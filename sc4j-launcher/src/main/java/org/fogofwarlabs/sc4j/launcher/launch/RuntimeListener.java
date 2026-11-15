package org.fogofwarlabs.sc4j.launcher.launch;

import org.jetbrains.annotations.NotNull;

/**
 * Receives the output and the end of a runtime process ({@link RuntimeProcess}). Both methods are called on the process's
 * output thread, in output order; a user interface passes them on to its own thread.
 */
public interface RuntimeListener {
    /**
     * Called when the runtime prints a line on its standard output or standard error.
     *
     * @param line the line without its line terminator
     */
    void onOutput(@NotNull String line);

    /**
     * Called once when the runtime process has ended, after its last output line.
     *
     * @param status the process exit status ({@link RuntimeExitStatus#fromStatus(int)})
     */
    void onExit(int status);
}
