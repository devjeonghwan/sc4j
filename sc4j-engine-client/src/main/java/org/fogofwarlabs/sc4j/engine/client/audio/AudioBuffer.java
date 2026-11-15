package org.fogofwarlabs.sc4j.engine.client.audio;

/**
 * Holds the samples of one {@link org.fogofwarlabs.sc4j.engine.resource.audio.SoundClip} on an {@link AudioDevice}, ready for its voices.
 */
public interface AudioBuffer {
    /**
     * @return the number of sample frames per second
     */
    int getSampleRate();

    /**
     * @return the number of channels
     */
    int getChannelCount();

    /**
     * @return the number of sample frames
     */
    int getFrameCount();


    /**
     * Releases the samples. A released buffer must not be used again; a voice must not hold it when it is released. Never throws.
     */
    void release();
}
