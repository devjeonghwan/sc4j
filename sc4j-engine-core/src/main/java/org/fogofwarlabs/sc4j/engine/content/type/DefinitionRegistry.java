package org.fogofwarlabs.sc4j.engine.content.type;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Holds the identities of one content kind together with their definitions, the immutable initial values each game
 * copies into its per-game table.
 * <p>
 * Determinism contract: identities and definitions are kept in registration order ({@link TypeRegistry}); the
 * definition of an identity is at the identity's index.
 *
 * @param <T> the identity type
 * @param <D> the definition type
 */
public final class DefinitionRegistry<T extends ContentType, D> {
    private final @NotNull TypeRegistry<T> types;
    private final @NotNull List<@NotNull D> definitions;

    /**
     * Creates an empty, open {@link DefinitionRegistry} instance.
     *
     * @param kindName the name of the content kind, for diagnostics
     */
    public DefinitionRegistry(@NotNull String kindName) {
        this.types       = new TypeRegistry<>(kindName);
        this.definitions = new ArrayList<>();
    }

    /**
     * @return the identities, for lookups by index and for resolving references
     */
    public @NotNull TypeRegistry<T> getTypes() {
        return this.types;
    }

    /**
     * @return the number of registered identities
     */
    public int getCount() {
        return this.types.getCount();
    }

    /**
     * Returns the definition at an index.
     *
     * @param index the registration-order index
     * @return the definition
     * @throws IndexOutOfBoundsException if no identity has the index
     */
    public @NotNull D getDefinition(int index) {
        this.types.get(index);

        return this.definitions.get(index);
    }

    /**
     * Returns the definition of an identity.
     *
     * @param type the identity
     * @return the definition
     * @throws IllegalArgumentException if the identity is not registered here
     */
    public @NotNull D getDefinition(@NotNull T type) {
        return this.definitions.get(this.types.requireIndex(type));
    }


    /**
     * Registers an identity at the next index with its definition.
     *
     * @param type       the identity
     * @param definition the definition
     * @return the registered identity, for constant declarations
     * @throws IllegalArgumentException if the identity is already registered here
     * @throws IllegalStateException    if the registry is frozen or the identity has another index
     */
    public @NotNull T register(@NotNull T type, @NotNull D definition) {
        this.types.register(type);
        this.definitions.add(definition);

        return type;
    }

    /**
     * Ends the registration; later registrations fail. Freezing again has no effect.
     */
    public void freeze() {
        this.types.freeze();
    }
}
