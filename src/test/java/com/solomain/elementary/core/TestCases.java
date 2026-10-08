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

    private static LocalizedText text(String ru) {
        return new LocalizedText(Map.of("ru", ru));
    }
}
