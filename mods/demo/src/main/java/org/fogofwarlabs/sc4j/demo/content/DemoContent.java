package org.fogofwarlabs.sc4j.demo.content;

import org.fogofwarlabs.sc4j.base.exception.IgnoreException;
import org.fogofwarlabs.sc4j.demo.order.DemoOrders;
import org.fogofwarlabs.sc4j.engine.content.economy.ResourceKind;
import org.fogofwarlabs.sc4j.engine.content.faction.Faction;
import org.fogofwarlabs.sc4j.engine.content.registry.ContentRegistry;
import org.fogofwarlabs.sc4j.engine.content.requirement.declarative.DeclarativeRequirements;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.game.GameSetupCodec;
import org.fogofwarlabs.sc4j.engine.pack.PackContent;
import org.fogofwarlabs.sc4j.engine.pack.StandardPack;
import org.jetbrains.annotations.NotNull;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

/**
 * Holds the content of the demo: its engine standard data pack, read from the demo's own class loader, the order types and the types of the
 * pack's content definitions. The two factions are Dawnhold, the valley folk, and Cindermaw, the hill clans; each has a worker, a town hall, a
 * supply building, a production building, a melee and a ranged unit, and both mine gold from neutral gold mines.
 */
public final class DemoContent implements Closeable {
    /**
     * The resource path of the demo's pack root.
     */
    public static final @NotNull String PACK_ROOT = "demo/pack";
    /**
     * The pack id of the valley folk faction.
     */
    public static final @NotNull String DAWNHOLD = "dawnhold";
    /**
     * The pack id of the hill clans faction.
     */
    public static final @NotNull String CINDERMAW = "cindermaw";
    /**
     * The pack id of the only resource kind.
     */
    public static final @NotNull String GOLD = "gold";

    /**
     * Opens the demo's pack in the resources of a class loader and reads its content.
     *
     * @param loader the class loader of the demo's classes and resources
     * @return the content; the caller closes it
     * @throws IOException if the pack or its content cannot be read
     */
    public static @NotNull DemoContent open(@NotNull ClassLoader loader) throws IOException {
        DemoOrders orders = new DemoOrders();
        StandardPack pack = StandardPack.openResources(loader, PACK_ROOT);

        try {
            return new DemoContent(pack, orders, pack.readContent(orders.getOrderTypes()));
        } catch (IOException | RuntimeException exception) {
            IgnoreException.execute(
                    pack::close
            );

            throw exception;
        }
    }


    private final @NotNull StandardPack pack;
    private final @NotNull DemoOrders orders;
    private final @NotNull PackContent content;

    private final @NotNull Faction dawnhold;
    private final @NotNull Faction cindermaw;
    private final @NotNull ResourceKind gold;

    private DemoContent(@NotNull StandardPack pack, @NotNull DemoOrders orders, @NotNull PackContent content) {
        this.pack    = pack;
        this.orders  = orders;
        this.content = content;

        this.dawnhold  = content.requireFaction(DAWNHOLD);
        this.cindermaw = content.requireFaction(CINDERMAW);
        this.gold      = content.requireResourceKind(GOLD);
    }

    /**
     * @return the demo's pack, the provider of its resources
     */
    public @NotNull StandardPack getPack() {
        return this.pack;
    }

    /**
     * @return the order types and the command stream of the demo
     */
    public @NotNull DemoOrders getOrders() {
        return this.orders;
    }

    /**
     * @return the declarative requirements of the pack content
     */
    public @NotNull DeclarativeRequirements getRequirements() {
        return this.content.getRequirements();
    }

    /**
     * @return the valley folk faction
     */
    public @NotNull Faction getDawnhold() {
        return this.dawnhold;
    }

    /**
     * @return the hill clans faction
     */
    public @NotNull Faction getCindermaw() {
        return this.cindermaw;
    }

    /**
     * @return the factions in content order, the factions a game setup may name
     */
    public @NotNull List<@NotNull Faction> getFactions() {
        return this.content.getFactions();
    }

    /**
     * @return the gold resource kind
     */
    public @NotNull ResourceKind getGold() {
        return this.gold;
    }

    /**
     * @return the unit types in content order, the types a map names by their debug names
     */
    public @NotNull List<@NotNull UnitType> getUnitTypes() {
        return this.content.getUnitTypes();
    }

    /**
     * @return the resource kinds in content order
     */
    public @NotNull List<@NotNull ResourceKind> getResourceKinds() {
        return this.content.getResourceKinds();
    }


    /**
     * Creates the codec of the demo's game setups: the factions in content order, no options. Every machine builds the same one, so setups
     * cross the network and replays by it.
     *
     * @return the created {@link GameSetupCodec} instance
     */
    public @NotNull GameSetupCodec createSetupCodec() {
        return new GameSetupCodec(this.getFactions(), List.of());
    }

    /**
     * Returns a unit type of the content.
     *
     * @param id the pack id, such as {@code "tiller"}
     * @return the unit type
     * @throws IllegalArgumentException if the content has no unit type of the id
     */
    public @NotNull UnitType requireUnitType(@NotNull String id) {
        return this.content.requireUnitType(id);
    }

    /**
     * Registers the order types and the pack's types with their definitions in the content of the demo's games.
     *
     * @param registry the content registry
     */
    public void register(@NotNull ContentRegistry registry) {
        this.orders.register(registry);
        this.content.register(registry);
    }

    /**
     * Closes the pack.
     *
     * @throws IOException if the pack cannot be closed
     */
    @Override
    public void close() throws IOException {
        this.pack.close();
    }
}
