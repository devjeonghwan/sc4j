package org.fogofwarlabs.sc4j.engine.client.command;

import org.fogofwarlabs.sc4j.engine.client.SkirmishSessionScenario;
import org.fogofwarlabs.sc4j.engine.client.view.GameView;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Tests the selection filter on the skirmish map with the default rules (own units, a fallback to another player's unit, same kind, adding)
 * and with game rules whose replacement keys decide the units of a full selection.
 */
final class SelectionFilterTest {
    private static final int CAPACITY = 24;
    private static final int FRAMES = 4;

    /**
     * Rules that take every unit and key each unit by its place in a list.
     */
    private static final class ListedKeyRules implements SelectionRules<Unit> {
        private final @NotNull List<Unit> order;

        private ListedKeyRules(@NotNull List<Unit> order) {
            this.order = order;
        }

        @Override
        public @NotNull Unit getMainUnit(@NotNull Unit unit) {
            return unit;
        }

        @Override
        public boolean isListable(@NotNull Unit unit) {
            return true;
        }

        @Override
        public boolean canJoinMultipleSelection(@NotNull Unit unit) {
            return true;
        }

        @Override
        public int getReplacementKey(@NotNull Unit unit) {
            return this.order.indexOf(unit);
        }

        @Override
        public boolean isSameKind(@NotNull Unit unit, @NotNull Unit clicked) {
            return true;
        }
    }

    private static @NotNull GameView createView() throws IOException {
        SkirmishSessionScenario skirmish = new SkirmishSessionScenario();

        skirmish.runFrames(FRAMES);

        return skirmish.getSession().getView();
    }

    @Test
    void standard_rules_select_own_units_and_fall_back_to_a_seen_unit() throws IOException {
        GameView view = createView();
        StandardSelectionRules rules = new StandardSelectionRules(view);
        SelectionFilter<Unit> filter = new SelectionFilter<>(CAPACITY, rules);
        Player player = view.getLocalPlayer();
        List<Unit> own = new ArrayList<>();
        List<Unit> others = new ArrayList<>();

        for (Unit unit : view.getUnits()) {
            if (!rules.isListable(unit)) {
                continue;
            }

            if (view.getUnitView(unit).getOwner() == player) {
                own.add(unit);
            } else {
                others.add(unit);
            }
        }

        Assertions.assertFalse(own.isEmpty(), "the local player has units");
        Assertions.assertFalse(others.isEmpty(), "the local player sees other units");

        List<Unit> all = new ArrayList<>(others);

        all.addAll(own);

        Assertions.assertEquals(own.subList(0, Math.min(own.size(), CAPACITY)), filter.filterBox(all), "a box takes own units only");
        Assertions.assertEquals(List.of(others.getLast()), filter.filterBox(others), "the last seen unit when no own unit is in the box");

        List<Unit> typed = filter.filterType(all, own.getFirst());

        Assertions.assertSame(own.getFirst(), typed.getFirst(), "the clicked unit comes first");

        for (Unit unit : typed) {
            Assertions.assertTrue(rules.isSameKind(unit, own.getFirst()));
        }

        Assertions.assertEquals(List.of(others.getFirst()), filter.merge(List.of(others.getFirst()), own),
                                "another player's unit takes no additions");
        Assertions.assertEquals(0, rules.getReplacementKey(own.getFirst()));
    }

    @Test
    void full_selection_keeps_the_units_with_the_higher_keys() throws IOException {
        List<Unit> units = createView().getUnits();

        Assertions.assertTrue(units.size() >= 3, "the skirmish map has units");

        List<Unit> order = List.of(units.get(0), units.get(1), units.get(2));
        SelectionFilter<Unit> filter = new SelectionFilter<>(1, new ListedKeyRules(order));

        Assertions.assertEquals(List.of(units.get(2)), filter.filterBox(order), "each later unit has a higher key and replaces the first");
        Assertions.assertEquals(List.of(units.get(0)), filter.filterType(order, units.get(0)), "the clicked unit is never replaced");
        Assertions.assertThrows(IllegalArgumentException.class, () -> new SelectionFilter<>(0, new ListedKeyRules(order)));
    }
}
