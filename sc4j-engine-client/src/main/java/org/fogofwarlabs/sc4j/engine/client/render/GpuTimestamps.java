package org.fogofwarlabs.sc4j.engine.client.render;

/**
 * Holds a fixed number of GPU timestamp slots of a {@link GraphicsDevice}: a slot records the GPU time at which the commands submitted before
 * {@link #write(int)} completed, and its value is read later without waiting ({@link #isAvailable(int)}, {@link #read(int)}). Frame
 * diagnostics measure GPU phase times with them.
 */
public interface GpuTimestamps extends AutoCloseable {
    /**
     * @return the number of timestamp slots
     */
    int getCount();

    /**
     * Records a timestamp in a slot after the commands submitted so far.
     *
     * @param index the slot, from {@code 0} below {@link #getCount()}
     * @throws IllegalStateException if the timestamps are closed
     */
    void write(int index);

    /**
     * Checks whether the timestamp of a slot is ready to read without waiting.
     *
     * @param index the slot, from {@code 0} below {@link #getCount()}
     * @return {@code true} if the last timestamp written to the slot is ready, {@code false} otherwise
     * @throws IllegalStateException if the timestamps are closed
     */
    boolean isAvailable(int index);

    /**
     * Reads the timestamp of a slot; waits for it when it is not available yet.
     *
     * @param index the slot, from {@code 0} below {@link #getCount()}
     * @return the GPU time in nanoseconds
     * @throws IllegalStateException if the timestamps are closed
     */
    long read(int index);


    /**
     * Disposes resources: deletes the timestamp slots. Closing again does nothing.
     */
    @Override
    void close();
}
