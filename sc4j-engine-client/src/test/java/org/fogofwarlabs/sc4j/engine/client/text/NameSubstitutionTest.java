package org.fogofwarlabs.sc4j.engine.client.text;

import org.fogofwarlabs.sc4j.engine.resource.text.StringTable;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Tests the name substitution of interface text and its application to string tables.
 */
final class NameSubstitutionTest {
    private static final @NotNull NameSubstitution SUBSTITUTION = new NameSubstitution("Example.net", "Sample.Link");

    @Test
    void substitute_should_replace_name_in_every_case_style() {
        Assertions.assertEquals("Sample.Link", SUBSTITUTION.substitute("Example.net"));
        Assertions.assertEquals("Sample.Link®", SUBSTITUTION.substitute("Example.net®"));
        Assertions.assertEquals("Your Sample.Link Account", SUBSTITUTION.substitute("Your Example.net Account"));
        Assertions.assertEquals("SAMPLE.LINK", SUBSTITUTION.substitute("EXAMPLE.NET"));
        Assertions.assertEquals("Sample.Link", SUBSTITUTION.substitute("Example.Net"));
        Assertions.assertEquals("친구 {friend} 님이 Sample.Link에 접속했습니다.", SUBSTITUTION.substitute("친구 {friend} 님이 Example.net에 접속했습니다."));
        Assertions.assertEquals("Sample.Link help topics: Sample.Link", SUBSTITUTION.substitute("Example.net help topics: Example.net"));
    }

    @Test
    void substitute_should_keep_identifiers_and_host_names() {
        String unchanged = "menuExamplenetChat friendJoined_enteredExamplenet";

        Assertions.assertSame(unchanged, SUBSTITUTION.substitute(unchanged));
        Assertions.assertEquals("https://www.examplenet.org/support", SUBSTITUTION.substitute("https://www.examplenet.org/support"));
        Assertions.assertEquals("see us.example.net or example.net", SUBSTITUTION.substitute("see us.example.net or example.net"));
        Assertions.assertEquals("www.Example.net/x Example.network", SUBSTITUTION.substitute("www.Example.net/x Example.network"));
    }

    @Test
    void substitute_should_rewrite_string_tables_and_keep_unchanged_tables() {
        StringTable unchanged = new StringTable(List.of("Single Player", "example.net"));
        StringTable table = new StringTable(List.of("Single Player", "Example.net"));

        Assertions.assertSame(unchanged, SUBSTITUTION.substitute(unchanged));
        Assertions.assertEquals(List.of("Single Player", "Sample.Link"), SUBSTITUTION.substitute(table).getStrings());
        Assertions.assertSame(table, TextSubstitutionHook.NONE.substitute(table));
    }
}
