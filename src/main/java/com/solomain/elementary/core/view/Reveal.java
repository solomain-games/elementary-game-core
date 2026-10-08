package com.solomain.elementary.core.view;

import com.solomain.elementary.core.model.LocalizedText;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * То, что раскрывается в финале (ТЗ, 4.8). Попадает в представление только в фазе {@code FINISHED}.
 *
 * @param discard        содержимое сброса
 * @param relevantCards  номера всех карт дела, которые относились к делу
 * @param solution       истинная картина
 * @param correctAnswers правильные ответы: id вопроса → id варианта
 * @param scores         очки игроков: id игрока → очки
 */
public record Reveal(List<CardFace> discard,
                     Set<Integer> relevantCards,
                     LocalizedText solution,
                     Map<String, String> correctAnswers,
                     Map<String, Integer> scores) {

    public Reveal {
        discard = List.copyOf(discard);
        relevantCards = Set.copyOf(relevantCards);
        Objects.requireNonNull(solution, "solution");
        correctAnswers = Map.copyOf(correctAnswers);
        scores = Map.copyOf(scores);
    }
}
