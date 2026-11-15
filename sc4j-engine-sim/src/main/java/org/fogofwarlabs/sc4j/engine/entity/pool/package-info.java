/**
 * Defines the entity pools: fixed-capacity slot storage with a documented allocation order.
 * <p>
 * An {@link org.fogofwarlabs.sc4j.engine.entity.pool.EntityPool} creates every entity object and every extension column
 * array when the game is created; allocation and release only relink slot indices
 * ({@link org.fogofwarlabs.sc4j.engine.entity.pool.SlotList}). Which free slot the next allocation takes follows the
 * pool's {@link org.fogofwarlabs.sc4j.engine.entity.pool.SlotPolicy}, so two runs with the same calls always use the same
 * slots, and a mod that must reproduce another engine's slot order supplies its own policy.
 */
package org.fogofwarlabs.sc4j.engine.entity.pool;
