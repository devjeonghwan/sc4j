package org.fogofwarlabs.sc4j.runtime.session;

import org.fogofwarlabs.sc4j.engine.animation.AnimationLibrary;
import org.fogofwarlabs.sc4j.engine.client.app.FirstSceneFactory;
import org.fogofwarlabs.sc4j.engine.content.definition.UnitDefinition;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.mod.HeadlessRunner;
import org.fogofwarlabs.sc4j.engine.mod.ModApplication;
import org.fogofwarlabs.sc4j.engine.mod.ModLaunch;
import org.fogofwarlabs.sc4j.engine.replay.ReplayFormatFactory;
import org.fogofwarlabs.sc4j.engine.resource.ResourceKey;
import org.fogofwarlabs.sc4j.engine.resource.ResourceNotFoundException;
import org.fogofwarlabs.sc4j.engine.resource.ResourceProvider;
import org.fogofwarlabs.sc4j.engine.resource.ResourceType;
import org.fogofwarlabs.sc4j.engine.resource.audio.SoundClip;
import org.fogofwarlabs.sc4j.engine.resource.dialog.DialogDefinition;
import org.fogofwarlabs.sc4j.engine.resource.graphics.Palette;
import org.fogofwarlabs.sc4j.engine.resource.graphics.SpriteSheet;
import org.fogofwarlabs.sc4j.engine.resource.graphics.TextureSheet;
import org.fogofwarlabs.sc4j.engine.resource.graphics.TileGraphics;
import org.fogofwarlabs.sc4j.engine.resource.text.FontFace;
import org.fogofwarlabs.sc4j.engine.resource.text.StringTable;
import org.fogofwarlabs.sc4j.engine.save.SaveFormatFactory;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

/**
 * Tests the registration of a mod's resource provider (one per mod), content, formats and how it runs (an own application, or a first
 * scene and a headless runner), only while the registration is open.
 */
final class RuntimeModRegistrationTest {
    /**
     * A provider without resources.
     */
    private static final class EmptyProvider implements ResourceProvider {
        @Override
        public @NotNull SpriteSheet loadSpriteSheet(@NotNull ResourceKey key) throws IOException {
            throw ResourceNotFoundException.create(ResourceType.SPRITE_SHEET, key);
        }

        @Override
        public @NotNull Palette loadPalette(@NotNull ResourceKey key) throws IOException {
            throw ResourceNotFoundException.create(ResourceType.PALETTE, key);
        }

        @Override
        public @NotNull TextureSheet loadTextureSheet(@NotNull ResourceKey key) throws IOException {
            throw ResourceNotFoundException.create(ResourceType.TEXTURE_SHEET, key);
        }

        @Override
        public @NotNull TileGraphics loadTileGraphics(@NotNull ResourceKey key) throws IOException {
            throw ResourceNotFoundException.create(ResourceType.TILE_GRAPHICS, key);
        }

        @Override
        public @NotNull AnimationLibrary loadAnimations() throws IOException {
            throw ResourceNotFoundException.create(ResourceType.ANIMATION_LIBRARY, null);
        }

        @Override
        public @NotNull SoundClip loadSound(@NotNull ResourceKey key) throws IOException {
            throw ResourceNotFoundException.create(ResourceType.SOUND, key);
        }

        @Override
        public @NotNull StringTable loadStringTable(@NotNull ResourceKey key) throws IOException {
            throw ResourceNotFoundException.create(ResourceType.STRING_TABLE, key);
        }

        @Override
        public @NotNull FontFace loadFont(@NotNull ResourceKey key) throws IOException {
            throw ResourceNotFoundException.create(ResourceType.FONT, key);
        }

        @Override
        public @NotNull DialogDefinition loadDialog(@NotNull ResourceKey key) throws IOException {
            throw ResourceNotFoundException.create(ResourceType.DIALOG, key);
        }
    }

    @Test
    void register_resource_provider_should_accept_one_provider_while_open() {
        RuntimeModRegistration registration = new RuntimeModRegistration();
        ResourceProvider provider = new EmptyProvider();

        Assertions.assertNull(registration.getResourceProvider());

        registration.registerResourceProvider(provider);

        Assertions.assertSame(provider, registration.getResourceProvider());
        Assertions.assertThrows(IllegalStateException.class, () -> registration.registerResourceProvider(new EmptyProvider()));

        RuntimeModRegistration closed = new RuntimeModRegistration();

        closed.close();

        Assertions.assertThrows(IllegalStateException.class, () -> closed.registerResourceProvider(provider));
    }

    @Test
    void register_formats_should_keep_the_registration_order_while_open() {
        RuntimeModRegistration registration = new RuntimeModRegistration();
        ReplayFormatFactory replay = identity -> Assertions.fail("the registration creates no format");
        SaveFormatFactory first = identity -> Assertions.fail("the registration creates no format");
        SaveFormatFactory second = identity -> Assertions.fail("the registration creates no format");

        registration.registerReplayFormat(replay);
        registration.registerSaveFormat(first);
        registration.registerSaveFormat(second);
        registration.close();

        Assertions.assertEquals(List.of(replay), registration.getReplayFormats());
        Assertions.assertEquals(List.of(first, second), registration.getSaveFormats());
        Assertions.assertThrows(IllegalStateException.class, () -> registration.registerReplayFormat(replay));
        Assertions.assertThrows(IllegalStateException.class, () -> registration.registerSaveFormat(first));
    }

    @Test
    void register_first_scene_should_exclude_an_own_application() {
        RuntimeModRegistration engineOnly = new RuntimeModRegistration();
        RuntimeModRegistration ownLoop = new RuntimeModRegistration();
        FirstSceneFactory scene = client -> Assertions.fail("the registration creates no scene");
        HeadlessRunner runner = launch -> Assertions.fail("the registration runs nothing");
        ModApplication application = new ModApplication() {
            @Override
            public boolean isHeadlessCapable() {
                return true;
            }

            @Override
            public void run(@NotNull ModLaunch launch) {
                Assertions.fail("the registration runs nothing");
            }
        };

        engineOnly.registerFirstScene(scene);
        engineOnly.registerHeadlessRunner(runner);

        Assertions.assertSame(scene, engineOnly.getFirstScene());
        Assertions.assertSame(runner, engineOnly.getHeadlessRunner());
        Assertions.assertThrows(IllegalStateException.class, () -> engineOnly.registerFirstScene(scene));
        Assertions.assertThrows(IllegalStateException.class, () -> engineOnly.registerHeadlessRunner(runner));
        Assertions.assertThrows(IllegalStateException.class, () -> engineOnly.registerApplication(application));

        ownLoop.registerApplication(application);

        Assertions.assertThrows(IllegalStateException.class, () -> ownLoop.registerFirstScene(scene));
        Assertions.assertThrows(IllegalStateException.class, () -> ownLoop.registerHeadlessRunner(runner));
        Assertions.assertNull(ownLoop.getFirstScene());

        RuntimeModRegistration closed = new RuntimeModRegistration();

        closed.close();

        Assertions.assertThrows(IllegalStateException.class, () -> closed.registerFirstScene(scene));
        Assertions.assertThrows(IllegalStateException.class, () -> closed.registerHeadlessRunner(runner));
    }

    @Test
    void get_content_should_register_into_the_game_definition_while_open() {
        RuntimeModRegistration registration = new RuntimeModRegistration();
        UnitType worker = registration.getContent().getUnitTypes().register(new UnitType("worker"), new UnitDefinition.Builder().build());

        Assertions.assertSame(worker, registration.buildGameDefinition().getContent().getUnitTypes().getTypes().get(0));
        Assertions.assertThrows(IllegalStateException.class, registration::getContent);
    }
}
