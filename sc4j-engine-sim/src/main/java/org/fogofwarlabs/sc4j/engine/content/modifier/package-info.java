/**
 * Defines the stats and the stat modifiers through which upgrades and techs change type values.
 * <p>
 * A mod adds modifiers per stat to the content; the game applies them in registration order
 * ({@link org.fogofwarlabs.sc4j.engine.content.table.GameContent#applyModifiers}). The engine supplies the common
 * modifiers: a bonus per upgrade level, a bonus for a researched tech, and the armor and weapon damage upgrades read from
 * the table columns.
 */
package org.fogofwarlabs.sc4j.engine.content.modifier;
