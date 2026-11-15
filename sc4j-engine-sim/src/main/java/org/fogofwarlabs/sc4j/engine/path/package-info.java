/**
 * Defines the path finding system: path updates, waypoints and reachability of units over the terrain. The engine default
 * {@link org.fogofwarlabs.sc4j.engine.path.StandardPathfindingSystem} searches paths over the walk cells of the terrain with their clearance
 * (A* over the walk grid topology, string pulled into straight legs, structures blocking their cells); a mod with its own region model may route
 * over its {@link org.fogofwarlabs.sc4j.engine.path.RegionGraph} with the {@link org.fogofwarlabs.sc4j.engine.path.RegionRouteSearch}.
 */
package org.fogofwarlabs.sc4j.engine.path;
