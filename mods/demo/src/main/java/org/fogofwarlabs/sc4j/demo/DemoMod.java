package org.fogofwarlabs.sc4j.demo;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.base.exception.IgnoreException;
import org.fogofwarlabs.sc4j.demo.client.MainMenuScene;
import org.fogofwarlabs.sc4j.demo.content.DemoContent;
import org.fogofwarlabs.sc4j.demo.map.DemoMaps;
import org.fogofwarlabs.sc4j.demo.rules.DemoRules;
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

/**
 * Represents the demo, a small real-time strategy game made only of the engine: its content comes from the engine standard data pack in the
 * demo's jar, its maps are files of the engine map format, its rules are the engine default systems with declarative requirements, and its
 * replays and saved games are the engine formats. It needs no external data and has no pre-run settings.
 */
public final class DemoMod implements Mod {
    private @Nullable ModContext context;
    private @Nullable DemoContent content;
    private @Nullable DemoMaps maps;

    /**
     * Creates a {@link DemoMod} instance; the runtime creates it through the manifest's entry.
     */
    public DemoMod() {
        this.context = null;
        this.content = null;
        this.maps    = null;
    }

    /**
     * Opens and validates what the mod needs, such as external data and its version.
     * <p>
     * Opens the demo's pack and reads its content and maps from the demo's class loader.
     *
     * @param context the settings, storage, logger and paths of the run
     * @throws ModException if the mod refuses the run, for example for unsupported external data
     */
    @Override
    public void initialize(@NotNull ModContext context) throws ModException {
        ClassLoader loader = DemoMod.class.getClassLoader();
        DemoContent opened;

        try {
            opened = DemoContent.open(loader);
        } catch (IOException exception) {
            throw ModException.create(exception, "The demo pack cannot be read: {0}", exception.getMessage());
        }

        try {
            this.maps = DemoMaps.load(loader, opened, context.getStorage().getMapFolders());
        } catch (IOException exception) {
            IgnoreException.execute(
                    opened::close
            );

            throw ModException.create(exception, "The demo maps cannot be read: {0}", exception.getMessage());
        }

        this.context = context;
        this.content = opened;
    }

    /**
     * Registers what the mod adds to the engine. The registration is closed when this method returns.
     * <p>
     * Registers the rules and content of the demo's games, the pack as the resource provider, the engine replay and save formats, the
     * headless runner and the first scene of the demo's client, the main menu.
     *
     * @param registration receives the mod's registrations
     */
    @Override
    public void register(@NotNull ModRegistration registration) {
        ModContext modContext = this.context;
        DemoContent demoContent = this.content;
        DemoMaps demoMaps = this.maps;

        if (modContext == null || demoContent == null || demoMaps == null) {
            throw Exceptions.newIllegalStateException("The demo is not initialized.");
        }

        GameSetupCodec setupCodec = demoContent.createSetupCodec();

        new DemoRules(demoContent, demoMaps).define(registration.getSimulation());
        registration.registerResourceProvider(demoContent.getPack());
        registration.registerReplayFormat(identity -> new EngineReplayFormat(identity, demoContent.getOrders().getCommands(), setupCodec));
        registration.registerSaveFormat(identity -> new EngineSaveFormat(identity, setupCodec));
        registration.registerHeadlessRunner(new DemoHeadlessRunner(demoContent, demoMaps, modContext.getStorage(), modContext.getArguments(),
                                                                   modContext.getLogger()));
        registration.registerFirstScene(client -> MainMenuScene.createFirstScene(client, demoContent, demoMaps));
    }

    /**
     * Releases what {@link #initialize} opened, in reverse order.
     */
    @Override
    public void close() {
        DemoContent demoContent = this.content;

        this.maps    = null;
        this.content = null;
        this.context = null;

        if (demoContent != null) {
            IgnoreException.execute(
                    demoContent::close
            );
        }
    }
}
