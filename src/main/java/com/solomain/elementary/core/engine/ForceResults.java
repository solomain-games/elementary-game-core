package com.solomain.elementary.core.engine;

import java.util.Objects;

/**
 * Кнопка хозяина «Показать результаты» (ТЗ, 4.8): завершить ответы, не дожидаясь всех.
 * Кто не успел ответить, получает 0 очков.
 *
 * @param playerId кто нажал (должен быть хозяином комнаты)
 */
public record ForceResults(String playerId) implements Command {

    public ForceResults {
        Objects.requireNonNull(playerId, "playerId");
    }
}
