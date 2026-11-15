/**
 * Defines the per-game content tables: one primitive array per column, indexed by the type index.
 * <p>
 * {@link org.fogofwarlabs.sc4j.engine.content.table.GameContent} creates every table when the game is created. The values
 * are writable during the game, by the simulation only, and are part of the state hash; accessors take the type identity
 * or, for hot loops and mod adapters, the {@code int} index, and allocate nothing.
 */
package org.fogofwarlabs.sc4j.engine.content.table;
