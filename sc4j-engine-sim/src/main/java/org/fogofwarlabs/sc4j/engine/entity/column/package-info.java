/**
 * Defines the extension columns of the entity pools: per-entity values a mod adds to an engine entity kind.
 * <p>
 * A mod declares a {@link org.fogofwarlabs.sc4j.engine.entity.column.ColumnKey} as a constant and registers it for an
 * entity kind. When a game is created, each pool creates one primitive array per registered column, indexed by the
 * entity index, in registration order ({@link org.fogofwarlabs.sc4j.engine.entity.column.ColumnTable}). A system
 * resolves its columns once, when it is attached, and keeps the typed column in a field; the hot path then reads and
 * writes plain arrays without lookup, boxing or allocation. A reference to another entity is stored as its index in an
 * {@link org.fogofwarlabs.sc4j.engine.entity.column.IntColumn}, {@code 0} for none.
 */
package org.fogofwarlabs.sc4j.engine.entity.column;
