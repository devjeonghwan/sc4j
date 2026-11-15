/**
 * Defines state snapshots: the complete simulation state of a game between two frames
 * ({@link org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshot}), written and read by every part of the state
 * ({@link org.fogofwarlabs.sc4j.engine.state.snapshot.SnapshotContributor}) through
 * {@link org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotWriter} and
 * {@link org.fogofwarlabs.sc4j.engine.state.snapshot.StateSnapshotReader}. A snapshot is restored into another game created from the same
 * definition and setup, which then runs on as the captured game; save games and replay seeks are built on it. Snapshots are optional for a
 * mod: every engine default system and pool contributes, so an engine-only mod gets complete snapshots.
 */
package org.fogofwarlabs.sc4j.engine.state.snapshot;
