package org.fogofwarlabs.sc4j.engine.client.host;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.foreign.*;
import java.lang.invoke.MethodHandle;
import java.util.Locale;

/**
 * Confines the system cursor to a screen rectangle through {@code ClipCursor} of {@code user32.dll}, for a {@link GlfwHostWindow}'s
 * {@link PointerClip} on Windows.
 */
final class WindowsCursorClip {
    private static final @NotNull String OPERATING_SYSTEM_PROPERTY = "os.name";
    private static final @NotNull String WINDOWS_PREFIX = "windows";
    private static final @NotNull String LIBRARY_NAME = "user32.dll";
    private static final @NotNull String FUNCTION_NAME = "ClipCursor";
    /**
     * The edges of a {@code RECT}: left, top, right and bottom.
     */
    private static final int RECTANGLE_EDGE_COUNT = 4;
    private static final int LEFT_INDEX = 0;
    private static final int TOP_INDEX = 1;
    private static final int RIGHT_INDEX = 2;
    private static final int BOTTOM_INDEX = 3;

    /**
     * Creates a {@link WindowsCursorClip} instance on Windows. The client starts with native access enabled for the platform library lookup and
     * downcall binding.
     *
     * @return the created {@link WindowsCursorClip} instance, or {@code null} on another platform or without {@code ClipCursor}
     */
    @SuppressWarnings("restricted")
    static @Nullable WindowsCursorClip create() {
        if (!System.getProperty(OPERATING_SYSTEM_PROPERTY, "").toLowerCase(Locale.ROOT).startsWith(WINDOWS_PREFIX)) {
            return null;
        }

        try {
            MemorySegment function = SymbolLookup.libraryLookup(LIBRARY_NAME, Arena.global()).find(FUNCTION_NAME).orElse(null);

            if (function == null) {
                return null;
            }

            return new WindowsCursorClip(Linker.nativeLinker().downcallHandle(function,
                                                                              FunctionDescriptor.of(ValueLayout.JAVA_INT, ValueLayout.ADDRESS)));
        } catch (IllegalArgumentException | IllegalCallerException exception) {
            return null;
        }
    }

    private final @NotNull MethodHandle clipCursor;

    private WindowsCursorClip(@NotNull MethodHandle clipCursor) {
        this.clipCursor = clipCursor;
    }

    /**
     * Confines the cursor to a screen rectangle.
     *
     * @param left   the first screen column inside
     * @param top    the first screen row inside
     * @param right  the screen column after the rectangle
     * @param bottom the screen row after the rectangle
     * @return {@code true} if the cursor is confined, {@code false} if the call failed
     */
    boolean clip(int left, int top, int right, int bottom) {
        try (Arena arena = Arena.ofConfined()) {
            MemorySegment rectangle = arena.allocate(ValueLayout.JAVA_INT, RECTANGLE_EDGE_COUNT);

            rectangle.setAtIndex(ValueLayout.JAVA_INT, LEFT_INDEX, left);
            rectangle.setAtIndex(ValueLayout.JAVA_INT, TOP_INDEX, top);
            rectangle.setAtIndex(ValueLayout.JAVA_INT, RIGHT_INDEX, right);
            rectangle.setAtIndex(ValueLayout.JAVA_INT, BOTTOM_INDEX, bottom);

            return (int) this.clipCursor.invokeExact(rectangle) != 0;
        } catch (Throwable exception) {
            return false;
        }
    }

    /**
     * Lets the cursor move over the whole screen again.
     *
     * @return {@code true} if the cursor is free, {@code false} if the call failed
     */
    boolean release() {
        try {
            return (int) this.clipCursor.invokeExact(MemorySegment.NULL) != 0;
        } catch (Throwable exception) {
            return false;
        }
    }
}
