package org.fogofwarlabs.sc4j.demo.client;

import org.fogofwarlabs.sc4j.demo.content.DemoContent;
import org.fogofwarlabs.sc4j.engine.client.display.DetailSet;
import org.fogofwarlabs.sc4j.engine.client.display.DetailUtils;
import org.fogofwarlabs.sc4j.engine.client.world.ProjectileSprite;
import org.fogofwarlabs.sc4j.engine.client.world.UnitPose;
import org.fogofwarlabs.sc4j.engine.client.world.UnitSprite;
import org.fogofwarlabs.sc4j.engine.content.definition.UnitDefinition;
import org.fogofwarlabs.sc4j.engine.content.definition.UnitTrait;
import org.fogofwarlabs.sc4j.engine.content.registry.ContentDefinition;
import org.fogofwarlabs.sc4j.engine.content.type.DefinitionRegistry;
import org.fogofwarlabs.sc4j.engine.content.type.MotionProfile;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.resource.ResourceKey;
import org.fogofwarlabs.sc4j.engine.resource.graphics.SpriteSheet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * Binds the demo's unit types to their sprite sheets in the pack (the sheet of a type has the type's id) and to the frame columns of the
 * demo art: a unit sheet has {@value #UNIT_FACINGS} facing rows with the columns idle, walk a, walk b, attack wind-up, attack strike and
 * three death frames; a building sheet has one row with the columns foundation, frame, half, done and rubble, of which the construction shows
 * the first three over its thirds; a gold mine one row with its full and worked-out frames. The projectiles' motion profiles are bound to their
 * sheets the same way: the arrow flies in {@value #UNIT_FACINGS} facings and ends in a spark, the hurled stone tumbles in two frames and ends
 * in dust. Every sheet comes with its detail sheets of the pack ({@link DetailUtils#loadSpriteSheets}).
 */
public final class DemoSprites {
    /**
     * The facing rows of a unit sheet.
     */
    public static final int UNIT_FACINGS = 8;

    private static final int IDLE_COLUMN = 0;
    private static final int FIRST_WALK_COLUMN = 1;
    private static final int SECOND_WALK_COLUMN = 2;
    private static final int WIND_UP_COLUMN = 3;
    private static final int STRIKE_COLUMN = 4;
    private static final int FOUNDATION_COLUMN = 0;
    private static final int FRAME_COLUMN = 1;
    private static final int HALF_COLUMN = 2;
    private static final int DONE_COLUMN = 3;
    private static final int WALK_STEP_FRAMES = 4;
    private static final int FLIGHT_STEP_FRAMES = 3;
    private static final int IMPACT_STEP_FRAMES = 3;

    /**
     * The projectiles of the demo art: the motion profile's id (its sheet), its facing rows and the sheet of its impact.
     */
    private static final @NotNull List<@NotNull ProjectileArt> PROJECTILES = List.of(new ProjectileArt("arrow", UNIT_FACINGS, "spark"),
                                                                                     new ProjectileArt("hurled-stone", 1, "dust"));

    /**
     * Names the sheets of one projectile of the demo art.
     */
    private static final class ProjectileArt {
        private final @NotNull String id;
        private final int rows;
        private final @NotNull String impactId;

        private ProjectileArt(@NotNull String id, int rows, @NotNull String impactId) {
            this.id       = id;
            this.rows     = rows;
            this.impactId = impactId;
        }
    }

    /**
     * Loads the sprites of every unit type of the content that has a sheet in the pack, and of the projectiles of the demo art.
     *
     * @param content    the demo content with its pack
     * @param definition the content definition of the demo's games, with the unit types' traits
     * @return the created {@link DemoSprites} instance
     * @throws IOException if a sheet cannot be read
     */
    public static @NotNull DemoSprites load(@NotNull DemoContent content, @NotNull ContentDefinition definition) throws IOException {
        Map<UnitType, UnitSprite> sprites = new IdentityHashMap<>();
        DefinitionRegistry<UnitType, UnitDefinition> unitTypes = definition.getUnitTypes();

        for (UnitType type : content.getUnitTypes()) {
            String id = type.getDebugName();

            if (!content.getPack().getSpriteSheetIds().contains(id)) {
                continue;
            }

            DetailSet<SpriteSheet> sheets = DetailUtils.loadSpriteSheets(content.getPack(), ResourceKey.of(id));
            int traits = unitTypes.getDefinition(type).getTraits();

            if ((traits & UnitTrait.BUILDING.getMask()) != 0) {
                sprites.put(type, new UnitSprite.Builder(sheets, 1)
                        .setColumns(UnitPose.CONSTRUCTING, FOUNDATION_COLUMN, FRAME_COLUMN, HALF_COLUMN)
                        .setColumns(UnitPose.IDLE, DONE_COLUMN)
                        .build());
            } else if ((traits & UnitTrait.RESOURCE_CONTAINER.getMask()) != 0) {
                sprites.put(type, new UnitSprite.Builder(sheets, 1).setColumns(UnitPose.IDLE, IDLE_COLUMN).build());
            } else {
                sprites.put(type, new UnitSprite.Builder(sheets, UNIT_FACINGS)
                        .setColumns(UnitPose.IDLE, IDLE_COLUMN)
                        .setColumns(UnitPose.MOVING, FIRST_WALK_COLUMN, IDLE_COLUMN, SECOND_WALK_COLUMN, IDLE_COLUMN)
                        .setColumns(UnitPose.ATTACKING, WIND_UP_COLUMN, STRIKE_COLUMN, STRIKE_COLUMN)
                        .setStepFrames(WALK_STEP_FRAMES)
                        .build());
            }
        }

        Map<MotionProfile, ProjectileSprite> projectiles = new IdentityHashMap<>();

        for (MotionProfile type : definition.getMotionProfiles().getTypes().getTypes()) {
            for (ProjectileArt art : PROJECTILES) {
                if (art.id.equals(type.getDebugName())) {
                    DetailSet<SpriteSheet> flight = DetailUtils.loadSpriteSheets(content.getPack(), ResourceKey.of(art.id));
                    DetailSet<SpriteSheet> impact = DetailUtils.loadSpriteSheets(content.getPack(), ResourceKey.of(art.impactId));

                    projectiles.put(type, new ProjectileSprite(flight, art.rows, FLIGHT_STEP_FRAMES, impact, IMPACT_STEP_FRAMES));
                }
            }
        }

        return new DemoSprites(sprites, projectiles);
    }

    private final @NotNull Map<@NotNull UnitType, @NotNull UnitSprite> sprites;
    private final @NotNull Map<@NotNull MotionProfile, @NotNull ProjectileSprite> projectiles;

    private DemoSprites(@NotNull Map<@NotNull UnitType, @NotNull UnitSprite> sprites,
                        @NotNull Map<@NotNull MotionProfile, @NotNull ProjectileSprite> projectiles) {
        this.sprites     = sprites;
        this.projectiles = projectiles;
    }

    /**
     * Finds the sprite of a unit type.
     *
     * @param type the unit type
     * @return the sprite, or {@code null} for a type without a sheet
     */
    public @Nullable UnitSprite findSprite(@NotNull UnitType type) {
        return this.sprites.get(type);
    }

    /**
     * Finds the sprite of a projectile's motion profile.
     *
     * @param type the motion profile
     * @return the sprite, or {@code null} for a type without a sheet
     */
    public @Nullable ProjectileSprite findProjectileSprite(@NotNull MotionProfile type) {
        return this.projectiles.get(type);
    }
}
