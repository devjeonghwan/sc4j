/**
 * Holds the OpenAL default of the engine audio output: {@link org.fogofwarlabs.sc4j.engine.client.audio.openal.OpenAlAudioDevice}, on the system
 * output or on a loopback device that renders into memory.
 * <p>
 * Internal: not part of the engine API ({@code docs/api-policy.md}); mods play sound through the engine audio output and open the
 * default device with {@link org.fogofwarlabs.sc4j.engine.client.audio.AudioDevice#openDefault}.
 */
package org.fogofwarlabs.sc4j.engine.client.audio.openal;
