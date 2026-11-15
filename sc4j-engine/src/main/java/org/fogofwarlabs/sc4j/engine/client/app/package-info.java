/**
 * Runs the client of an engine-only mod: the runtime opens the window and the devices, starts the mod's first scene
 * ({@link org.fogofwarlabs.sc4j.engine.client.app.FirstSceneFactory}) on a
 * {@link org.fogofwarlabs.sc4j.engine.client.app.SceneClient} and runs its main loop (input, scene update, drawing) in a
 * {@link org.fogofwarlabs.sc4j.engine.client.app.WindowClient}. Scenes of such a client are
 * {@link org.fogofwarlabs.sc4j.engine.client.app.ClientScene}s; the engine default scene of stored games
 * ({@link org.fogofwarlabs.sc4j.engine.client.app.GameFileListScene}) lists the replays or saved games a mod's registered formats read.
 */
package org.fogofwarlabs.sc4j.engine.client.app;
