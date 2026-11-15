/**
 * Holds the engine client framework that every mod's client runs on: the runtime host (window, loop, frame pacing, aspect
 * ratio), the scene stack, the renderer abstraction with its OpenGL defaults, text, dialogs and controls, input events and
 * key bindings, input to command translation with selection and targeting, the console command framework, the audio mixer with
 * its OpenAL default, video clips with their FFmpeg default, the mod's settings store, the read-only game view, the world view with camera
 * and selection, the HUD blocks, and the game sessions with their default screen and replay autosave.
 * <p>
 * Client code reads the simulation and never changes it except through commands; it is not simulation code, so it may use
 * clocks and floating point.
 */
package org.fogofwarlabs.sc4j.engine.client;
