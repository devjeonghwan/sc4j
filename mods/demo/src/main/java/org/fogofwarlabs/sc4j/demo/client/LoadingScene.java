package org.fogofwarlabs.sc4j.demo.client;

import org.fogofwarlabs.sc4j.engine.client.app.ClientScene;
import org.fogofwarlabs.sc4j.engine.client.app.SceneClient;
import org.fogofwarlabs.sc4j.engine.client.cursor.CursorContext;
import org.fogofwarlabs.sc4j.engine.client.input.InputEvent;
import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.client.text.Font;
import org.fogofwarlabs.sc4j.engine.content.faction.Faction;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;

/**
 * Shows the loading picture of the person's faction while a local game starts: the picture over the whole screen, the faction's emblem and the
 * game's description on a dark band near the bottom. Once it has been drawn, it hands over to the game scene, which creates the game when it
 * opens; the loading picture stays on the screen until the game's first frame.
 */
public final class LoadingScene implements ClientScene {
    private static final float EMBLEM_SIZE = 128;
    private static final float BAND_HEIGHT = 56;
    private static final float BOTTOM_MARGIN = 48;
    private static final float GAP = 16;
    private static final int BAND_COLOR = 0x000000A0;
    private static final float HALF = 2;

    private final @NotNull DemoClient demo;
    private final @NotNull Faction faction;
    private final @NotNull String description;
    private final @NotNull ClientScene game;

    private boolean drawn;
    private boolean handedOver;

    /**
     * Creates a {@link LoadingScene} instance.
     *
     * @param demo        the demo's shared services
     * @param faction     the person's faction, whose loading picture and emblem show
     * @param description the game's description
     * @param game        the scene of the game, shown next
     */
    public LoadingScene(@NotNull DemoClient demo, @NotNull Faction faction, @NotNull String description, @NotNull ClientScene game) {
        this.demo        = demo;
        this.faction     = faction;
        this.description = description;
        this.game        = game;

        this.drawn      = false;
        this.handedOver = false;
    }

    /**
     * @return the scene of the game, shown next
     */
    public @NotNull ClientScene getGame() {
        return this.game;
    }

    /**
     * Opens the scene: loads what it shows. Called once, before the first {@link #update}.
     * <p>
     * Shows the arrow cursor; the pictures are read when they are drawn.
     *
     * @param context the client's shared services
     */
    @Override
    public void open(@NotNull SceneClient context) {
        this.demo.getSkin().getCursor().update(CursorContext.NORMAL, null);
    }

    /**
     * Updates the scene at the supplied time.
     * <p>
     * Hands over to the game scene once the loading picture has been drawn.
     *
     * @param time the client time in milliseconds
     */
    @Override
    public void update(long time) {
        if (this.drawn && !this.handedOver) {
            this.handedOver = true;
            this.demo.getClient().getScenes().replace(this.game);
        }
    }

    /**
     * Draws the scene: the faction's loading picture, its emblem and the game's description.
     *
     * @param surface the surface
     * @throws IOException if a picture cannot be read
     */
    @Override
    public void draw(@NotNull Canvas surface) throws IOException {
        DemoSkin skin = this.demo.getSkin();
        Font font = this.demo.getFont();
        float width = surface.getWidth();
        float bandTop = surface.getHeight() - BOTTOM_MARGIN - BAND_HEIGHT;

        skin.drawLoadingScreen(surface, this.faction);
        skin.drawEmblem(surface, this.faction, width / HALF, bandTop - GAP - EMBLEM_SIZE / HALF, EMBLEM_SIZE);
        surface.fillRectangle(0, bandTop, width, BAND_HEIGHT, BAND_COLOR);
        font.draw(surface, this.description, (width - font.measure(this.description)) / HALF, bandTop + (BAND_HEIGHT - font.getLineHeight()) / HALF,
                  this.demo.getTheme().getTextColor());

        this.drawn = true;
    }

    /**
     * Handles an input event the scene's stack hands to it: the loading screen takes none.
     *
     * @param event the event
     * @return {@code false}, the scene uses no event
     */
    @Override
    public boolean handleInput(@NotNull InputEvent event) {
        return false;
    }

    /**
     * Closes the scene: releases what it holds. The pictures belong to the demo's skin.
     */
    @Override
    public void close() {
        // The game scene owns the game; the skin owns the pictures.
    }
}
