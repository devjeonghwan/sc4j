package org.fogofwarlabs.sc4j.engine.client.cursor;

import org.fogofwarlabs.sc4j.engine.client.SkirmishSessionScenario;
import org.fogofwarlabs.sc4j.engine.client.command.OrderAction;
import org.fogofwarlabs.sc4j.engine.client.display.DetailSet;
import org.fogofwarlabs.sc4j.engine.client.display.DisplayScale;
import org.fogofwarlabs.sc4j.engine.client.display.UiSurface;
import org.fogofwarlabs.sc4j.engine.client.game.GameScreen;
import org.fogofwarlabs.sc4j.engine.client.game.GameSession;
import org.fogofwarlabs.sc4j.engine.client.input.InputEvent;
import org.fogofwarlabs.sc4j.engine.client.projection.SquareProjection;
import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.client.render.image.ImageCanvas;
import org.fogofwarlabs.sc4j.engine.client.text.Font;
import org.fogofwarlabs.sc4j.engine.client.text.TextStyle;
import org.fogofwarlabs.sc4j.engine.client.world.TerrainColors;
import org.fogofwarlabs.sc4j.engine.content.type.ContentType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.order.IssueOrderCommandType;
import org.fogofwarlabs.sc4j.engine.resource.graphics.Picture;
import org.fogofwarlabs.sc4j.engine.resource.graphics.PixelLayer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests the cursor choice without window: the controller that shows a picture only when it changes, and the cursor contexts of the offscreen
 * game screen of the skirmish map (edges, HUD, waiting action, hovered unit).
 */
final class CursorControllerTest {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int LINE_HEIGHT = 6;
    private static final int FRAMES = 8;
    private static final int COMMAND_ID = 0;
    private static final int SCOUT_X = 400;
    private static final int SCOUT_Y = 144;
    private static final int GROUND_X = 500;
    private static final int GROUND_Y = 200;
    private static final int EDGE = 1;
    private static final int MINIMAP_X = 80;
    private static final int PANEL_X = 300;
    private static final int PANEL_Y = HEIGHT - 80;
    private static final @NotNull String DEFAULT_SHOWN = "default";

    /**
     * Measures nothing and draws nothing.
     */
    private static final class EmptyFont implements Font {
        @Override
        public int getLineHeight() {
            return LINE_HEIGHT;
        }

        @Override
        public @NotNull TextStyle getDefaultStyle() {
            return TextStyle.DEFAULT;
        }

        @Override
        public float measure(@NotNull CharSequence text, @NotNull TextStyle style) {
            return 0;
        }

        @Override
        public float draw(@NotNull Canvas canvas, @NotNull CharSequence text, float x, float y, int color, @NotNull TextStyle style) {
            return x;
        }
    }

    /**
     * Records every picture the controller passes on, and the platform arrow as {@link #DEFAULT_SHOWN}.
     */
    private static final class RecordingSurface implements CursorSurface {
        private final @NotNull List<@NotNull Object> shown;

        private RecordingSurface() {
            this.shown = new ArrayList<>();
        }

        @Override
        public boolean isAvailable() {
            return true;
        }

        @Override
        public void setScale(int scale) {
        }

        @Override
        public void show(@NotNull DetailSet<Picture> pictures) {
            this.shown.add(pictures);
        }

        @Override
        public void showDefault() {
            this.shown.add(DEFAULT_SHOWN);
        }

        @Override
        public void hide() {
            this.shown.clear();
        }

        @Override
        public void close() {
            this.shown.clear();
        }
    }

    private static @NotNull DetailSet<Picture> createCursor(int hotSpot) {
        return DetailSet.of(new Picture(2, 2, PixelLayer.ofRgba(new int[4]), null, hotSpot, hotSpot, 0, 0, 0, 0));
    }

    private static @NotNull CursorContext pointAt(@NotNull GameScreen screen, float x, float y) {
        screen.handleInput(InputEvent.createPointerMoved(x, y));
        screen.update(0);

        return screen.findCursorContext();
    }

    @Test
    void headless_surface_is_never_available_and_forgets_a_hidden_picture() {
        HeadlessCursorSurface surface = new HeadlessCursorSurface();
        DetailSet<Picture> arrow = createCursor(0);

        surface.show(arrow);

        Assertions.assertFalse(surface.isAvailable());
        Assertions.assertSame(arrow, surface.getShown());

        surface.hide();

        Assertions.assertNull(surface.getShown());
    }

    @Test
    void update_should_show_a_picture_only_when_it_changes() {
        DetailSet<Picture> arrow = createCursor(0);
        DetailSet<Picture> attack = createCursor(1);
        CursorSet cursors = new CursorSet();
        RecordingSurface surface = new RecordingSurface();
        CursorController controller = new CursorController(surface);

        cursors.setCursor(CursorContext.NORMAL, arrow);
        cursors.setCursor(CursorContext.HOVER_ENEMY, attack);
        controller.update(CursorContext.NORMAL, null);
        controller.setCursors(cursors);
        controller.update(CursorContext.NORMAL, null);
        controller.update(CursorContext.HOVER_FRIEND, null);
        controller.update(CursorContext.HOVER_ENEMY, null);
        controller.update(CursorContext.HOVER_ENEMY, null);

        Assertions.assertEquals(List.of(DEFAULT_SHOWN, arrow, attack), surface.shown, "the friend falls back to the shown arrow");
        Assertions.assertSame(attack, controller.getShown());
        Assertions.assertEquals(CursorContext.HOVER_ENEMY, controller.getContext());
    }

    @Test
    void game_screen_finds_the_cursor_context_of_the_pointer_and_shows_it() throws IOException {
        SkirmishSessionScenario skirmish = new SkirmishSessionScenario();
        GameSession session = skirmish.getSession();

        skirmish.runFrames(FRAMES);

        Unit scout = session.getView().findUnitAt(SCOUT_X, SCOUT_Y);
        OrderAction move = new OrderAction(session.getGame(), new IssueOrderCommandType(COMMAND_ID, 0), skirmish.getScenario().getMove(), true);
        DetailSet<Picture> moveCursor = createCursor(1);
        CursorSet cursors = new CursorSet();
        HeadlessCursorSurface surface = new HeadlessCursorSurface();
        CursorController controller = new CursorController(surface);

        Assertions.assertNotNull(scout);

        cursors.setOrderCursor(skirmish.getScenario().getMove(), moveCursor);
        controller.setCursors(cursors);

        try (GameScreen screen = new GameScreen(session, new ImageCanvas(WIDTH, HEIGHT), new EmptyFont(), new SquareProjection(), new TerrainColors(),
                                                ContentType::getDebugName, "supply", ContentType::getDebugName)) {
            Assertions.assertEquals(CursorContext.NORMAL, screen.findCursorContext(), "no pointer yet");

            screen.draw(UiSurface.of(WIDTH, HEIGHT, DisplayScale.ONE));
            screen.setCursor(controller);

            Assertions.assertEquals(CursorContext.NORMAL, pointAt(screen, GROUND_X, GROUND_Y));
            Assertions.assertEquals(CursorContext.HOVER_FRIEND, pointAt(screen, SCOUT_X, SCOUT_Y), "the local scout");
            Assertions.assertEquals(CursorContext.NORMAL, pointAt(screen, 0, GROUND_Y), "no scroll past the map's left edge");
            Assertions.assertEquals(CursorContext.SCROLL_RIGHT, pointAt(screen, WIDTH - EDGE, 0), "no scroll past the map's top edge");
            Assertions.assertEquals(CursorContext.SCROLL_DOWN_RIGHT, pointAt(screen, WIDTH - EDGE, HEIGHT - EDGE));
            Assertions.assertEquals(CursorContext.NORMAL, pointAt(screen, PANEL_X, PANEL_Y), "the selection panel");

            screen.getSelection().select(scout);
            screen.getTranslator().run(move, false);

            Assertions.assertEquals(CursorContext.TARGET, pointAt(screen, MINIMAP_X, PANEL_Y), "the minimap takes targets");
            Assertions.assertEquals(CursorContext.NORMAL, pointAt(screen, PANEL_X, PANEL_Y));
            Assertions.assertEquals(CursorContext.TARGET, pointAt(screen, GROUND_X, GROUND_Y));

            screen.update(0);

            Assertions.assertSame(moveCursor, surface.getShown(), "the waiting move shows its order cursor");
            Assertions.assertEquals(CursorContext.TARGET, controller.getContext());

            screen.getTranslator().cancel();
            screen.update(0);

            Assertions.assertNull(surface.getShown(), "the platform arrow without a normal cursor");
        }
    }
}
