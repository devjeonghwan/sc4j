package org.fogofwarlabs.sc4j.engine.client.host;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.util.Locale;

/**
 * Reads the Windows uptime clock {@code GetTickCount}: the engine default {@link HostTickClock} on Windows.
 */
public final class WindowsHostTickClock implements HostTickClock {
    private static final @NotNull String OPERATING_SYSTEM_PROPERTY = "os.name";
    private static final @NotNull String WINDOWS_PREFIX = "windows";
    private static final @NotNull String KERNEL_LIBRARY = "kernel32.dll";
    private static final @NotNull String GET_TICK_COUNT_FUNCTION = "GetTickCount";

    /**
     * Creates a {@link WindowsHostTickClock} instance on Windows. The client starts with native access enabled for the platform library lookup
     * and downcall binding.
     *
     * @return the created {@link WindowsHostTickClock} instance, or {@code null} on another platform or without {@code GetTickCount}
     */
    @SuppressWarnings("restricted")
    public static @Nullable WindowsHostTickClock create() {
        if (!System.getProperty(OPERATING_SYSTEM_PROPERTY, "").toLowerCase(Locale.ROOT).startsWith(WINDOWS_PREFIX)) {
            return null;
        }

        try {
            MemorySegment function = SymbolLookup.libraryLookup(KERNEL_LIBRARY, Arena.global()).find(GET_TICK_COUNT_FUNCTION).orElse(null);

            if (function == null) {
                return null;
            }

            return new WindowsHostTickClock(Linker.nativeLinker().downcallHandle(function, FunctionDescriptor.of(ValueLayout.JAVA_INT)));
        } catch (IllegalArgumentException | IllegalCallerException exception) {
            return null;
        }
    }

    private final @NotNull MethodHandle tickCount;

    /**
     * Creates a {@link WindowsHostTickClock} instance.
     *
     * @param tickCount the bound {@code GetTickCount} function
     */
    private WindowsHostTickClock(@NotNull MethodHandle tickCount) {
        this.tickCount = tickCount;
    }

    /**
     * Reads the tick counter.
     *
     * @return the current {@code GetTickCount} value, wrapping every {@code 49.7} days
     * @throws IllegalStateException if the {@code GetTickCount} call fails
     */
    @Override
    public int readTicks() {
        try {
            return (int) this.tickCount.invokeExact();
        } catch (Throwable exception) {
            throw Exceptions.newIllegalStateException(exception, "Windows GetTickCount invocation failed.");
        }
    }
}
