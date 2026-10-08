package com.solomain.elementary.core.model;

import java.util.Map;

/**
 * Текст, переведённый на несколько языков.
 *
 * <p>Может содержать разметку: {@code **жирный**}, {@code __подчёркнутый__},
 * {@code [[15]]} — ссылка на карту №15.
 *
 * @param values код языка (ISO 639-1, например {@code ru}) → текст
 */
public record LocalizedText(Map<String, String> values) {
    public LocalizedText {
        if (values.isEmpty()) {
            throw new IllegalArgumentException("localizedText must have at least 1 language");
        }
        values = Map.copyOf(values);
    }
}
