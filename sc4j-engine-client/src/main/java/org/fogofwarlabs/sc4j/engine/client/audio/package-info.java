/**
 * Defines the engine audio: the mixer ({@link org.fogofwarlabs.sc4j.engine.client.audio.AudioMixer}) with its buses
 * ({@link org.fogofwarlabs.sc4j.engine.client.audio.AudioBus}), sound channels with replaceable channel rules
 * ({@link org.fogofwarlabs.sc4j.engine.client.audio.SoundChannels}, {@link org.fogofwarlabs.sc4j.engine.client.audio.ChannelPolicy},
 * {@link org.fogofwarlabs.sc4j.engine.client.audio.SoundCue}), music channels and the music player with ducking and fades
 * ({@link org.fogofwarlabs.sc4j.engine.client.audio.MusicPlayer}), the WAV and Ogg Vorbis decoder
 * ({@link org.fogofwarlabs.sc4j.engine.client.audio.WaveData}), streams, positional volume and stereo position
 * ({@link org.fogofwarlabs.sc4j.engine.client.audio.SoundPositioning}) and the background fade of the master bus
 * ({@link org.fogofwarlabs.sc4j.engine.client.audio.MasterFade}); and its output backend, the
 * {@link org.fogofwarlabs.sc4j.engine.client.audio.AudioDevice} with its {@link org.fogofwarlabs.sc4j.engine.client.audio.AudioBuffer}s and
 * {@link org.fogofwarlabs.sc4j.engine.client.audio.AudioVoice}s, and the {@link org.fogofwarlabs.sc4j.engine.client.audio.SilentAudioDevice}
 * that plays on a clock without output. The OpenAL default device is in {@code openal}.
 */
package org.fogofwarlabs.sc4j.engine.client.audio;
