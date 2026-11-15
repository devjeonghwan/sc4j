package org.fogofwarlabs.sc4j.engine.pack;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.resource.audio.SoundClip;
import org.jetbrains.annotations.NotNull;

import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.ByteArrayInputStream;
import java.io.IOException;

/**
 * Reads a WAV file of a pack into a {@link SoundClip}: uncompressed PCM of {@code 8} or {@code 16} bits, converted to
 * signed {@code 16}-bit samples.
 */
final class PackSoundUtils {
    /**
     * The sample size of the decoded samples, in bits.
     */
    private static final int SAMPLE_BITS = 16;
    /**
     * The number of bytes of one decoded sample.
     */
    private static final int SAMPLE_BYTES = 2;
    /**
     * The mask of the low byte of a little-endian sample.
     */
    private static final int LOW_BYTE_MASK = 0xFF;
    /**
     * The shift of the high byte of a little-endian sample.
     */
    private static final int HIGH_BYTE_SHIFT = 8;

    /**
     * Reads a sound.
     *
     * @param source the pack files
     * @param path   the pack path of the WAV file
     * @return the sound clip
     * @throws IOException if the file cannot be read or is not uncompressed PCM
     */
    static @NotNull SoundClip read(@NotNull PackSource source, @NotNull String path) throws IOException {
        byte[] contents = source.read(path);

        try (AudioInputStream input = AudioSystem.getAudioInputStream(new ByteArrayInputStream(contents))) {
            AudioFormat format = input.getFormat();
            AudioFormat.Encoding encoding = format.getEncoding();

            if (!AudioFormat.Encoding.PCM_SIGNED.equals(encoding) && !AudioFormat.Encoding.PCM_UNSIGNED.equals(encoding)) {
                throw Exceptions.newIOException("{0}: the sound is not uncompressed PCM: {1}", path, encoding);
            }

            AudioFormat decoded = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, format.getSampleRate(), SAMPLE_BITS, format.getChannels(),
                                                  format.getChannels() * SAMPLE_BYTES, format.getSampleRate(), false);

            try (AudioInputStream converted = AudioSystem.getAudioInputStream(decoded, input)) {
                byte[] bytes = converted.readAllBytes();
                short[] samples = new short[bytes.length / SAMPLE_BYTES];

                for (int index = 0; index < samples.length; index++) {
                    samples[index] = (short) (bytes[index * SAMPLE_BYTES] & LOW_BYTE_MASK | bytes[index * SAMPLE_BYTES + 1] << HIGH_BYTE_SHIFT);
                }

                return new SoundClip(Math.round(format.getSampleRate()), format.getChannels(), samples);
            }
        } catch (UnsupportedAudioFileException | IllegalArgumentException exception) {
            throw Exceptions.newIOException(exception, "{0}: the sound is not a readable WAV file: {1}", path, exception.getMessage());
        }
    }

    private PackSoundUtils() {
    }
}
