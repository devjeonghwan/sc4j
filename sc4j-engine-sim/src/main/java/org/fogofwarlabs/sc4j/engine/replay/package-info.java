/**
 * Defines replay formats ({@link org.fogofwarlabs.sc4j.engine.replay.ReplayFormat}, the format of a mod's replay files) and the engine replay
 * format, the default of a mod without its own ({@link org.fogofwarlabs.sc4j.engine.replay.EngineReplayFormat}): a recorded game
 * ({@link org.fogofwarlabs.sc4j.engine.replay.EngineReplay}) with the identity of the recording mod, its setup, its commands and state hash
 * checkpoints; the recorder at the game's command source ({@link org.fogofwarlabs.sc4j.engine.replay.ReplayRecorder}), the
 * {@link org.fogofwarlabs.sc4j.engine.controller.ControllerSource#REPLAY} controller that plays a replay back
 * ({@link org.fogofwarlabs.sc4j.engine.replay.ReplayController}) and the refusal of a replay of another mod identity
 * ({@link org.fogofwarlabs.sc4j.engine.mod.identity.ModIdentityException}); a replay seek restores a state snapshot
 * ({@link org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshot}) and plays on from its loop
 * ({@link org.fogofwarlabs.sc4j.engine.replay.EngineReplay#createController(CheckpointMismatchListener, long)}).
 */
package org.fogofwarlabs.sc4j.engine.replay;
