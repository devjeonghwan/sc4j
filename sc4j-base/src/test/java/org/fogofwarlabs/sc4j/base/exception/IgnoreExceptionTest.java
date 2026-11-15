package org.fogofwarlabs.sc4j.base.exception;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Checks that cleanup callbacks keep running after one of them fails.
 */
final class IgnoreExceptionTest {
    private static final @NotNull String FIRST = "first";
    private static final @NotNull String THIRD = "third";

    @Test
    void execute_should_run_the_callbacks_after_a_failing_one() {
        List<String> run = new ArrayList<>();

        IgnoreException.execute(
                () -> run.add(FIRST),
                () -> {
                    throw Exceptions.newIllegalStateException("The second callback fails.");
                },
                () -> run.add(THIRD)
        );

        Assertions.assertEquals(List.of(FIRST, THIRD), run);
    }
}
