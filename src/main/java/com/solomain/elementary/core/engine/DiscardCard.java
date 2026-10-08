package com.solomain.elementary.core.engine;

import java.util.Objects;

/**
 * Сбросить карту из руки в закрытый сброс, если игрок считает её ложным следом (ТЗ, 4.2).
 *
 * @param playerId   кто ходит
 * @param cardNumber номер карты из руки
 */
public record DiscardCard(String playerId, int cardNumber) implements Command {

    public DiscardCard {
        Objects.requireNonNull(playerId, "playerId");
    }
}
