package com.solomain.elementary.core.engine;

import java.util.Objects;

/**
 * Игрок вернулся в партию (ТЗ, 4.5).
 *
 * <p>Статус становится {@code ACTIVE}. Если он выходил, то забирает из резерва свои карты,
 * которые ещё никто не взял.
 *
 * @param playerId кто вернулся
 */
public record PlayerReturned(String playerId) implements Command {

    public PlayerReturned {
        Objects.requireNonNull(playerId, "playerId");
    }
}
