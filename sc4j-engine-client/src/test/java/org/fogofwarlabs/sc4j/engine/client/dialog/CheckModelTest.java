package org.fogofwarlabs.sc4j.engine.client.dialog;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Tests the check model: a check box toggles when activated, an option button checks itself.
 */
final class CheckModelTest {
    @Test
    void activate_should_toggle_a_check_box_and_check_an_option_button() {
        CheckModel box = new CheckModel(false);
        CheckModel option = new CheckModel(true);

        box.activate();

        Assertions.assertTrue(box.isChecked());

        box.activate();

        Assertions.assertFalse(box.isChecked());

        option.activate();
        option.activate();

        Assertions.assertTrue(option.isChecked());
        Assertions.assertTrue(option.isOption());

        option.setChecked(false);

        Assertions.assertFalse(option.isChecked());
    }
}
