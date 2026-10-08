package com.solomain.elementary.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("TextCard")
class TextCardTest {

    // Валидные значения по умолчанию.
    // В каждом тесте меняется только то, что проверяется, — так сразу видна суть теста.
    private static final int NUMBER = 3;
    private static final LocalizedText TEXT = new LocalizedText(Map.of("ru", "Книга в странном переплете"));
    private static final String BACKGROUND = "backgrounds/paper.webp";
    private static final TextAlign ALIGN = TextAlign.CENTER;

    @Test
    @DisplayName("создаётся с корректными данными")
    void createsWithValidData() {
        TextCard card = new TextCard(NUMBER, true, TEXT, BACKGROUND, ALIGN);

        assertThat(card.number()).isEqualTo(NUMBER);
        assertThat(card.relevant()).isTrue();
        assertThat(card.text()).isEqualTo(TEXT);
        assertThat(card.background()).isEqualTo(BACKGROUND);
        assertThat(card.align()).isEqualTo(ALIGN);
    }

    @ParameterizedTest(name = "номер {0}")
    @ValueSource(ints = {0, -1, Integer.MIN_VALUE})
    @DisplayName("отклоняет номер меньше 1")
    void rejectsNonPositiveNumber(int number) {
        assertThatThrownBy(() -> new TextCard(number, true, TEXT, BACKGROUND, ALIGN))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(String.valueOf(number));
    }

    @Test
    @DisplayName("принимает номер 1 — граничное значение")
    void acceptsNumberOne() {
        TextCard card = new TextCard(1, false, TEXT, BACKGROUND, ALIGN);

        assertThat(card.number()).isEqualTo(1);
    }

    @Test
    @DisplayName("отклоняет отсутствующий текст")
    void rejectsMissingText() {
        assertThatNullPointerException()
                .isThrownBy(() -> new TextCard(NUMBER, true, null, BACKGROUND, ALIGN))
                .withMessage("text");
    }

    @Test
    @DisplayName("отклоняет отсутствующий фон")
    void rejectsMissingBackground() {
        assertThatNullPointerException()
                .isThrownBy(() -> new TextCard(NUMBER, true, TEXT, null, ALIGN))
                .withMessage("background");
    }

    @Test
    @DisplayName("без выравнивания — по левому краю")
    void defaultsAlignToLeft() {
        TextCard card = new TextCard(NUMBER, true, TEXT, BACKGROUND, null);

        assertThat(card.align()).isEqualTo(TextAlign.LEFT);
    }
}
