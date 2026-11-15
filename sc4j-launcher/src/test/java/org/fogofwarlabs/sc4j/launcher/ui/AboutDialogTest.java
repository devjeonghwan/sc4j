package org.fogofwarlabs.sc4j.launcher.ui;

import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.DialogPane;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import org.fogofwarlabs.sc4j.launcher.i18n.LauncherTexts;
import org.fogofwarlabs.sc4j.launcher.i18n.UiLanguage;
import org.fogofwarlabs.sc4j.manifest.ManifestException;
import org.fogofwarlabs.sc4j.manifest.ModManifest;
import org.fogofwarlabs.sc4j.manifest.ModManifestReader;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * Tests the About box: SC4J's own notice and the Beacon.Link privacy statement, then the notices of the installed mods under their names in the
 * display language (English when a notice has no text in it), a mod without notices left out and a mod id shown once, at 100% and 200% in
 * English and Korean.
 */
final class AboutDialogTest {
    private static final @NotNull String VERSION = "1.0.0";
    private static final double PERCENT_SCALE = 100;
    private static final @NotNull String NOTICE_MOD = """
                                                      {"format": 1, "id": "example-game", "name": {"en": "Example Game", "ko": "예제 게임"}, "version": "1.0.0", "engine": "0.1",
                                                       "entry": "org.example.ExampleMod",
                                                       "notices": [{"en": "Example Game is a trademark of Example Company.", "ko": "Example Game은 Example Company의 상표입니다."},
                                                                   {"en": "Art: Example Artist, CC BY 4.0."}]}
                                                      """;
    private static final @NotNull String PLAIN_MOD = """
                                                     {"format": 1, "id": "plain-mod", "name": {"en": "Plain Mod"}, "version": "1.0.0", "engine": "0.1", "entry": "org.example.PlainMod"}
                                                     """;

    @BeforeAll
    static void startToolkit() {
        LauncherFxScenario.startToolkit();
    }

    private static @NotNull UiLanguage selectLanguage(@NotNull String languageCode) {
        return languageCode.equals(UiLanguage.KOREAN.getCode()) ? UiLanguage.KOREAN : UiLanguage.ENGLISH;
    }

    private static @NotNull ModManifest parse(@NotNull String manifest) throws ManifestException {
        return ModManifestReader.parse(manifest.getBytes(StandardCharsets.UTF_8));
    }

    private static @NotNull DialogPane layPane(@NotNull LauncherTexts texts, double scale, @NotNull List<@NotNull ModManifest> mods) {
        Stage owner = new Stage();
        DialogPane pane;
        double width;

        // A dialog takes the style sheets of its owner's scene.
        owner.setScene(new Scene(new Group()));
        pane = new AboutDialog(owner, texts, VERSION, scale, mods, address -> {
        }).getPane();
        // The first layout creates the skins (the button bar's buttons among them), so the pane is sized to its preferred size after it.
        pane.applyCss();
        pane.layout();
        width = pane.prefWidth(-1);
        pane.resize(width, pane.prefHeight(width));
        pane.layout();

        return pane;
    }

    private static @NotNull List<@NotNull String> listSc4jTexts(@NotNull LauncherTexts texts) {
        return List.of(texts.get("about.license"), texts.get("about.fonts"), texts.get("about.privacy.title"), texts.get("about.privacy.address"),
                       texts.get("about.privacy.shared"), texts.get("about.privacy.files"));
    }

    private static @NotNull List<@NotNull String> listTextsFrom(@NotNull DialogPane pane, @NotNull String first) {
        List<String> texts = pane.lookupAll(".label").stream().filter(Label.class::isInstance).map(node -> ((Label) node).getText()).toList();

        return texts.subList(texts.indexOf(first), texts.size());
    }

    @ParameterizedTest
    @CsvSource({"1.0, en", "2.0, en", "1.0, ko", "2.0, ko"})
    void about_dialog_shows_the_sc4j_notice_then_the_notices_of_each_mod_under_its_name(double scale, @NotNull String languageCode) throws Exception {
        UiLanguage language = selectLanguage(languageCode);
        LauncherTexts texts = LauncherTexts.of(language);
        List<ModManifest> mods = List.of(parse(NOTICE_MOD), parse(PLAIN_MOD), parse(NOTICE_MOD));
        boolean korean = language == UiLanguage.KOREAN;
        List<String> modTexts = List.of(korean ? "예제 게임" : "Example Game",
                                        korean ? "Example Game은 Example Company의 상표입니다." : "Example Game is a trademark of Example Company.",
                                        "Art: Example Artist, CC BY 4.0.");
        String suffix = Math.round(scale * PERCENT_SCALE) + "-" + language.getCode();

        LauncherFxScenario.run(() -> {
            DialogPane pane = layPane(texts, scale, mods);
            List<String> shown = listTextsFrom(pane, texts.get("about.license"));
            List<String> sc4jTexts = listSc4jTexts(texts);

            Assertions.assertEquals(sc4jTexts, shown.subList(0, sc4jTexts.size()));
            Assertions.assertEquals(modTexts, shown.subList(sc4jTexts.size(), shown.size()));
            LauncherFxScenario.saveSample(pane, scale, "about-" + suffix);

            return null;
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"en", "ko"})
    void about_dialog_without_mod_notices_shows_only_the_sc4j_texts(@NotNull String languageCode) throws Exception {
        LauncherTexts texts = LauncherTexts.of(selectLanguage(languageCode));
        List<ModManifest> mods = List.of(parse(PLAIN_MOD));

        LauncherFxScenario.run(() -> {
            DialogPane pane = layPane(texts, 1, mods);

            Assertions.assertEquals(listSc4jTexts(texts), listTextsFrom(pane, texts.get("about.license")));
            Assertions.assertEquals(List.of(texts.get("about.privacy.title")),
                                    pane.lookupAll(".text-heading").stream().map(node -> ((Label) node).getText()).toList(), "no mod heading");

            return null;
        });
    }
}
