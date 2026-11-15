package org.fogofwarlabs.sc4j.engine.client.app;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.client.audio.AudioMixer;
import org.fogofwarlabs.sc4j.engine.client.audio.SilentAudioDevice;
import org.fogofwarlabs.sc4j.engine.client.render.image.ImageCanvas;
import org.fogofwarlabs.sc4j.engine.client.text.BitmapFont;
import org.fogofwarlabs.sc4j.engine.client.text.TextColorHook;
import org.fogofwarlabs.sc4j.engine.game.GameDefinition;
import org.fogofwarlabs.sc4j.engine.mod.ModLaunch;
import org.fogofwarlabs.sc4j.engine.mod.ModStorage;
import org.fogofwarlabs.sc4j.engine.mod.identity.ModIdentity;
import org.fogofwarlabs.sc4j.engine.replay.ReplayFormat;
import org.fogofwarlabs.sc4j.engine.resource.graphics.PixelLayer;
import org.fogofwarlabs.sc4j.engine.resource.text.FontFace;
import org.fogofwarlabs.sc4j.engine.resource.text.FontGlyph;
import org.fogofwarlabs.sc4j.manifest.ManifestException;
import org.fogofwarlabs.sc4j.manifest.ModManifest;
import org.fogofwarlabs.sc4j.manifest.ModManifestReader;
import org.fogofwarlabs.sc4j.manifest.version.EngineVersion;
import org.jetbrains.annotations.NotNull;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Creates the parts of an offscreen engine client: a launch request of a small mod without simulation, a mod storage folder, an image canvas,
 * a silent audio mixer and a font whose printable ASCII glyphs are filled squares.
 */
public final class ClientLaunchScenario {
    /**
     * The width of the offscreen frames.
     */
    public static final int WIDTH = 800;
    /**
     * The height of the offscreen frames.
     */
    public static final int HEIGHT = 600;

    private static final int DIGEST_LENGTH = 32;
    private static final int GLYPH_SIZE = 4;
    private static final int GLYPH_ADVANCE = 5;
    private static final int LINE_HEIGHT = 6;
    private static final char FIRST_GLYPH = '!';
    private static final char LAST_GLYPH = '~';
    private static final @NotNull String MANIFEST = "{\"format\": 1, \"id\": \"scenes\", \"name\": {\"en\": \"Scenes\"}, \"version\": \"1.0.0\"," +
                                                    " \"engine\": \"0.1\", \"entry\": \"org.example.ScenesMod\"}";

    /**
     * Stores the mod's files under a folder.
     */
    private static final class FolderStorage implements ModStorage {
        private final @NotNull Path root;

        private FolderStorage(@NotNull Path root) {
            this.root = root;
        }

        @Override
        public @NotNull Path getRoot() {
            return this.root;
        }

        @Override
        public @NotNull Path resolve(@NotNull String relativePath) {
            Path resolved = this.root.resolve(relativePath).normalize();

            if (!resolved.startsWith(this.root)) {
                throw Exceptions.newIllegalArgumentException("The path {0} leaves the storage.", relativePath);
            }

            return resolved;
        }
    }

    /**
     * Creates a {@link ModLaunch} instance of a windowed run without replay.
     *
     * @param replayFormats the mod's replay formats
     * @return the created {@link ModLaunch} instance
     * @throws ManifestException if the test manifest is invalid
     */
    public static @NotNull ModLaunch createLaunch(@NotNull List<@NotNull ReplayFormat> replayFormats) throws ManifestException {
        ModManifest manifest = ModManifestReader.parse(MANIFEST.getBytes(StandardCharsets.UTF_8));
        ModIdentity identity = ModIdentity.create(EngineVersion.CURRENT, manifest, new byte[DIGEST_LENGTH], List.of());

        return new ModLaunch(false, null, null, null, identity, new GameDefinition.Builder().build(), null, replayFormats, List.of(), null);
    }

    /**
     * Creates a {@link ModStorage} instance under a folder.
     *
     * @param root the folder
     * @return the created {@link ModStorage} instance
     */
    public static @NotNull ModStorage createStorage(@NotNull Path root) {
        return new FolderStorage(root.toAbsolutePath().normalize());
    }

    /**
     * Creates a {@link SceneClient} instance on an image canvas of {@link #WIDTH} by {@link #HEIGHT} with a silent audio mixer.
     *
     * @param launch  the launch request
     * @param storage the mod storage
     * @param canvas  the canvas
     * @return the created {@link SceneClient} instance
     */
    public static @NotNull SceneClient createClient(@NotNull ModLaunch launch, @NotNull ModStorage storage, @NotNull ImageCanvas canvas) {
        return new SceneClient(launch, storage, canvas, new AudioMixer(new SilentAudioDevice(System::currentTimeMillis)));
    }

    /**
     * Creates a {@link BitmapFont} instance whose printable ASCII glyphs are all one filled square.
     *
     * @param canvas the canvas the font draws on
     * @return the created {@link BitmapFont} instance
     */
    public static @NotNull BitmapFont createFont(@NotNull ImageCanvas canvas) {
        byte[] alphas = new byte[GLYPH_SIZE * GLYPH_SIZE];
        List<FontGlyph> glyphs = new ArrayList<>();

        Arrays.fill(alphas, (byte) -1);
        glyphs.add(new FontGlyph(' ', GLYPH_ADVANCE, 0, 0, 0, 0, 0, 0));

        for (char character = FIRST_GLYPH; character <= LAST_GLYPH; character++) {
            glyphs.add(new FontGlyph(character, GLYPH_ADVANCE, 0, 0, GLYPH_SIZE, GLYPH_SIZE, 0, 0));
        }

        return new BitmapFont(canvas, new FontFace(LINE_HEIGHT, GLYPH_SIZE, PixelLayer.ofAlpha(alphas), glyphs), TextColorHook.NONE);
    }

    private ClientLaunchScenario() {
    }
}
