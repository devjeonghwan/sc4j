/**
 * Holds the engine input model: {@link org.fogofwarlabs.sc4j.engine.client.input.InputEvent} (keys, typed characters, pointer
 * movements, button presses and releases, wheel movements) with {@link org.fogofwarlabs.sc4j.engine.client.input.Key},
 * {@link org.fogofwarlabs.sc4j.engine.client.input.KeyModifier} and {@link org.fogofwarlabs.sc4j.engine.client.input.PointerButton},
 * the {@link org.fogofwarlabs.sc4j.engine.client.input.InputListener} that receives the events, the
 * {@link org.fogofwarlabs.sc4j.engine.client.input.DoublePressDetector} of button presses, and the key binding table of an input mode
 * ({@link org.fogofwarlabs.sc4j.engine.client.input.KeyBindingTable}, {@link org.fogofwarlabs.sc4j.engine.client.input.KeyBinding}).
 * <p>
 * The GLFW host feeds the events ({@link org.fogofwarlabs.sc4j.engine.client.host.WindowInput}); a scene stack routes them to its top
 * scene ({@link org.fogofwarlabs.sc4j.engine.client.scene.SceneStack#handleInput}). An
 * {@link org.fogofwarlabs.sc4j.engine.client.input.InputScript} feeds scripted window input to a hidden or offscreen client in tests.
 */
package org.fogofwarlabs.sc4j.engine.client.input;
