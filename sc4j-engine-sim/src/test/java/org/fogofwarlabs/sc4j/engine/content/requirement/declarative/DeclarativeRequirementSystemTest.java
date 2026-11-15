package org.fogofwarlabs.sc4j.engine.content.requirement.declarative;

import org.fogofwarlabs.sc4j.engine.content.definition.OrderDefinition;
import org.fogofwarlabs.sc4j.engine.content.definition.TechDefinition;
import org.fogofwarlabs.sc4j.engine.content.definition.UnitDefinition;
import org.fogofwarlabs.sc4j.engine.content.definition.UpgradeDefinition;
import org.fogofwarlabs.sc4j.engine.content.requirement.MissingUnitResult;
import org.fogofwarlabs.sc4j.engine.content.requirement.RequirementAction;
import org.fogofwarlabs.sc4j.engine.content.requirement.RequirementSystem;
import org.fogofwarlabs.sc4j.engine.content.requirement.StandardRequirementResult;
import org.fogofwarlabs.sc4j.engine.content.table.PlayerProgressTable;
import org.fogofwarlabs.sc4j.engine.content.type.OrderType;
import org.fogofwarlabs.sc4j.engine.content.type.TechType;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.content.type.UpgradeType;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.*;
import org.fogofwarlabs.sc4j.engine.player.Player;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the built-in checks and the declared conditions of the {@link DeclarativeRequirementSystem}.
 */
final class DeclarativeRequirementSystemTest {
    private static final int PLAYER = 0;

    private static @NotNull Unit createUnit(@NotNull UnitType type) {
        Unit unit = new Unit(1);

        unit.setType(type);

        return unit;
    }

    private final @NotNull UnitType worker;
    private final @NotNull UnitType barracks;
    private final @NotNull UnitType soldier;
    private final @NotNull UnitType knight;
    private final @NotNull UpgradeType armor;
    private final @NotNull TechType charge;
    private final @NotNull OrderType patrol;
    private final @NotNull GameDefinition.Builder definition;

    DeclarativeRequirementSystemTest() {
        this.worker     = new UnitType("worker");
        this.barracks   = new UnitType("barracks");
        this.soldier    = new UnitType("soldier");
        this.knight     = new UnitType("knight");
        this.armor      = new UpgradeType("armor");
        this.charge     = new TechType("charge");
        this.patrol     = new OrderType("patrol");
        this.definition = new GameDefinition.Builder();

        this.definition.getContent().getUnitTypes().register(this.worker, new UnitDefinition.Builder().build());
        this.definition.getContent().getUnitTypes().register(this.barracks, new UnitDefinition.Builder().build());
        this.definition.getContent().getUnitTypes().register(this.soldier, new UnitDefinition.Builder().build());
        this.definition.getContent().getUnitTypes().register(this.knight, new UnitDefinition.Builder().build());
        this.definition.getContent().getUpgradeTypes().register(this.armor, new UpgradeDefinition.Builder().setMaximumLevel(1).build());
        this.definition.getContent().getTechTypes().register(this.charge, new TechDefinition.Builder().build());
        this.definition.getContent().getOrderTypes().register(this.patrol, new OrderDefinition.Builder().build());
    }

    @Test
    void check_should_apply_the_declared_conditions_in_order() {
        DeclarativeRequirements requirements = new DeclarativeRequirements.Builder()
                .requireBuild(this.soldier, Requirement.allOf(new ActorTypeCondition(this.barracks), new CompletedUnitCondition(this.barracks)))
                .requireBuild(this.knight, Requirement.allOf(new AnyOfCondition(new TechResearchedCondition(this.charge),
                                                                                new UpgradeLevelCondition(this.armor, 1))))
                .build();
        Game game = this.createGame(requirements);
        RequirementSystem system = game.getModSystem(RequirementSystem.KEY);
        PlayerProgressTable progress = game.getContent().getProgress();
        Player player = game.getPlayer(PLAYER);
        Unit worker = createUnit(this.worker);
        Unit barracks = createUnit(this.barracks);

        Assertions.assertEquals(StandardRequirementResult.WRONG_ACTOR, system.check(RequirementAction.BUILD_UNIT, this.soldier, player, worker));
        Assertions.assertSame(this.barracks,
                              Assertions.assertInstanceOf(MissingUnitResult.class,
                                                          system.check(RequirementAction.BUILD_UNIT, this.soldier, player, barracks)).getUnitType());

        progress.setCompletedUnitCount(player, this.barracks, 1);

        Assertions.assertEquals(StandardRequirementResult.MET, system.check(RequirementAction.BUILD_UNIT, this.soldier, player, barracks));
        Assertions.assertEquals(StandardRequirementResult.MISSING_TECH, system.check(RequirementAction.BUILD_UNIT, this.knight, player, null));

        progress.setUpgradeLevel(player, this.armor, 1);

        Assertions.assertEquals(StandardRequirementResult.MET, system.check(RequirementAction.BUILD_UNIT, this.knight, player, null));
    }

    @Test
    void check_should_apply_the_declared_conditions_of_every_action() {
        DeclarativeRequirements requirements = new DeclarativeRequirements.Builder()
                .requireUpgrade(this.armor, Requirement.allOf(new CompletedUnitCondition(this.barracks)))
                .requireResearch(this.charge, Requirement.allOf(new CompletedUnitCondition(this.barracks)))
                .requireTechUse(this.charge, Requirement.allOf(new CompletedUnitCondition(this.knight)))
                .requireOrder(this.patrol, Requirement.allOf(new CompletedUnitCondition(this.barracks)))
                .build();
        Game game = this.createGame(requirements);
        RequirementSystem system = game.getModSystem(RequirementSystem.KEY);
        PlayerProgressTable progress = game.getContent().getProgress();
        Player player = game.getPlayer(PLAYER);

        Assertions.assertInstanceOf(MissingUnitResult.class, system.check(RequirementAction.RESEARCH_UPGRADE, this.armor, player, null));
        Assertions.assertInstanceOf(MissingUnitResult.class, system.check(RequirementAction.RESEARCH_TECH, this.charge, player, null));
        Assertions.assertInstanceOf(MissingUnitResult.class, system.check(RequirementAction.ISSUE_ORDER, this.patrol, player, null));

        progress.setCompletedUnitCount(player, this.barracks, 1);

        Assertions.assertEquals(StandardRequirementResult.MET, system.check(RequirementAction.RESEARCH_UPGRADE, this.armor, player, null));
        Assertions.assertEquals(StandardRequirementResult.MET, system.check(RequirementAction.RESEARCH_TECH, this.charge, player, null));
        Assertions.assertEquals(StandardRequirementResult.MET, system.check(RequirementAction.ISSUE_ORDER, this.patrol, player, null));

        progress.setTechResearched(player, this.charge, true);

        Assertions.assertSame(this.knight,
                              Assertions.assertInstanceOf(MissingUnitResult.class,
                                                          system.check(RequirementAction.USE_TECH, this.charge, player, null)).getUnitType());

        progress.setCompletedUnitCount(player, this.knight, 1);

        Assertions.assertEquals(StandardRequirementResult.MET, system.check(RequirementAction.USE_TECH, this.charge, player, null));
    }

    @Test
    void check_should_run_the_built_in_checks_before_the_declared_ones() {
        Game game = this.createGame(new DeclarativeRequirements.Builder().build());
        RequirementSystem system = game.getModSystem(RequirementSystem.KEY);
        PlayerProgressTable progress = game.getContent().getProgress();
        Player player = game.getPlayer(PLAYER);

        progress.setUnitAvailable(player, this.worker, false);

        Assertions.assertEquals(StandardRequirementResult.UNAVAILABLE, system.check(RequirementAction.BUILD_UNIT, this.worker, player, null));
        Assertions.assertEquals(StandardRequirementResult.MISSING_TECH, system.check(RequirementAction.USE_TECH, this.charge, player, null));
        Assertions.assertEquals(StandardRequirementResult.MET, system.check(RequirementAction.RESEARCH_TECH, this.charge, player, null));

        progress.setTechResearched(player, this.charge, true);
        progress.setUpgradeLevel(player, this.armor, 1);

        Assertions.assertEquals(StandardRequirementResult.ALREADY_RESEARCHED,
                                system.check(RequirementAction.RESEARCH_TECH, this.charge, player, null));
        Assertions.assertEquals(StandardRequirementResult.MAXIMUM_LEVEL_REACHED,
                                system.check(RequirementAction.RESEARCH_UPGRADE, this.armor, player, null));
        Assertions.assertEquals(StandardRequirementResult.MET, system.check(RequirementAction.ISSUE_ORDER, this.patrol, player, null));
    }

    @Test
    void attach_should_reject_a_requirement_of_an_unregistered_type() {
        DeclarativeRequirements requirements = new DeclarativeRequirements.Builder()
                .requireBuild(new UnitType("unregistered"), Requirement.allOf())
                .build();

        Assertions.assertThrows(IllegalArgumentException.class, () -> this.createGame(requirements));
    }

    private @NotNull Game createGame(@NotNull DeclarativeRequirements requirements) {
        GameDefinition built = this.definition.useSystem(RequirementSystem.KEY, context -> new DeclarativeRequirementSystem(requirements)).build();
        GameSetup setup = new GameSetup.Builder(new MapReference("empty", new byte[0]))
                .addPlayer(new PlayerSlot(PLAYER, ControllerKind.HUMAN, 0, 0, null))
                .build();

        return Game.create(built, setup);
    }
}
