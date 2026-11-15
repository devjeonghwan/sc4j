package org.fogofwarlabs.sc4j.engine.client.world;

import org.fogofwarlabs.sc4j.engine.client.SkirmishSessionScenario;
import org.fogofwarlabs.sc4j.engine.client.command.CommandTarget;
import org.fogofwarlabs.sc4j.engine.client.command.OrderedSelection;
import org.fogofwarlabs.sc4j.engine.client.command.SelectionRules;
import org.fogofwarlabs.sc4j.engine.client.input.InputEvent;
import org.fogofwarlabs.sc4j.engine.client.input.KeyModifier;
import org.fogofwarlabs.sc4j.engine.client.input.PointerButton;
import org.fogofwarlabs.sc4j.engine.client.projection.IsometricProjection;
import org.fogofwarlabs.sc4j.engine.client.projection.SquareProjection;
import org.fogofwarlabs.sc4j.engine.client.view.GameView;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

/**
 * Tests pointer selection and picking in the engine world view on the skirmish map, by the selection rules: a click selects the pointed unit, a
 * click on the ground clears, Shift removes a selected unit and adds only units that join the selection, a box selects the local player's
 * units or else another unit alone, a double press selects the clicked unit's kind, a game's own rules decide, and the picker gives the
 * pointed unit or position.
 */
final class SelectionBoxTest {
    private static final int SCREEN_WIDTH = 800;
    private static final int SCREEN_HEIGHT = 600;
    private static final float SCOUT_X = 400;
    private static final float SCOUT_Y = 144;
    private static final float GROUND_X = 700;
    private static final float GROUND_Y = 500;
    private static final float KEEP_X = 208;
    private static final float KEEP_Y = 208;
    private static final int CAPACITY = 12;
    private static final int FRAMES = 4;
    private static final int ISOMETRIC_TILE_WIDTH = 64;
    private static final int ISOMETRIC_TILE_HEIGHT = 32;
    private static final float BOX_HALF_SIZE = 20;
    private static final float KEEP_BOX_HALF_SIZE = 40;
    private static final float SCOUT_BOX_HALF_SIZE = 30;

    /**
     * Rules of a game that lets the player select nothing.
     */
    private static final @NotNull SelectionRules<Unit> NOTHING_LISTED = new SelectionRules<>() {
        @Override
        public @NotNull Unit getMainUnit(@NotNull Unit unit) {
            return unit;
        }

        @Override
        public boolean isListable(@NotNull Unit unit) {
            return false;
        }

        @Override
        public boolean canJoinMultipleSelection(@NotNull Unit unit) {
            return true;
        }

        @Override
        public int getReplacementKey(@NotNull Unit unit) {
            return 0;
        }

        @Override
        public boolean isSameKind(@NotNull Unit unit, @NotNull Unit clicked) {
            return true;
        }
    };

    private static void click(@NotNull SelectionBox box, float x, float y, int modifiers) {
        box.handleInput(InputEvent.createButtonPressed(PointerButton.LEFT, modifiers, x, y, false));
        box.handleInput(InputEvent.createButtonReleased(PointerButton.LEFT, modifiers, x, y));
    }

    private static void doubleClick(@NotNull SelectionBox box, float x, float y) {
        click(box, x, y, 0);
        box.handleInput(InputEvent.createButtonPressed(PointerButton.LEFT, 0, x, y, true));
        box.handleInput(InputEvent.createButtonReleased(PointerButton.LEFT, 0, x, y));
    }

    private static void drag(@NotNull SelectionBox box, float startX, float startY, float endX, float endY, int modifiers) {
        box.handleInput(InputEvent.createButtonPressed(PointerButton.LEFT, modifiers, startX, startY, false));
        box.handleInput(InputEvent.createPointerMoved(endX, endY));
        Assertions.assertTrue(box.isDragging());
        box.handleInput(InputEvent.createButtonReleased(PointerButton.LEFT, modifiers, endX, endY));
        Assertions.assertFalse(box.isDragging());
    }

    @Test
    void selection_box_selects_by_click_shift_click_box_and_double_press_by_the_rules() throws IOException {
        SkirmishSessionScenario skirmish = new SkirmishSessionScenario();
        GameView view = skirmish.getSession().getView();
        Camera camera = new Camera(new SquareProjection(), view.getMapWidth(), view.getMapHeight());
        WorldPicker picker = new WorldPicker(view, camera);
        OrderedSelection selection = new OrderedSelection(CAPACITY);
        SelectionBox box = new SelectionBox(view, picker, selection);
        int shift = KeyModifier.SHIFT.getMask();

        skirmish.runFrames(FRAMES);
        camera.setScreenSize(SCREEN_WIDTH, SCREEN_HEIGHT);

        Unit scout = view.findUnitAt((int) SCOUT_X, (int) SCOUT_Y);
        Unit keep = view.findUnitAt((int) KEEP_X, (int) KEEP_Y);

        Assertions.assertNotNull(scout);
        Assertions.assertNotNull(keep);
        Assertions.assertSame(view.getLocalPlayer(), view.getUnitView(scout).getOwner());
        Assertions.assertNotSame(view.getLocalPlayer(), view.getUnitView(keep).getOwner());

        click(box, SCOUT_X, SCOUT_Y, 0);

        Assertions.assertEquals(List.of(scout), selection.getUnits());

        click(box, KEEP_X, KEEP_Y, shift);

        Assertions.assertEquals(List.of(scout), selection.getUnits(), "Shift adds no other player's unit to the player's units");

        click(box, SCOUT_X, SCOUT_Y, shift);

        Assertions.assertTrue(selection.isEmpty(), "Shift removes a selected unit");

        click(box, KEEP_X, KEEP_Y, 0);

        Assertions.assertEquals(List.of(keep), selection.getUnits(), "a click selects another player's unit alone");

        click(box, SCOUT_X, SCOUT_Y, shift);

        Assertions.assertEquals(List.of(keep), selection.getUnits(), "nothing joins another player's unit");

        click(box, GROUND_X, GROUND_Y, 0);

        Assertions.assertTrue(selection.isEmpty());

        drag(box, KEEP_X - KEEP_BOX_HALF_SIZE, KEEP_Y - KEEP_BOX_HALF_SIZE, KEEP_X + KEEP_BOX_HALF_SIZE, KEEP_Y + KEEP_BOX_HALF_SIZE, 0);

        Assertions.assertEquals(1, selection.getSize(), "a box without the player's units selects one other unit alone");
        Assertions.assertNotSame(view.getLocalPlayer(), view.getUnitView(selection.getUnits().getFirst()).getOwner());

        drag(box, SCOUT_X - SCOUT_BOX_HALF_SIZE, SCOUT_Y - SCOUT_BOX_HALF_SIZE, SCOUT_X + SCOUT_BOX_HALF_SIZE, SCOUT_Y + SCOUT_BOX_HALF_SIZE, 0);

        Assertions.assertEquals(List.of(scout), selection.getUnits(), "the player's units win over other units");

        drag(box, KEEP_X - KEEP_BOX_HALF_SIZE, KEEP_Y - KEEP_BOX_HALF_SIZE, KEEP_X + KEEP_BOX_HALF_SIZE, KEEP_Y + KEEP_BOX_HALF_SIZE, shift);

        Assertions.assertEquals(List.of(scout), selection.getUnits(), "Shift adds no other player's unit by a box either");

        doubleClick(box, KEEP_X, KEEP_Y);

        Assertions.assertEquals(List.of(keep), selection.getUnits(), "a double press keeps the clicked unit first");

        doubleClick(box, SCOUT_X, SCOUT_Y);

        Assertions.assertEquals(List.of(scout), selection.getUnits(), "the shown units of the scout's kind");

        CommandTarget unitTarget = picker.pick(SCOUT_X, SCOUT_Y);
        CommandTarget groundTarget = picker.pick(GROUND_X, GROUND_Y);

        Assertions.assertNotNull(unitTarget);
        Assertions.assertSame(scout, unitTarget.getUnit());
        Assertions.assertNotNull(groundTarget);
        Assertions.assertNull(groundTarget.getUnit());
        Assertions.assertEquals((int) GROUND_X, groundTarget.getX());
        Assertions.assertNull(picker.pick(SCREEN_WIDTH, 0), "outside the world view");
    }

    @Test
    void selection_box_selects_by_a_game_s_own_rules() throws IOException {
        SkirmishSessionScenario skirmish = new SkirmishSessionScenario();
        GameView view = skirmish.getSession().getView();
        Camera camera = new Camera(new SquareProjection(), view.getMapWidth(), view.getMapHeight());
        OrderedSelection selection = new OrderedSelection(CAPACITY);
        SelectionBox box = new SelectionBox(view, new WorldPicker(view, camera), selection, NOTHING_LISTED);

        skirmish.runFrames(FRAMES);
        camera.setScreenSize(SCREEN_WIDTH, SCREEN_HEIGHT);

        click(box, SCOUT_X, SCOUT_Y, 0);
        drag(box, SCOUT_X - SCOUT_BOX_HALF_SIZE, SCOUT_Y - SCOUT_BOX_HALF_SIZE, SCOUT_X + SCOUT_BOX_HALF_SIZE, SCOUT_Y + SCOUT_BOX_HALF_SIZE, 0);
        doubleClick(box, SCOUT_X, SCOUT_Y);

        Assertions.assertTrue(selection.isEmpty());
    }

    @Test
    void selection_box_and_picker_find_units_through_the_isometric_projection() throws IOException {
        SkirmishSessionScenario skirmish = new SkirmishSessionScenario();
        GameView view = skirmish.getSession().getView();
        IsometricProjection projection = new IsometricProjection(view.getTerrain().getTileSize(), ISOMETRIC_TILE_WIDTH, ISOMETRIC_TILE_HEIGHT);
        Camera camera = new Camera(projection, view.getMapWidth(), view.getMapHeight());
        WorldPicker picker = new WorldPicker(view, camera);
        OrderedSelection selection = new OrderedSelection(CAPACITY);
        SelectionBox box = new SelectionBox(view, picker, selection);

        skirmish.runFrames(FRAMES);
        camera.setScreenSize(SCREEN_WIDTH, SCREEN_HEIGHT);

        Unit scout = view.findUnitAt((int) SCOUT_X, (int) SCOUT_Y);

        Assertions.assertNotNull(scout);

        camera.centerOn(view.getUnitView(scout).getX(), view.getUnitView(scout).getY());

        float scoutX = (projection.toProjectedX(view.getUnitView(scout).getX(), view.getUnitView(scout).getY()) - camera.getX()) * camera.getZoom();
        float scoutY = (projection.toProjectedY(view.getUnitView(scout).getX(), view.getUnitView(scout).getY()) - camera.getY()) * camera.getZoom();

        Assertions.assertEquals(view.getUnitView(scout).getX(), picker.toWorldX(scoutX, scoutY), 1);
        Assertions.assertEquals(view.getUnitView(scout).getY(), picker.toWorldY(scoutX, scoutY), 1);
        Assertions.assertSame(scout, picker.pickUnit(scoutX, scoutY), "the unit shown under the pointer");

        click(box, scoutX, scoutY, 0);

        Assertions.assertEquals(List.of(scout), selection.getUnits());

        click(box, SCREEN_WIDTH - 1, SCREEN_HEIGHT - 1, 0);

        Assertions.assertTrue(selection.isEmpty());

        drag(box, scoutX - BOX_HALF_SIZE, scoutY - BOX_HALF_SIZE, scoutX + BOX_HALF_SIZE, scoutY + BOX_HALF_SIZE, 0);

        Assertions.assertEquals(List.of(scout), selection.getUnits(), "a screen box selects what it shows");
    }
}
