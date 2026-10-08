package com.solomain.elementary.core.engine;

import java.util.Objects;

/**
 * Игрок сам нажал «Выйти» (ТЗ, 4.5).
 *
 * <p>Карты сразу уходят в резерв с пометкой владельца, статус {@code LEFT}, его ходы пропускаются.
 *
 * @param playerId кто вышел
 */
public record PlayerLeft(String playerId) implements Command {

    public PlayerLeft {
        Objects.requireNonNull(playerId, "playerId");
    }
}
