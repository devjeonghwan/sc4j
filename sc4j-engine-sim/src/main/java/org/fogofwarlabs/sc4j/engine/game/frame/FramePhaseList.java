package org.fogofwarlabs.sc4j.engine.game.frame;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.game.Game;
import org.fogofwarlabs.sc4j.engine.game.system.GameSystem;
import org.fogofwarlabs.sc4j.engine.game.system.SystemKey;
import org.fogofwarlabs.sc4j.engine.game.system.SystemRegistry;
import org.fogofwarlabs.sc4j.engine.game.system.SystemSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the ordered phases of one game's frames. The order and the number of phases are fixed when the game is created;
 * the phases of each frame kind (advancing, paused, setup) are resolved into their own arrays then, so a frame runs a
 * plain array.
 */
public final class FramePhaseList {
    /**
     * Builds {@link FramePhaseList} instances: the phase order a mod registers, with phases that are objects and phases
     * that are game systems.
     */
    public static final class Builder {
        /**
         * Names a phase of a game system: the system's key and the selector of its phase.
         *
         * @param <S> the system type
         */
        private static final class SystemPhase<S extends GameSystem> {
            private final @NotNull SystemKey<S> key;
            private final @NotNull FramePhaseSelector<? super S> selector;

            private SystemPhase(@NotNull SystemKey<S> key, @NotNull FramePhaseSelector<? super S> selector) {
                this.key      = key;
                this.selector = selector;
            }

            private @NotNull FramePhase resolve(@NotNull SystemSet systems) {
                return this.selector.selectPhase(systems.get(this.key));
            }
        }

        private final @NotNull List<@Nullable FramePhase> phases;
        private final @NotNull List<@Nullable SystemPhase<?>> systemPhases;

        /**
         * Creates a {@link Builder} instance without phases.
         */
        public Builder() {
            this.phases       = new ArrayList<>();
            this.systemPhases = new ArrayList<>();
        }

        /**
         * Creates a {@link Builder} instance with the phases of another builder.
         *
         * @param other the builder to copy
         */
        public Builder(@NotNull Builder other) {
            this.phases       = new ArrayList<>(other.phases);
            this.systemPhases = new ArrayList<>(other.systemPhases);
        }

        /**
         * @return the number of listed phases
         */
        public int getCount() {
            return this.phases.size();
        }


        /**
         * Appends a phase object, shared by every game of the definition.
         *
         * @param phase the phase
         * @return this instance for chaining
         */
        public @NotNull Builder add(@NotNull FramePhase phase) {
            this.phases.add(phase);
            this.systemPhases.add(null);

            return this;
        }

        /**
         * Appends a game system that is also a phase; each game runs its own instance of the system.
         *
         * @param <S> the system type
         * @param key the key of the system
         * @return this instance for chaining
         */
        public <S extends GameSystem & FramePhase> @NotNull Builder add(@NotNull SystemKey<S> key) {
            return this.add(key, system -> system);
        }

        /**
         * Appends a phase of a game system; each game runs the phase its own instance of the system selects.
         *
         * @param <S>      the system type
         * @param key      the key of the system
         * @param selector selects the phase of the system
         * @return this instance for chaining
         */
        public <S extends GameSystem> @NotNull Builder add(@NotNull SystemKey<S> key, @NotNull FramePhaseSelector<? super S> selector) {
            this.phases.add(null);
            this.systemPhases.add(new SystemPhase<>(key, selector));

            return this;
        }

        /**
         * Removes every listed phase.
         *
         * @return this instance for chaining
         */
        public @NotNull Builder clear() {
            this.phases.clear();
            this.systemPhases.clear();

            return this;
        }

        /**
         * Checks that every system phase is registered.
         *
         * @param systems the system registrations
         * @throws IllegalStateException if a system phase is not registered
         */
        public void checkSystems(@NotNull SystemRegistry systems) {
            for (SystemPhase<?> systemPhase : this.systemPhases) {
                if (systemPhase != null && !systems.contains(systemPhase.key)) {
                    throw Exceptions.newIllegalStateException("The frame phase system {0} is not registered.", systemPhase.key);
                }
            }
        }

        /**
         * Builds a {@link FramePhaseList} instance for one game: system phases resolve to the game's systems.
         *
         * @param systems the systems of the game
         * @return the built {@link FramePhaseList} instance
         * @throws IllegalArgumentException if a system phase is not registered
         * @throws IllegalStateException    if a listed phase is missing
         */
        public @NotNull FramePhaseList build(@NotNull SystemSet systems) {
            FramePhase[] resolved = new FramePhase[this.phases.size()];

            for (int index = 0; index < resolved.length; index++) {
                FramePhase phase = this.phases.get(index);
                SystemPhase<?> systemPhase = this.systemPhases.get(index);

                if (phase == null && systemPhase != null) {
                    phase = systemPhase.resolve(systems);
                }

                if (phase == null) {
                    throw Exceptions.newIllegalStateException("The frame phase at position {0} is missing.", index);
                }

                resolved[index] = phase;
            }

            return new FramePhaseList(resolved);
        }
    }

    private static void runPhases(@NotNull FramePhase @NotNull [] phases, @NotNull Game game) {
        for (int index = 0; index < phases.length; index++) {
            phases[index].run(game);
        }
    }

    private final @NotNull FramePhase @NotNull [] phases;
    private final @NotNull FramePhase @NotNull [] pausedPhases;
    private final @NotNull FramePhase @NotNull [] setupPhases;

    private FramePhaseList(@NotNull FramePhase @NotNull [] phases) {
        List<FramePhase> paused = new ArrayList<>();
        List<FramePhase> setup = new ArrayList<>();

        for (FramePhase phase : phases) {
            PhaseSchedule schedule = phase.getSchedule();

            if (schedule.isRunWhilePaused()) {
                paused.add(phase);
            }

            if (schedule.isRunInSetup()) {
                setup.add(phase);
            }
        }

        this.phases       = phases;
        this.pausedPhases = paused.toArray(new FramePhase[0]);
        this.setupPhases  = setup.toArray(new FramePhase[0]);
    }

    /**
     * @return the number of phases
     */
    public int getCount() {
        return this.phases.length;
    }

    /**
     * Returns one phase.
     *
     * @param index the position in run order
     * @return the phase
     */
    public @NotNull FramePhase get(int index) {
        return this.phases[index];
    }


    /**
     * Runs every phase of a frame that advances the game, in list order.
     *
     * @param game the game
     */
    void runRunning(@NotNull Game game) {
        runPhases(this.phases, game);
    }

    /**
     * Runs the phases of a paused frame, in list order.
     *
     * @param game the game
     */
    void runPaused(@NotNull Game game) {
        runPhases(this.pausedPhases, game);
    }

    /**
     * Runs the phases of a setup frame, in list order.
     *
     * @param game the game
     */
    void runSetup(@NotNull Game game) {
        runPhases(this.setupPhases, game);
    }
}
