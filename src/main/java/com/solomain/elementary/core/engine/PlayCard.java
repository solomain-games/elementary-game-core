package com.solomain.elementary.core.engine;

import java.util.Objects;

/**
 * Выложить карту из руки на стол (ТЗ, 4.2).
 *
 * @param playerId   кто ходит
 * @param cardNumber номер карты из руки
 */
public record PlayCard(String playerId, int cardNumber) implements Command {

    public PlayCard {
        Objects.requireNonNull(playerId, "playerId");
    }
}
