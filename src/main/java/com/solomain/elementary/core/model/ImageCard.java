package com.solomain.elementary.core.model;

import java.util.Objects;

/**
 * Карта-картинка: изображение с необязательной подписью.
 *
 * @param number   номер карты, начиная с 1
 * @param relevant относится ли карта к делу ({@code false} — ложный след); раскрывается только в финале
 * @param image    путь к изображению относительно папки дела
 * @param caption  подпись; {@code null}, если её нет
 */
public record ImageCard(int number, boolean relevant, String image, LocalizedText caption) implements Card {
    public ImageCard {
        if (number < 1) {
            throw new IllegalArgumentException("Card number must be >= 1, got " + number);
        }

        Objects.requireNonNull(image, "image");
    }
}
