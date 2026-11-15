/**
 * Defines the runtime host: the windowing library and the window with its OpenGL context
 * ({@link org.fogofwarlabs.sc4j.engine.client.host.ClientHost}, {@link org.fogofwarlabs.sc4j.engine.client.host.HostWindow}),
 * the frame loop that drives a {@link org.fogofwarlabs.sc4j.engine.client.host.HostApplication}, frame pacing
 * ({@link org.fogofwarlabs.sc4j.engine.client.host.FramePacer}, {@link org.fogofwarlabs.sc4j.engine.client.host.PresentThrottle}),
 * the aspect ratio range of a logical screen
 * ({@link org.fogofwarlabs.sc4j.engine.client.host.AspectRange}), the window's input as engine input events
 * ({@link org.fogofwarlabs.sc4j.engine.client.host.WindowInput}), the window's input method
 * ({@link org.fogofwarlabs.sc4j.engine.client.host.TextInputMethod}) and the platform services: the system pointer speed
 * ({@link org.fogofwarlabs.sc4j.engine.client.host.SystemPointerSpeed}) and the host tick clock
 * ({@link org.fogofwarlabs.sc4j.engine.client.host.HostTickClock}), and the frame phase timing diagnostic
 * ({@link org.fogofwarlabs.sc4j.engine.client.host.FrameTiming}).
 * <p>
 * Internal: the GLFW implementations {@link org.fogofwarlabs.sc4j.engine.client.host.GlfwClientHost},
 * {@link org.fogofwarlabs.sc4j.engine.client.host.GlfwHostWindow}, {@link org.fogofwarlabs.sc4j.engine.client.host.GlfwInput},
 * {@link org.fogofwarlabs.sc4j.engine.client.host.GlfwKeyCodes} and {@link org.fogofwarlabs.sc4j.engine.client.host.GlfwCursorSurface} and the
 * Windows platform services {@link org.fogofwarlabs.sc4j.engine.client.host.WindowsTextInputMethod},
 * {@link org.fogofwarlabs.sc4j.engine.client.host.WindowsSystemPointerSpeed} and
 * {@link org.fogofwarlabs.sc4j.engine.client.host.WindowsHostTickClock} are not part of the engine API ({@code docs/api-policy.md});
 * clients reach them through the interfaces ({@link org.fogofwarlabs.sc4j.engine.client.host.ClientHost#initialize},
 * {@link org.fogofwarlabs.sc4j.engine.client.host.HostWindow}, {@link org.fogofwarlabs.sc4j.engine.client.host.SystemPointerSpeed#getDefault},
 * {@link org.fogofwarlabs.sc4j.engine.client.host.HostTickClock#getDefault}).
 * The other classes are API.
 */
package org.fogofwarlabs.sc4j.engine.client.host;
