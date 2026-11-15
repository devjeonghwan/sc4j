package org.fogofwarlabs.sc4j.tools.api;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Checks the API listing of compiled classes: what it lists, its text form and the difference of two listings.
 */
final class ApiListingTest {
    private static final @NotNull String GETTER = """
                                                      public int getHealth() {
                                                          return this.health;
                                                      }
                                                  """;
    private static final @NotNull String DAMAGE = """
                                                      protected void damage(int amount) {
                                                          this.health -= amount;
                                                      }
                                                  """;
    private static final @NotNull String UNIT_START = """
                                                      package org.example;
                                                      
                                                      public class Unit {
                                                          public static final int LIMIT = 12;
                                                          protected int health;
                                                          int packageField;
                                                          private int privateField;
                                                      
                                                          public Unit() {
                                                          }
                                                      
                                                          Unit(int health) {
                                                              this.health = health;
                                                          }
                                                      
                                                          private void reset() {
                                                          }
                                                      
                                                          public static final class Part {
                                                              public void attach() {
                                                              }
                                                          }
                                                      
                                                          private static final class Secret {
                                                              public void reveal() {
                                                              }
                                                          }
                                                      """;
    private static final @NotNull String UNIT_END = "}\n";
    private static final @NotNull String UNIT_SOURCE = UNIT_START + GETTER + DAMAGE + UNIT_END;
    private static final @NotNull Map<@NotNull String, @NotNull String> OTHER_SOURCES = Map.of(
            "org.example.Helper", """
                                  package org.example;
                                  
                                  final class Helper {
                                      public static final class Inner {
                                          public void help() {
                                          }
                                      }
                                  }
                                  """,
            "org.example.Hidden", """
                                  package org.example;

                                  import org.jetbrains.annotations.ApiStatus;

                                  @ApiStatus.Internal
                                  public final class Hidden {
                                      public void hide() {
                                      }
                                  }
                                  """,
            "org.example.loading.package-info", """
                                                @ApiStatus.Internal
                                                package org.example.loading;

                                                import org.jetbrains.annotations.ApiStatus;
                                                """,
            "org.example.loading.Loader", """
                                          package org.example.loading;
                                          
                                          public final class Loader {
                                              public void load() {
                                              }
                                          }
                                          """
    );
    private static final @NotNull String EXPECTED_LISTING = """
                                                            public class org.example.Unit {
                                                              protected int health;
                                                              protected void damage(int);
                                                              public int getHealth();
                                                              public org.example.Unit();
                                                              public static final int LIMIT = 12;
                                                            }
                                                            
                                                            public final class org.example.Unit$Part {
                                                              public org.example.Unit$Part();
                                                              public void attach();
                                                            }
                                                            """;

    private static @NotNull ApiListing createListing(@NotNull Path directory, @NotNull String unitSource) throws IOException {
        Path classes = directory.resolve("classes");

        CompilationScenario.compile(directory.resolve("sources"), classes, OTHER_SOURCES);
        CompilationScenario.compile(directory.resolve("unit-sources"), classes, Map.of("org.example.Unit", unitSource));

        return ApiListing.create(List.of(classes), InternalElements.read(List.of(classes)));
    }

    @Test
    void create_should_list_public_and_protected_members_of_api_classes_only(@TempDir @NotNull Path directory) throws IOException {
        ApiListing listing = createListing(directory, UNIT_SOURCE);

        Assertions.assertEquals(Set.of("org.example.Unit", "org.example.Unit$Part"), listing.getClassNames());
        Assertions.assertEquals(EXPECTED_LISTING, listing.format());
    }

    @Test
    void create_should_not_depend_on_the_order_of_declarations(@TempDir @NotNull Path directory) throws IOException {
        ApiListing listing = createListing(directory, UNIT_START + DAMAGE + GETTER + UNIT_END);

        Assertions.assertEquals(EXPECTED_LISTING, listing.format());
    }

    @Test
    void parse_should_read_the_text_form_back(@TempDir @NotNull Path directory) throws IOException {
        ApiListing listing = createListing(directory, UNIT_SOURCE);
        ApiListing parsed = ApiListing.parse(listing.format());

        Assertions.assertEquals(listing, parsed);
        Assertions.assertEquals(List.of(), listing.describeDifference(parsed));
    }

    @Test
    void describe_difference_should_name_removed_and_added_members_per_class(@TempDir @NotNull Path directory) throws IOException {
        ApiListing expected = createListing(directory.resolve("expected"), UNIT_SOURCE);
        String changedSource = UNIT_SOURCE.replace("public int getHealth()", "public long getHealth()")
                                          .replace("private void reset()", "public void reset()");
        ApiListing actual = createListing(directory.resolve("actual"), changedSource);

        Assertions.assertEquals(List.of("org.example.Unit", "  - public int getHealth();", "  + public long getHealth();", "  + public void reset();"),
                                expected.describeDifference(actual));
    }

    @Test
    void describe_difference_should_name_removed_and_added_classes(@TempDir @NotNull Path directory) throws IOException {
        ApiListing listing = createListing(directory, UNIT_SOURCE);

        Assertions.assertEquals(List.of("org.example.Unit", "  + public class org.example.Unit", "  + protected int health;",
                                        "  + protected void damage(int);", "  + public int getHealth();", "  + public org.example.Unit();",
                                        "  + public static final int LIMIT = 12;", "org.example.Unit$Part",
                                        "  + public final class org.example.Unit$Part", "  + public org.example.Unit$Part();",
                                        "  + public void attach();"),
                                ApiListing.parse("").describeDifference(listing));
        Assertions.assertEquals(List.of("org.example.Unit$Part", "  - public final class org.example.Unit$Part",
                                        "  - public org.example.Unit$Part();", "  - public void attach();"),
                                listing.describeDifference(ApiListing.parse(EXPECTED_LISTING.substring(0, EXPECTED_LISTING.indexOf("\n\n") + 1))));
    }

    @Test
    void parse_should_reject_text_that_is_no_listing() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> ApiListing.parse("public class org.example.Unit {\n  public void x();\n"));
        Assertions.assertThrows(IllegalArgumentException.class, () -> ApiListing.parse("  public void x();\n"));
    }
}
