package org.fogofwarlabs.sc4j.engine.client.host;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;
import java.util.List;

/**
 * Receives the files dropped onto a {@link HostWindow} ({@link HostWindow#setFileDropListener}); it is called on the thread that polls the
 * window's events.
 */
public interface FileDropListener {
    /**
     * Handles the files of one drop.
     *
     * @param files the dropped files in the platform's order; never empty
     */
    void onFilesDropped(@NotNull List<Path> files);
}
