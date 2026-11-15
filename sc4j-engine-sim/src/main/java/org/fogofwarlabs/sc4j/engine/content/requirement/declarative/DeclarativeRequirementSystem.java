package org.fogofwarlabs.sc4j.engine.content.requirement.declarative;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.content.registry.ContentDefinition;
import org.fogofwarlabs.sc4j.engine.content.requirement.RequirementAction;
import org.fogofwarlabs.sc4j.engine.content.requirement.RequirementResult;
import org.fogofwarlabs.sc4j.engine.content.requirement.RequirementSystem;
import org.fogofwarlabs.sc4j.engine.content.requirement.StandardRequirementResult;
import org.fogofwarlabs.sc4j.engine.content.table.GameContent;
import org.fogofwarlabs.sc4j.engine.content.table.PlayerProgressTable;
import org.fogofwarlabs.sc4j.engine.content.type.ContentType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Checks requirements from data: the engine default {@link RequirementSystem}.
 * <p>
 * A check runs the built-in check of the action on the game's {@link PlayerProgressTable} first and then the
 * {@link Requirement} the mod declared for the type, if any:
 * <ul>
 *     <li>{@link RequirementAction#BUILD_UNIT}: the unit type must be available to the player.</li>
 *     <li>{@link RequirementAction#RESEARCH_UPGRADE}: the player's level must be below the maximum level.</li>
 *     <li>{@link RequirementAction#RESEARCH_TECH}: the tech must be available and not researched yet.</li>
 *     <li>{@link RequirementAction#USE_TECH}: the tech must be researched.</li>
 *     <li>{@link RequirementAction#ISSUE_ORDER}: no built-in check.</li>
 * </ul>
 * The declared requirements are resolved to arrays by type index when the system is attached; a check is then two
 * array reads and the conditions, without allocation.
 */
public final class DeclarativeRequirementSystem implements RequirementSystem {
    private static @NotNull RequirementResult checkBuiltIn(@NotNull PlayerProgressTable progress, @NotNull RequirementAction action, int typeIndex,
                                                           int player) {
        return switch (action) {
            case BUILD_UNIT -> progress.isUnitAvailable(player, typeIndex) ? StandardRequirementResult.MET : StandardRequirementResult.UNAVAILABLE;
            case RESEARCH_UPGRADE -> progress.getUpgradeLevel(player, typeIndex) < progress.getMaximumUpgradeLevel(player, typeIndex)
                                     ? StandardRequirementResult.MET
                                     : StandardRequirementResult.MAXIMUM_LEVEL_REACHED;
            case RESEARCH_TECH -> {
                if (!progress.isTechAvailable(player, typeIndex)) {
                    yield StandardRequirementResult.UNAVAILABLE;
                }

                yield progress.isTechResearched(player, typeIndex) ? StandardRequirementResult.ALREADY_RESEARCHED : StandardRequirementResult.MET;
            }
            case USE_TECH -> progress.isTechResearched(player, typeIndex) ? StandardRequirementResult.MET : StandardRequirementResult.MISSING_TECH;
            case ISSUE_ORDER -> StandardRequirementResult.MET;
        };
    }

    private final @NotNull DeclarativeRequirements requirements;

    private @Nullable GameContent content;
    private @Nullable Requirement @Nullable [] @NotNull [] resolved;

    /**
     * Creates a {@link DeclarativeRequirementSystem} instance; it is attached to its game afterwards.
     *
     * @param requirements the declared requirements, shared by every game
     */
    public DeclarativeRequirementSystem(@NotNull DeclarativeRequirements requirements) {
        this.requirements = requirements;

        this.content  = null;
        this.resolved = null;
    }

    /**
     * Resolves the game content and the declared requirements by type index.
     *
     * @param game the game
     * @throws IllegalArgumentException if a requirement names a type that is not registered
     */
    @Override
    public void attach(@NotNull Game game) {
        GameContent gameContent = game.getContent();
        ContentDefinition definition = gameContent.getDefinition();
        RequirementAction[] actions = RequirementAction.values();
        Requirement[][] byAction = new Requirement[actions.length][];

        for (RequirementAction action : actions) {
            byAction[action.ordinal()] = switch (action) {
                case BUILD_UNIT -> this.requirements.resolve(action, definition.getUnitTypes().getCount(),
                                                             definition.getUnitTypes().getTypes()::contains);
                case RESEARCH_UPGRADE -> this.requirements.resolve(action, definition.getUpgradeTypes().getCount(),
                                                                   definition.getUpgradeTypes().getTypes()::contains);
                case RESEARCH_TECH, USE_TECH -> this.requirements.resolve(action, definition.getTechTypes().getCount(),
                                                                          definition.getTechTypes().getTypes()::contains);
                case ISSUE_ORDER -> this.requirements.resolve(action, definition.getOrderTypes().getCount(),
                                                              definition.getOrderTypes().getTypes()::contains);
            };
        }

        this.content  = gameContent;
        this.resolved = byAction;
    }

    /**
     * Checks whether a player may do an action with a type: the built-in check of the action, then the declared
     * requirement of the type.
     *
     * @param action the action
     * @param type   the type, of the content kind the action names
     * @param player the player
     * @param actor  the acting unit, whose type the declared requirement tests, or {@code null} without one
     * @return the result; {@link RequirementResult#isMet()} if the player may do it
     * @throws IllegalStateException if the system is not attached
     */
    @Override
    public @NotNull RequirementResult check(@NotNull RequirementAction action, @NotNull ContentType type, @NotNull Player player, @Nullable Unit actor) {
        GameContent gameContent = this.content;
        Requirement[][] byAction = this.resolved;

        if (gameContent == null || byAction == null) {
            throw Exceptions.newIllegalStateException("The requirement system is not attached.");
        }

        RequirementResult builtIn = checkBuiltIn(gameContent.getProgress(), action, type.getIndex(), player.getIndex());

        if (!builtIn.isMet()) {
            return builtIn;
        }

        Requirement requirement = byAction[action.ordinal()][type.getIndex()];

        if (requirement == null) {
            return StandardRequirementResult.MET;
        }

        return requirement.test(gameContent, player, actor != null ? actor.getType() : null);
    }
}
