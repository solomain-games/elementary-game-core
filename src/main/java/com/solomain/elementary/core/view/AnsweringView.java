package com.solomain.elementary.core.view;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ответы глазами игрока (ТЗ, 4.8): вопросы, кто участвует, кто уже ответил и свои ответы.
 * Чужие ответы не видны.
 *
 * @param questions    вопросы без правильных ответов
 * @param participants кто участвует в ответах
 * @param answered     кто уже ответил
 * @param ownAnswers   свои ответы: id вопроса → id варианта; пусто, если ещё не ответил
 */
public record AnsweringView(List<QuestionView> questions,
                            Set<String> participants,
                            Set<String> answered,
                            Map<String, String> ownAnswers) {

    public AnsweringView {
        questions = List.copyOf(questions);
        participants = Set.copyOf(participants);
        answered = Set.copyOf(answered);
        ownAnswers = Map.copyOf(ownAnswers);
    }
}
