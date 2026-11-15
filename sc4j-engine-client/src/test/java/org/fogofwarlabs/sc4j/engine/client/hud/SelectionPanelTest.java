package org.fogofwarlabs.sc4j.engine.client.hud;

import org.fogofwarlabs.sc4j.engine.client.SkirmishSessionScenario;
import org.fogofwarlabs.sc4j.engine.client.command.OrderedSelection;
import org.fogofwarlabs.sc4j.engine.client.input.InputEvent;
import org.fogofwarlabs.sc4j.engine.client.input.PointerButton;
import org.fogofwarlabs.sc4j.engine.client.render.Canvas;
import org.fogofwarlabs.sc4j.engine.client.render.image.ImageCanvas;
import org.fogofwarlabs.sc4j.engine.client.text.Font;
import org.fogofwarlabs.sc4j.engine.client.text.TextStyle;
import org.fogofwarlabs.sc4j.engine.client.view.GameView;
import org.fogofwarlabs.sc4j.engine.content.type.ContentType;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.content.type.WeaponType;
import org.fogofwarlabs.sc4j.engine.economy.HarvestSystem;
import org.fogofwarlabs.sc4j.engine.economy.StandardHarvestSystem;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.game.SkirmishScenario;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.fogofwarlabs.sc4j.engine.production.StandardProductionSystem;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests the engine default selection panel: one unit shows its name across the top and its hit points, armour, weapon damage and kills, its
 * order with the target and the gold it carries or holds, a building under construction its construction progress, and a group shows its
 * units grouped by type, a small group in a wide panel as cards with each unit's name, hit points and order, one too large for cells of the
 * slot size every unit in smaller cells, the hovered cell's unit named with its hit points in a tooltip over the panel, and with nothing selected
 * the panel shows the player's forces by unit type with their numbers, a press on one selecting the player's units of that type.
 */
final class SelectionPanelTest {
    private static final int WIDTH = 640;
    private static final int HEIGHT = 360;
    private static final int LINE_HEIGHT = 6;
    private static final int CHARACTER_WIDTH = 5;
    /**
     * The slot of the player the spearman kills a unit of, and how far that unit stands from it.
     */
    private static final int ENEMY_SLOT = 1;
    private static final int ENEMY_DISTANCE = 64;
    private static final float PANEL_LEFT = 10;
    private static final float PANEL_TOP = 200;
    private static final float PANEL_WIDTH = 300;
    private static final float NARROW_PANEL_WIDTH = 150;
    private static final float WIDE_PANEL_WIDTH = 600;
    private static final float PANEL_HEIGHT = 148;
    private static final float PADDING = 6;
    private static final int UNIT_X = 300;
    private static final int UNIT_Y = 300;
    private static final int GROUP_SIZE = 24;
    private static final int SMALL_GROUP_SIZE = 3;
    private static final int GOLD = 2000;
    private static final int SITE_TRIES = 16;
    private static final int GATHER_FRAMES = 100;
    private static final int MINE_DISTANCE = 40;
    private static final @NotNull String SPEARMAN = "spearman";
    private static final @NotNull String MOVING_TO = "Moving to";

    /**
     * Keeps every drawn line with its position and measures each character as one width.
     */
    private static final class RecordingFont implements Font {
        private final @NotNull List<@NotNull String> lines;
        private final @NotNull List<float @NotNull []> positions;

        private RecordingFont() {
            this.lines     = new ArrayList<>();
            this.positions = new ArrayList<>();
        }

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
            return text.length() * CHARACTER_WIDTH;
        }

        @Override
        public float draw(@NotNull Canvas canvas, @NotNull CharSequence text, float x, float y, int color, @NotNull TextStyle style) {
            this.lines.add(text.toString());
            this.positions.add(new float[]{x, y});

            return x + this.measure(text, style);
        }
    }

    private static @NotNull UnitType findSpearmanType(@NotNull Game game) {
        for (UnitType type : game.getContent().getDefinition().getUnitTypes().getTypes().getTypes()) {
            if (type.getDebugName().equals(SPEARMAN)) {
                return type;
            }
        }

        return Assertions.fail("The skirmish has a unit type " + SPEARMAN);
    }

    private static @NotNull SelectionPanel createPanel(@NotNull GameView view, @NotNull OrderedSelection selection, @NotNull Font font,
                                                       float width) {
        SelectionPanel panel = new SelectionPanel(view, selection, font, ContentType::getDebugName, ContentType::getDebugName);

        panel.setBounds(PANEL_LEFT, PANEL_TOP, width, PANEL_HEIGHT);

        return panel;
    }

    private static void draw(@NotNull SelectionPanel panel) {
        ImageCanvas canvas = new ImageCanvas(WIDTH, HEIGHT);

        canvas.begin(WIDTH, HEIGHT);
        panel.draw(canvas);
        canvas.end();
    }

    @Test
    void one_selected_unit_shows_its_name_across_the_top_and_its_stats() throws IOException {
        SkirmishSessionScenario skirmish = new SkirmishSessionScenario();
        Game game = skirmish.getSession().getGame();
        Player player = skirmish.getSession().getLocalPlayer();
        StandardProductionSystem production = (StandardProductionSystem) game.getProduction();
        UnitType spearmanType = findSpearmanType(game);

        Assertions.assertNotNull(player);

        Unit spearman = production.createUnit(player, spearmanType, UNIT_X, UNIT_Y, 0);
        WeaponType weapon = game.getContent().getUnitTypes().getGroundWeapon(spearmanType);
        OrderedSelection selection = new OrderedSelection(GROUP_SIZE);
        RecordingFont font = new RecordingFont();
        SelectionPanel panel = createPanel(skirmish.getSession().getView(), selection, font, NARROW_PANEL_WIDTH);

        Player enemy = game.getPlayer(ENEMY_SLOT);
        Unit enemySpearman = production.createUnit(enemy, spearmanType, UNIT_X + ENEMY_DISTANCE, UNIT_Y, 0);

        Assertions.assertNotNull(spearman);
        Assertions.assertNotNull(enemySpearman);
        Assertions.assertNotNull(weapon);

        game.getCombat().dealDamage(enemySpearman, enemySpearman.getHitPoints(), spearman, 0);
        selection.select(spearman);
        draw(panel);

        int hitPoints = game.getContent().getUnitTypes().getHitPoints(spearmanType);

        Assertions.assertEquals(SPEARMAN, font.lines.getFirst(), "the name comes first");
        Assertions.assertArrayEquals(new float[]{PANEL_LEFT + PADDING, PANEL_TOP + PADDING}, font.positions.getFirst(),
                                     "the name is at the top left");
        Assertions.assertTrue(font.lines.contains(hitPoints + "/" + hitPoints), font.lines.toString());
        Assertions.assertTrue(font.lines.contains("Armor " + game.getContent().getUnitTypes().getArmor(spearmanType)), font.lines.toString());
        Assertions.assertTrue(font.lines.contains("Damage " + game.getContent().getWeaponTypes().getDamage(weapon) + "   Kills 1"),
                              "the damage line counts the kill: " + font.lines);
    }

    @Test
    void one_selected_worker_shows_its_order_with_the_target_and_the_gold_it_carries_beside_or_under_its_stats() throws IOException {
        SkirmishSessionScenario skirmish = new SkirmishSessionScenario();
        SkirmishScenario content = skirmish.getScenario();
        Game game = skirmish.getSession().getGame();
        Player player = skirmish.getSession().getLocalPlayer();
        StandardProductionSystem production = (StandardProductionSystem) game.getProduction();
        StandardHarvestSystem harvest = (StandardHarvestSystem) game.getModSystem(HarvestSystem.KEY);

        Assertions.assertNotNull(player);

        Unit worker = production.createUnit(player, content.getKeeper(), UNIT_X, UNIT_Y, 0);
        Unit mine = production.createUnit(player, content.getGoldMine(), UNIT_X + MINE_DISTANCE, UNIT_Y, 0);

        Assertions.assertNotNull(worker);
        Assertions.assertNotNull(mine);

        harvest.setResources(mine, content.getGold(), GOLD);

        boolean gathered = false;

        for (int frame = 0; frame < GATHER_FRAMES && !gathered; frame++) {
            gathered = harvest.gather(worker, mine);
        }

        Assertions.assertTrue(gathered);

        worker.setOrderType(content.getMove());
        worker.setOrderTargetUnit(mine);

        OrderTexts texts = new OrderTexts();
        OrderedSelection selection = new OrderedSelection(GROUP_SIZE);
        RecordingFont font = new RecordingFont();
        SelectionPanel wide = createPanel(skirmish.getSession().getView(), selection, font, WIDE_PANEL_WIDTH);
        String order = MOVING_TO + " " + content.getGoldMine().getDebugName();
        String carried = "Carrying " + harvest.getCarriedAmount(worker) + " " + content.getGold().getDebugName();

        texts.setText(content.getMove(), MOVING_TO, true);
        wide.setOrderTexts(texts);
        selection.select(worker);
        draw(wide);

        int hitPointsLine = font.lines.indexOf(content.getKeeper().getDebugName()) + 1;
        int orderLine = font.lines.indexOf(order);

        Assertions.assertTrue(orderLine > 0, "the order names its target: " + font.lines);
        Assertions.assertTrue(font.lines.contains(carried), "the carried gold shows: " + font.lines);
        Assertions.assertEquals(font.positions.get(hitPointsLine)[1], font.positions.get(orderLine)[1],
                                "a wide panel shows the order beside the stats");
        Assertions.assertTrue(font.positions.get(orderLine)[0] > font.positions.get(hitPointsLine)[0], "the order stands right of the stats");

        RecordingFont narrowFont = new RecordingFont();
        SelectionPanel narrow = createPanel(skirmish.getSession().getView(), selection, narrowFont, PANEL_WIDTH);

        narrow.setOrderTexts(texts);
        draw(narrow);

        int narrowOrderLine = narrowFont.lines.indexOf(order);

        Assertions.assertTrue(narrowOrderLine > 0, narrowFont.lines.toString());
        Assertions.assertTrue(narrowFont.positions.get(narrowOrderLine)[1] > narrowFont.positions.get(narrowOrderLine - 1)[1],
                              "a narrow panel shows the order under the stats");

        RecordingFont mineFont = new RecordingFont();
        SelectionPanel minePanel = createPanel(skirmish.getSession().getView(), selection, mineFont, WIDE_PANEL_WIDTH);

        selection.select(mine);
        draw(minePanel);

        Assertions.assertTrue(mineFont.lines.contains(harvest.getResourceAmount(mine) + " " + content.getGold().getDebugName()),
                              "a gold mine shows the gold it holds: " + mineFont.lines);
    }

    @Test
    void a_mixed_group_shows_its_units_grouped_by_type_in_the_order_of_each_type_first_selected() throws IOException {
        SkirmishSessionScenario skirmish = new SkirmishSessionScenario();
        SkirmishScenario content = skirmish.getScenario();
        Game game = skirmish.getSession().getGame();
        Player player = skirmish.getSession().getLocalPlayer();
        StandardProductionSystem production = (StandardProductionSystem) game.getProduction();
        UnitType spearmanType = findSpearmanType(game);

        Assertions.assertNotNull(player);

        Unit firstWorker = production.createUnit(player, content.getKeeper(), UNIT_X, UNIT_Y, 0);
        Unit spearman = production.createUnit(player, spearmanType, UNIT_X + 1, UNIT_Y, 0);
        Unit secondWorker = production.createUnit(player, content.getKeeper(), UNIT_X + 2, UNIT_Y, 0);

        Assertions.assertNotNull(firstWorker);
        Assertions.assertNotNull(spearman);
        Assertions.assertNotNull(secondWorker);

        OrderedSelection selection = new OrderedSelection(GROUP_SIZE);
        SelectionPanel panel = createPanel(skirmish.getSession().getView(), selection, new RecordingFont(), WIDE_PANEL_WIDTH);
        List<Unit> cells = new ArrayList<>();

        selection.select(List.of(firstWorker, spearman, secondWorker));
        draw(panel);

        for (float y = panel.getY(); y < panel.getY() + panel.getHeight(); y++) {
            for (float x = panel.getX(); x < panel.getX() + panel.getWidth(); x++) {
                Unit unit = panel.findUnitAt(x, y);

                if (unit != null && !cells.contains(unit)) {
                    cells.add(unit);
                }
            }
        }

        Assertions.assertEquals(List.of(firstWorker, secondWorker, spearman), cells, "the workers stand together before the spearman");
    }

    @Test
    void a_small_group_in_a_wide_panel_shows_each_unit_as_a_card_with_its_name_hit_points_and_order() throws IOException {
        SkirmishSessionScenario skirmish = new SkirmishSessionScenario();
        SkirmishScenario content = skirmish.getScenario();
        Game game = skirmish.getSession().getGame();
        Player player = skirmish.getSession().getLocalPlayer();
        StandardProductionSystem production = (StandardProductionSystem) game.getProduction();
        OrderedSelection selection = new OrderedSelection(GROUP_SIZE);
        OrderTexts texts = new OrderTexts();
        List<Unit> group = new ArrayList<>();

        Assertions.assertNotNull(player);

        for (int index = 0; index < SMALL_GROUP_SIZE; index++) {
            Unit worker = production.createUnit(player, content.getKeeper(), UNIT_X + index, UNIT_Y, 0);

            Assertions.assertNotNull(worker);
            worker.setOrderType(content.getMove());
            group.add(worker);
        }

        RecordingFont font = new RecordingFont();
        SelectionPanel panel = createPanel(skirmish.getSession().getView(), selection, font, WIDE_PANEL_WIDTH);
        int hitPoints = game.getContent().getUnitTypes().getHitPoints(content.getKeeper());
        String name = content.getKeeper().getDebugName();

        texts.setText(content.getMove(), MOVING_TO, false);
        panel.setOrderTexts(texts);
        selection.select(group);
        draw(panel);

        Assertions.assertEquals(SMALL_GROUP_SIZE, font.lines.stream().filter(name::equals).count(), "every card names its unit: " + font.lines);
        Assertions.assertEquals(SMALL_GROUP_SIZE, font.lines.stream().filter((hitPoints + "/" + hitPoints)::equals).count(), font.lines.toString());
        Assertions.assertEquals(SMALL_GROUP_SIZE, font.lines.stream().filter(MOVING_TO::equals).count(), "every card shows its unit's order");

        int nameLine = font.lines.indexOf(name);
        float textX = font.positions.get(nameLine)[0];
        float textY = font.positions.get(nameLine)[1];

        Assertions.assertSame(group.getFirst(), panel.findUnitAt(textX + 1, textY + 1), "a press on a card's texts finds its unit");
        Assertions.assertTrue(textX - panel.getX() - PADDING > PANEL_HEIGHT / SMALL_GROUP_SIZE,
                              "the portrait is larger than a third of the panel's height");

        RecordingFont narrowFont = new RecordingFont();
        SelectionPanel narrow = createPanel(skirmish.getSession().getView(), selection, narrowFont, NARROW_PANEL_WIDTH);

        narrow.setOrderTexts(texts);
        draw(narrow);

        Assertions.assertFalse(narrowFont.lines.contains(name), "a narrow panel keeps the cells without texts");
    }

    @Test
    void nothing_selected_shows_the_forces_of_the_player_by_type_and_a_press_selects_a_type() throws IOException {
        SkirmishSessionScenario skirmish = new SkirmishSessionScenario();
        SkirmishScenario content = skirmish.getScenario();
        Game game = skirmish.getSession().getGame();
        Player player = skirmish.getSession().getLocalPlayer();
        StandardProductionSystem production = (StandardProductionSystem) game.getProduction();
        OrderedSelection selection = new OrderedSelection(GROUP_SIZE);
        RecordingFont font = new RecordingFont();
        SelectionPanel panel = createPanel(skirmish.getSession().getView(), selection, font, WIDE_PANEL_WIDTH);
        String keeperName = content.getKeeper().getDebugName();

        Assertions.assertNotNull(player);
        Assertions.assertNull(panel.getOverviewPlayer(), "the overview shows the local player by default");

        int keepers = 0;

        for (Unit unit : skirmish.getSession().getView().getUnits()) {
            keepers += unit.getOwner() == player && unit.getType() == content.getKeeper() ? 1 : 0;
        }

        for (int index = 0; index < SMALL_GROUP_SIZE; index++) {
            Assertions.assertNotNull(production.createUnit(player, content.getKeeper(), UNIT_X + index, UNIT_Y, 0));
        }

        keepers += SMALL_GROUP_SIZE;
        draw(panel);

        int nameLine = font.lines.indexOf(keeperName);

        Assertions.assertTrue(nameLine >= 0, "the overview names the player's unit types: " + font.lines);
        Assertions.assertEquals(Integer.toString(keepers), font.lines.get(nameLine + 1), "the type's number follows its name");

        panel.handleInput(InputEvent.createButtonPressed(PointerButton.LEFT, 0, font.positions.get(nameLine)[0] + 1,
                                                         font.positions.get(nameLine)[1] + 1, false));

        Assertions.assertEquals(keepers, selection.getUnits().size(), "a press on the type selects the player's units of it");
        Assertions.assertTrue(selection.getUnits().stream().allMatch(unit -> unit.getType() == content.getKeeper()));

        Player enemy = game.getPlayer(ENEMY_SLOT);
        RecordingFont enemyFont = new RecordingFont();
        OrderedSelection enemySelection = new OrderedSelection(GROUP_SIZE);
        SelectionPanel enemyPanel = createPanel(skirmish.getSession().getView(), enemySelection, enemyFont, WIDE_PANEL_WIDTH);

        int enemyKeepers = 0;

        for (Unit unit : skirmish.getSession().getView().getUnits()) {
            enemyKeepers += unit.getOwner() == enemy && unit.getType() == content.getKeeper() ? 1 : 0;
        }

        enemyPanel.setOverviewPlayer(enemy);
        draw(enemyPanel);

        int enemyNameLine = enemyFont.lines.indexOf(keeperName);

        Assertions.assertEquals(enemyKeepers > 0, enemyNameLine >= 0, "another player's overview shows that player's types: " + enemyFont.lines);

        if (enemyNameLine >= 0) {
            Assertions.assertEquals(Integer.toString(enemyKeepers), enemyFont.lines.get(enemyNameLine + 1), "and counts that player's units");
        }
    }

    @Test
    void long_texts_of_a_card_are_cut_at_its_edge() throws IOException {
        SkirmishSessionScenario skirmish = new SkirmishSessionScenario();
        SkirmishScenario content = skirmish.getScenario();
        Game game = skirmish.getSession().getGame();
        Player player = skirmish.getSession().getLocalPlayer();
        StandardProductionSystem production = (StandardProductionSystem) game.getProduction();
        OrderedSelection selection = new OrderedSelection(GROUP_SIZE);
        List<Unit> group = new ArrayList<>();

        Assertions.assertNotNull(player);

        for (int index = 0; index < SMALL_GROUP_SIZE; index++) {
            Unit worker = production.createUnit(player, content.getKeeper(), UNIT_X + index, UNIT_Y, 0);

            Assertions.assertNotNull(worker);
            group.add(worker);
        }

        RecordingFont font = new RecordingFont();
        String longName = "keeper of the long and winding road through the hills";
        SelectionPanel panel = new SelectionPanel(skirmish.getSession().getView(), selection, font, type -> longName, ContentType::getDebugName);

        panel.setBounds(PANEL_LEFT, PANEL_TOP, WIDE_PANEL_WIDTH, PANEL_HEIGHT);
        selection.select(group);
        draw(panel);

        String shown = font.lines.getFirst();

        Assertions.assertTrue(shown.endsWith("...") && longName.startsWith(shown.substring(0, shown.length() - "...".length())), shown);
        Assertions.assertTrue(font.positions.getFirst()[0] + shown.length() * CHARACTER_WIDTH <= PANEL_LEFT + WIDE_PANEL_WIDTH, "the cut name fits");
    }

    @Test
    void a_building_under_construction_shows_its_progress() throws IOException {
        SkirmishSessionScenario skirmish = new SkirmishSessionScenario();
        SkirmishScenario content = skirmish.getScenario();
        Game game = skirmish.getSession().getGame();
        Player player = skirmish.getSession().getLocalPlayer();
        StandardProductionSystem production = (StandardProductionSystem) game.getProduction();

        Assertions.assertNotNull(player);

        Unit worker = production.createUnit(player, content.getKeeper(), UNIT_X, UNIT_Y, 0);
        Unit building = null;

        Assertions.assertNotNull(worker);

        game.getEconomy().setAmount(player, content.getGold(), GOLD);

        for (int step = 1; step <= SITE_TRIES && building == null; step++) {
            building = production.placeBuilding(worker, content.getKeep(), UNIT_X + step * SkirmishScenario.TILE_SIZE,
                                                UNIT_Y + step * SkirmishScenario.TILE_SIZE, 0);
        }

        Assertions.assertNotNull(building);

        OrderedSelection selection = new OrderedSelection(GROUP_SIZE);
        RecordingFont font = new RecordingFont();
        SelectionPanel panel = createPanel(skirmish.getSession().getView(), selection, font, PANEL_WIDTH);

        selection.select(building);
        draw(panel);

        Assertions.assertTrue(font.lines.contains("Building 0%"), font.lines.toString());
    }

    @Test
    void a_large_group_shows_every_unit_in_smaller_cells_and_names_the_hovered_one() throws IOException {
        SkirmishSessionScenario skirmish = new SkirmishSessionScenario();
        SkirmishScenario content = skirmish.getScenario();
        Game game = skirmish.getSession().getGame();
        Player player = skirmish.getSession().getLocalPlayer();
        StandardProductionSystem production = (StandardProductionSystem) game.getProduction();
        OrderedSelection selection = new OrderedSelection(GROUP_SIZE);
        RecordingFont font = new RecordingFont();
        SelectionPanel panel = createPanel(skirmish.getSession().getView(), selection, font, NARROW_PANEL_WIDTH);
        List<Unit> group = new ArrayList<>();
        float[] hovered = null;

        Assertions.assertNotNull(player);

        for (int index = 0; index < GROUP_SIZE; index++) {
            Unit worker = production.createUnit(player, content.getKeeper(), UNIT_X + index, UNIT_Y, 0);

            Assertions.assertNotNull(worker);
            group.add(worker);
        }

        selection.select(group);
        draw(panel);

        List<Unit> found = new ArrayList<>();

        for (float y = panel.getY(); y < panel.getY() + panel.getHeight(); y++) {
            for (float x = panel.getX(); x < panel.getX() + panel.getWidth(); x++) {
                Unit unit = panel.findUnitAt(x, y);

                if (unit != null && !found.contains(unit)) {
                    found.add(unit);
                }

                if (unit == group.get(1) && hovered == null) {
                    hovered = new float[]{x, y};
                }
            }
        }

        Assertions.assertEquals(GROUP_SIZE, found.size(), "every selected unit has a cell");
        Assertions.assertNotNull(hovered);
        Assertions.assertFalse(font.lines.contains(content.getKeeper().getDebugName()), "no tooltip without hover");

        int hitPoints = game.getContent().getUnitTypes().getHitPoints(content.getKeeper());

        panel.handleInput(InputEvent.createPointerMoved(hovered[0], hovered[1]));
        draw(panel);

        Assertions.assertEquals(List.of(content.getKeeper().getDebugName(), hitPoints + "/" + hitPoints),
                                font.lines.subList(font.lines.size() - 2, font.lines.size()),
                                "the hovered cell shows its unit's name and hit points");
        Assertions.assertTrue(font.positions.getLast()[1] < panel.getY(), "the tooltip stands over the panel");
    }
}
