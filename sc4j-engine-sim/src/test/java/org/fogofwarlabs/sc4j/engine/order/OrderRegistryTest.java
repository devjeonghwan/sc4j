package org.fogofwarlabs.sc4j.engine.order;

import org.fogofwarlabs.sc4j.engine.content.definition.OrderDefinition;
import org.fogofwarlabs.sc4j.engine.content.definition.UnitDefinition;
import org.fogofwarlabs.sc4j.engine.content.registry.ContentRegistry;
import org.fogofwarlabs.sc4j.engine.content.table.GameContent;
import org.fogofwarlabs.sc4j.engine.content.table.UnitTypeTable;
import org.fogofwarlabs.sc4j.engine.content.type.ContentType;
import org.fogofwarlabs.sc4j.engine.content.type.OrderType;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.ControllerKind;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Tests that an {@link OrderRegistry} holds one behavior per order type of its list and that a unit type keeps an idle order per controller kind.
 */
final class OrderRegistryTest {
    private static final int PLAYER_COUNT = 1;

    private static final @NotNull OrderBehavior<OrderContext, Unit> WALK = (context, unit) -> unit.setPosition(unit.getPositionX() + 1,
                                                                                                               unit.getPositionY());
    private static final @NotNull OrderBehavior<OrderContext, Unit> HOLD = (context, unit) -> {
    };

    private final @NotNull OrderType move;
    private final @NotNull OrderType stop;
    private final @NotNull OrderType patrol;

    OrderRegistryTest() {
        this.move   = new OrderType("move");
        this.stop   = new OrderType("stop");
        this.patrol = new OrderType("patrol");
    }

    @Test
    void register_should_fill_the_slot_of_each_type_and_share_instances() {
        OrderRegistry<OrderBehavior<OrderContext, Unit>> registry = new OrderRegistry<>(List.of(this.move, this.stop, this.patrol));

        registry.register(this.move, WALK);
        registry.register(this.patrol, WALK);
        registry.register(this.stop, HOLD);
        registry.freeze();

        Assertions.assertEquals(3, registry.getCapacity());
        Assertions.assertSame(WALK, registry.get(this.move));
        Assertions.assertSame(WALK, registry.find(2));
        Assertions.assertSame(HOLD, registry.find(this.stop));
        Assertions.assertNull(registry.find(3));
        Assertions.assertNull(registry.find(-1));
        Assertions.assertThrows(IllegalStateException.class, () -> registry.register(this.move, HOLD));
    }

    @Test
    void register_should_reject_types_outside_the_list() {
        OrderRegistry<OrderBehavior<OrderContext, Unit>> registry = new OrderRegistry<>(List.of(this.move));

        Assertions.assertThrows(IllegalArgumentException.class, () -> registry.register(this.stop, HOLD));
        Assertions.assertNull(registry.find(this.stop));
        Assertions.assertThrows(IllegalArgumentException.class, () -> registry.get(this.move));
    }

    @Test
    void behavior_should_run_on_the_unit_it_is_given() {
        OrderRegistry<OrderBehavior<OrderContext, Unit>> registry = new OrderRegistry<>(List.of(this.move));
        OrderContext context = new OrderContext() {
            @Override
            public @NotNull Game getGame() {
                return Assertions.fail("The behavior does not read the game.");
            }

            @Override
            public int getInstructionLimit() {
                return 0;
            }
        };
        Unit unit = new Unit(0);

        registry.register(this.move, WALK);
        registry.get(this.move).update(context, unit);

        Assertions.assertEquals(1, unit.getPositionX());
    }

    @Test
    void unit_type_table_should_keep_an_idle_order_per_controller_kind() {
        ContentRegistry content = new ContentRegistry();
        UnitType worker = new UnitType("worker");

        content.getOrderTypes().register(this.move, new OrderDefinition.Builder().build());
        content.getOrderTypes().register(this.stop, new OrderDefinition.Builder().build());
        content.getUnitTypes().register(worker, new UnitDefinition.Builder().setIdleOrder(ControllerKind.HUMAN, this.stop)
                                                                            .setIdleOrder(ControllerKind.COMPUTER, this.move)
                                                                            .build());

        UnitTypeTable units = GameContent.create(content.build(), PLAYER_COUNT).getUnitTypes();

        Assertions.assertSame(this.stop, units.getIdleOrder(worker, ControllerKind.HUMAN));
        Assertions.assertSame(this.move, units.getIdleOrder(worker, ControllerKind.COMPUTER));
        Assertions.assertNull(units.getIdleOrder(worker, ControllerKind.NEUTRAL));
        Assertions.assertEquals(ContentType.NO_INDEX, units.getIdleOrderIndex(worker.getIndex(), ControllerKind.OBSERVER));

        units.setIdleOrder(worker, ControllerKind.NEUTRAL, this.stop);

        Assertions.assertEquals(this.stop.getIndex(), units.getIdleOrderIndex(worker.getIndex(), ControllerKind.NEUTRAL));
    }
}
