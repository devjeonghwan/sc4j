package org.fogofwarlabs.sc4j.engine.game;

import org.fogofwarlabs.sc4j.base.exception.Exceptions;
import org.fogofwarlabs.sc4j.engine.content.registry.ContentDefinition;
import org.fogofwarlabs.sc4j.engine.content.registry.ContentRegistry;
import org.fogofwarlabs.sc4j.engine.entity.EntityKind;
import org.fogofwarlabs.sc4j.engine.entity.column.ColumnKey;
import org.fogofwarlabs.sc4j.engine.entity.pool.SlotPolicy;
import org.fogofwarlabs.sc4j.engine.entity.pool.StandardSlotPolicy;
import org.fogofwarlabs.sc4j.engine.game.frame.FramePhase;
import org.fogofwarlabs.sc4j.engine.game.frame.FramePhaseList;
import org.fogofwarlabs.sc4j.engine.game.frame.FramePhaseSelector;
import org.fogofwarlabs.sc4j.engine.game.system.*;
import org.fogofwarlabs.sc4j.engine.hook.HookFactory;
import org.fogofwarlabs.sc4j.engine.hook.HookPoint;
import org.fogofwarlabs.sc4j.engine.hook.HookRegistry;
import org.fogofwarlabs.sc4j.engine.hook.HookTable;
import org.fogofwarlabs.sc4j.engine.random.PcgRandom;
import org.fogofwarlabs.sc4j.engine.random.RandomFactory;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds what a mod registered for its games: the random generator, the limits, the slot policy and the extension
 * columns of each entity kind, the systems, the hooks, the frame phases and the content. Every game of a session is created from the same
 * definition; the definition is immutable.
 */
public final class GameDefinition {
    /**
     * Builds the {@link GameDefinition} of a mod during its registration. Without registrations, a definition has the
     * engine default generator ({@link PcgRandom} with {@link PcgRandom#DEFAULT_SOURCE_COUNT} sources),
     * {@link EngineLimits#EMPTY}, {@link StandardSlotPolicy#REUSE_RECENT} for every kind, no column, system, hook or phase and
     * an empty content.
     * <p>
     * {@link #build()} closes the builder and builds the content ({@link ContentRegistry#build()}): later registrations
     * fail, and later calls of {@link #build()} return the same definition.
     */
    public static final class Builder {
        private @NotNull RandomFactory randomFactory;
        private @NotNull EngineLimits limits;
        private final @NotNull SlotPolicy @NotNull [] slotPolicies;
        private final boolean @NotNull [] modPools;
        private final @NotNull List<@NotNull List<@NotNull ColumnKey<?>>> columns;
        private final @NotNull SystemRegistry systems;
        private final @NotNull HookRegistry hooks;
        private final FramePhaseList.@NotNull Builder phases;
        private final @NotNull ContentRegistry content;

        private @Nullable GameDefinition definition;

        /**
         * Creates an open {@link Builder} instance with the engine defaults.
         */
        public Builder() {
            EntityKind[] kinds = EntityKind.values();

            this.randomFactory = PcgRandom.factory(PcgRandom.DEFAULT_SOURCE_COUNT);
            this.limits        = EngineLimits.EMPTY;
            this.slotPolicies  = new SlotPolicy[kinds.length];
            this.modPools      = new boolean[kinds.length];
            this.columns       = new ArrayList<>(kinds.length);
            this.systems       = new SystemRegistry();
            this.hooks         = new HookRegistry();
            this.phases        = new FramePhaseList.Builder();
            this.content       = new ContentRegistry();

            this.definition = null;

            for (EntityKind kind : kinds) {
                this.slotPolicies[kind.ordinal()] = StandardSlotPolicy.REUSE_RECENT;
                this.columns.add(new ArrayList<>());
            }
        }

        /**
         * Sets the factory of the random generator, for a mod that must reproduce a generator of its own.
         *
         * @param randomFactory the factory
         * @return this instance for chaining
         * @throws IllegalStateException if the builder is closed
         */
        public @NotNull Builder setRandomFactory(@NotNull RandomFactory randomFactory) {
            this.requireOpen();
            this.randomFactory = randomFactory;

            return this;
        }

        /**
         * Sets the pool sizes of every game; a game setup may replace them.
         *
         * @param limits the limits
         * @return this instance for chaining
         * @throws IllegalStateException if the builder is closed
         */
        public @NotNull Builder setLimits(@NotNull EngineLimits limits) {
            this.requireOpen();
            this.limits = limits;

            return this;
        }

        /**
         * Sets the slot policy of the pool of an entity kind.
         *
         * @param kind   the entity kind
         * @param policy the policy
         * @return this instance for chaining
         * @throws IllegalStateException if the builder is closed
         */
        public @NotNull Builder setSlotPolicy(@NotNull EntityKind kind, @NotNull SlotPolicy policy) {
            this.requireOpen();
            this.slotPolicies[kind.ordinal()] = policy;

            return this;
        }

        /**
         * Declares that the mod pools an entity kind itself, with its own entity type in its own pool sized by the game's
         * limits: the engine pool of the kind is created empty, with capacity {@code 0}.
         *
         * @param kind the entity kind
         * @return this instance for chaining
         * @throws IllegalStateException if the builder is closed
         */
        public @NotNull Builder useModPool(@NotNull EntityKind kind) {
            this.requireOpen();
            this.modPools[kind.ordinal()] = true;

            return this;
        }


        /**
         * Adds an extension column to the pool of an entity kind; columns are created in the order they are added.
         *
         * @param kind the entity kind
         * @param key  the column key
         * @return this instance for chaining
         * @throws IllegalArgumentException if the key is already added for the kind
         * @throws IllegalStateException    if the builder is closed
         */
        public @NotNull Builder addColumn(@NotNull EntityKind kind, @NotNull ColumnKey<?> key) {
            this.requireOpen();

            List<ColumnKey<?>> kindColumns = this.columns.get(kind.ordinal());

            for (ColumnKey<?> added : kindColumns) {
                if (added == key) {
                    throw Exceptions.newIllegalArgumentException("The column {0} is already added for {1}.", key, kind);
                }
            }

            kindColumns.add(key);

            return this;
        }

        /**
         * Registers the factory of a system: appends the key, or replaces the factory of a registered key at its position.
         *
         * @param <S>     the system type
         * @param key     the key
         * @param factory creates the system of each game; it may pass the systems registered before it to the new system
         * @return this instance for chaining
         * @throws IllegalStateException if the builder is closed
         */
        public <S extends GameSystem> @NotNull Builder useSystem(@NotNull SystemKey<S> key, @NotNull SystemFactory<? extends S> factory) {
            this.requireOpen();
            this.systems.use(key, factory);

            return this;
        }

        /**
         * Registers the factory of one hook: appends it after the hooks already registered. The game creates the hooks
         * after its systems and attaches the hooks that are systems after the systems, in registration order.
         *
         * @param <H>     the hook interface
         * @param point   the hook point
         * @param factory creates the hook of each game; it may pass the game's systems to the hook or return one of them
         * @return this instance for chaining
         * @throws IllegalStateException if the builder is closed, or the point allows at most one hook and has one
         */
        public <H> @NotNull Builder addHook(@NotNull HookPoint<H> point, @NotNull HookFactory<? extends H> factory) {
            this.requireOpen();
            this.hooks.add(point, factory);

            return this;
        }

        /**
         * Appends a phase object to the frame phases.
         *
         * @param phase the phase, shared by every game of the definition
         * @return this instance for chaining
         * @throws IllegalStateException if the builder is closed
         */
        public @NotNull Builder addFramePhase(@NotNull FramePhase phase) {
            this.requireOpen();
            this.phases.add(phase);

            return this;
        }

        /**
         * Appends a registered system that is also a frame phase to the frame phases.
         *
         * @param <S> the system type
         * @param key the key of the system
         * @return this instance for chaining
         * @throws IllegalStateException if the builder is closed
         */
        public <S extends GameSystem & FramePhase> @NotNull Builder addFramePhase(@NotNull SystemKey<S> key) {
            this.requireOpen();
            this.phases.add(key);

            return this;
        }

        /**
         * Appends a phase of a registered system to the frame phases: the selector picks the phase of each game's
         * instance of the system once, when the game is created.
         *
         * @param <S>      the system type
         * @param key      the key of the system
         * @param selector selects the phase of the system
         * @return this instance for chaining
         * @throws IllegalStateException if the builder is closed
         */
        public <S extends GameSystem> @NotNull Builder addFramePhase(@NotNull SystemKey<S> key, @NotNull FramePhaseSelector<? super S> selector) {
            this.requireOpen();
            this.phases.add(key, selector);

            return this;
        }

        /**
         * Returns the content registry: the types, definitions, table extension columns and modifiers of the mod's games.
         *
         * @return the open content registry
         * @throws IllegalStateException if the builder is closed
         */
        public @NotNull ContentRegistry getContent() {
            this.requireOpen();

            return this.content;
        }

        /**
         * Builds the {@link GameDefinition} instance and closes the builder; a closed builder returns the same instance.
         *
         * @return the built {@link GameDefinition} instance
         * @throws IllegalStateException if a frame phase names a system that is not registered
         */
        public @NotNull GameDefinition build() {
            GameDefinition built = this.definition;

            if (built != null) {
                return built;
            }

            this.phases.checkSystems(this.systems);

            List<List<ColumnKey<?>>> frozenColumns = new ArrayList<>(this.columns.size());

            for (List<ColumnKey<?>> kindColumns : this.columns) {
                frozenColumns.add(List.copyOf(kindColumns));
            }

            built = new GameDefinition(this.randomFactory, this.limits, this.slotPolicies.clone(), this.modPools.clone(), List.copyOf(frozenColumns),
                                       new SystemRegistry(this.systems), new HookRegistry(this.hooks), new FramePhaseList.Builder(this.phases),
                                       this.content.build());

            this.definition = built;

            return built;
        }

        private void requireOpen() {
            if (this.definition != null) {
                throw Exceptions.newIllegalStateException("The game definition is built; the registration is closed.");
            }
        }
    }

    private final @NotNull RandomFactory randomFactory;
    private final @NotNull EngineLimits limits;
    private final @NotNull SlotPolicy @NotNull [] slotPolicies;
    private final boolean @NotNull [] modPools;
    private final @NotNull List<@NotNull List<@NotNull ColumnKey<?>>> columns;
    private final @NotNull SystemRegistry systems;
    private final @NotNull HookRegistry hooks;
    private final FramePhaseList.@NotNull Builder phases;
    private final @NotNull ContentDefinition content;

    private GameDefinition(@NotNull RandomFactory randomFactory, @NotNull EngineLimits limits,
                           @NotNull SlotPolicy @NotNull [] slotPolicies, boolean @NotNull [] modPools,
                           @NotNull List<@NotNull List<@NotNull ColumnKey<?>>> columns,
                           @NotNull SystemRegistry systems, @NotNull HookRegistry hooks, FramePhaseList.@NotNull Builder phases,
                           @NotNull ContentDefinition content) {
        this.randomFactory = randomFactory;
        this.limits        = limits;
        this.slotPolicies  = slotPolicies;
        this.modPools      = modPools;
        this.columns       = columns;
        this.systems       = systems;
        this.hooks         = hooks;
        this.phases        = phases;
        this.content       = content;
    }

    /**
     * @return the factory of the random generator
     */
    public @NotNull RandomFactory getRandomFactory() {
        return this.randomFactory;
    }

    /**
     * @return the pool sizes of a game whose setup sets none
     */
    public @NotNull EngineLimits getLimits() {
        return this.limits;
    }

    /**
     * Returns the slot policy of the pool of an entity kind.
     *
     * @param kind the entity kind
     * @return the policy
     */
    public @NotNull SlotPolicy getSlotPolicy(@NotNull EntityKind kind) {
        return this.slotPolicies[kind.ordinal()];
    }

    /**
     * Checks whether the mod pools an entity kind itself ({@link Builder#useModPool}).
     *
     * @param kind the entity kind
     * @return {@code true} if the mod pools the kind and the engine pool of it is empty, {@code false} otherwise
     */
    public boolean isModPool(@NotNull EntityKind kind) {
        return this.modPools[kind.ordinal()];
    }

    /**
     * Returns the extension columns of the pool of an entity kind.
     *
     * @param kind the entity kind
     * @return the column keys in the order they were added
     */
    public @NotNull List<@NotNull ColumnKey<?>> getColumns(@NotNull EntityKind kind) {
        return this.columns.get(kind.ordinal());
    }

    /**
     * @return the number of registered systems
     */
    public int getSystemCount() {
        return this.systems.getCount();
    }

    /**
     * Returns the key of one registered system.
     *
     * @param index the position in registration order
     * @return the key
     */
    public @NotNull SystemKey<?> getSystemKey(int index) {
        return this.systems.getKey(index);
    }

    /**
     * @return the number of registered hooks, of every point
     */
    public int getHookCount() {
        return this.hooks.getCount();
    }

    /**
     * @return the number of frame phases
     */
    public int getFramePhaseCount() {
        return this.phases.getCount();
    }

    /**
     * @return the frozen content every game creates its tables from
     */
    public @NotNull ContentDefinition getContent() {
        return this.content;
    }


    /**
     * Creates the systems of a new game, in registration order; they are not attached yet.
     *
     * @param setup the game setup
     * @return the created systems
     */
    @NotNull SystemSet createSystems(@NotNull GameSetup setup) {
        return this.systems.createSystems(setup);
    }

    /**
     * Creates the hooks of a new game, in registration order; they are not attached yet.
     *
     * @param setup   the game setup
     * @param systems the created systems of the game, which the hook factories may use
     * @return the created hooks
     */
    @NotNull HookTable createHooks(@NotNull GameSetup setup, @NotNull SystemSet systems) {
        return this.hooks.createHooks(systems.createContext(setup));
    }

    /**
     * Creates the frame phase list of a new game.
     *
     * @param systems the systems of the game
     * @return the phase list
     */
    @NotNull FramePhaseList createFramePhases(@NotNull SystemSet systems) {
        return this.phases.build(systems);
    }
}
