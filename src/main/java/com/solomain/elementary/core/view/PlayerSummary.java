package com.solomain.elementary.core.view;

import com.solomain.elementary.core.model.PlayerStatus;

import java.util.Objects;

/**
 * Игрок глазами других: имя, статус и сколько у него карт, но не какие (ТЗ, 4.6).
 *
 * @param id       идентификатор игрока
 * @param name     отображаемое имя
 * @param status   в игре, отключён или вышел
 * @param handSize число карт в руке
 */
public record PlayerSummary(String id, String name, PlayerStatus status, int handSize) {

    public PlayerSummary {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(status, "status");
    }
}
