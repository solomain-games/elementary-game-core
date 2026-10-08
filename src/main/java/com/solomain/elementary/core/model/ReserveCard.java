package com.solomain.elementary.core.model;

import java.util.Objects;

/**
 * Карта в резерве: принадлежала игроку, который отключился или вышел.
 *
 * <p>Владелец запоминается, чтобы вернуть ему карту, если он вернётся в партию.
 *
 * @param cardNumber номер карты
 * @param ownerId    идентификатор игрока, которому принадлежала карта
 */
public record ReserveCard(int cardNumber, String ownerId) {
    public ReserveCard {
        if (cardNumber < 1) {
            throw new IllegalArgumentException("Card number must be >= 1, got " + cardNumber);
        }

        Objects.requireNonNull(ownerId, "ownerId");
    }
}
