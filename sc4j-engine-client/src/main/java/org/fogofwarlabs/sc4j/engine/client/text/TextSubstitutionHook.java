package org.fogofwarlabs.sc4j.engine.client.text;

import org.fogofwarlabs.sc4j.engine.resource.text.StringTable;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

/**
 * Rewrites the interface text a mod shows before it is laid out: a mod replaces names or terms of its installed string tables (for
 * example a product name) once, when the strings are loaded, never in keys, file names or text that a map or a player wrote.
 */
public interface TextSubstitutionHook {
    /**
     * The hook that keeps every text unchanged.
     */
    @NotNull TextSubstitutionHook NONE = new TextSubstitutionHook() {
        /**
         * Rewrites one interface string.
         *
         * @param text the interface text
         * @return the same instance, unchanged
         */
        @Override
        public @NotNull String substitute(@NotNull String text) {
            return text;
        }
    };

    /**
     * Rewrites one interface string.
     *
     * @param text the interface text
     * @return the rewritten text, the same instance when nothing changes
     */
    @NotNull String substitute(@NotNull String text);

    /**
     * Rewrites every string of a table.
     *
     * @param table the loaded string table
     * @return a table of the rewritten strings in the same order, the same instance when no string changes
     */
    default @NotNull StringTable substitute(@NotNull StringTable table) {
        List<String> strings = new ArrayList<>(table.getSize());
        boolean changed = false;

        for (String text : table.getStrings()) {
            String substituted = this.substitute(text);

            changed |= substituted != text;

            strings.add(substituted);
        }

        return changed ? new StringTable(strings) : table;
    }
}
