package com.solomain.elementary.core.model;

import java.util.Objects;

/**
 * Вариант ответа на финальный вопрос.
 *
 * @param id   идентификатор варианта, уникальный в пределах вопроса
 * @param text текст варианта
 */
public record AnswerOption(String id, LocalizedText text) {
    public AnswerOption {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(text, "text");
    }
}
