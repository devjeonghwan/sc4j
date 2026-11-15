package org.fogofwarlabs.sc4j.engine.client.host;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.util.Locale;

/**
 * Reads and sets the Windows system pointer speed for the current session through {@code SystemParametersInfoW}: the engine default
 * {@link SystemPointerSpeed} on Windows. Changes are not saved to the user profile.
 */
public final class WindowsSystemPointerSpeed implements SystemPointerSpeed {
    private static final @NotNull String OPERATING_SYSTEM_PROPERTY = "os.name";
    private static final @NotNull String WINDOWS_PREFIX = "windows";
    private static final @NotNull String LIBRARY_NAME = "user32.dll";
    private static final @NotNull String FUNCTION_NAME = "SystemParametersInfoW";

    private static final int SPI_GETMOUSESPEED = 0x70;
    private static final int SPI_SETMOUSESPEED = 0x71;
    private static final int NO_PARAMETER = 0;
    private static final int NOT_SAVED = 0;

    /**
     * Creates a {@link WindowsSystemPointerSpeed} instance on Windows. The client starts with native access enabled for the platform library
     * lookup and downcall binding.
     *
     * @return the created {@link WindowsSystemPointerSpeed} instance, or {@code null} on another platform or without {@code user32.dll}
     */
    @SuppressWarnings("restricted")
    public static @Nullable WindowsSystemPointerSpeed create() {
        if (!System.getProperty(OPERATING_SYSTEM_PROPERTY, "").toLowerCase(Locale.ROOT).startsWith(WINDOWS_PREFIX)) {
            return null;
        }

        try {
            MemorySegment function = SymbolLookup.libraryLookup(LIBRARY_NAME, Arena.global()).find(FUNCTION_NAME).orElse(null);

            if (function == null) {
                return null;
            }

            return new WindowsSystemPointerSpeed(Linker.nativeLinker().downcallHandle(
                    function,
                    FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.ADDRESS, ValueLayout.JAVA_INT)
            ));
        } catch (IllegalArgumentException | IllegalCallerException exception) {
            return null;
        }
    }

    private final @NotNull MethodHandle systemParametersInfo;

    /**
     * Creates a {@link WindowsSystemPointerSpeed} instance.
     *
     * @param systemParametersInfo the bound system pointer-speed function
     */
    private WindowsSystemPointerSpeed(@NotNull MethodHandle systemParametersInfo) {
        this.systemParametersInfo = systemParametersInfo;
    }

    /**
     * Reads the current system pointer speed.
     *
     * @return the speed from {@link #MINIMUM_SPEED} to {@link #MAXIMUM_SPEED}, or {@code -1} if it cannot be read
     * @throws IllegalStateException if the platform call fails
     */
    @Override
    public int readSpeed() {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment speed = arena.allocate(ValueLayout.JAVA_INT);
            int result = (int) this.systemParametersInfo.invokeExact(SPI_GETMOUSESPEED, NO_PARAMETER, speed, NOT_SAVED);

            return result == 0 ? -1 : speed.get(ValueLayout.JAVA_INT, 0);
        } catch (Throwable exception) {
            throw Exceptions.newIllegalStateException(exception, "SystemParametersInfoW failed to read the pointer speed");
        }
    }

    /**
     * Sets the system pointer speed for the session (not saved).
     *
     * @param speed the speed from {@link #MINIMUM_SPEED} to {@link #MAXIMUM_SPEED}
     * @throws IllegalStateException if the platform refuses the speed or the call fails
     */
    @Override
    public void writeSpeed(int speed) {
        try {
            int result = (int) this.systemParametersInfo.invokeExact(SPI_SETMOUSESPEED, NO_PARAMETER, MemorySegment.ofAddress(speed), NOT_SAVED);

            if (result == 0) {
                throw Exceptions.newIllegalStateException("SystemParametersInfoW refused the pointer speed {0}", speed);
            }
        } catch (IllegalStateException exception) {
            throw exception;
        } catch (Throwable exception) {
            throw Exceptions.newIllegalStateException(exception, "SystemParametersInfoW failed to set the pointer speed {0}", speed);
        }
    }
}
