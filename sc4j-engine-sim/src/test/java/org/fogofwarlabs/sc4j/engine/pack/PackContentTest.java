package org.fogofwarlabs.sc4j.engine.pack;

import org.fogofwarlabs.sc4j.engine.content.definition.*;
import org.fogofwarlabs.sc4j.engine.content.economy.ResourceKind;
import org.fogofwarlabs.sc4j.engine.content.faction.FactionDefinition;
import org.fogofwarlabs.sc4j.engine.content.registry.ContentDefinition;
import org.fogofwarlabs.sc4j.engine.content.registry.ContentRegistry;
import org.fogofwarlabs.sc4j.engine.content.type.OrderType;
import org.fogofwarlabs.sc4j.engine.content.type.UnitType;
import org.fogofwarlabs.sc4j.engine.game.ControllerKind;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * Tests the content definitions of the engine standard data pack: a pack in class loader resources with its resource kinds, motion profiles, weapons,
 * factions, units and requirements registered in a content registry, and the failures of broken content files.
 */
final class PackContentTest {
    private static final @NotNull String RESOURCE_ROOT = "packs/valley";
    private static final @NotNull String CONTENT_MANIFEST = "{\"format\": 1, \"content\": \"content.json\"}";

    private final @NotNull OrderType idle;
    private final @NotNull OrderType guard;

    PackContentTest() {
        this.idle  = new OrderType("idle");
        this.guard = new OrderType("guard");
    }

    @Test
    void read_content_should_define_the_types_of_a_pack_in_class_loader_resources() throws IOException {
        PackContent content;

        try (StandardPack pack = StandardPack.openResources(PackContentTest.class.getClassLoader(), RESOURCE_ROOT)) {
            Assertions.assertTrue(pack.hasContent());

            content = pack.readContent(List.of(this.idle, this.guard));
        }

        ContentRegistry registry = new ContentRegistry();

        registry.getOrderTypes().register(this.idle, new OrderDefinition.Builder().build());
        registry.getOrderTypes().register(this.guard, new OrderDefinition.Builder().build());
        content.register(registry);

        ContentDefinition definition = registry.build();
        UnitType farmer = content.requireUnitType("farmer");
        UnitType hall = content.requireUnitType("hall");
        UnitDefinition farmerDefinition = definition.getUnitTypes().getDefinition(farmer);
        UnitDefinition mineDefinition = definition.getUnitTypes().getDefinition(content.requireUnitType("mine"));
        UnitDefinition hallDefinition = definition.getUnitTypes().getDefinition(hall);
        FactionDefinition valley = definition.getFactions().getDefinition(content.requireFaction("valley"));
        WeaponDefinition sling = definition.getWeaponTypes().getDefinition(definition.getWeaponTypes().getTypes().get(1));

        Assertions.assertEquals(List.of("gold", "wood"), content.getResourceKinds().stream().map(ResourceKind::getDebugName).toList());
        Assertions.assertEquals(List.of("mine", "farmer", "hall", "barn", "slinger"), content.getUnitTypes().stream().map(UnitType::getDebugName).toList());
        Assertions.assertEquals(40, farmerDefinition.getHitPoints());
        Assertions.assertEquals(6, farmerDefinition.getCollisionLeft());
        Assertions.assertEquals(6, farmerDefinition.getCollisionDown());
        Assertions.assertEquals(50, farmerDefinition.getCost().getAmount(content.requireResourceKind("gold")));
        Assertions.assertSame(this.idle, farmerDefinition.getIdleOrder(ControllerKind.COMPUTER));
        Assertions.assertNull(farmerDefinition.getIdleOrder(ControllerKind.NEUTRAL));
        Assertions.assertNotEquals(0, farmerDefinition.getTraits() & UnitTrait.WORKER.getMask());
        Assertions.assertEquals(31, mineDefinition.getCollisionLeft());
        Assertions.assertEquals(29, mineDefinition.getCollisionDown());
        Assertions.assertEquals(50, hallDefinition.getCost().getAmount(content.requireResourceKind("wood")));
        Assertions.assertNotEquals(0, hallDefinition.getTraits() & UnitTrait.RESOURCE_DEPOT.getMask());
        Assertions.assertSame(farmer, valley.getWorker());
        Assertions.assertSame(hall, valley.getTownHall());
        Assertions.assertEquals(List.of(farmer, farmer), valley.getStartUnits());
        Assertions.assertEquals(2, sling.getDamageFactor());
        Assertions.assertFalse(sling.isHoming());
        Assertions.assertEquals(12, sling.getSplashOuterRadius());
        Assertions.assertEquals(WeaponTarget.GROUND.getMask(), sling.getTargets());
        Assertions.assertSame(definition.getMotionProfiles().getTypes().get(1), sling.getProjectile());
        Assertions.assertNotNull(content.getRequirements());
        Assertions.assertThrows(IllegalArgumentException.class, () -> content.requireUnitType("knight"));
    }

    @Test
    void read_content_should_reject_unknown_fields_ids_and_formats(@TempDir @NotNull Path directory) throws IOException {
        this.assertBroken(directory.resolve("field"), "{\"format\": 1, \"units\": {\"a\": {\"speed\": 3}}}", "units.a.speed");
        this.assertBroken(directory.resolve("id"), "{\"format\": 1, \"units\": {\"a\": {\"faction\": \"elves\"}}}", "elves");
        this.assertBroken(directory.resolve("order"), "{\"format\": 1, \"units\": {\"a\": {\"idleOrders\": {\"human\": \"dance\"}}}}", "dance");
        this.assertBroken(directory.resolve("trait"), "{\"format\": 1, \"units\": {\"a\": {\"traits\": [\"giant\"]}}}", "giant");
        this.assertBroken(directory.resolve("condition"),
                          "{\"format\": 1, \"units\": {\"a\": {}}, \"requirements\": {\"a\": [{\"actor\": \"a\", \"completed\": \"a\"}]}}",
                          "requirements.a[0]");
        this.assertBroken(directory.resolve("format"), "{\"format\": 2, \"races\": {}}", "format 2");
    }

    @Test
    void read_content_should_fail_for_a_pack_without_content_file(@TempDir @NotNull Path directory) throws IOException {
        Files.writeString(directory.resolve(PackManifest.FILE_NAME), "{\"format\": 1}");

        try (StandardPack pack = StandardPack.open(directory)) {
            Assertions.assertFalse(pack.hasContent());
            Assertions.assertThrows(IOException.class, () -> pack.readContent(List.of()));
        }

        Assertions.assertThrows(IOException.class, () -> StandardPack.openResources(PackContentTest.class.getClassLoader(), "packs/missing"));
    }

    private void assertBroken(@NotNull Path directory, @NotNull String contents, @NotNull String expected) throws IOException {
        Files.createDirectories(directory);
        Files.writeString(directory.resolve(PackManifest.FILE_NAME), CONTENT_MANIFEST);
        Files.writeString(directory.resolve("content.json"), contents);

        try (StandardPack pack = StandardPack.open(directory)) {
            IOException exception = Assertions.assertThrows(IOException.class, () -> pack.readContent(List.of(this.idle)));

            Assertions.assertTrue(exception.getMessage().contains(expected), exception.getMessage());
        }
    }
}
