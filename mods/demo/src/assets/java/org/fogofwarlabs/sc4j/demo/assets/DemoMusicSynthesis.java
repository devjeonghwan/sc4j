package org.fogofwarlabs.sc4j.demo.assets;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Synthesizes the demo's music at build time as 16-bit mono PCM WAV files {@code music/<id>.wav} of its pack: three pieces written for the demo,
 * each a sixteen-bar tune of a melody over a chord per bar, played by simple instruments: plucked strings (a Karplus-Strong delay line), a soft flute
 * (sine harmonics with vibrato), a pad of held chord tones, a plucked bass and, in the game pieces, a frame drum with taps. Each piece plays its
 * tune in three sections ({@link #ARRANGEMENT}): the tune, the tune again on the other melody instrument over a rolling arpeggio and another drum
 * pattern, and a quiet bridge over the second half of the chords without melody or drums. A small Schroeder reverb gives the room; the reverb tail
 * of the last bar is added onto the first bars, so each piece repeats without a seam, and every piece is brought to the same loudness. The string
 * noise comes from a fixed linear congruential sequence, so every run writes the same bytes.
 */
public final class DemoMusicSynthesis {
    private static final int EIGHTHS_PER_BAR = 8;
    private static final int EIGHTHS_PER_BEAT = 2;
    private static final double SECONDS_PER_MINUTE = 60;
    private static final double TAIL_SECONDS = 3;

    /**
     * The root mean square level every piece is brought to before the soft clip that rounds its loudest peaks, so the pieces sound equally loud.
     */
    private static final double LOUDNESS = 0.2;

    private static final double FULL_CYCLE = 2 * Math.PI;
    private static final int SECOND_HARMONIC = 2;
    private static final int THIRD_HARMONIC = 3;

    private static final int A4_NOTE = 69;
    private static final double A4_FREQUENCY = 440;
    private static final double SEMITONES_PER_OCTAVE = 12;
    private static final double OCTAVE_FREQUENCY_RATIO = 2;
    private static final int OCTAVE = 12;
    /**
     * The lowest root of a chord the arpeggio and the pad play as written; a lower chord plays an octave up.
     */
    private static final int LOWEST_CHORD_ROOT = 48;

    private static final int SAMPLE_RATE = DemoSoundSynthesis.SAMPLE_RATE;
    private static final long NOISE_MULTIPLIER = DemoSoundSynthesis.NOISE_MULTIPLIER;
    private static final long NOISE_INCREMENT = DemoSoundSynthesis.NOISE_INCREMENT;
    private static final long NOISE_SEED = 0x4D75_5349L;

    /**
     * How fast a plucked string loses its energy per pass of its delay line, and how long a pluck is rendered.
     */
    private static final double PLUCK_DAMPING = 0.996;
    private static final double PLUCK_SECONDS = 2.5;
    private static final double PLUCK_RELEASE_SECONDS = 0.02;
    private static final int SMALLEST_PERIOD = 2;

    private static final double FLUTE_ATTACK_SECONDS = 0.06;
    private static final double FLUTE_RELEASE_SECONDS = 0.12;
    private static final double FLUTE_SECOND_HARMONIC = 0.22;
    private static final double FLUTE_THIRD_HARMONIC = 0.06;
    private static final double FLUTE_VIBRATO_FREQUENCY = 5;
    private static final double FLUTE_VIBRATO_DEPTH = 0.004;
    private static final double FLUTE_VIBRATO_DELAY_SECONDS = 0.15;
    private static final double FLUTE_BREATH_LEVEL = 0.04;
    private static final double FLUTE_BREATH_SMOOTHING = 0.15;

    private static final double PAD_ATTACK_SECONDS = 0.5;
    private static final double PAD_RELEASE_SECONDS = 0.6;
    private static final double PAD_SECOND_HARMONIC = 0.3;

    private static final double DRUM_SECONDS = 0.45;
    private static final double DRUM_LOW_FREQUENCY = 62;
    private static final double DRUM_FREQUENCY_DROP = 60;
    private static final double DRUM_FREQUENCY_DECAY = 18;
    private static final double DRUM_TONE_DECAY = 7;
    private static final double DRUM_SKIN_LEVEL = 0.35;
    private static final double DRUM_SKIN_DECAY = 45;
    private static final double TAP_SECONDS = 0.1;
    private static final double TAP_DECAY = 55;

    /**
     * The comb filters and all-pass filters of the reverb: delays in samples, feedback, damping and the wet share of the mix.
     */
    private static final int @NotNull [] COMB_DELAYS = {558, 594, 638, 678};
    private static final double COMB_FEEDBACK = 0.78;
    private static final double COMB_DAMPING = 0.25;
    private static final int @NotNull [] ALLPASS_DELAYS = {278, 112};
    private static final double ALLPASS_GAIN = 0.5;
    private static final double REVERB_WET = 0.22;

    /**
     * The voices of a chord in the arpeggio, as indices into root, fifth, octave and the tenth of the chord, per eighth of a bar; {@link #REST}
     * plays nothing: the tune's arpeggio, the rolling one of its second pass and the sparse one of the bridge.
     */
    private static final int @NotNull [] ARPEGGIO = {0, 1, 2, 1, 3, 1, 2, 1};
    private static final int @NotNull [] ROLLING_ARPEGGIO = {0, 1, 2, 3, 2, 1, 2, 3};
    private static final int @NotNull [] SPARSE_ARPEGGIO = {0, -1, 2, -1, 3, -1, 2, -1};
    private static final int REST = -1;

    /**
     * The drum patterns of a bar per eighth: a frame drum, a tap or nothing; the tune's pattern, the busier one of its second pass and none.
     */
    private static final int @NotNull [] DRUM_PATTERN = {1, 0, 2, 0, 1, 2, 2, 0};
    private static final int @NotNull [] SECOND_DRUM_PATTERN = {1, 2, 2, 0, 1, 0, 2, 2};
    private static final int @NotNull [] NO_DRUMS = {0, 0, 0, 0, 0, 0, 0, 0};
    private static final int DRUM = 1;
    private static final int TAP = 2;

    private static final int TUNE_BARS = 16;
    private static final int BRIDGE_FIRST_BAR = 8;
    private static final int BRIDGE_BARS = 8;

    private static final int THIRD = 1;
    private static final int FIFTH = 2;
    /**
     * The entries of one melody note: the note and its length in eighths.
     */
    private static final int MELODY_NOTE_ENTRIES = 2;

    /**
     * The menu piece: a slow harp and flute song in D minor.
     */
    private static final @NotNull Piece HEARTH = new Piece(
            "hearth", 76, false, Instrument.FLUTE,
            new int[][]{
                    {50, 53, 57}, {50, 53, 57}, {48, 52, 55}, {48, 52, 55}, {46, 50, 53}, {46, 50, 53}, {48, 52, 55}, {45, 49, 52},
                    {50, 53, 57}, {50, 53, 57}, {48, 52, 55}, {48, 52, 55}, {46, 50, 53}, {43, 46, 50}, {45, 49, 52}, {45, 49, 52}
            },
            new int[]{
                    69, 2, 74, 2, 77, 3, 76, 1, 74, 2, 72, 1, 74, 1, 69, 4, 67, 2, 72, 2, 76, 3, 74, 1, 72, 2, 74, 1, 76, 1, 67, 4,
                    65, 2, 70, 2, 74, 3, 72, 1, 70, 2, 69, 1, 70, 1, 74, 4, 76, 2, 79, 2, 76, 2, 72, 2, 73, 3, 76, 1, 69, 4,
                    74, 2, 77, 2, 81, 3, 79, 1, 77, 2, 76, 1, 77, 1, 74, 4, 76, 2, 79, 2, 76, 3, 74, 1, 72, 2, 76, 1, 74, 1, 72, 4,
                    74, 2, 70, 2, 77, 3, 74, 1, 70, 2, 67, 2, 74, 3, 70, 1, 69, 2, 73, 2, 76, 2, 73, 2, 76, 3, 74, 1, 73, 4
            }
    );
    /**
     * The first game piece: a lute tune over a frame drum in A minor.
     */
    private static final @NotNull Piece FORD = new Piece(
            "ford", 96, true, Instrument.LUTE,
            new int[][]{
                    {45, 48, 52}, {45, 48, 52}, {43, 47, 50}, {43, 47, 50}, {41, 45, 48}, {41, 45, 48}, {40, 44, 47}, {40, 44, 47},
                    {45, 48, 52}, {45, 48, 52}, {43, 47, 50}, {43, 47, 50}, {41, 45, 48}, {43, 47, 50}, {45, 48, 52}, {40, 44, 47}
            },
            new int[]{
                    76, 2, 69, 1, 71, 1, 72, 2, 76, 2, 74, 1, 72, 1, 71, 2, 69, 4, 74, 2, 67, 1, 69, 1, 71, 2, 74, 2, 72, 1, 71, 1, 69, 2, 67, 4,
                    69, 2, 72, 1, 74, 1, 77, 2, 76, 2, 74, 1, 72, 1, 74, 2, 69, 4, 71, 2, 68, 1, 69, 1, 71, 2, 76, 2, 74, 2, 72, 1, 71, 1, 68, 4,
                    81, 2, 79, 1, 76, 1, 72, 2, 76, 2, 79, 1, 77, 1, 76, 2, 69, 4, 79, 2, 74, 1, 71, 1, 67, 2, 71, 2, 74, 2, 76, 1, 74, 1, 71, 4,
                    72, 2, 77, 2, 81, 3, 79, 1, 77, 2, 74, 2, 71, 2, 74, 2, 72, 2, 76, 2, 81, 3, 76, 1, 76, 2, 68, 2, 71, 3, 76, 1
            }
    );
    /**
     * The second game piece: a quicker flute tune over a frame drum in E.
     */
    private static final @NotNull Piece RIDGE = new Piece(
            "ridge", 108, true, Instrument.FLUTE,
            new int[][]{
                    {40, 43, 47}, {40, 43, 47}, {38, 42, 45}, {38, 42, 45}, {36, 40, 43}, {36, 40, 43}, {38, 42, 45}, {38, 42, 45},
                    {40, 43, 47}, {40, 43, 47}, {43, 47, 50}, {43, 47, 50}, {36, 40, 43}, {38, 42, 45}, {40, 43, 47}, {40, 43, 47}
            },
            new int[]{
                    71, 2, 76, 2, 79, 2, 78, 1, 76, 1, 74, 2, 76, 2, 71, 4, 69, 2, 74, 2, 78, 3, 76, 1, 74, 2, 73, 1, 74, 1, 69, 4,
                    76, 2, 79, 2, 76, 2, 72, 2, 74, 1, 76, 1, 74, 2, 67, 4, 66, 2, 69, 2, 74, 3, 73, 1, 74, 2, 76, 1, 78, 1, 69, 4,
                    76, 2, 71, 1, 76, 1, 79, 2, 78, 2, 76, 3, 74, 1, 71, 4, 74, 2, 79, 2, 71, 2, 74, 2, 67, 2, 69, 1, 71, 1, 74, 4,
                    76, 2, 72, 2, 67, 2, 72, 2, 78, 2, 74, 2, 69, 2, 66, 2, 67, 2, 71, 2, 76, 3, 78, 1, 76, 6, 74, 1, 71, 1
            }
    );

    private static final double MELODY_LEVEL = 0.32;
    private static final double LUTE_LEVEL = 0.5;
    private static final double LUTE_BRIGHTNESS = 0.9;
    private static final double HARP_BRIGHTNESS = 0.55;
    private static final double ARPEGGIO_LEVEL = 0.16;
    private static final double BASS_LEVEL = 0.34;
    private static final double BASS_BRIGHTNESS = 0.4;
    private static final double PAD_LEVEL = 0.05;
    private static final double DRUM_LEVEL = 0.35;
    private static final double TAP_LEVEL = 0.1;

    /**
     * The arrangement of every piece: the tune, the tune on the other instrument, the bridge.
     */
    private static final @NotNull Section @NotNull [] ARRANGEMENT = {
            new Section(0, TUNE_BARS, true, false, ARPEGGIO, DRUM_PATTERN),
            new Section(0, TUNE_BARS, true, true, ROLLING_ARPEGGIO, SECOND_DRUM_PATTERN),
            new Section(BRIDGE_FIRST_BAR, BRIDGE_BARS, false, false, SPARSE_ARPEGGIO, NO_DRUMS)
    };

    /**
     * The instrument of a piece's melody.
     */
    private enum Instrument {
        LUTE,
        FLUTE;

        /**
         * @return the instrument of the tune's second pass: the flute for the lute and the lute for the flute
         */
        private @NotNull Instrument getOther() {
            return this == LUTE ? FLUTE : LUTE;
        }
    }

    /**
     * One section of a piece's arrangement: the bars of the tune's chords it plays, whether the melody plays and on which instrument (the
     * piece's own or the other one), the arpeggio and the drum pattern (played only in a piece with drums).
     */
    private static final class Section {
        private final int firstBar;
        private final int barCount;
        private final boolean melody;
        private final boolean otherInstrument;
        private final int @NotNull [] arpeggio;
        private final int @NotNull [] drumPattern;

        private Section(int firstBar, int barCount, boolean melody, boolean otherInstrument, int @NotNull [] arpeggio, int @NotNull [] drumPattern) {
            this.firstBar        = firstBar;
            this.barCount        = barCount;
            this.melody          = melody;
            this.otherInstrument = otherInstrument;
            this.arpeggio        = arpeggio;
            this.drumPattern     = drumPattern;
        }
    }

    /**
     * One piece: its id, tempo, whether drums play, the melody's instrument, one chord per bar (three notes from the root up) and the melody as
     * pairs of a note and its length in eighths.
     */
    private static final class Piece {
        private final @NotNull String id;
        private final int beatsPerMinute;
        private final boolean drums;
        private final @NotNull Instrument instrument;
        private final int @NotNull [] @NotNull [] chords;
        private final int @NotNull [] melody;

        private Piece(@NotNull String id, int beatsPerMinute, boolean drums, @NotNull Instrument instrument, int @NotNull [] @NotNull [] chords,
                      int @NotNull [] melody) {
            this.id             = id;
            this.beatsPerMinute = beatsPerMinute;
            this.drums          = drums;
            this.instrument     = instrument;
            this.chords         = chords;
            this.melody         = melody;
        }
    }

    /**
     * Writes every piece of the demo into the pack folder's {@code music} folder.
     *
     * @param packFolder the output pack folder
     * @throws IOException if a file cannot be written
     */
    public static void writePieces(@NotNull Path packFolder) throws IOException {
        Path folder = packFolder.resolve("music");

        Files.createDirectories(folder);

        for (Piece piece : new Piece[]{HEARTH, FORD, RIDGE}) {
            DemoSoundSynthesis.writeWave(folder.resolve(piece.id + ".wav"), synthesize(piece), false);
        }
    }

    private static double @NotNull [] synthesize(@NotNull Piece piece) {
        int eighthSamples = (int) Math.round(SAMPLE_RATE * SECONDS_PER_MINUTE / piece.beatsPerMinute / EIGHTHS_PER_BEAT);
        int barSamples = EIGHTHS_PER_BAR * eighthSamples;
        int bars = 0;

        for (Section section : ARRANGEMENT) {
            bars += section.barCount;
        }

        int length = barSamples * bars;
        double[] rendered = new double[length + (int) (SAMPLE_RATE * TAIL_SECONDS)];
        long noise = NOISE_SEED;
        int sectionStart = 0;

        for (Section section : ARRANGEMENT) {
            for (int index = 0; index < section.barCount; index++) {
                noise = addBar(rendered, sectionStart + index * barSamples, eighthSamples, piece.chords[section.firstBar + index], section,
                               piece.drums, noise);
            }

            if (section.melody) {
                Instrument instrument = section.otherInstrument ? piece.instrument.getOther() : piece.instrument;

                noise = addMelody(rendered, sectionStart, eighthSamples, piece, instrument, noise);
            }

            sectionStart += section.barCount * barSamples;
        }

        double[] wet = reverberate(rendered);
        double[] looped = new double[length];
        double energy = 0;

        for (int index = 0; index < rendered.length; index++) {
            looped[index % length] += rendered[index] * (1 - REVERB_WET) + wet[index] * REVERB_WET;
        }

        for (double sample : looped) {
            energy += sample * sample;
        }

        double gain = LOUDNESS / Math.sqrt(energy / length);

        for (int index = 0; index < length; index++) {
            looped[index] = Math.tanh(looped[index] * gain);
        }

        return looped;
    }

    /**
     * Adds the accompaniment of one bar: the section's arpeggio over the chord, the drums of the section's pattern, the bass on the root and
     * the fifth, and the pad of the chord's tones.
     *
     * @return the next state of the noise sequence
     */
    private static long addBar(double @NotNull [] rendered, int barStart, int eighthSamples, int @NotNull [] chord, @NotNull Section section,
                               boolean drums, long noise) {
        int lift = chord[0] < LOWEST_CHORD_ROOT ? OCTAVE : 0;
        int[] voices = {chord[0] + lift, chord[FIFTH] + lift, chord[0] + lift + OCTAVE, chord[THIRD] + lift + OCTAVE};
        long state = noise;

        for (int eighth = 0; eighth < EIGHTHS_PER_BAR; eighth++) {
            int start = barStart + eighth * eighthSamples;
            int voice = section.arpeggio[eighth];

            if (voice != REST) {
                state = addPluck(rendered, start, toFrequency(voices[voice]), ARPEGGIO_LEVEL, HARP_BRIGHTNESS, state);
            }

            if (drums && section.drumPattern[eighth] == DRUM) {
                state = addDrum(rendered, start, state);
            } else if (drums && section.drumPattern[eighth] == TAP) {
                state = addTap(rendered, start, state);
            }
        }

        state = addPluck(rendered, barStart, toFrequency(chord[0] + lift - OCTAVE), BASS_LEVEL, BASS_BRIGHTNESS, state);
        state = addPluck(rendered, barStart + EIGHTHS_PER_BAR / 2 * eighthSamples, toFrequency(chord[FIFTH] + lift - OCTAVE), BASS_LEVEL,
                         BASS_BRIGHTNESS, state);

        for (int note : chord) {
            addPad(rendered, barStart, EIGHTHS_PER_BAR * eighthSamples, toFrequency(note + lift));
        }

        return state;
    }

    /**
     * Adds the piece's melody from a position on an instrument: plucked on the lute, held on the flute.
     *
     * @return the next state of the noise sequence
     */
    private static long addMelody(double @NotNull [] rendered, int start, int eighthSamples, @NotNull Piece piece, @NotNull Instrument instrument,
                                  long noise) {
        int position = start;
        long state = noise;

        for (int index = 0; index < piece.melody.length; index += MELODY_NOTE_ENTRIES) {
            int samples = piece.melody[index + 1] * eighthSamples;
            double frequency = toFrequency(piece.melody[index]);

            if (instrument == Instrument.LUTE) {
                state = addPluck(rendered, position, frequency, LUTE_LEVEL, LUTE_BRIGHTNESS, state);
            } else {
                state = addFlute(rendered, position, samples, frequency, state);
            }

            position += samples;
        }

        return state;
    }

    private static double toFrequency(int note) {
        return A4_FREQUENCY * Math.pow(OCTAVE_FREQUENCY_RATIO, (note - A4_NOTE) / SEMITONES_PER_OCTAVE);
    }

    /**
     * Adds a plucked string: a delay line of one period filled with noise, smoothed by the brightness, then averaged and damped at each pass.
     *
     * @return the next state of the noise sequence
     */
    private static long addPluck(double @NotNull [] samples, int start, double frequency, double level, double brightness, long noise) {
        int period = Math.max(SMALLEST_PERIOD, (int) Math.round(SAMPLE_RATE / frequency));
        double[] line = new double[period];
        int length = Math.min(samples.length - start, (int) (SAMPLE_RATE * PLUCK_SECONDS));
        int release = (int) (SAMPLE_RATE * PLUCK_RELEASE_SECONDS);
        long state = noise;
        double smoothed = 0;

        for (int index = 0; index < period; index++) {
            state       = NOISE_MULTIPLIER * state + NOISE_INCREMENT;
            smoothed += brightness * (DemoSoundSynthesis.toNoise(state) - smoothed);
            line[index] = smoothed;
        }

        int position = 0;

        for (int index = 0; index < length; index++) {
            int next = (position + 1) % period;
            double value = line[position];

            line[position] = PLUCK_DAMPING * (line[position] + line[next]) / 2;
            position       = next;
            samples[start + index] += value * level * Math.min(1, (double) (length - index) / release);
        }

        return state;
    }

    /**
     * Adds a flute note: the tone and two harmonics under a short attack and release, a delayed vibrato and a little breath noise.
     *
     * @return the next state of the noise sequence
     */
    private static long addFlute(double @NotNull [] samples, int start, int length, double frequency, long noise) {
        int attack = (int) (SAMPLE_RATE * FLUTE_ATTACK_SECONDS);
        int release = (int) (SAMPLE_RATE * FLUTE_RELEASE_SECONDS);
        int end = Math.min(samples.length - start, length + release);
        long state = noise;
        double phase = 0;
        double breath = 0;

        for (int index = 0; index < end; index++) {
            double seconds = (double) index / SAMPLE_RATE;
            double vibrato = seconds > FLUTE_VIBRATO_DELAY_SECONDS
                             ? FLUTE_VIBRATO_DEPTH * Math.sin(FULL_CYCLE * FLUTE_VIBRATO_FREQUENCY * (seconds - FLUTE_VIBRATO_DELAY_SECONDS))
                             : 0;
            double envelope = Math.min(1, (double) index / attack) * (index < length ? 1 : 1 - (double) (index - length) / release);

            state = NOISE_MULTIPLIER * state + NOISE_INCREMENT;
            breath += FLUTE_BREATH_SMOOTHING * (DemoSoundSynthesis.toNoise(state) - breath);
            phase += FULL_CYCLE * frequency * (1 + vibrato) / SAMPLE_RATE;

            double tone = Math.sin(phase) + FLUTE_SECOND_HARMONIC * Math.sin(SECOND_HARMONIC * phase) +
                          FLUTE_THIRD_HARMONIC * Math.sin(THIRD_HARMONIC * phase);

            samples[start + index] += (tone * MELODY_LEVEL + breath * FLUTE_BREATH_LEVEL) * envelope;
        }

        return state;
    }

    /**
     * Adds a held chord tone over a bar, with a slow attack and a release into the next bar.
     */
    private static void addPad(double @NotNull [] samples, int start, int length, double frequency) {
        int attack = (int) (SAMPLE_RATE * PAD_ATTACK_SECONDS);
        int release = (int) (SAMPLE_RATE * PAD_RELEASE_SECONDS);
        int end = Math.min(samples.length - start, length + release);

        for (int index = 0; index < end; index++) {
            double angle = FULL_CYCLE * frequency * index / SAMPLE_RATE;
            double envelope = Math.min(1, (double) index / attack) * (index < length ? 1 : 1 - (double) (index - length) / release);

            samples[start + index] += (Math.sin(angle) + PAD_SECOND_HARMONIC * Math.sin(SECOND_HARMONIC * angle)) * PAD_LEVEL * envelope;
        }
    }

    /**
     * Adds a frame drum: a low tone falling in pitch and a short burst of skin noise.
     *
     * @return the next state of the noise sequence
     */
    private static long addDrum(double @NotNull [] samples, int start, long noise) {
        int length = Math.min(samples.length - start, (int) (SAMPLE_RATE * DRUM_SECONDS));
        long state = noise;
        double phase = 0;

        for (int index = 0; index < length; index++) {
            double seconds = (double) index / SAMPLE_RATE;

            state = NOISE_MULTIPLIER * state + NOISE_INCREMENT;
            phase += FULL_CYCLE * (DRUM_LOW_FREQUENCY + DRUM_FREQUENCY_DROP * Math.exp(-seconds * DRUM_FREQUENCY_DECAY)) / SAMPLE_RATE;
            samples[start + index] += (Math.sin(phase) * Math.exp(-seconds * DRUM_TONE_DECAY) +
                                       DemoSoundSynthesis.toNoise(state) * DRUM_SKIN_LEVEL * Math.exp(-seconds * DRUM_SKIN_DECAY)) * DRUM_LEVEL;
        }

        return state;
    }

    /**
     * Adds a tap on the drum's rim: a short burst of high noise, the difference of two noise values.
     *
     * @return the next state of the noise sequence
     */
    private static long addTap(double @NotNull [] samples, int start, long noise) {
        int length = Math.min(samples.length - start, (int) (SAMPLE_RATE * TAP_SECONDS));
        long state = noise;
        double previous = 0;

        for (int index = 0; index < length; index++) {
            state = NOISE_MULTIPLIER * state + NOISE_INCREMENT;

            double value = DemoSoundSynthesis.toNoise(state);

            samples[start + index] += (value - previous) * TAP_LEVEL * Math.exp(-(double) index / SAMPLE_RATE * TAP_DECAY);
            previous = value;
        }

        return state;
    }

    /**
     * Returns the wet signal of a Schroeder reverb: parallel damped comb filters, then all-pass filters in series.
     */
    private static double @NotNull [] reverberate(double @NotNull [] samples) {
        double[] wet = new double[samples.length];

        for (int delay : COMB_DELAYS) {
            double[] line = new double[delay];
            double filtered = 0;

            for (int index = 0; index < samples.length; index++) {
                double output = line[index % delay];

                filtered += COMB_DAMPING * (output - filtered);
                line[index % delay] = samples[index] + filtered * COMB_FEEDBACK;
                wet[index] += output / COMB_DELAYS.length;
            }
        }

        for (int delay : ALLPASS_DELAYS) {
            double[] line = new double[delay];

            for (int index = 0; index < wet.length; index++) {
                double delayed = line[index % delay];
                double input = wet[index];

                line[index % delay] = input + delayed * ALLPASS_GAIN;
                wet[index]          = delayed - input * ALLPASS_GAIN;
            }
        }

        return wet;
    }

    private DemoMusicSynthesis() {
    }
}
