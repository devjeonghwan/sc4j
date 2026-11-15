package org.fogofwarlabs.sc4j.beaconlink.protocol.identity;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the player name rules: typed names are normalized (NFC, white space trimmed and runs of it made one space), and empty, too long names
 * and names with the tag separator or control and format characters are refused.
 */
final class PlayerNameUtilsTest {
    @Test
    void normalize_should_trim_and_join_white_space_and_compose_characters() {
        Assertions.assertEquals("Morgan", PlayerNameUtils.normalize("  Morgan\t"));
        Assertions.assertEquals("Ada Morgan", PlayerNameUtils.normalize("Ada \u3000  Morgan"));
        Assertions.assertEquals("line break", PlayerNameUtils.normalize("line\nbreak"));
        Assertions.assertEquals("\u00e9clair", PlayerNameUtils.normalize("e\u0301clair"));
        Assertions.assertEquals("\uc0ac\uc6a9\uc790", PlayerNameUtils.normalize("\uc0ac\uc6a9\uc790"));
        Assertions.assertTrue(PlayerNameUtils.isValid("Ada Morgan"));
        Assertions.assertFalse(PlayerNameUtils.isValid("Ada  Morgan"));
        Assertions.assertFalse(PlayerNameUtils.isValid("e\u0301clair"));
    }

    @Test
    void normalize_should_refuse_names_outside_the_rules() {
        String longest = "a".repeat(PlayerNameUtils.MAXIMUM_LENGTH);

        Assertions.assertEquals(longest, PlayerNameUtils.normalize(longest));
        Assertions.assertEquals("\uac00".repeat(10), PlayerNameUtils.normalize("\uac00".repeat(10)));

        for (String typed : new String[]{"", "   ", longest + "a", "\uac00".repeat(11), "with#tag", "bell\u0007", "zero\u200bwidth",
                                         "private\ue000use"}) {
            Assertions.assertNull(PlayerNameUtils.normalize(typed), typed);
            Assertions.assertFalse(PlayerNameUtils.isValid(typed), typed);
        }
    }
}
