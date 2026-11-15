package org.fogofwarlabs.sc4j.engine.content;

import org.fogofwarlabs.sc4j.engine.content.definition.*;
import org.fogofwarlabs.sc4j.engine.content.economy.ResourceCost;
import org.fogofwarlabs.sc4j.engine.content.economy.ResourceKind;
import org.fogofwarlabs.sc4j.engine.content.faction.Faction;
import org.fogofwarlabs.sc4j.engine.content.faction.FactionDefinition;
import org.fogofwarlabs.sc4j.engine.content.modifier.ArmorUpgradeModifier;
import org.fogofwarlabs.sc4j.engine.content.modifier.Stat;
import org.fogofwarlabs.sc4j.engine.content.registry.ContentRegistry;
import org.fogofwarlabs.sc4j.engine.content.requirement.*;
import org.fogofwarlabs.sc4j.engine.content.requirement.declarative.CompletedUnitCondition;
import org.fogofwarlabs.sc4j.engine.content.requirement.declarative.DeclarativeRequirementSystem;
import org.fogofwarlabs.sc4j.engine.content.requirement.declarative.DeclarativeRequirements;
import org.fogofwarlabs.sc4j.engine.content.requirement.declarative.Requirement;
import org.fogofwarlabs.sc4j.engine.content.table.GameContent;
import org.fogofwarlabs.sc4j.engine.content.table.UnitTypeTable;
import org.fogofwarlabs.sc4j.engine.content.type.*;
import org.fogofwarlabs.sc4j.engine.entity.Unit;
import org.fogofwarlabs.sc4j.engine.game.*;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the content model headless, without a mod: types of every kind registered through the game definition, per-game
 * tables changed by map settings and during the game, an upgrade modifier, a declarative requirement and the state hash.
 */
final class ContentModelTest {
    private static final int FRAME_COUNT = 120;
    private static final int RESEARCH_FRAME = 40;
    private static final int TRIGGER_FRAME = 80;
    private static final int PLAYER_COUNT = 4;
    private static final int HUMAN = 0;
    private static final int ORC = 1;

    private static final int FOOTMAN_HIT_POINTS = 60;
    private static final int FOOTMAN_SIGHT_RANGE = 7;
    private static final int MAP_FOOTMAN_HIT_POINTS = 75;
    private static final int TRIGGER_FOOTMAN_HIT_POINTS = 90;
    private static final int FOOTMAN_ARMOR = 2;
    private static final int FOOTMAN_GOLD = 60;
    private static final int MAP_FOOTMAN_GOLD = 40;

    private final @NotNull ResourceKind gold;
    private final @NotNull Faction humans;
    private final @NotNull Faction orcs;
    private final @NotNull UnitType peasant;
    private final @NotNull UnitType townHall;
    private final @NotNull UnitType barracks;
    private final @NotNull UnitType footman;
    private final @NotNull UnitType grunt;
    private final @NotNull WeaponType sword;
    private final @NotNull MotionProfile walker;
    private final @NotNull SpriteType footmanSprite;
    private final @NotNull ImageType footmanImage;
    private final @NotNull UpgradeType shields;
    private final @NotNull TechType rally;
    private final @NotNull OrderType attack;
    private final @NotNull SoundType clash;

    ContentModelTest() {
        this.gold          = new ResourceKind("gold");
        this.humans        = new Faction("humans");
        this.orcs          = new Faction("orcs");
        this.peasant       = new UnitType("peasant");
        this.townHall      = new UnitType("town hall");
        this.barracks      = new UnitType("barracks");
        this.footman       = new UnitType("footman");
        this.grunt         = new UnitType("grunt");
        this.sword         = new WeaponType("sword");
        this.walker        = new MotionProfile("walker");
        this.footmanSprite = new SpriteType("footman sprite");
        this.footmanImage  = new ImageType("footman image");
        this.shields       = new UpgradeType("shields");
        this.rally         = new TechType("rally");
        this.attack        = new OrderType("attack");
        this.clash         = new SoundType("clash");
    }

    @Test
    void content_tables_follow_map_settings_research_and_triggers() {
        Game game = this.createGame(FOOTMAN_SIGHT_RANGE);
        GameContent content = game.getContent();
        UnitTypeTable units = content.getUnitTypes();
        RequirementSystem requirements = game.getModSystem(RequirementSystem.KEY);

        Assertions.assertEquals(PLAYER_COUNT, content.getProgress().getPlayerCount());
        Assertions.assertEquals(MAP_FOOTMAN_HIT_POINTS, units.getHitPoints(this.footman));
        Assertions.assertEquals(MAP_FOOTMAN_GOLD, units.getCost(this.footman, this.gold));
        Assertions.assertSame(this.humans, units.getFaction(this.footman));
        Assertions.assertSame(this.peasant, content.getDefinition().getFactions().getDefinition(this.humans).getWorker());
        Assertions.assertSame(this.barracks,
                              Assertions.assertInstanceOf(MissingUnitResult.class, this.checkFootman(game, requirements)).getUnitType());

        this.runFrames(game, FRAME_COUNT);

        int footmanIndex = this.footman.getIndex();
        int footmanArmor = units.getArmor(footmanIndex);

        Assertions.assertEquals(TRIGGER_FOOTMAN_HIT_POINTS, units.getHitPoints(footmanIndex));
        Assertions.assertEquals(FOOTMAN_ARMOR + 1, content.applyModifiers(Stat.ARMOR, game.getPlayer(HUMAN), this.footman, null, footmanArmor));
        Assertions.assertEquals(FOOTMAN_ARMOR, content.applyModifiers(Stat.ARMOR, game.getPlayer(ORC), this.footman, null, footmanArmor));
        Assertions.assertEquals(StandardRequirementResult.MET, this.checkFootman(game, requirements));
        Assertions.assertEquals(StandardRequirementResult.MAXIMUM_LEVEL_REACHED,
                                requirements.check(RequirementAction.RESEARCH_UPGRADE, this.shields, game.getPlayer(HUMAN), null));
    }

    @Test
    void state_hash_covers_the_content_tables() {
        Game first = this.createGame(FOOTMAN_SIGHT_RANGE);
        Game second = this.createGame(FOOTMAN_SIGHT_RANGE);
        Game other = this.createGame(FOOTMAN_SIGHT_RANGE + 1);

        this.runFrames(first, FRAME_COUNT);
        this.runFrames(second, FRAME_COUNT);
        this.runFrames(other, FRAME_COUNT);

        Assertions.assertEquals(first.computeStateHash(), second.computeStateHash());

        second.getContent().getWeaponTypes().setCooldown(this.sword, 1);

        Assertions.assertNotEquals(first.computeStateHash(), second.computeStateHash());
        Assertions.assertNotEquals(first.computeStateHash(), other.computeStateHash());
    }

    private @NotNull RequirementResult checkFootman(@NotNull Game game, @NotNull RequirementSystem requirements) {
        Unit barracks = new Unit(1);

        barracks.setType(this.barracks);

        return requirements.check(RequirementAction.BUILD_UNIT, this.footman, game.getPlayer(HUMAN), barracks);
    }

    private @NotNull Game createGame(int footmanSightRange) {
        GameDefinition.Builder builder = new GameDefinition.Builder();
        ContentRegistry content = builder.getContent();

        content.setPlayerCount(PLAYER_COUNT);
        content.getResourceKinds().register(this.gold);
        content.getFactions().register(this.humans, new FactionDefinition.Builder().setWorker(this.peasant).setTownHall(this.townHall)
                                                                                   .addResourceKind(this.gold).addStartUnit(this.townHall)
                                                                                   .addStartUnit(this.peasant).build());
        content.getFactions().register(this.orcs, new FactionDefinition.Builder().addResourceKind(this.gold).build());
        content.getSoundTypes().register(this.clash, new SoundDefinition.Builder().setPriority(1).setVolume(100).build());
        content.getImageTypes().register(this.footmanImage, new ImageDefinition.Builder().setTurning(true).setClickable(true).build());
        content.getSpriteTypes().register(this.footmanSprite, new SpriteDefinition.Builder().setImage(this.footmanImage).build());
        content.getMotionProfiles().register(this.walker, new MotionProfileDefinition.Builder().setSprite(this.footmanSprite).setTopSpeed(4).build());
        content.getUpgradeTypes().register(this.shields, new UpgradeDefinition.Builder().setFaction(this.humans).setMaximumLevel(1)
                                                                                        .setBaseCost(ResourceCost.of(this.gold, 100)).build());
        content.getTechTypes().register(this.rally, new TechDefinition.Builder().setFaction(this.humans).build());
        content.getWeaponTypes().register(this.sword, new WeaponDefinition.Builder().setDamage(6).setCooldown(15).addTarget(WeaponTarget.GROUND)
                                                                                    .build());
        content.getOrderTypes().register(this.attack, new OrderDefinition.Builder().setWeapon(this.sword).setInterruptible(true).build());
        content.getUnitTypes().register(this.peasant, this.createUnit(this.humans, 30).addTrait(UnitTrait.WORKER).build());
        content.getUnitTypes().register(this.townHall, this.createUnit(this.humans, 1200).addTrait(UnitTrait.BUILDING)
                                                           .addTrait(UnitTrait.RESOURCE_DEPOT).build());
        content.getUnitTypes().register(this.barracks, this.createUnit(this.humans, 800).addTrait(UnitTrait.BUILDING).build());

        UnitDefinition footmanDefinition = this.createUnit(this.humans, FOOTMAN_HIT_POINTS).setArmor(FOOTMAN_ARMOR).setArmorUpgrade(this.shields)
                                               .setMotionProfile(this.walker)
                                               .setSightRange(footmanSightRange).setGroundWeapon(this.sword)
                                               .setCost(ResourceCost.of(this.gold, FOOTMAN_GOLD)).build();

        content.getUnitTypes().register(this.footman, footmanDefinition);
        content.getUnitTypes().register(this.grunt, this.createUnit(this.orcs, 70).setGroundWeapon(this.sword).build());
        content.addModifier(Stat.ARMOR, new ArmorUpgradeModifier(1));

        DeclarativeRequirements requirements = new DeclarativeRequirements.Builder()
                .requireBuild(this.footman, Requirement.allOf(new CompletedUnitCondition(this.barracks)))
                .build();

        builder.useSystem(RequirementSystem.KEY, context -> new DeclarativeRequirementSystem(requirements));
        builder.addFramePhase(game -> {
            int frame = game.getClock().getFrame();

            if (frame == RESEARCH_FRAME) {
                game.getContent().getProgress().setUpgradeLevel(game.getPlayer(HUMAN), this.shields, 1);
                game.getContent().getProgress().setCompletedUnitCount(game.getPlayer(HUMAN), this.barracks, 1);
            }

            if (frame == TRIGGER_FRAME) {
                game.getContent().getUnitTypes().setHitPoints(this.footman.getIndex(), TRIGGER_FOOTMAN_HIT_POINTS);
            }
        });

        GameSetup setup = new GameSetup.Builder(new MapReference("empty", new byte[0]))
                .addPlayer(new PlayerSlot(HUMAN, ControllerKind.HUMAN, 0, 0, this.humans))
                .addPlayer(new PlayerSlot(ORC, ControllerKind.COMPUTER, 1, 1, this.orcs))
                .build();
        Game game = Game.create(builder.build(), setup);

        // Map settings write the tables before the first frame.
        game.getContent().getUnitTypes().setHitPoints(this.footman, MAP_FOOTMAN_HIT_POINTS);
        game.getContent().getUnitTypes().setCost(this.footman, this.gold, MAP_FOOTMAN_GOLD);

        return game;
    }

    private @NotNull UnitDefinition.Builder createUnit(@NotNull Faction faction, int hitPoints) {
        return new UnitDefinition.Builder().setFaction(faction).setHitPoints(hitPoints);
    }

    private void runFrames(@NotNull Game game, int count) {
        for (int frame = 0; frame < count; frame++) {
            game.getFrameRunner().runFrame();
        }
    }
}
