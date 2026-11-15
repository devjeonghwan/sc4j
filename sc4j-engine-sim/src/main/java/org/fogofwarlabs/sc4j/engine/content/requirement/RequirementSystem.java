package org.fogofwarlabs.sc4j.engine.content.requirement;

import org.fogofwarlabs.sc4j.engine.content.type.ContentType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.system.GameSystem;
import org.fogofwarlabs.sc4j.engine.game.system.SystemKey;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Decides whether a player may do something with a type: build a unit, research an upgrade or a tech, use a tech or
 * issue an order. The engine default is
 * {@link org.fogofwarlabs.sc4j.engine.content.requirement.declarative.DeclarativeRequirementSystem}; a mod replaces the
 * whole system by registering another factory for {@link #KEY}.
 * <p>
 * Determinism contract: a check reads the game state and changes nothing, draws no random value and allocates nothing;
 * the same state gives the same result on every machine. Production, the command card and the computer players all
 * ask the same system, so they agree.
 */
public interface RequirementSystem extends GameSystem {
    /**
     * The key of the requirement system.
     */
    @NotNull SystemKey<RequirementSystem> KEY = SystemKey.of("requirements", RequirementSystem.class);

    /**
     * Checks whether a player may do an action with a type.
     *
     * @param action the action
     * @param type   the type, of the content kind the action names
     * @param player the player
     * @param actor  the acting unit, whose type the requirement may test, or {@code null} without one
     * @return the result; {@link RequirementResult#isMet()} if the player may do it
     */
    @NotNull RequirementResult check(@NotNull RequirementAction action, @NotNull ContentType type, @NotNull Player player, @Nullable Unit actor);
}
