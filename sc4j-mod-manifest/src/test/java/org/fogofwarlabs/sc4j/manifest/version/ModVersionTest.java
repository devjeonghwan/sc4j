package org.fogofwarlabs.sc4j.manifest.version;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the mod version syntax: major.minor.patch with an optional pre-release label.
 */
final class ModVersionTest {
    @Test
    void parse_should_read_releases_and_pre_releases() {
        ModVersion release = ModVersion.parse("1.23.10");
        ModVersion preRelease = ModVersion.parse("2.0.0-beta.1");

        Assertions.assertEquals(23, release.getMinor());
        Assertions.assertEquals(10, release.getPatch());
        Assertions.assertEquals("", release.getPreRelease());
        Assertions.assertEquals("beta.1", preRelease.getPreRelease());
        Assertions.assertEquals(ModVersion.parse("2.0.0-beta.1"), preRelease);
        Assertions.assertThrows(IllegalArgumentException.class, () -> ModVersion.parse("1.0"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> ModVersion.parse("1.0.0-"));
    }
}
