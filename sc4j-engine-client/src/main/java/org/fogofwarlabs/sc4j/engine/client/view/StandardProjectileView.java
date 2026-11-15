package org.fogofwarlabs.sc4j.engine.client.view;

import org.fogofwarlabs.sc4j.engine.client.projection.WorldProjection;
import org.fogofwarlabs.sc4j.engine.content.type.MotionProfile;
import org.fogofwarlabs.sc4j.engine.entity.Projectile;
import org.fogofwarlabs.sc4j.engine.entity.pool.EntityPool;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Reads one projectile slot of an engine game's projectile pool: the engine default {@link ProjectileView}. The flight is the slot's pool
 * generation; the heading is converted from the projectile system's directions to the world directions.
 */
public final class StandardProjectileView implements ProjectileView {
    private final @NotNull EntityPool<@NotNull Projectile> pool;
    private final @NotNull Projectile projectile;
    private final int directionCount;

    /**
     * Creates a {@link StandardProjectileView} instance.
     *
     * @param pool           the game's projectile pool
     * @param projectile     the projectile of one slot of the pool
     * @param directionCount the number of direction steps of a full turn of the projectiles' headings
     */
    public StandardProjectileView(@NotNull EntityPool<@NotNull Projectile> pool, @NotNull Projectile projectile, int directionCount) {
        this.pool           = pool;
        this.projectile     = projectile;
        this.directionCount = directionCount;
    }

    /**
     * @return the projectile
     */
    @Override
    public @NotNull Projectile getProjectile() {
        return this.projectile;
    }

    /**
     * @return the flight of the projectile's pool slot, which changes when the slot carries a new projectile
     */
    @Override
    public int getFlight() {
        return this.pool.getGeneration(this.projectile.getIndex());
    }

    /**
     * @return the motion profile of the projectile, which names its picture, or {@code null} if it has none
     */
    @Override
    public @Nullable MotionProfile getMotionProfile() {
        return this.projectile.getMotionProfile();
    }

    /**
     * @return the world x of the projectile's ground position
     */
    @Override
    public int getX() {
        return this.projectile.getPositionX();
    }

    /**
     * @return the world y of the projectile's ground position
     */
    @Override
    public int getY() {
        return this.projectile.getPositionY();
    }

    /**
     * @return the direction the projectile flies, {@code 0} up and clockwise in the world's directions
     */
    @Override
    public int getHeading() {
        return WorldProjection.toWorldDirection(this.projectile.getHeading(), this.directionCount);
    }
}
