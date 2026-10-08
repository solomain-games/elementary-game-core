package com.solomain.elementary.core.model;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * Финальный вопрос с вариантами ответа; правильный ровно один.
 *
 * @param id              идентификатор вопроса, уникальный в пределах дела
 * @param text            текст вопроса
 * @param options         варианты ответа: не меньше двух, {@code id} не повторяются
 * @param correctOptionId {@code id} правильного варианта
 */
public record Question(String id, LocalizedText text, List<AnswerOption> options, String correctOptionId) {
    public Question {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(text, "text");
        options = List.copyOf(options);
        Objects.requireNonNull(correctOptionId, "correctOptionId");

        if (options.size() < 2) {
            throw new IllegalArgumentException(
                    "question " + id + " must have at least 2 options, got " + options.size());
        }
        Set<String> optionIds = new HashSet<>();
        for (AnswerOption option : options) {
            if (!optionIds.add(option.id())) {
                throw new IllegalArgumentException("question " + id + ": duplicate option id: " + option.id());
            }
        }

        boolean correctExists = options.stream()
                .anyMatch(option -> option.id().equals(correctOptionId));
        if (!correctExists) {
            throw new IllegalArgumentException(
                    "question " + id + ": correct option " + correctOptionId + " not found");
        }
    }
}
