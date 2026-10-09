package com.solomain.elementary.core;

import com.solomain.elementary.core.model.AnswerOption;
import com.solomain.elementary.core.model.Card;
import com.solomain.elementary.core.model.CaseDefinition;
import com.solomain.elementary.core.model.ImageCard;
import com.solomain.elementary.core.model.LocalizedText;
import com.solomain.elementary.core.model.Question;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

/**
 * Тестовые дела для тестов движка. Общие для всех тестов game-core.
 */
public final class TestCases {

    private TestCases() {
    }

    /**
     * Дело с карточками №1…{@code cardCount} и одним вопросом.
     * Карты с нечётными номерами относятся к делу, с чётными — ложные следы.
     */
    public static CaseDefinition withCards(int cardCount) {
        List<Card> cards = IntStream.rangeClosed(1, cardCount)
                .<Card>mapToObj(n -> new ImageCard(n, n % 2 == 1, "cards/%02d.webp".formatted(n), null))
                .toList();
        var yes = new AnswerOption("yes", text("Да"));
        var no = new AnswerOption("no", text("Нет"));
        var question = new Question("culprit", text("Виновна гостья?"), List.of(yes, no), "yes");
        return new CaseDefinition("test-" + cardCount, cards, List.of(question), text("Разгадка [[1]]"));
    }

    /**
     * Дело с картами 1–30 и тремя вопросами для тестов финала.
     * Правильные ответы: q1 → a, q2 → b, q3 → c.
     */
    public static CaseDefinition quiz() {
        var base = withCards(30);
        var q1 = new Question("q1", text("Кто?"), List.of(option("a"), option("b")), "a");
        var q2 = new Question("q2", text("Как?"), List.of(option("a"), option("b")), "b");
        var q3 = new Question("q3", text("Зачем?"), List.of(option("a"), option("b"), option("c")), "c");
        return new CaseDefinition("test-quiz", base.cards(), List.of(q1, q2, q3), base.solution());
    }

    private static AnswerOption option(String id) {
        return new AnswerOption(id, text("Вариант " + id));
    }

    private static LocalizedText text(String ru) {
        return new LocalizedText(Map.of("ru", ru));
    }
}
