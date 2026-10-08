package com.solomain.elementary.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("CaseDefinition")
class CaseDefinitionTest {

    private static final String ID = "demo";
    private static final LocalizedText SOLUTION = new LocalizedText(Map.of("ru", "Браслет взяла гостья [[1]]."));

    @Test
    @DisplayName("создаётся с корректными данными")
    void createsWithValidData() {
        var cards = List.of(card(1), card(2), card(3));
        var questions = List.of(question("culprit"), question("motive"));

        var caseDef = new CaseDefinition(ID, cards, questions, SOLUTION);

        assertThat(caseDef.id()).isEqualTo(ID);
        assertThat(caseDef.cards()).isEqualTo(cards);
        assertThat(caseDef.questions()).isEqualTo(questions);
        assertThat(caseDef.solution()).isEqualTo(SOLUTION);
    }

    @Test
    @DisplayName("принимает дело из одной стартовой карты")
    void acceptsSingleStartingCard() {
        var caseDef = new CaseDefinition(ID, List.of(card(1)), List.of(question("culprit")), SOLUTION);

        assertThat(caseDef.cards()).hasSize(1);
    }

    @Test
    @DisplayName("отклоняет отсутствующий идентификатор")
    void rejectsMissingId() {
        assertThatNullPointerException()
                .isThrownBy(() -> new CaseDefinition(null, List.of(card(1)), List.of(question("culprit")), SOLUTION))
                .withMessage("id");
    }

    @Test
    @DisplayName("отклоняет отсутствующую истинную картину")
    void rejectsMissingSolution() {
        assertThatNullPointerException()
                .isThrownBy(() -> new CaseDefinition(ID, List.of(card(1)), List.of(question("culprit")), null))
                .withMessage("solution");
    }

    @Test
    @DisplayName("отклоняет дело без вопросов")
    void rejectsEmptyQuestions() {
        List<Question> noQuestions = List.of();

        assertThatThrownBy(() -> new CaseDefinition(ID, List.of(card(1)), noQuestions, SOLUTION))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("no questions");
    }

    @Test
    @DisplayName("отклоняет повторяющийся номер карты")
    void rejectsDuplicateCardNumber() {
        var cards = List.of(card(1), card(2), card(2));

        assertThatThrownBy(() -> new CaseDefinition(ID, cards, List.of(question("culprit")), SOLUTION))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("duplicate card number")
                .hasMessageContaining("2");
    }

    @Test
    @DisplayName("отклоняет дело без стартовой карты №1")
    void rejectsMissingStartingCard() {
        var cards = List.of(card(2), card(3));

        assertThatThrownBy(() -> new CaseDefinition(ID, cards, List.of(question("culprit")), SOLUTION))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("#1");
    }

    @Test
    @DisplayName("отклоняет повторяющийся идентификатор вопроса")
    void rejectsDuplicateQuestionId() {
        var questions = List.of(question("culprit"), question("culprit"));

        assertThatThrownBy(() -> new CaseDefinition(ID, List.of(card(1)), questions, SOLUTION))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("culprit");
    }

    @Test
    @DisplayName("не зависит от изменений исходного списка карт")
    void copiesCards() {
        var cards = new ArrayList<Card>(List.of(card(1), card(2)));
        var caseDef = new CaseDefinition(ID, cards, List.of(question("culprit")), SOLUTION);

        cards.add(card(3));

        assertThat(caseDef.cards()).extracting(Card::number).containsExactly(1, 2);
    }

    @Test
    @DisplayName("находит карту по номеру")
    void findsCardByNumber() {
        var caseDef = new CaseDefinition(ID, List.of(card(1), card(2), card(3)), List.of(question("culprit")), SOLUTION);

        assertThat(caseDef.card(2).number()).isEqualTo(2);
    }

    @Test
    @DisplayName("отклоняет запрос несуществующей карты")
    void rejectsUnknownCardNumber() {
        var caseDef = new CaseDefinition(ID, List.of(card(1)), List.of(question("culprit")), SOLUTION);

        assertThatThrownBy(() -> caseDef.card(99))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("99");
    }

    private static Card card(int number) {
        return new ImageCard(number, true, "cards/%02d.webp".formatted(number), null);
    }

    private static Question question(String id) {
        var yes = new AnswerOption("yes", new LocalizedText(Map.of("ru", "Да")));
        var no = new AnswerOption("no", new LocalizedText(Map.of("ru", "Нет")));
        return new Question(id, new LocalizedText(Map.of("ru", "Вопрос " + id)), List.of(yes, no), "yes");
    }
}
