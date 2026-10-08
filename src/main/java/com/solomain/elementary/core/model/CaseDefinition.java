package com.solomain.elementary.core.model;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Дело, загруженное из файла: всё, что нужно правилам игры и представлению игрока.
 *
 * <p>Не меняется во время партии. Предыстория, обложка и прочий контент,
 * не нужный правилам, в ядро не попадают.
 *
 * @param id        идентификатор дела, совпадает с {@code id} в case.json
 * @param cards     все карты дела, включая стартовую карту №1
 * @param questions финальные вопросы, хотя бы один; {@code id} не повторяются
 * @param solution  истинная картина; ссылки на карты размечены как {@code [[15]]}
 */
public record CaseDefinition(String id, List<Card> cards, List<Question> questions, LocalizedText solution) {
    public CaseDefinition {
        Objects.requireNonNull(id, "id");

        cards = List.copyOf(cards);
        Set<Integer> cardNumbers = new HashSet<>();
        for (Card card : cards) {
            if (!cardNumbers.add(card.number())) {
                throw new IllegalArgumentException("duplicate card number: " + card.number());
            }
        }
        if (!cardNumbers.contains(1)) {
            throw new IllegalArgumentException("case " + id + " has no starting card #1");
        }

        questions = List.copyOf(questions);
        if (questions.isEmpty()) {
            throw new IllegalArgumentException("case " + id + " has no questions");
        }
        Set<String> questionIds = new HashSet<>();
        for (Question question : questions) {
            if (!questionIds.add(question.id())) {
                throw new IllegalArgumentException("duplicate question id: " + question.id());
            }
        }

        Objects.requireNonNull(solution, "solution");
    }

    /**
     * Карта по номеру.
     *
     * @throws IllegalArgumentException если в деле нет карты с таким номером
     */
    public Card card(int number) {
        return cards.stream()
                .filter(card -> card.number() == number)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("case " + id + " has no card #" + number));
    }
}
