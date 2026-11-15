package org.fogofwarlabs.sc4j.engine.client.skin;

import org.fogofwarlabs.sc4j.engine.client.SkirmishSessionScenario;
import org.fogofwarlabs.sc4j.engine.client.command.OrderAction;
import org.fogofwarlabs.sc4j.engine.client.command.TrainAction;
import org.fogofwarlabs.sc4j.engine.client.display.DisplayScale;
import org.fogofwarlabs.sc4j.engine.client.display.UiSurface;
import org.fogofwarlabs.sc4j.engine.client.game.GameScreen;
import org.fogofwarlabs.sc4j.engine.client.game.GameSession;
import org.fogofwarlabs.sc4j.engine.client.hud.CommandButton;
import org.fogofwarlabs.sc4j.engine.client.input.Key;
import org.fogofwarlabs.sc4j.engine.client.projection.SquareProjection;
import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.client.render.PictureRenderer;
import org.fogofwarlabs.sc4j.engine.client.render.image.ImageCanvas;
import org.fogofwarlabs.sc4j.engine.client.text.Font;
import org.fogofwarlabs.sc4j.engine.client.text.TextStyle;
import org.fogofwarlabs.sc4j.engine.client.view.GameView;
import org.fogofwarlabs.sc4j.engine.client.world.TerrainColors;
import org.fogofwarlabs.sc4j.engine.content.faction.Faction;
import org.fogofwarlabs.sc4j.engine.content.type.ContentType;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.SkirmishScenario;
import org.fogofwarlabs.sc4j.engine.order.IssueOrderCommandType;
import org.fogofwarlabs.sc4j.engine.pack.StandardPack;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.fogofwarlabs.sc4j.engine.production.TrainUnitCommandType;
import org.fogofwarlabs.sc4j.engine.resource.graphics.Picture;
import org.fogofwarlabs.sc4j.engine.resource.graphics.PixelLayer;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

/**
 * Tests the HUD skin on the offscreen game screen of the skirmish map: without a skin the blocks keep the engine default look; with one they
 * draw its boxes (a nine-slice button), the order icon on its command button, the portrait in the owner's team colour in the selection panel
 * and the icons in the resource bar; a skin table chooses the skin by the local player's faction.
 */
final class HudSkinTest {
    private static final int WIDTH = 800;
    private static final int HEIGHT = 600;
    private static final int LINE_HEIGHT = 6;
    private static final int FRAMES = 8;
    private static final int COMMAND_ID = 0;
    private static final int SCOUT_X = 400;
    private static final int SCOUT_Y = 144;
    private static final int RGB_MASK = 0xFFFFFF;
    private static final int OPAQUE = 0xFF000000;
    private static final int DEFAULT_BUTTON = 0x303848;
    private static final int DEFAULT_PENDING_BUTTON = 0x607030;
    private static final int DEFAULT_BUTTON_OUTLINE = 0x8090A0;
    private static final int CARD_COLOR = 0x112233;
    private static final int BUTTON_BORDER_COLOR = 0x203040;
    private static final int BUTTON_CENTER_COLOR = 0x405060;
    private static final int PRESSED_COLOR = 0x445566;
    private static final int ICON_COLOR = 0xFFCC00;
    private static final int GOLD_COLOR = 0xEEDD00;
    private static final int SUPPLY_COLOR = 0x00DDEE;
    private static final int RGBA_SHIFT = 8;
    private static final int OPAQUE_ALPHA = 0xFF;
    /**
     * The first button of the command card a section gap from the bottom-right corner of the 160 high console, the card a third of the
     * 628 wide room right of the 148 minimap (209 by 148, buttons 63 by 42): its top-left corner (the first whole pixel), a pixel inside its
     * box outside the fitted icon, and its centre; and a pixel of the card's box right of the buttons.
     */
    private static final int BUTTON_LEFT = (int) Math.ceil(WIDTH - 6 - (WIDTH - 148 - 4 * 6) / 3f + 6);
    private static final int BUTTON_TOP = HEIGHT - 160 + 6 + 6;
    private static final int BUTTON_INSIDE_X = BUTTON_LEFT + 4;
    private static final int BUTTON_INSIDE_Y = BUTTON_TOP + 4;
    private static final int BUTTON_CENTER_X = BUTTON_LEFT + 31;
    private static final int BUTTON_CENTER_Y = BUTTON_TOP + 21;
    private static final int CARD_BOX_X = WIDTH - 8;
    private static final int CARD_BOX_Y = HEIGHT - 8;
    /**
     * The centre of the portrait slot of one selected unit: the left of the selection panel (right of the 148 minimap and two section
     * gaps) plus the padding plus half the 128 slot, which stands below the name line.
     */
    private static final int PORTRAIT_CENTER_X = 6 + 148 + 6 + 6 + 64;
    private static final int PORTRAIT_CENTER_Y = HEIGHT - 160 + 6 + 6 + LINE_HEIGHT + 2 + 64;
    /**
     * The icons of the 24 pixel resource bar with a font that measures nothing: 20 pixel squares, the supply's ending 4 pixels before the
     * right padding of 6, the gold's 16 pixels further left.
     */
    private static final int SUPPLY_ICON_X = WIDTH - 6 - 4 - 10;
    private static final int GOLD_ICON_X = WIDTH - 6 - 4 - 20 - 16 - 4 - 10;
    private static final int ICON_Y = 12;

    /**
     * Measures nothing and draws nothing: the tests look at the boxes and pictures only.
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

    private static @NotNull Picture createPicture(int color) {
        return new Picture(1, 1, PixelLayer.ofRgba(new int[]{OPAQUE | color}));
    }

    /**
     * Creates a {@link Picture} instance of three by three pixels whose border pixels and centre pixel differ, with one pixel borders.
     *
     * @return the created {@link Picture} instance
     */
    private static @NotNull Picture createFrame() {
        int[] colors = new int[9];

        Arrays.fill(colors, OPAQUE | BUTTON_BORDER_COLOR);
        colors[4] = OPAQUE | BUTTON_CENTER_COLOR;

        return new Picture(3, 3, PixelLayer.ofRgba(colors), null, 0, 0, 1, 1, 1, 1);
    }

    /**
     * Creates a {@link Picture} instance of a black portrait whose mask takes the whole team colour.
     *
     * @return the created {@link Picture} instance
     */
    private static @NotNull Picture createPortrait() {
        return new Picture(2, 2, PixelLayer.ofRgba(new int[]{OPAQUE, OPAQUE, OPAQUE, OPAQUE}), PixelLayer.ofAlpha(new byte[]{-1, -1, -1, -1}),
                           0, 0, 0, 0, 0, 0);
    }

    private static int toRgba(int color) {
        return color << RGBA_SHIFT | OPAQUE_ALPHA;
    }

    private static int readColor(@NotNull ImageCanvas canvas, int x, int y) {
        return canvas.getImage().getRGB(x, y) & RGB_MASK;
    }

    private static @NotNull HudSkin createSkin(@NotNull PictureRenderer renderer, @NotNull SkirmishScenario scenario, @NotNull Player owner,
                                               @NotNull UnitType type, @NotNull GameView view) {
        ButtonStyle buttons = new ButtonStyle(new PictureBoxStyle(renderer, createFrame()), new PictureBoxStyle(renderer, createFrame()),
                                              new ColorBoxStyle(toRgba(PRESSED_COLOR)), new PictureBoxStyle(renderer, createFrame()));
        Faction faction = view.getFaction(owner);

        Assertions.assertNotNull(faction);

        return HudSkin.builder(renderer)
                      .setCommandCardBackground(new ColorBoxStyle(toRgba(CARD_COLOR)))
                      .setCommandButton(buttons)
                      .setMinimapBorder(ColorBoxStyle.NONE)
                      .setOrderIcon(scenario.getMove(), createPicture(ICON_COLOR))
                      .setPortrait(type, createPortrait())
                      .setResourceIcon(scenario.getGold(), createPicture(GOLD_COLOR))
                      .setSupplyIcon(faction, createPicture(SUPPLY_COLOR))
                      .build();
    }

    @Test
    void hud_skin_draws_boxes_icons_and_portraits_and_the_default_keeps_the_engine_look(@TempDir @NotNull Path folder) throws IOException {
        SkirmishSessionScenario skirmish = new SkirmishSessionScenario();
        GameSession session = skirmish.getSession();
        GameView view = session.getView();

        skirmish.runFrames(FRAMES);

        Unit scout = view.findUnitAt(SCOUT_X, SCOUT_Y);

        Assertions.assertNotNull(scout);

        Player owner = view.getUnitView(scout).getOwner();
        UnitType type = view.getUnitView(scout).getType();

        Assertions.assertNotNull(owner);
        Assertions.assertNotNull(type);

        OrderAction move = new OrderAction(session.getGame(), new IssueOrderCommandType(COMMAND_ID, 0), skirmish.getScenario().getMove(), true);
        ImageCanvas canvas = new ImageCanvas(WIDTH, HEIGHT);

        Files.writeString(folder.resolve("pack.json"), "{\"format\": 1}", StandardCharsets.UTF_8);

        try (StandardPack pack = StandardPack.open(folder);
             PictureRenderer renderer = new PictureRenderer(canvas, pack);
             GameScreen screen = new GameScreen(session, canvas, new EmptyFont(), new SquareProjection(), new TerrainColors(),
                                                ContentType::getDebugName, "supply", ContentType::getDebugName)) {
            screen.setCommands(List.of(new CommandButton("Move", Key.M, move)), move);
            screen.getSelection().select(scout);
            screen.draw(UiSurface.of(WIDTH, HEIGHT, DisplayScale.ONE));

            Assertions.assertEquals(DEFAULT_BUTTON_OUTLINE, readColor(canvas, BUTTON_LEFT, BUTTON_TOP), "the default button outline");
            Assertions.assertEquals(DEFAULT_BUTTON, readColor(canvas, BUTTON_INSIDE_X, BUTTON_INSIDE_Y), "the default button fill");

            screen.getTranslator().run(move, false);
            screen.draw(UiSurface.of(WIDTH, HEIGHT, DisplayScale.ONE));

            Assertions.assertEquals(DEFAULT_PENDING_BUTTON, readColor(canvas, BUTTON_INSIDE_X, BUTTON_INSIDE_Y), "the default pending fill");

            screen.getTranslator().cancel();
            screen.setSkin(createSkin(renderer, skirmish.getScenario(), owner, type, view));
            screen.draw(UiSurface.of(WIDTH, HEIGHT, DisplayScale.ONE));

            Assertions.assertEquals(CARD_COLOR, readColor(canvas, CARD_BOX_X, CARD_BOX_Y), "the card box");
            Assertions.assertEquals(BUTTON_BORDER_COLOR, readColor(canvas, BUTTON_LEFT, BUTTON_TOP), "the sliced button corner");
            Assertions.assertEquals(BUTTON_CENTER_COLOR, readColor(canvas, BUTTON_INSIDE_X, BUTTON_INSIDE_Y), "the stretched button centre");
            Assertions.assertEquals(ICON_COLOR, readColor(canvas, BUTTON_CENTER_X, BUTTON_CENTER_Y), "the fitted order icon");
            Assertions.assertEquals(view.getTeamColor(owner), readColor(canvas, PORTRAIT_CENTER_X, PORTRAIT_CENTER_Y), "the portrait in team colour");
            Assertions.assertEquals(SUPPLY_COLOR, readColor(canvas, SUPPLY_ICON_X, ICON_Y), "the supply icon");
            Assertions.assertEquals(GOLD_COLOR, readColor(canvas, GOLD_ICON_X, ICON_Y), "the gold icon");

            screen.getTranslator().run(move, false);
            screen.draw(UiSurface.of(WIDTH, HEIGHT, DisplayScale.ONE));

            Assertions.assertEquals(PRESSED_COLOR, readColor(canvas, BUTTON_INSIDE_X, BUTTON_INSIDE_Y), "the pending button is pressed");
        }
    }

    @Test
    void hud_skin_table_chooses_the_skin_of_the_local_player_faction() throws IOException {
        SkirmishSessionScenario skirmish = new SkirmishSessionScenario();
        SkirmishScenario scenario = skirmish.getScenario();
        HudSkin wardens = HudSkin.builder().setCommandCardBackground(new ColorBoxStyle(toRgba(CARD_COLOR))).build();
        HudSkinTable table = new HudSkinTable(HudSkin.DEFAULT);
        ImageCanvas canvas = new ImageCanvas(WIDTH, HEIGHT);

        table.setFactionSkin(scenario.getWardens(), wardens);

        Assertions.assertSame(wardens, table.findSkin(scenario.getWardens()));
        Assertions.assertSame(HudSkin.DEFAULT, table.findSkin(scenario.getRaiders()));
        Assertions.assertSame(HudSkin.DEFAULT, table.findSkin(null));
        Assertions.assertThrows(IllegalStateException.class,
                                () -> HudSkin.builder().setOrderIcon(scenario.getMove(), createPicture(ICON_COLOR)).build(),
                                "pictures need a renderer");
        Assertions.assertEquals(HudSkin.DEFAULT_TOOLTIP_PADDING, HudSkin.DEFAULT.getTooltipPadding());
        Assertions.assertEquals(HudSkin.DEFAULT_DIALOG_PADDING, HudSkin.DEFAULT.getDialogPadding());
        Assertions.assertThrows(IllegalArgumentException.class, () -> HudSkin.builder().setTooltip(ColorBoxStyle.NONE, -1));
        Assertions.assertThrows(IllegalArgumentException.class, () -> HudSkin.builder().setDialog(ColorBoxStyle.NONE, -1));

        try (GameScreen screen = new GameScreen(skirmish.getSession(), canvas, new EmptyFont(), new SquareProjection(), new TerrainColors(),
                                                ContentType::getDebugName, "supply", ContentType::getDebugName)) {
            screen.setSkins(table);
            screen.update(0);
            screen.draw(UiSurface.of(WIDTH, HEIGHT, DisplayScale.ONE));

            Assertions.assertSame(wardens, screen.getCommandCard().getSkin(), "the local player plays the wardens");
            Assertions.assertEquals(CARD_COLOR, readColor(canvas, CARD_BOX_X, CARD_BOX_Y));
        }
    }

    @Test
    void find_button_icon_should_map_orders_to_their_icons_and_production_to_the_unit_icon(@TempDir @NotNull Path folder) throws IOException {
        SkirmishSessionScenario skirmish = new SkirmishSessionScenario();
        SkirmishScenario scenario = skirmish.getScenario();
        Picture moveIcon = createPicture(ICON_COLOR);
        Picture keeperIcon = createPicture(GOLD_COLOR);
        Picture cancelIcon = createPicture(PRESSED_COLOR);
        Picture rallyIcon = createPicture(SUPPLY_COLOR);

        Files.writeString(folder.resolve("pack.json"), "{\"format\": 1}", StandardCharsets.UTF_8);

        try (StandardPack pack = StandardPack.open(folder);
             PictureRenderer renderer = new PictureRenderer(new ImageCanvas(1, 1), pack)) {
            HudSkin skin = HudSkin.builder(renderer).setOrderIcon(scenario.getMove(), moveIcon).setUnitIcon(scenario.getKeeper(), keeperIcon).build();
            IssueOrderCommandType orders = new IssueOrderCommandType(COMMAND_ID, 0);

            Assertions.assertSame(moveIcon, skin.findButtonIcon(new OrderAction(skirmish.getSession().getGame(), orders, scenario.getMove(), true)));
            Assertions.assertNull(skin.findButtonIcon(new OrderAction(skirmish.getSession().getGame(), orders, scenario.getPatrol(), true)));
            Assertions.assertSame(keeperIcon,
                                  skin.findButtonIcon(new TrainAction(skirmish.getSession().getGame(), new TrainUnitCommandType(COMMAND_ID),
                                                                      scenario.getKeeper())));
            Assertions.assertNull(skin.findCancelIcon());
            Assertions.assertNull(skin.findRallyIcon());

            HudSkin icons = HudSkin.builder(renderer).setCancelIcon(cancelIcon).setRallyIcon(rallyIcon).build();

            Assertions.assertSame(cancelIcon, icons.findCancelIcon());
            Assertions.assertSame(rallyIcon, icons.findRallyIcon());
        }
    }
}
