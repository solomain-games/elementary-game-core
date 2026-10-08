package com.solomain.elementary.core.engine;

import java.util.Objects;

/**
 * Игрок потерял связь (ТЗ, 4.5). Отправляет game-service при обрыве соединения.
 *
 * <p>Статус становится {@code DISCONNECTED}, карты остаются в руке, его ход ждёт.
 *
 * @param playerId кто отключился
 */
public record PlayerDisconnected(String playerId) implements Command {

    public PlayerDisconnected {
        Objects.requireNonNull(playerId, "playerId");
    }
}
