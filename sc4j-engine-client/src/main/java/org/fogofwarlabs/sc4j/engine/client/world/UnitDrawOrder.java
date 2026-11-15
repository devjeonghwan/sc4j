package org.fogofwarlabs.sc4j.engine.client.world;

import org.fogofwarlabs.sc4j.engine.client.projection.WorldProjection;
import org.fogofwarlabs.sc4j.engine.client.view.GameView;
import org.fogofwarlabs.sc4j.engine.client.view.UnitView;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Sorts the units of a game view into the draw order of a projection: increasing depth of their positions
 * ({@link WorldProjection#getDepth}), units of equal depth in pool order (a stable insertion sort). The world layers that draw units keep one
 * and sort every frame; the arrays and the list are reused, so a frame allocates nothing once they have grown to the unit count.
 */
public final class UnitDrawOrder {
    private final @NotNull List<@NotNull Unit> sorted;

    private float @NotNull [] depths;
    private int @NotNull [] order;

    /**
     * Creates a {@link UnitDrawOrder} instance.
     */
    public UnitDrawOrder() {
        this.sorted = new ArrayList<>();

        this.depths = new float[0];
        this.order  = new int[0];
    }

    /**
     * Sorts the units of a game view into the draw order of a projection.
     *
     * @param view       the game view
     * @param projection the projection of the world view
     * @return the units in draw order, a list owned by this instance and valid until the next sort
     */
    public @NotNull List<@NotNull Unit> sort(@NotNull GameView view, @NotNull WorldProjection projection) {
        List<Unit> units = view.getUnits();
        int count = units.size();

        if (this.depths.length < count) {
            this.depths = Arrays.copyOf(this.depths, count);
            this.order  = Arrays.copyOf(this.order, count);
        }

        for (int index = 0; index < count; index++) {
            UnitView unitView = view.getUnitView(units.get(index));

            this.depths[index] = projection.getDepth(unitView.getX(), unitView.getY());
            this.order[index]  = index;
        }

        for (int index = 1; index < count; index++) {
            int current = this.order[index];
            int position = index - 1;

            while (position >= 0 && this.depths[this.order[position]] > this.depths[current]) {
                this.order[position + 1] = this.order[position];
                position--;
            }

            this.order[position + 1] = current;
        }

        this.sorted.clear();

        for (int index = 0; index < count; index++) {
            this.sorted.add(units.get(this.order[index]));
        }

        return this.sorted;
    }
}
