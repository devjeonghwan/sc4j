package org.fogofwarlabs.sc4j.testmod;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.command.CommandRegistry;
import org.fogofwarlabs.sc4j.engine.command.CommandType;
import org.fogofwarlabs.sc4j.engine.game.EngineLimits;
import org.fogofwarlabs.sc4j.engine.game.GameSetupCodec;
import org.fogofwarlabs.sc4j.engine.mod.Mod;
import org.fogofwarlabs.sc4j.engine.mod.ModContext;
import org.fogofwarlabs.sc4j.engine.mod.ModException;
import org.fogofwarlabs.sc4j.engine.mod.ModRegistration;
import org.fogofwarlabs.sc4j.engine.replay.EngineReplayFormat;
import org.fogofwarlabs.sc4j.engine.save.EngineSaveFormat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * A test mod that registers a {@link RecordingApplication}, a unit limit of {@value #UNIT_LIMIT} and the engine replay and save formats
 * (no command types, no factions or options), and writes {@value #CLOSED_FILE} into its storage when it is closed. Its package is outside
 * the protected engine packages, so the runtime loads it from the test jar.
 */
public final class RecordingMod implements Mod {
    /**
     * The storage file written by {@link #close()}.
     */
    public static final @NotNull String CLOSED_FILE = "closed.txt";

    /**
     * The boolean setting that says whether the application can run headless.
     */
    public static final @NotNull String HEADLESS_SETTING = "headlessCapable";

    /**
     * The unit limit the mod registers for its games.
     */
    public static final int UNIT_LIMIT = 12;

    private @Nullable ModContext context;

    /**
     * Creates a {@link RecordingMod} instance.
     */
    public RecordingMod() {
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

        registration.addIdentityData(initialized.getManifest().getId().getBytes(StandardCharsets.UTF_8));
        registration.getSimulation().setLimits(new EngineLimits.Builder().setUnits(UNIT_LIMIT).build());
        registration.registerApplication(new RecordingApplication(initialized.getStorage(), initialized.getSettings().getBoolean(HEADLESS_SETTING),
                                                                  initialized.getArguments()));

        CommandRegistry<CommandType<?>> commands = new CommandRegistry<>(0);
        GameSetupCodec setupCodec = new GameSetupCodec(List.of(), List.of());

        commands.freeze();
        registration.registerReplayFormat(identity -> new EngineReplayFormat(identity, commands, setupCodec));
        registration.registerSaveFormat(identity -> new EngineSaveFormat(identity, setupCodec));
    }

    /**
     * Releases what {@link #initialize} opened, in reverse order.
     */
    @Override
    public void close() {
        ModContext initialized = this.context;

        if (initialized == null) {
            return;
        }

        Path closed = initialized.getStorage().resolve(CLOSED_FILE);

        try {
            Files.writeString(closed, "closed");
        } catch (IOException exception) {
            throw Exceptions.newUncheckedIOException(exception);
        }
    }
}
