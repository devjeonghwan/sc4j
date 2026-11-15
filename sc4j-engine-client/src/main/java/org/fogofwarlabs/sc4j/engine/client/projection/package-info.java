/**
 * Defines how a client shows the world plane on the screen ({@link org.fogofwarlabs.sc4j.engine.client.projection.WorldProjection}): world to
 * projected plane and back, depth order, footprint outlines, facing directions and map bounds. The engine defaults are the top-down
 * {@link org.fogofwarlabs.sc4j.engine.client.projection.SquareProjection} and the 2:1
 * {@link org.fogofwarlabs.sc4j.engine.client.projection.IsometricProjection}; the camera, the picker, the selection box, the world layers and
 * the minimap go through the projection of their client.
 */
package org.fogofwarlabs.sc4j.engine.client.projection;
