package com.solomain.elementary.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Question")
class QuestionTest {

    private static final String ID = "culprit";
    private static final LocalizedText TEXT = text("Кто взял браслет?");
    private static final AnswerOption OWNER = new AnswerOption("owner", text("Хозяйка"));
    private static final AnswerOption GUEST = new AnswerOption("guest", text("Гостья"));
    private static final List<AnswerOption> OPTIONS = List.of(OWNER, GUEST);

    @Test
    @DisplayName("создаётся с корректными данными")
    void createsWithValidData() {
        Question question = new Question(ID, TEXT, OPTIONS, "guest");

        assertThat(question.id()).isEqualTo(ID);
        assertThat(question.text()).isEqualTo(TEXT);
        assertThat(question.options()).containsExactly(OWNER, GUEST);
        assertThat(question.correctOptionId()).isEqualTo("guest");
    }

    @Test
    @DisplayName("отклоняет правильный ответ, которого нет среди вариантов")
    void rejectsUnknownCorrectOption() {
        assertThatThrownBy(() -> new Question(ID, TEXT, OPTIONS, "gardener"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(ID)
                .hasMessageContaining("gardener");
    }

    @Test
    @DisplayName("отклоняет отсутствующий правильный ответ")
    void rejectsMissingCorrectOption() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Question(ID, TEXT, OPTIONS, null))
                .withMessage("correctOptionId");
    }

    @Test
    @DisplayName("отклоняет отсутствующий идентификатор")
    void rejectsMissingId() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Question(null, TEXT, OPTIONS, "guest"))
                .withMessage("id");
    }

    @Test
    @DisplayName("отклоняет отсутствующий текст")
    void rejectsMissingText() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Question(ID, null, OPTIONS, "guest"))
                .withMessage("text");
    }

    @Test
    @DisplayName("отклоняет вопрос с одним вариантом ответа")
    void rejectsSingleOption() {
        var options = List.of(GUEST);

        assertThatThrownBy(() -> new Question(ID, TEXT, options, "guest"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 2");
    }

    @Test
    @DisplayName("принимает вопрос с двумя вариантами — граничное значение")
    void acceptsTwoOptions() {
        Question question = new Question(ID, TEXT, List.of(OWNER, GUEST), "owner");

        assertThat(question.options()).hasSize(2);
    }

    @Test
    @DisplayName("отклоняет повторяющийся идентификатор варианта")
    void rejectsDuplicateOptionId() {
        AnswerOption anotherOwner = new AnswerOption("owner", text("Хозяйка дома"));
        List<AnswerOption> options = List.of(OWNER, anotherOwner, GUEST);

        assertThatThrownBy(() -> new Question(ID, TEXT, options, "guest"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate option id")
                .hasMessageContaining("owner");
    }

    @Test
    @DisplayName("не зависит от изменений исходного списка вариантов")
    void copiesOptions() {
        List<AnswerOption> options = new ArrayList<>(OPTIONS);
        Question question = new Question(ID, TEXT, options, "guest");

        options.add(new AnswerOption("gardener", text("Садовник")));

        assertThat(question.options()).containsExactly(OWNER, GUEST);
    }

    @Test
    @DisplayName("не позволяет изменить варианты через options()")
    void exposesUnmodifiableOptions() {
        Question question = new Question(ID, TEXT, OPTIONS, "guest");

        assertThatThrownBy(() -> question.options().clear())
                .isInstanceOf(UnsupportedOperationException.class);
    }

    private static LocalizedText text(String ru) {
        return new LocalizedText(Map.of("ru", ru));
    }
}
