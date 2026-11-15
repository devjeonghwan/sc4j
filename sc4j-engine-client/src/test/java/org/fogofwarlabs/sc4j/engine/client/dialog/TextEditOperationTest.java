package org.fogofwarlabs.sc4j.engine.client.dialog;

import org.fogofwarlabs.sc4j.engine.client.input.Key;
import org.fogofwarlabs.sc4j.engine.client.input.KeyModifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the key mapping of the edit operations.
 */
final class TextEditOperationTest {
    @Test
    void of_key_should_map_accelerator_keys_and_reject_other_modifiers() {
        Assertions.assertEquals(TextEditOperation.SELECT_ALL, TextEditOperation.ofKey(Key.A, KeyModifier.CONTROL.getMask()));
        Assertions.assertEquals(TextEditOperation.CUT, TextEditOperation.ofKey(Key.X, KeyModifier.CONTROL.getMask()));
        Assertions.assertEquals(TextEditOperation.COPY, TextEditOperation.ofKey(Key.C, KeyModifier.CONTROL.getMask()));
        Assertions.assertEquals(TextEditOperation.PASTE, TextEditOperation.ofKey(Key.V, KeyModifier.CONTROL.getMask()));
        Assertions.assertEquals(TextEditOperation.UNDO, TextEditOperation.ofKey(Key.Z, KeyModifier.CONTROL.getMask()));
        Assertions.assertEquals(TextEditOperation.REDO, TextEditOperation.ofKey(Key.Z, KeyModifier.CONTROL.getMask() | KeyModifier.SHIFT.getMask()));
        Assertions.assertNull(TextEditOperation.ofKey(Key.V, 0));
        Assertions.assertNull(TextEditOperation.ofKey(Key.C, KeyModifier.CONTROL.getMask() | KeyModifier.ALT.getMask()));
    }
}
