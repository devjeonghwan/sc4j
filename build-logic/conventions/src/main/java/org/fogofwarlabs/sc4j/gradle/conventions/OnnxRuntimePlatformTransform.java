package org.fogofwarlabs.sc4j.gradle.conventions;

import org.gradle.api.artifacts.transform.InputArtifact;
import org.gradle.api.artifacts.transform.TransformAction;
import org.gradle.api.artifacts.transform.TransformOutputs;
import org.gradle.api.artifacts.transform.TransformParameters;
import org.gradle.api.file.FileSystemLocation;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.PathSensitive;
import org.gradle.api.tasks.PathSensitivity;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

/**
 * Keeps only one platform's native libraries in the ONNX Runtime jar ({@code onnxruntime-<version>.jar}), which carries those of every
 * platform it supports in {@code ai/onnxruntime/native/<platform>/}: the folder of the parameter's platform stays without its debug symbols
 * ({@code .dSYM}), the other platforms' folders are left out. ONNX Runtime unpacks the library of the platform it runs on from that folder.
 * Every other jar passes unchanged.
 */
public abstract class OnnxRuntimePlatformTransform implements TransformAction<OnnxRuntimePlatformTransform.Parameters> {
    private static final @NotNull String JAR_PREFIX = "onnxruntime-";
    private static final @NotNull String JAR_SUFFIX = ".jar";
    private static final @NotNull String NATIVE_FOLDER = "ai/onnxruntime/native/";
    private static final @NotNull String DEBUG_SYMBOLS_FOLDER = ".dSYM/";

    /**
     * Holds the platform whose native libraries stay.
     */
    public interface Parameters extends TransformParameters {
        /**
         * @return the ONNX Runtime name of the platform, such as {@code win-x64}, {@code linux-aarch64} or {@code osx-aarch64}
         */
        @Input
        @NotNull Property<String> getPlatform();
    }

    /**
     * @return the jar to transform
     */
    @InputArtifact
    @PathSensitive(PathSensitivity.NAME_ONLY)
    public abstract @NotNull Provider<FileSystemLocation> getInputArtifact();

    /**
     * Writes the ONNX Runtime jar with the native libraries of one platform, or passes another jar on.
     *
     * @param outputs the outputs of the transform
     */
    @Override
    public void transform(@NotNull TransformOutputs outputs) {
        File input = this.getInputArtifact().get().getAsFile();
        String name = input.getName();

        if (!name.startsWith(JAR_PREFIX) || !name.endsWith(JAR_SUFFIX)) {
            outputs.file(this.getInputArtifact());

            return;
        }

        String platformFolder = NATIVE_FOLDER + this.getParameters().getPlatform().get() + "/";
        File output = outputs.file(name);

        try (ZipInputStream source = new ZipInputStream(new FileInputStream(input));
             ZipOutputStream destination = new ZipOutputStream(new FileOutputStream(output))) {
            ZipEntry entry = source.getNextEntry();

            while (entry != null) {
                String path = entry.getName();
                boolean otherNative = path.startsWith(NATIVE_FOLDER) && path.length() > NATIVE_FOLDER.length() && !path.startsWith(platformFolder);

                if (!otherNative && !path.contains(DEBUG_SYMBOLS_FOLDER)) {
                    ZipEntry copied = new ZipEntry(path);

                    copied.setTime(entry.getTime());
                    destination.putNextEntry(copied);
                    source.transferTo(destination);
                    destination.closeEntry();
                }

                entry = source.getNextEntry();
            }
        } catch (IOException exception) {
            throw new UncheckedIOException("Cannot repack " + input + ": " + exception.getMessage(), exception);
        }
    }
}
