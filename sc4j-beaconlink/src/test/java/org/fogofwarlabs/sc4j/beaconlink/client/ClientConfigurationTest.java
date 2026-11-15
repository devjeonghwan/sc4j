package org.fogofwarlabs.sc4j.beaconlink.client;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the client configuration builder: the set request, registration, room and presence timings and their range checks.
 */
final class ClientConfigurationTest {
    @Test
    void build_should_keep_every_set_timing() {
        ClientConfiguration configuration = new ClientConfiguration.Builder().setResendMillis(500L)
                                                                             .setRequestAttempts(3)
                                                                             .setRetryMillis(20_000L)
                                                                             .setRegistrationSeconds(90)
                                                                             .setRoomLifetimeMillis(60_000L)
                                                                             .setRoomRefreshMillis(20_000L)
                                                                             .setPresenceLifetimeMillis(80_000L)
                                                                             .setPresenceRefreshMillis(30_000L)
                                                                             .build();

        Assertions.assertEquals(500L, configuration.getResendMillis());
        Assertions.assertEquals(3, configuration.getRequestAttempts());
        Assertions.assertEquals(20_000L, configuration.getRetryMillis());
        Assertions.assertEquals(90, configuration.getRegistrationSeconds());
        Assertions.assertEquals(60_000L, configuration.getRoomLifetimeMillis());
        Assertions.assertEquals(20_000L, configuration.getRoomRefreshMillis());
        Assertions.assertEquals(80_000L, configuration.getPresenceLifetimeMillis());
        Assertions.assertEquals(30_000L, configuration.getPresenceRefreshMillis());
    }

    @Test
    void build_should_refuse_a_refresh_not_shorter_than_its_lifetime() {
        Assertions.assertThrows(IllegalArgumentException.class,
                                () -> new ClientConfiguration.Builder().setRoomLifetimeMillis(20_000L).setRoomRefreshMillis(20_000L).build());
        Assertions.assertThrows(IllegalArgumentException.class,
                                () -> new ClientConfiguration.Builder().setPresenceLifetimeMillis(30_000L).setPresenceRefreshMillis(40_000L).build());
    }
}
