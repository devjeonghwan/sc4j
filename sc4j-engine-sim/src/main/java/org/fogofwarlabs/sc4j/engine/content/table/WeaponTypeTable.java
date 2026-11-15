package org.fogofwarlabs.sc4j.engine.content.table;

import org.fogofwarlabs.sc4j.engine.content.combat.DamageKind;
import org.fogofwarlabs.sc4j.engine.content.definition.WeaponDefinition;
import org.fogofwarlabs.sc4j.engine.content.definition.WeaponTarget;
import org.fogofwarlabs.sc4j.engine.content.registry.ContentDefinition;
import org.fogofwarlabs.sc4j.engine.content.registry.TableKind;
import org.fogofwarlabs.sc4j.engine.content.type.*;
import org.fogofwarlabs.sc4j.engine.state.StateHash;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotReader;
import org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotWriter;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Holds the per-game values of every {@link WeaponType} as one array per column, indexed by the type index.
 * <p>
 * The engine columns, in state hash order, are {@code damage}, {@code damageBonus}, {@code damageFactor},
 * {@code damageKind}, {@code upgrade}, {@code cooldown}, {@code minimumRange}, {@code maximumRange}, {@code targets},
 * {@code projectile}, {@code splashInnerRadius}, {@code splashMiddleRadius}, {@code splashOuterRadius} and {@code homing}. A type
 * reference holds the index of the referenced type, {@link ContentType#NO_INDEX} for none. The extension columns the
 * mod registered for {@link TableKind#WEAPON} follow ({@link #getExtensionColumns()}).
 * <p>
 * The values start as the {@link WeaponDefinition} of each type and may be written during the game; see {@link TypeTable}
 * for the write rules. Every accessor is a plain array access: an index outside the table throws
 * {@link ArrayIndexOutOfBoundsException}.
 */
public final class WeaponTypeTable extends TypeTable {
    private final int @NotNull [] damage;
    private final int @NotNull [] damageBonus;
    private final int @NotNull [] damageFactor;
    private final int @NotNull [] damageKind;
    private final int @NotNull [] upgrade;
    private final int @NotNull [] cooldown;
    private final int @NotNull [] minimumRange;
    private final int @NotNull [] maximumRange;
    private final int @NotNull [] targets;
    private final int @NotNull [] projectile;
    private final int @NotNull [] splashInnerRadius;
    private final int @NotNull [] splashMiddleRadius;
    private final int @NotNull [] splashOuterRadius;
    private final boolean @NotNull [] homing;

    /**
     * Creates a {@link WeaponTypeTable} instance from the definitions of a content.
     *
     * @param content the content definition
     * @throws IllegalArgumentException if a definition refers to a type that is not registered in the content
     */
    WeaponTypeTable(@NotNull ContentDefinition content) {
        super(content, content.getWeaponTypes().getCount(), content.getColumns(TableKind.WEAPON));

        DefinitionRegistry<WeaponType, WeaponDefinition> definitions = content.getWeaponTypes();
        int count = this.getCount();

        this.damage             = new int[count];
        this.damageBonus        = new int[count];
        this.damageFactor       = new int[count];
        this.damageKind         = new int[count];
        this.upgrade            = new int[count];
        this.cooldown           = new int[count];
        this.minimumRange       = new int[count];
        this.maximumRange       = new int[count];
        this.targets            = new int[count];
        this.projectile         = new int[count];
        this.splashInnerRadius  = new int[count];
        this.splashMiddleRadius = new int[count];
        this.splashOuterRadius  = new int[count];
        this.homing             = new boolean[count];

        for (int index = 0; index < count; index++) {
            WeaponDefinition definition = definitions.getDefinition(index);

            this.damage[index]             = definition.getDamage();
            this.damageBonus[index]        = definition.getDamageBonus();
            this.damageFactor[index]       = definition.getDamageFactor();
            this.damageKind[index]         = content.getDamageKinds().requireIndex(definition.getDamageKind());
            this.upgrade[index]            = content.getUpgradeTypes().getTypes().requireIndex(definition.getUpgrade());
            this.cooldown[index]           = definition.getCooldown();
            this.minimumRange[index]       = definition.getMinimumRange();
            this.maximumRange[index]       = definition.getMaximumRange();
            this.targets[index]            = definition.getTargets();
            this.projectile[index]         = content.getMotionProfiles().getTypes().requireIndex(definition.getProjectile());
            this.splashInnerRadius[index]  = definition.getSplashInnerRadius();
            this.splashMiddleRadius[index] = definition.getSplashMiddleRadius();
            this.splashOuterRadius[index]  = definition.getSplashOuterRadius();
            this.homing[index]             = definition.isHoming();
        }

        this.initializeExtensionColumns();
    }

    /**
     * @param type the weapon type
     * @return the base damage of one hit
     */
    public int getDamage(@NotNull WeaponType type) {
        return this.damage[type.getIndex()];
    }

    /**
     * @param typeIndex the index of the weapon type
     * @return the base damage of one hit
     */
    public int getDamage(int typeIndex) {
        return this.damage[typeIndex];
    }

    /**
     * Sets the base damage of one hit.
     *
     * @param type  the weapon type
     * @param value the value
     */
    public void setDamage(@NotNull WeaponType type, int value) {
        this.damage[type.getIndex()] = value;

        this.recordWrite();
    }

    /**
     * Sets the base damage of one hit.
     *
     * @param typeIndex the index of the weapon type
     * @param value     the value
     */
    public void setDamage(int typeIndex, int value) {
        this.damage[typeIndex] = value;

        this.recordWrite();
    }

    /**
     * @param type the weapon type
     * @return the damage added per level of the weapon upgrade
     */
    public int getDamageBonus(@NotNull WeaponType type) {
        return this.damageBonus[type.getIndex()];
    }

    /**
     * @param typeIndex the index of the weapon type
     * @return the damage added per level of the weapon upgrade
     */
    public int getDamageBonus(int typeIndex) {
        return this.damageBonus[typeIndex];
    }

    /**
     * Sets the damage added per level of the weapon upgrade.
     *
     * @param type  the weapon type
     * @param value the value
     */
    public void setDamageBonus(@NotNull WeaponType type, int value) {
        this.damageBonus[type.getIndex()] = value;

        this.recordWrite();
    }

    /**
     * Sets the damage added per level of the weapon upgrade.
     *
     * @param typeIndex the index of the weapon type
     * @param value     the value
     */
    public void setDamageBonus(int typeIndex, int value) {
        this.damageBonus[typeIndex] = value;

        this.recordWrite();
    }

    /**
     * @param type the weapon type
     * @return the number of hits of one attack
     */
    public int getDamageFactor(@NotNull WeaponType type) {
        return this.damageFactor[type.getIndex()];
    }

    /**
     * @param typeIndex the index of the weapon type
     * @return the number of hits of one attack
     */
    public int getDamageFactor(int typeIndex) {
        return this.damageFactor[typeIndex];
    }

    /**
     * Sets the number of hits of one attack.
     *
     * @param type  the weapon type
     * @param value the value
     */
    public void setDamageFactor(@NotNull WeaponType type, int value) {
        this.damageFactor[type.getIndex()] = value;

        this.recordWrite();
    }

    /**
     * Sets the number of hits of one attack.
     *
     * @param typeIndex the index of the weapon type
     * @param value     the value
     */
    public void setDamageFactor(int typeIndex, int value) {
        this.damageFactor[typeIndex] = value;

        this.recordWrite();
    }

    /**
     * @param type the weapon type
     * @return the damage kind, or {@code null} for none
     * @throws IndexOutOfBoundsException if the stored index names no registered damage kind
     */
    public @Nullable DamageKind getDamageKind(@NotNull WeaponType type) {
        return resolve(this.getContent().getDamageKinds(), this.damageKind[type.getIndex()]);
    }

    /**
     * @param typeIndex the index of the weapon type
     * @return the index of the damage kind, or {@link ContentType#NO_INDEX} for none
     */
    public int getDamageKind(int typeIndex) {
        return this.damageKind[typeIndex];
    }

    /**
     * Sets the damage kind.
     *
     * @param type  the weapon type
     * @param value the damage kind registered in the same content, or {@code null} for none
     */
    public void setDamageKind(@NotNull WeaponType type, @Nullable DamageKind value) {
        this.damageKind[type.getIndex()] = ContentType.indexOf(value);

        this.recordWrite();
    }

    /**
     * Sets the damage kind.
     *
     * @param typeIndex the index of the weapon type
     * @param value     the index of the damage kind, or {@link ContentType#NO_INDEX} for none
     */
    public void setDamageKind(int typeIndex, int value) {
        this.damageKind[typeIndex] = value;

        this.recordWrite();
    }

    /**
     * @param type the weapon type
     * @return the upgrade that raises the damage, or {@code null} for none
     * @throws IndexOutOfBoundsException if the stored index names no registered type
     */
    public @Nullable UpgradeType getUpgrade(@NotNull WeaponType type) {
        return resolve(this.getContent().getUpgradeTypes().getTypes(), this.upgrade[type.getIndex()]);
    }

    /**
     * @param typeIndex the index of the weapon type
     * @return the index of the upgrade that raises the damage, or {@link ContentType#NO_INDEX} for none
     */
    public int getUpgradeIndex(int typeIndex) {
        return this.upgrade[typeIndex];
    }

    /**
     * Sets the upgrade that raises the damage.
     *
     * @param type  the weapon type
     * @param value the upgrade type registered in the same content, or {@code null} for none
     */
    public void setUpgrade(@NotNull WeaponType type, @Nullable UpgradeType value) {
        this.upgrade[type.getIndex()] = ContentType.indexOf(value);

        this.recordWrite();
    }

    /**
     * Sets the upgrade that raises the damage.
     *
     * @param typeIndex the index of the weapon type
     * @param value     the index of the referenced type, or {@link ContentType#NO_INDEX} for none
     */
    public void setUpgradeIndex(int typeIndex, int value) {
        this.upgrade[typeIndex] = value;

        this.recordWrite();
    }

    /**
     * @param type the weapon type
     * @return the frames between two attacks
     */
    public int getCooldown(@NotNull WeaponType type) {
        return this.cooldown[type.getIndex()];
    }

    /**
     * @param typeIndex the index of the weapon type
     * @return the frames between two attacks
     */
    public int getCooldown(int typeIndex) {
        return this.cooldown[typeIndex];
    }

    /**
     * Sets the frames between two attacks.
     *
     * @param type  the weapon type
     * @param value the value
     */
    public void setCooldown(@NotNull WeaponType type, int value) {
        this.cooldown[type.getIndex()] = value;

        this.recordWrite();
    }

    /**
     * Sets the frames between two attacks.
     *
     * @param typeIndex the index of the weapon type
     * @param value     the value
     */
    public void setCooldown(int typeIndex, int value) {
        this.cooldown[typeIndex] = value;

        this.recordWrite();
    }

    /**
     * @param type the weapon type
     * @return the minimum range, in the distance unit of the mod
     */
    public int getMinimumRange(@NotNull WeaponType type) {
        return this.minimumRange[type.getIndex()];
    }

    /**
     * @param typeIndex the index of the weapon type
     * @return the minimum range, in the distance unit of the mod
     */
    public int getMinimumRange(int typeIndex) {
        return this.minimumRange[typeIndex];
    }

    /**
     * Sets the minimum range, in the distance unit of the mod.
     *
     * @param type  the weapon type
     * @param value the value
     */
    public void setMinimumRange(@NotNull WeaponType type, int value) {
        this.minimumRange[type.getIndex()] = value;

        this.recordWrite();
    }

    /**
     * Sets the minimum range, in the distance unit of the mod.
     *
     * @param typeIndex the index of the weapon type
     * @param value     the value
     */
    public void setMinimumRange(int typeIndex, int value) {
        this.minimumRange[typeIndex] = value;

        this.recordWrite();
    }

    /**
     * @param type the weapon type
     * @return the maximum range, in the distance unit of the mod
     */
    public int getMaximumRange(@NotNull WeaponType type) {
        return this.maximumRange[type.getIndex()];
    }

    /**
     * @param typeIndex the index of the weapon type
     * @return the maximum range, in the distance unit of the mod
     */
    public int getMaximumRange(int typeIndex) {
        return this.maximumRange[typeIndex];
    }

    /**
     * Sets the maximum range, in the distance unit of the mod.
     *
     * @param type  the weapon type
     * @param value the value
     */
    public void setMaximumRange(@NotNull WeaponType type, int value) {
        this.maximumRange[type.getIndex()] = value;

        this.recordWrite();
    }

    /**
     * Sets the maximum range, in the distance unit of the mod.
     *
     * @param typeIndex the index of the weapon type
     * @param value     the value
     */
    public void setMaximumRange(int typeIndex, int value) {
        this.maximumRange[typeIndex] = value;

        this.recordWrite();
    }

    /**
     * @param type the weapon type
     * @return the targets the weapon can attack, a mask of {@link WeaponTarget#getMask()} bits
     */
    public int getTargets(@NotNull WeaponType type) {
        return this.targets[type.getIndex()];
    }

    /**
     * @param typeIndex the index of the weapon type
     * @return the targets the weapon can attack, a mask of {@link WeaponTarget#getMask()} bits
     */
    public int getTargets(int typeIndex) {
        return this.targets[typeIndex];
    }

    /**
     * Sets the targets the weapon can attack.
     *
     * @param type  the weapon type
     * @param value a mask of {@link WeaponTarget#getMask()} bits
     */
    public void setTargets(@NotNull WeaponType type, int value) {
        this.targets[type.getIndex()] = value;

        this.recordWrite();
    }

    /**
     * Sets the targets the weapon can attack.
     *
     * @param typeIndex the index of the weapon type
     * @param value     a mask of {@link WeaponTarget#getMask()} bits
     */
    public void setTargets(int typeIndex, int value) {
        this.targets[typeIndex] = value;

        this.recordWrite();
    }

    /**
     * Checks one of the targets the weapon can attack.
     *
     * @param type   the weapon type
     * @param target the target
     * @return {@code true} if the type has the target, {@code false} otherwise
     */
    public boolean hasTarget(@NotNull WeaponType type, @NotNull WeaponTarget target) {
        return (this.targets[type.getIndex()] & target.getMask()) != 0;
    }

    /**
     * Checks one of the targets the weapon can attack.
     *
     * @param typeIndex the index of the weapon type
     * @param target    the target
     * @return {@code true} if the type has the target, {@code false} otherwise
     */
    public boolean hasTarget(int typeIndex, @NotNull WeaponTarget target) {
        return (this.targets[typeIndex] & target.getMask()) != 0;
    }

    /**
     * @param type the weapon type
     * @return the motion profile of the projectile, or {@code null} for none
     * @throws IndexOutOfBoundsException if the stored index names no registered type
     */
    public @Nullable MotionProfile getProjectile(@NotNull WeaponType type) {
        return resolve(this.getContent().getMotionProfiles().getTypes(), this.projectile[type.getIndex()]);
    }

    /**
     * @param typeIndex the index of the weapon type
     * @return the index of the motion profile of the projectile, or {@link ContentType#NO_INDEX} for none
     */
    public int getProjectileIndex(int typeIndex) {
        return this.projectile[typeIndex];
    }

    /**
     * Sets the motion profile of the projectile.
     *
     * @param type  the weapon type
     * @param value the motion profile registered in the same content, or {@code null} for none
     */
    public void setProjectile(@NotNull WeaponType type, @Nullable MotionProfile value) {
        this.projectile[type.getIndex()] = ContentType.indexOf(value);

        this.recordWrite();
    }

    /**
     * Sets the motion profile of the projectile.
     *
     * @param typeIndex the index of the weapon type
     * @param value     the index of the referenced type, or {@link ContentType#NO_INDEX} for none
     */
    public void setProjectileIndex(int typeIndex, int value) {
        this.projectile[typeIndex] = value;

        this.recordWrite();
    }

    /**
     * @param type the weapon type
     * @return the radius of full splash damage, in pixels
     */
    public int getSplashInnerRadius(@NotNull WeaponType type) {
        return this.splashInnerRadius[type.getIndex()];
    }

    /**
     * @param typeIndex the index of the weapon type
     * @return the radius of full splash damage, in pixels
     */
    public int getSplashInnerRadius(int typeIndex) {
        return this.splashInnerRadius[typeIndex];
    }

    /**
     * Sets the radius of full splash damage, in pixels.
     *
     * @param type  the weapon type
     * @param value the value
     */
    public void setSplashInnerRadius(@NotNull WeaponType type, int value) {
        this.splashInnerRadius[type.getIndex()] = value;

        this.recordWrite();
    }

    /**
     * Sets the radius of full splash damage, in pixels.
     *
     * @param typeIndex the index of the weapon type
     * @param value     the value
     */
    public void setSplashInnerRadius(int typeIndex, int value) {
        this.splashInnerRadius[typeIndex] = value;

        this.recordWrite();
    }

    /**
     * @param type the weapon type
     * @return the radius of half splash damage, in pixels
     */
    public int getSplashMiddleRadius(@NotNull WeaponType type) {
        return this.splashMiddleRadius[type.getIndex()];
    }

    /**
     * @param typeIndex the index of the weapon type
     * @return the radius of half splash damage, in pixels
     */
    public int getSplashMiddleRadius(int typeIndex) {
        return this.splashMiddleRadius[typeIndex];
    }

    /**
     * Sets the radius of half splash damage, in pixels.
     *
     * @param type  the weapon type
     * @param value the value
     */
    public void setSplashMiddleRadius(@NotNull WeaponType type, int value) {
        this.splashMiddleRadius[type.getIndex()] = value;

        this.recordWrite();
    }

    /**
     * Sets the radius of half splash damage, in pixels.
     *
     * @param typeIndex the index of the weapon type
     * @param value     the value
     */
    public void setSplashMiddleRadius(int typeIndex, int value) {
        this.splashMiddleRadius[typeIndex] = value;

        this.recordWrite();
    }

    /**
     * @param type the weapon type
     * @return the radius of quarter splash damage, in pixels
     */
    public int getSplashOuterRadius(@NotNull WeaponType type) {
        return this.splashOuterRadius[type.getIndex()];
    }

    /**
     * @param typeIndex the index of the weapon type
     * @return the radius of quarter splash damage, in pixels
     */
    public int getSplashOuterRadius(int typeIndex) {
        return this.splashOuterRadius[typeIndex];
    }

    /**
     * Sets the radius of quarter splash damage, in pixels.
     *
     * @param type  the weapon type
     * @param value the value
     */
    public void setSplashOuterRadius(@NotNull WeaponType type, int value) {
        this.splashOuterRadius[type.getIndex()] = value;

        this.recordWrite();
    }

    /**
     * Sets the radius of quarter splash damage, in pixels.
     *
     * @param typeIndex the index of the weapon type
     * @param value     the value
     */
    public void setSplashOuterRadius(int typeIndex, int value) {
        this.splashOuterRadius[typeIndex] = value;

        this.recordWrite();
    }

    /**
     * @param type the weapon type
     * @return {@code true} if the projectile follows its target, {@code false} if it lands where the target stood at its launch
     */
    public boolean isHoming(@NotNull WeaponType type) {
        return this.homing[type.getIndex()];
    }

    /**
     * @param typeIndex the index of the weapon type
     * @return {@code true} if the projectile follows its target, {@code false} if it lands where the target stood at its launch
     */
    public boolean isHoming(int typeIndex) {
        return this.homing[typeIndex];
    }

    /**
     * Sets whether the projectile follows its target.
     *
     * @param type  the weapon type
     * @param value the value
     */
    public void setHoming(@NotNull WeaponType type, boolean value) {
        this.homing[type.getIndex()] = value;

        this.recordWrite();
    }

    /**
     * Sets whether the projectile follows its target.
     *
     * @param typeIndex the index of the weapon type
     * @param value     the value
     */
    public void setHoming(int typeIndex, boolean value) {
        this.homing[typeIndex] = value;

        this.recordWrite();
    }


    /**
     * Mixes the engine columns into the hash, column by column in the order of the class description, each in type
     * index order.
     *
     * @param hash the hash being computed
     */
    @Override
    void contributeEngineColumns(@NotNull StateHash hash) {
        mixColumn(hash, this.damage);
        mixColumn(hash, this.damageBonus);
        mixColumn(hash, this.damageFactor);
        mixColumn(hash, this.damageKind);
        mixColumn(hash, this.upgrade);
        mixColumn(hash, this.cooldown);
        mixColumn(hash, this.minimumRange);
        mixColumn(hash, this.maximumRange);
        mixColumn(hash, this.targets);
        mixColumn(hash, this.projectile);
        mixColumn(hash, this.splashInnerRadius);
        mixColumn(hash, this.splashMiddleRadius);
        mixColumn(hash, this.splashOuterRadius);
        mixColumn(hash, this.homing);
    }

    /**
     * Writes the engine columns, column by column in the order of their hash, each whole in type index order.
     *
     * @param writer the writer of the snapshot
     */
    @Override
    void writeEngineColumns(@NotNull StateSnapshotWriter writer) {
        writer.writeInts(this.damage);
        writer.writeInts(this.damageBonus);
        writer.writeInts(this.damageFactor);
        writer.writeInts(this.damageKind);
        writer.writeInts(this.upgrade);
        writer.writeInts(this.cooldown);
        writer.writeInts(this.minimumRange);
        writer.writeInts(this.maximumRange);
        writer.writeInts(this.targets);
        writer.writeInts(this.projectile);
        writer.writeInts(this.splashInnerRadius);
        writer.writeInts(this.splashMiddleRadius);
        writer.writeInts(this.splashOuterRadius);
        writer.writeBooleans(this.homing);
    }

    /**
     * Reads the engine columns back, column by column in the order of their hash, replacing the current values.
     *
     * @param reader the reader of the snapshot
     * @throws IOException if the snapshot ends early or a column holds another number of values
     */
    @Override
    void readEngineColumns(@NotNull StateSnapshotReader reader) throws IOException {
        reader.readInts(this.damage);
        reader.readInts(this.damageBonus);
        reader.readInts(this.damageFactor);
        reader.readInts(this.damageKind);
        reader.readInts(this.upgrade);
        reader.readInts(this.cooldown);
        reader.readInts(this.minimumRange);
        reader.readInts(this.maximumRange);
        reader.readInts(this.targets);
        reader.readInts(this.projectile);
        reader.readInts(this.splashInnerRadius);
        reader.readInts(this.splashMiddleRadius);
        reader.readInts(this.splashOuterRadius);
        reader.readBooleans(this.homing);
    }
}
