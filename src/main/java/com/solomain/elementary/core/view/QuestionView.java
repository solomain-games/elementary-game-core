package com.solomain.elementary.core.view;

import com.solomain.elementary.core.model.AnswerOption;
import com.solomain.elementary.core.model.LocalizedText;
import com.solomain.elementary.core.model.Question;

import java.util.List;
import java.util.Objects;

/**
 * Финальный вопрос глазами игрока: текст и варианты, но без правильного ответа.
 *
 * @param id      идентификатор вопроса
 * @param text    текст вопроса
 * @param options варианты ответа
 */
public record QuestionView(String id, LocalizedText text, List<AnswerOption> options) {

    public QuestionView {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(text, "text");
        options = List.copyOf(options);
    }

    /** Вопрос без правильного ответа. */
    public static QuestionView of(Question question) {
        return new QuestionView(question.id(), question.text(), question.options());
    }
}
