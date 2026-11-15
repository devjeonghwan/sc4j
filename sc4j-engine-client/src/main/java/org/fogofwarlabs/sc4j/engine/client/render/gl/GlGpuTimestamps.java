package org.fogofwarlabs.sc4j.engine.client.render.gl;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.client.render.GpuTimestamps;
import org.jetbrains.annotations.NotNull;

import static org.lwjgl.opengl.GL33C.*;

/**
 * Holds OpenGL timestamp queries ({@code glQueryCounter} with {@code GL_TIMESTAMP}): the {@link GpuTimestamps} of the OpenGL default device.
 */
final class GlGpuTimestamps implements GpuTimestamps {
    private final int @NotNull [] queries;

    private boolean closed;

    /**
     * Creates a {@link GlGpuTimestamps} instance with new queries on the current context.
     *
     * @param count the number of timestamp slots
     */
    GlGpuTimestamps(int count) {
        this.queries = new int[count];
        glGenQueries(this.queries);

        this.closed = false;
    }

    /**
     * @return the number of timestamp slots
     */
    @Override
    public int getCount() {
        return this.queries.length;
    }

    /**
     * Records a timestamp in a slot after the commands submitted so far.
     *
     * @param index the slot, from {@code 0} below {@link #getCount()}
     * @throws IllegalStateException if the timestamps are closed
     */
    @Override
    public void write(int index) {
        this.requireOpen();
        glQueryCounter(this.queries[index], GL_TIMESTAMP);
    }

    /**
     * Checks whether the timestamp of a slot is ready to read without waiting.
     *
     * @param index the slot, from {@code 0} below {@link #getCount()}
     * @return {@code true} if the last timestamp written to the slot is ready, {@code false} otherwise
     * @throws IllegalStateException if the timestamps are closed
     */
    @Override
    public boolean isAvailable(int index) {
        this.requireOpen();

        return glGetQueryObjecti(this.queries[index], GL_QUERY_RESULT_AVAILABLE) == GL_TRUE;
    }

    /**
     * Reads the timestamp of a slot; waits for it when it is not available yet.
     *
     * @param index the slot, from {@code 0} below {@link #getCount()}
     * @return the GPU time in nanoseconds
     * @throws IllegalStateException if the timestamps are closed
     */
    @Override
    public long read(int index) {
        this.requireOpen();

        return glGetQueryObjecti64(this.queries[index], GL_QUERY_RESULT);
    }

    private void requireOpen() {
        if (this.closed) {
            throw Exceptions.newIllegalStateException("The GPU timestamps are closed.");
        }
    }


    /**
     * Disposes resources: deletes the timestamp slots, in the reverse order of their creation. Closing again does nothing.
     */
    @Override
    public void close() {
        if (this.closed) {
            return;
        }

        this.closed = true;

        int[] reversed = new int[this.queries.length];

        for (int index = 0; index < reversed.length; index++) {
            reversed[index] = this.queries[this.queries.length - 1 - index];
        }

        glDeleteQueries(reversed);
    }
}
