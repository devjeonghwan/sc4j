package org.fogofwarlabs.sc4j.engine.client.host;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.client.render.GpuCanvas;
import org.fogofwarlabs.sc4j.engine.client.render.GpuTimestamps;
import org.fogofwarlabs.sc4j.engine.client.render.GraphicsDevice;
import org.fogofwarlabs.sc4j.engine.client.render.ShaderProgram;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Tests the engine frame phase timing: the CSV rows, the GPU phase times read frames later and the flush at the last phase.
 */
final class FrameTimingTest {
    private static final @NotNull List<String> PHASES = List.of("update", "draw", "swap");
    /**
     * The GPU nanoseconds between two written timestamps.
     */
    private static final long GPU_STEP = 2000;
    private static final int GPU_STEP_MICROSECONDS = 2;
    private static final int FRAMES = FrameTiming.GPU_FRAMES + 1;
    /**
     * The header columns before the CPU phases and the columns between the CPU and the GPU phases.
     */
    private static final int LEADING_COLUMNS = 3;
    private static final int MIDDLE_COLUMNS = 4;

    /**
     * Writes timestamps that advance by {@link #GPU_STEP} per write and are available at once.
     */
    private static final class CountingTimestamps implements GpuTimestamps {
        private final long @NotNull [] values;

        private long clock;
        private boolean closed;

        private CountingTimestamps(int count) {
            this.values = new long[count];

            this.clock  = 0;
            this.closed = false;
        }

        /**
         * @return the number of timestamp slots
         */
        @Override
        public int getCount() {
            return this.values.length;
        }

        /**
         * Records a timestamp in a slot after the commands submitted so far.
         *
         * @param index the slot, from {@code 0} below {@link #getCount()}
         */
        @Override
        public void write(int index) {
                                 this.clock += GPU_STEP;
            this.values[index] = this.clock;
        }

        /**
         * Checks whether the timestamp of a slot is ready to read without waiting.
         *
         * @param index the slot, from {@code 0} below {@link #getCount()}
         * @return {@code true} always
         */
        @Override
        public boolean isAvailable(int index) {
            return true;
        }

        /**
         * Reads the timestamp of a slot; waits for it when it is not available yet.
         *
         * @param index the slot, from {@code 0} below {@link #getCount()}
         * @return the GPU time in nanoseconds
         */
        @Override
        public long read(int index) {
            return this.values[index];
        }

        /**
         * Disposes resources: deletes the timestamp slots. Closing again does nothing.
         */
        @Override
        public void close() {
            this.closed = true;
        }
    }

    /**
     * Creates counting timestamps and counts flushes; creates nothing else.
     */
    private static final class CountingDevice implements GraphicsDevice {
        private @Nullable CountingTimestamps timestamps;
        private int flushes;

        private CountingDevice() {
            this.timestamps = null;
            this.flushes    = 0;
        }

        /**
         * Creates a {@link GpuCanvas} instance on the current context.
         *
         * @param fieldPageSize the size in texels of the square distance field pages it draws glyphs from
         * @return never
         */
        @Override
        public @NotNull GpuCanvas createCanvas(int fieldPageSize) {
            throw Exceptions.newUnsupportedOperationException("No canvas in the timing test.");
        }

        /**
         * Creates a {@link ShaderProgram} instance.
         *
         * @param name           the renderer name that the failure messages start with
         * @param vertexSource   the vertex stage source
         * @param fragmentSource the fragment stage source
         * @return never
         */
        @Override
        public @NotNull ShaderProgram createShaderProgram(@NotNull String name, @NotNull String vertexSource, @NotNull String fragmentSource) {
            throw Exceptions.newUnsupportedOperationException("No shader in the timing test.");
        }

        /**
         * Selects color targets {@code 0} through {@code count - 1} of the bound framebuffer.
         *
         * @param count the number of color targets
         */
        @Override
        public void selectColorTargets(int count) {
        }

        /**
         * Creates a {@link GpuTimestamps} instance on the current context.
         *
         * @param count the number of timestamp slots
         * @return the created {@link GpuTimestamps} instance, closed by the caller
         */
        @Override
        public @NotNull GpuTimestamps createTimestamps(int count) {
            this.timestamps = new CountingTimestamps(count);

            return this.timestamps;
        }

        /**
         * Submits the commands issued so far to the GPU without waiting for them.
         */
        @Override
        public void flush() {
            this.flushes++;
        }
    }

    @Test
    void frames_write_csv_rows_with_gpu_phase_times_from_earlier_frames(@TempDir @NotNull Path directory) throws IOException {
        Path file = directory.resolve("timing.csv");
        CountingDevice device = new CountingDevice();

        try (FrameTiming timing = FrameTiming.open(file, PHASES, device)) {
            for (int frame = 0; frame < FRAMES; frame++) {
                timing.beginFrame();

                for (int phase = 0; phase < PHASES.size(); phase++) {
                    timing.mark(phase);
                }

                timing.endFrame(frame);
            }
        }

        List<String> lines = Files.readAllLines(file, StandardCharsets.US_ASCII);
        String[] header = lines.getFirst().split(",");
        String[] first = lines.get(1).split(",");
        String[] last = lines.getLast().split(",");
        int gpuTotal = LEADING_COLUMNS + PHASES.size() + MIDDLE_COLUMNS - 1;

        Assertions.assertEquals(FRAMES + 1, lines.size());
        Assertions.assertEquals("update", header[LEADING_COLUMNS]);
        Assertions.assertEquals("gpu_swap", header[header.length - 1]);
        Assertions.assertEquals("-1", first[gpuTotal], "No GPU result of an earlier frame yet.");
        Assertions.assertEquals(String.valueOf(PHASES.size() * GPU_STEP_MICROSECONDS), last[gpuTotal]);
        Assertions.assertEquals(String.valueOf(GPU_STEP_MICROSECONDS), last[gpuTotal + 1]);
        Assertions.assertEquals(FRAMES, device.flushes, "The last phase flushes once a frame.");

        CountingTimestamps timestamps = device.timestamps;

        Assertions.assertNotNull(timestamps);
        Assertions.assertTrue(timestamps.closed);
    }

    @Test
    void open_should_reject_an_empty_phase_list(@TempDir @NotNull Path directory) {
        Path file = directory.resolve("timing.csv");

        Assertions.assertThrows(IllegalArgumentException.class, () -> FrameTiming.open(file, List.of(), new CountingDevice()));
    }
}
