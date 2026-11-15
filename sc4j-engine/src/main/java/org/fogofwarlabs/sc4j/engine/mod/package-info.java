/**
 * Defines the mod API: the {@link org.fogofwarlabs.sc4j.engine.mod.Mod} life cycle and what the runtime gives a mod
 * (context, settings, storage) and takes from it (registration).
 * <p>
 * A mod registers its simulation as a {@link org.fogofwarlabs.sc4j.engine.game.GameDefinition}. An engine-only mod also
 * registers the first scene of its client and a {@link org.fogofwarlabs.sc4j.engine.mod.HeadlessRunner}; a mod with its
 * own application loop registers a {@link org.fogofwarlabs.sc4j.engine.mod.ModApplication} that the runtime runs
 * instead.
 */
package org.fogofwarlabs.sc4j.engine.mod;
