package org.fogofwarlabs.sc4j.engine.client.view;

import org.fogofwarlabs.sc4j.engine.content.type.MotionProfile;
import org.fogofwarlabs.sc4j.engine.entity.Projectile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Reads one projectile in flight for the client: the projectile, its flight, its motion profile, its ground position and its heading. The view
 * stays current while the game runs. A projectile's pool slot is used again for later projectiles; the flight tells them apart, so a client
 * sees where a flight ended (its impact).
 */
public interface ProjectileView {
    /**
     * @return the projectile
     */
    @NotNull Projectile getProjectile();

    /**
     * @return the flight of the projectile's pool slot, which changes when the slot carries a new projectile
     */
    int getFlight();

    /**
     * @return the motion profile of the projectile, which names its picture, or {@code null} if it has none
     */
    @Nullable MotionProfile getMotionProfile();

    /**
     * @return the world x of the projectile's ground position
     */
    int getX();

    /**
     * @return the world y of the projectile's ground position
     */
    int getY();

    /**
     * @return the direction the projectile flies, {@code 0} up and clockwise in the world's directions
     */
    int getHeading();
}
