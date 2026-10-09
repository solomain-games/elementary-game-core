package com.solomain.elementary.core.engine;

import java.util.Map;
import java.util.Objects;

/**
 * Отправить ответы на все финальные вопросы сразу (ТЗ, 4.8). Ответы других игроков не видны.
 *
 * @param playerId кто отвечает
 * @param answers  ответы: id вопроса → id выбранного варианта; по одному на каждый вопрос дела
 */
public record SubmitAnswers(String playerId, Map<String, String> answers) implements Command {

    public SubmitAnswers {
        Objects.requireNonNull(playerId, "playerId");
        answers = Map.copyOf(answers);
    }
}
