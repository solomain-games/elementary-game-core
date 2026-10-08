package com.solomain.elementary.core.engine;

import java.util.Objects;

/**
 * Отключённый игрок не вернулся за 2 минуты (ТЗ, 4.5). Отправляет таймер game-service.
 *
 * <p>Действует как выход: карты уходят в резерв, статус {@code LEFT}.
 * Если игрок уже вернулся, команда отклоняется — это устаревший сигнал таймера.
 *
 * @param playerId чьё ожидание истекло
 */
public record PlayerDisconnectTimeout(String playerId) implements Command {

    public PlayerDisconnectTimeout {
        Objects.requireNonNull(playerId, "playerId");
    }
}
