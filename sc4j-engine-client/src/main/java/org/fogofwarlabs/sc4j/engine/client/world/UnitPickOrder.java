package org.fogofwarlabs.sc4j.engine.client.world;

import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.jetbrains.annotations.Nullable;

/**
 * Decides which unit the pointer picks at a world position when units overlap there: the pick order hook of the {@link WorldPicker}. The
 * engine default is {@link org.fogofwarlabs.sc4j.engine.client.view.GameView#findUnitAt} (of the units whose bounds hold the position, the one
 * drawn last); a game with its own drawing order or pixel-exact unit shapes supplies its own.
 */
@FunctionalInterface
public interface UnitPickOrder {
    /**
     * Finds the unit picked at a world position.
     *
     * @param x the world x
     * @param y the world y
     * @return the unit, or {@code null} if no unit is picked there
     */
    @Nullable Unit findUnitAt(int x, int y);
}
