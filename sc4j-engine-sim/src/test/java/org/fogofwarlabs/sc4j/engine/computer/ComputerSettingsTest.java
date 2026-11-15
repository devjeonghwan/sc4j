package org.fogofwarlabs.sc4j.engine.computer;

import org.fogofwarlabs.sc4j.engine.content.type.OrderType;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the settings of the engine default computer player: the builder keeps every number a mod sets and rejects a number outside its range.
 */
final class ComputerSettingsTest {
    private static final int FIRST_WAVE_SIZE = 7;

    private final @NotNull OrderType gather;
    private final @NotNull OrderType attackMove;

    ComputerSettingsTest() {
        this.gather     = new OrderType("gather");
        this.attackMove = new OrderType("attack move");
    }

    @Test
    void build_should_keep_every_number_the_mod_sets() {
        ComputerSettings settings = this.createBuilder().build();

        Assertions.assertSame(this.gather, settings.getGatherOrder());
        Assertions.assertSame(this.attackMove, settings.getAttackMoveOrder());
        Assertions.assertEquals(3, settings.getDecisionInterval());
        Assertions.assertEquals(4, settings.getWorkerTarget());
        Assertions.assertEquals(5, settings.getSupplyMargin());
        Assertions.assertEquals(6, settings.getProductionBuildingTarget());
        Assertions.assertEquals(FIRST_WAVE_SIZE, settings.getFirstWaveSize());
        Assertions.assertEquals(8, settings.getWaveGrowth());
        Assertions.assertEquals(9, settings.getMaximumWaveSize());
        Assertions.assertEquals(10, settings.getBuildRadius());
        Assertions.assertEquals(11, settings.getBuildingSpacing());
        Assertions.assertEquals(12, settings.getResourceClearance());
        Assertions.assertEquals(13, settings.getResourceSearchRange());
    }

    @Test
    void build_should_reject_a_number_outside_its_range() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> this.createBuilder().setDecisionInterval(0).build());
        Assertions.assertThrows(IllegalArgumentException.class, () -> this.createBuilder().setMaximumWaveSize(FIRST_WAVE_SIZE - 1).build());
        Assertions.assertThrows(IllegalArgumentException.class, () -> this.createBuilder().setResourceSearchRange(-1).build());
    }

    private ComputerSettings.@NotNull Builder createBuilder() {
        return new ComputerSettings.Builder(this.gather, this.attackMove).setDecisionInterval(3)
                                                                         .setWorkerTarget(4)
                                                                         .setSupplyMargin(5)
                                                                         .setProductionBuildingTarget(6)
                                                                         .setFirstWaveSize(FIRST_WAVE_SIZE)
                                                                         .setWaveGrowth(8)
                                                                         .setMaximumWaveSize(9)
                                                                         .setBuildRadius(10)
                                                                         .setBuildingSpacing(11)
                                                                         .setResourceClearance(12)
                                                                         .setResourceSearchRange(13);
    }
}
