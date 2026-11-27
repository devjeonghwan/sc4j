package org.fogofwarlabs.sc4j.translate.service;

import org.fogofwarlabs.sc4j.translate.service.indextranslate.IndexTranslatePreset;
import org.fogofwarlabs.sc4j.translate.service.model.TranslationPreset;
import org.fogofwarlabs.sc4j.translate.service.nllb.NllbPreset;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Lists the translation presets that SC4J offers for download and finds them by id. The first preset, Index-Translate-2B, is the default.
 */
public final class TranslationPresetUtils {
    /**
     * The id of the default preset.
     */
    public static final @NotNull String DEFAULT_PRESET_ID = IndexTranslatePreset.INDEX_TRANSLATE_2B.getId();

    private static final @NotNull List<@NotNull TranslationPreset> PRESETS = List.of(IndexTranslatePreset.INDEX_TRANSLATE_2B,
                                                                                     NllbPreset.DISTILLED_600M, NllbPreset.DISTILLED_1_3B);

    /**
     * @return the offered presets in the order they are shown; unmodifiable
     */
    public static @NotNull List<@NotNull TranslationPreset> getPresets() {
        return PRESETS;
    }

    /**
     * Finds an offered preset by its id.
     *
     * @param id the preset id, such as {@code nllb-200-distilled-600m}
     * @return the preset, or {@code null} when no offered preset has the id
     */
    public static @Nullable TranslationPreset findPreset(@NotNull String id) {
        for (TranslationPreset preset : PRESETS) {
            if (preset.getId().equals(id)) {
                return preset;
            }
        }

        return null;
    }


    private TranslationPresetUtils() {
    }
}
