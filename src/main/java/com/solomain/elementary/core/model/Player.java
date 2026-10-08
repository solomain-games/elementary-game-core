package com.solomain.elementary.core.model;

import java.util.List;
import java.util.Objects;

/**
 * Игрок партии.
 *
 * @param id     идентификатор (guestId или userId из токена)
 * @param name   отображаемое имя
 * @param status в игре, отключён или вышел
 * @param hand   номера карт в руке
 */
public record Player(String id, String name, PlayerStatus status, List<Integer> hand) {
    public Player {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(status, "status");
        hand = List.copyOf(hand); // неизменяемая копия; заодно падает на null
    }

    public Player withHand(List<Integer> newHand) {
        return new Player(id, name, status, newHand);
    }

    public Player withStatus(PlayerStatus newStatus) {
        return new Player(id, name, newStatus, hand);
    }
}
