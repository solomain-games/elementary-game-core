package com.solomain.elementary.core.model;

import java.util.Objects;

/**
 * Текстовая карта: текст поверх фонового изображения.
 *
 * @param number     номер карты, начиная с 1
 * @param relevant   относится ли карта к делу ({@code false} — ложный след); раскрывается только в финале
 * @param text       текст карты
 * @param background путь к фоновому изображению относительно папки дела
 * @param align      выравнивание текста; если не задано — {@link TextAlign#LEFT}
 */
public record TextCard(int number, boolean relevant, LocalizedText text,
                       String background, TextAlign align) implements Card {
    public TextCard {
        if (number < 1) {
            throw new IllegalArgumentException("Card number must be >= 1, got " + number);
        }

        Objects.requireNonNull(text, "text");
        Objects.requireNonNull(background, "background");

        if (align == null) {
            align = TextAlign.LEFT;
        }
    }
}
