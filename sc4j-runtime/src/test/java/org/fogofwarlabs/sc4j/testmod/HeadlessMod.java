package org.fogofwarlabs.sc4j.testmod;

import org.fogofwarlabs.sc4j.engine.mod.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * A test mod on the engine-only path without a client: it registers a headless runner, which writes {@value #RUN_FILE} into its storage (the
 * launch's headless flag, identity, whether it offers Beacon.Link, which the runner does not open, and the value of its
 * {@value #GAME_FOLDER_SETTING} setting) when its {@value RecordingMod#HEADLESS_SETTING} setting is on, and nothing to run otherwise. It
 * registers no first scene, so the runtime tests open no window.
 */
public final class HeadlessMod implements Mod {
    /**
     * The storage file the headless runner writes.
     */
    public static final @NotNull String RUN_FILE = "headless-run.txt";

    /**
     * The directory setting whose value the headless runner writes; {@code null} is written without a value.
     */
    public static final @NotNull String GAME_FOLDER_SETTING = "gameFolder";

    private @Nullable ModContext context;

    /**
     * Creates a {@link HeadlessMod} instance.
     */
    public HeadlessMod() {
        this.context = null;
    }

    /**
     * Opens and validates what the mod needs, such as external data and its version.
     *
     * @param context the settings, storage, logger and paths of the run
     */
    @Override
    public void initialize(@NotNull ModContext context) {
        this.context = context;
    }

    /**
     * Registers what the mod adds to the engine. The registration is closed when this method returns.
     *
     * @param registration receives the mod's registrations
     * @throws ModException if the mod was not initialized
     */
    @Override
    public void register(@NotNull ModRegistration registration) throws ModException {
        ModContext initialized = this.context;

        if (initialized == null) {
            throw ModException.create("The test mod was not initialized.");
        }

        if (initialized.getSettings().getBoolean(RecordingMod.HEADLESS_SETTING)) {
            registration.registerHeadlessRunner(launch -> this.writeRun(initialized, launch));
        }
    }

    private void writeRun(@NotNull ModContext initialized, @NotNull ModLaunch launch) throws ModException {
        ModSettings settings = initialized.getSettings();
        Path game = settings.hasValue(GAME_FOLDER_SETTING) ? settings.getDirectory(GAME_FOLDER_SETTING) : null;
        String run = "headless=" + launch.isHeadless() + " identity=" + launch.getIdentity().getModId() +
                     " beaconLink=" + (launch.getBeaconLink() != null) + " game=" + game;

        try {
            Files.writeString(initialized.getStorage().resolve(RUN_FILE), run, StandardCharsets.UTF_8);
        } catch (IOException exception) {
            throw ModException.create(exception, "The test run cannot be written: {0}", exception.getMessage());
        }
    }

    /**
     * Releases what {@link #initialize} opened, in reverse order.
     */
    @Override
    public void close() {
        this.context = null;
    }
}
