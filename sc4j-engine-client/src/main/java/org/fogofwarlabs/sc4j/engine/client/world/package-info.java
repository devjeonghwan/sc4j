/**
 * Holds the engine default world view of a client: the {@link org.fogofwarlabs.sc4j.engine.client.world.Camera} (position, zoom limits) with
 * its slash commands ({@link org.fogofwarlabs.sc4j.engine.client.world.CameraCommandUtils}); the one camera scroll
 * {@link org.fogofwarlabs.sc4j.engine.client.world.CameraScroll} by keys and screen edges, stepped
 * ({@link org.fogofwarlabs.sc4j.engine.client.world.SteppedScrollRate} over a
 * {@link org.fogofwarlabs.sc4j.engine.client.world.ScrollStepTable}, the engine default
 * {@link org.fogofwarlabs.sc4j.engine.client.world.LinearScrollStepTable}) or continuous
 * ({@link org.fogofwarlabs.sc4j.engine.client.world.ContinuousScrollRate}); the scripted move
 * {@link org.fogofwarlabs.sc4j.engine.client.world.CameraTargetScroll}; camera slots and the alert position
 * ({@link org.fogofwarlabs.sc4j.engine.client.world.ViewJumps}); all of them move a
 * {@link org.fogofwarlabs.sc4j.engine.client.world.CameraControl} (the engine camera or a game's own); the
 * {@link org.fogofwarlabs.sc4j.engine.client.world.WorldPicker} that finds pointed targets by a
 * {@link org.fogofwarlabs.sc4j.engine.client.world.UnitPickOrder}, the
 * {@link org.fogofwarlabs.sc4j.engine.client.world.SelectionBox} for click, double-click and box selection by the selection rules, and the
 * default world layers: terrain ({@link org.fogofwarlabs.sc4j.engine.client.world.TerrainLayer} with
 * {@link org.fogofwarlabs.sc4j.engine.client.world.TerrainColors}), units ({@link org.fogofwarlabs.sc4j.engine.client.world.UnitLayer}),
 * selection ({@link org.fogofwarlabs.sc4j.engine.client.world.SelectionLayer}, status bars of
 * {@link org.fogofwarlabs.sc4j.engine.client.world.StatusBarUtils}) and fog ({@link org.fogofwarlabs.sc4j.engine.client.world.FogLayer}).
 * Every part reads the game through a {@link org.fogofwarlabs.sc4j.engine.client.view.GameView}.
 */
package org.fogofwarlabs.sc4j.engine.client.world;
