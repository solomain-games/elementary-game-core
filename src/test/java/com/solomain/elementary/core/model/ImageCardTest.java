package com.solomain.elementary.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ImageCard")
class ImageCardTest {

    // Валидные значения по умолчанию.
    // В каждом тесте меняется только то, что проверяется, — так сразу видна суть теста.
    private static final int NUMBER = 3;
    private static final String IMAGE = "cards/03.webp";
    private static final LocalizedText CAPTION = new LocalizedText(Map.of("ru", "Следы на ковре"));

    @Test
    @DisplayName("создаётся с корректными данными")
    void createsWithValidData() {
        var card = new ImageCard(NUMBER, true, IMAGE, CAPTION);

        assertThat(card.number()).isEqualTo(NUMBER);
        assertThat(card.relevant()).isTrue();
        assertThat(card.image()).isEqualTo(IMAGE);
        assertThat(card.caption()).isEqualTo(CAPTION);
    }

    @Test
    @DisplayName("подпись необязательна")
    void allowsMissingCaption() {
        var card = new ImageCard(NUMBER, false, IMAGE, null);

        assertThat(card.caption()).isNull();
    }

    @ParameterizedTest(name = "номер {0}")
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    @DisplayName("отклоняет номер меньше 1")
    void rejectsNonPositiveNumber(int number) {
        assertThatThrownBy(() -> new ImageCard(number, true, IMAGE, CAPTION))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(String.valueOf(number));
    }

    @Test
    @DisplayName("принимает номер 1 — граничное значение")
    void acceptsNumberOne() {
        var card = new ImageCard(1, true, IMAGE, CAPTION);

        assertThat(card.number()).isEqualTo(1);
    }

    @Test
    @DisplayName("отклоняет отсутствующее изображение")
    void rejectsMissingImage() {
        assertThatNullPointerException()
                .isThrownBy(() -> new ImageCard(NUMBER, true, null, CAPTION))
                .withMessage("image");
    }
}
