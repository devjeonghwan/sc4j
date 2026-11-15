/**
 * Defines the hooks: small, typed extension points at fixed places of a system, for differences too small to replace
 * the whole system.
 * <p>
 * The system that owns a place declares a {@link org.fogofwarlabs.sc4j.engine.hook.HookPoint} constant with the hook
 * interface and its {@link org.fogofwarlabs.sc4j.engine.hook.HookComposition}. A mod registers hook factories at
 * registration ({@link org.fogofwarlabs.sc4j.engine.hook.HookRegistry}); when a game is created, every factory creates its
 * hook once, in registration order ({@link org.fogofwarlabs.sc4j.engine.hook.HookTable}). The system resolves the
 * {@link org.fogofwarlabs.sc4j.engine.hook.HookList} of each of its points in its attach and calls the hooks by index;
 * nothing is looked up per call.
 * <p>
 * Determinism contract: the hooks of a point run in registration order, as the point's composition says; with no hook
 * the system runs its default behavior. A hook holds no simulation state of its own (state belongs to a system and its
 * state hash), draws random values only from the game generator with a source the mod documents, iterates entities in
 * pool order and uses no clock and no floating point. A hook that does nothing leaves the state hash unchanged.
 */
package org.fogofwarlabs.sc4j.engine.hook;
