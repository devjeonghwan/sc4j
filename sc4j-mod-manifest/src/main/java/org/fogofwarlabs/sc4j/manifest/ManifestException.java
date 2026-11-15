package org.fogofwarlabs.sc4j.manifest;

import org.fogofwarlabs.sc4j.manifest.validation.ValidationResult;
import org.jetbrains.annotations.NotNull;

import java.io.Serial;

/**
 * Signals that a mod jar cannot be used: it cannot be opened, it has no manifest, the manifest is broken or invalid, an
 * image it names is missing, or the runtime's engine version does not support it. The problems list every reason found.
 */
public final class ManifestException extends Exception {
    @Serial
    private static final long serialVersionUID = 1L;

    private final transient @NotNull ValidationResult result;

    /**
     * Creates a {@link ManifestException} instance.
     *
     * @param result the problems, at least one
     */
    public ManifestException(@NotNull ValidationResult result) {
        super(result.describe());

        this.result = result;
    }

    /**
     * @return the problems
     */
    public @NotNull ValidationResult getResult() {
        return this.result;
    }
}
