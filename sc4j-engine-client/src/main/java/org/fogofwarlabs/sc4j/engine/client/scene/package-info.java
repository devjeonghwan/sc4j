/**
 * Defines the scene stack of a client: a {@link org.fogofwarlabs.sc4j.engine.client.scene.Scene} is one screen (a menu, a
 * lobby, the game view) with its own open, update, draw and close; the
 * {@link org.fogofwarlabs.sc4j.engine.client.scene.SceneStack} holds the current scene, switches scenes on request (at the
 * next frame, optionally through a {@link org.fogofwarlabs.sc4j.engine.client.scene.SceneTransition}) and keeps modal scenes
 * over it. A {@link org.fogofwarlabs.sc4j.engine.client.scene.LoadingService} runs
 * {@link org.fogofwarlabs.sc4j.engine.client.scene.LoadingTask}s in time slices while a loading screen draws their title and progress.
 */
package org.fogofwarlabs.sc4j.engine.client.scene;
