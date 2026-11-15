/**
 * Defines the engine video: the {@link org.fogofwarlabs.sc4j.engine.client.video.VideoStream} that decodes an in-memory clip (WebM with VP8 or
 * VP9 video, an alpha plane and an optional audio track) to RGBA frames and PCM samples, frame by frame or by playback time; the
 * {@link org.fogofwarlabs.sc4j.engine.client.video.VideoClip} that holds a whole decoded clip with the display time of each frame; and the
 * {@link org.fogofwarlabs.sc4j.engine.client.video.VideoClipDecoder} that decodes whole clips. The default decoder uses FFmpeg.
 */
package org.fogofwarlabs.sc4j.engine.client.video;
