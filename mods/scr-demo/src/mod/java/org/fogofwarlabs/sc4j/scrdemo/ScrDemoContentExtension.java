package org.fogofwarlabs.sc4j.scrdemo;

import org.fogofwarlabs.sc4j.scr.data.resource.ScrModFiles;
import org.fogofwarlabs.sc4j.scr.game.extension.ScrContentExtension;
import org.fogofwarlabs.sc4j.scr.game.state.GameData;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Extends the game data of demo 2: the added units, Unity-chan ({@link ScrDemoUnitUtils}) and the Myriarch ({@link ScrDemoMyriarchUtils}),
 * join the installed data, and the demo's values ({@link ScrDemoValueUtils}) are written into every game's own tables. The demo's jar names it
 * in {@code META-INF/services/org.fogofwarlabs.sc4j.scr.game.extension.ScrContentExtension}.
 */
public final class ScrDemoContentExtension implements ScrContentExtension {
    /**
     * Creates a {@link ScrDemoContentExtension} instance.
     */
    public ScrDemoContentExtension() {
    }

    /**
     * Extends the installed data right after it is loaded, before any game is made from it: unit types, graphics and interface art added
     * through {@link GameData#getAddedUnitTypes()} and {@link GameData#getAddedGraphics()} belong here, so every game, replay and screen of
     * the session sees them.
     *
     * @param installed the installed data
     * @throws IOException if a file the extension reads cannot be read
     */
    @Override
    public void extendInstalledData(@NotNull GameData installed) throws IOException {
        ScrModFiles files = ScrDemoUnitGraphics.createFiles(installed);

        // Unity-chan first: replays and commands hold the added ids, 233 Unity-chan and 234 the Myriarch.
        ScrDemoUnitUtils.registerUnit(installed, files);
        ScrDemoMyriarchUtils.registerUnit(installed, files);
    }

    /**
     * Extends a game's own tables right after the game is created over its copy of the installed tables, before a Use Map Settings map
     * writes its unit, upgrade and technology settings, so such a map's own values still replace the values written here. Every game, live
     * or replay, passes here.
     *
     * @param game the game's own data
     */
    @Override
    public void extendGameData(@NotNull GameData game) {
        ScrDemoValueUtils.applyValues(game);
    }
}
