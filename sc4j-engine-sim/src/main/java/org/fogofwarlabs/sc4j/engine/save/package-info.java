/**
 * Defines saved-game formats ({@link org.fogofwarlabs.sc4j.engine.save.SaveFormat}, the format of a mod's saved games) and the engine save
 * format, the default of a mod without its own ({@link org.fogofwarlabs.sc4j.engine.save.EngineSaveFormat}): a saved game
 * ({@link org.fogofwarlabs.sc4j.engine.save.EngineSave}) with the identity of the saving mod, its setup and the state snapshot of the game
 * ({@link org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshot}), refused when another mod identity loads it
 * ({@link org.fogofwarlabs.sc4j.engine.mod.identity.ModIdentityException}).
 */
package org.fogofwarlabs.sc4j.engine.save;
