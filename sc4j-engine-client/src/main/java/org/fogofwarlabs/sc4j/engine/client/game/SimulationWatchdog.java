package org.fogofwarlabs.sc4j.engine.client.game;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.base.log.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Watches the simulation frames of a client for a frame that does not return: when one frame has run longer than the stall time, a daemon thread
 * logs the simulation thread's stack once at {@code WARN} as {@code SIM STALL} and then runs the stall listener ({@link #setStallListener}) on its
 * own thread, for example to save the running game's replay ({@link ReplayAutosave#saveStalled}) before the user closes the window.
 * <p>
 * The simulation thread marks each frame with {@link #begin} and {@link #end}; the watchdog reads only these marks and never touches the game.
 * Its thread starts with the first frame and ends with {@link #close}.
 */
public final class SimulationWatchdog implements AutoCloseable {
    /**
     * The time a frame may run before it counts as stalled.
     */
    public static final long DEFAULT_STALL_MILLISECONDS = 5_000;
    /**
     * The period of the watchdog's check.
     */
    public static final long DEFAULT_CHECK_MILLISECONDS = 1_000;

    private static final @NotNull Logger LOGGER = Logger.of(SimulationWatchdog.class);
    private static final @NotNull String THREAD_NAME = "sim-stall-watchdog";
    private static final long NANOSECONDS_PER_MILLISECOND = 1_000_000L;

    /**
     * Logs the stack of a stalled simulation thread.
     *
     * @param thread       the simulation thread
     * @param currentFrame the stalled frame
     * @param milliseconds how long the frame has run
     */
    private static void report(@NotNull Thread thread, long currentFrame, long milliseconds) {
        StringBuilder stack = new StringBuilder();

        for (StackTraceElement element : thread.getStackTrace()) {
            stack.append(System.lineSeparator()).append("    at ").append(element);
        }

        LOGGER.warning("SIM STALL: frame {0} has run {1} ms on thread {2}{3}", currentFrame, milliseconds, thread.getName(), stack);
    }


    private final long stallNanoseconds;
    private final long checkMilliseconds;

    private volatile @Nullable Thread simulationThread;
    /**
     * The start of the running frame, {@code 0} between frames.
     */
    private volatile long frameStartNanoseconds;
    private volatile long frame;
    private volatile @Nullable Runnable stallListener;

    private long reportedFrameStartNanoseconds;
    /**
     * Whether the watchdog thread was started (or the watchdog closed before the first frame); read on every frame.
     */
    private volatile boolean started;
    private @Nullable Thread watchdog;

    /**
     * Creates a {@link SimulationWatchdog} instance with the default stall time and check period.
     */
    public SimulationWatchdog() {
        this(DEFAULT_STALL_MILLISECONDS, DEFAULT_CHECK_MILLISECONDS);
    }

    /**
     * Creates a {@link SimulationWatchdog} instance.
     *
     * @param stallMilliseconds the time a frame may run before it counts as stalled
     * @param checkMilliseconds the period of the watchdog's check
     * @throws IllegalArgumentException if a time is not positive
     */
    public SimulationWatchdog(long stallMilliseconds, long checkMilliseconds) {
        if (stallMilliseconds <= 0 || checkMilliseconds <= 0) {
            throw Exceptions.newIllegalArgumentException("Invalid watchdog times: stall {0} ms, check {1} ms", stallMilliseconds, checkMilliseconds);
        }

        this.stallNanoseconds  = stallMilliseconds * NANOSECONDS_PER_MILLISECOND;
        this.checkMilliseconds = checkMilliseconds;

        this.simulationThread      = null;
        this.frameStartNanoseconds = 0;
        this.frame                 = 0;
        this.stallListener         = null;

        this.reportedFrameStartNanoseconds = 0;
        this.started                       = false;
        this.watchdog                      = null;
    }

    /**
     * Sets what runs once per stalled frame after its report, on the watchdog thread.
     *
     * @param listener the listener, or {@code null} for none
     */
    public void setStallListener(@Nullable Runnable listener) {
        this.stallListener = listener;
    }

    /**
     * Marks the start of a simulation frame on the calling thread, starting the watchdog thread on first use.
     *
     * @param currentFrame the frame about to run
     */
    public void begin(long currentFrame) {
        if (!this.started) {
            this.start();
        }

        this.simulationThread = Thread.currentThread();
        this.frame            = currentFrame;

        long now = System.nanoTime();

        this.frameStartNanoseconds = now == 0 ? 1 : now;
    }

    /**
     * Marks the end of the running simulation frame.
     */
    public void end() {
        this.frameStartNanoseconds = 0;
    }

    private synchronized void start() {
        if (this.started) {
            return;
        }

        this.started = true;

        Thread thread = new Thread(this::run, THREAD_NAME);

        thread.setDaemon(true);
        thread.start();

        this.watchdog = thread;
    }

    private void run() {
        while (true) {
            try {
                Thread.sleep(this.checkMilliseconds);
            } catch (InterruptedException exception) {
                return;
            }

            long observedFrameStartNanoseconds = this.frameStartNanoseconds;
            Thread thread = this.simulationThread;

            if (observedFrameStartNanoseconds == 0 || thread == null ||
                observedFrameStartNanoseconds == this.reportedFrameStartNanoseconds ||
                System.nanoTime() - observedFrameStartNanoseconds < this.stallNanoseconds) {
                continue;
            }

            this.reportedFrameStartNanoseconds = observedFrameStartNanoseconds;

            report(thread, this.frame, (System.nanoTime() - observedFrameStartNanoseconds) / NANOSECONDS_PER_MILLISECOND);

            Runnable listener = this.stallListener;

            if (listener != null) {
                try {
                    listener.run();
                } catch (RuntimeException exception) {
                    LOGGER.warning("SIM STALL listener failed: " + exception);
                }
            }
        }
    }

    /**
     * Stops the watchdog thread; later frames are no longer watched.
     */
    @Override
    public synchronized void close() {
        this.started = true;

        if (this.watchdog != null) {
            this.watchdog.interrupt();
        }

        this.watchdog = null;
    }
}
