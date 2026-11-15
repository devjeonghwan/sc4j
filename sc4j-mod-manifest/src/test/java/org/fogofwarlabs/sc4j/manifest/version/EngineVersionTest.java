package org.fogofwarlabs.sc4j.manifest.version;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the engine version syntax and the compatibility rule: same major version, required minor not above the runtime's.
 */
final class EngineVersionTest {
    @Test
    void supports_should_accept_the_same_major_and_a_lower_or_equal_minor_only() {
        EngineVersion runtime = EngineVersion.parse("1.2");

        Assertions.assertTrue(runtime.supports(EngineVersion.parse("1.0")));
        Assertions.assertTrue(runtime.supports(EngineVersion.parse("1.2")));
        Assertions.assertFalse(runtime.supports(EngineVersion.parse("1.3")));
        Assertions.assertFalse(runtime.supports(EngineVersion.parse("2.0")));
        Assertions.assertFalse(runtime.supports(EngineVersion.parse("0.2")));
    }

    @Test
    void supports_should_apply_the_same_rule_to_major_version_zero() {
        EngineVersion runtime = EngineVersion.parse("0.1");

        Assertions.assertTrue(runtime.supports(EngineVersion.parse("0.0")));
        Assertions.assertTrue(runtime.supports(EngineVersion.parse("0.1")));
        Assertions.assertFalse(runtime.supports(EngineVersion.parse("0.2")));
        Assertions.assertFalse(runtime.supports(EngineVersion.parse("1.0")));
        Assertions.assertFalse(runtime.supports(EngineVersion.parse("1.1")));
    }

    @Test
    void parse_should_reject_text_that_is_not_major_dot_minor() {
        Assertions.assertEquals("10.0", EngineVersion.parse("10.0").toString());
        Assertions.assertThrows(IllegalArgumentException.class, () -> EngineVersion.parse("1"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> EngineVersion.parse("1.0.0"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> EngineVersion.parse("01.0"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> EngineVersion.parse("1.x"));
    }
}
