package com.solomain.elementary.core.engine;

import java.util.Objects;

/**
 * Игрок, каким его знает игровой сервис до начала партии.
 *
 * @param id   идентификатор игрока (guestId или userId из токена)
 * @param name отображаемое имя
 */
public record PlayerInfo(String id, String name) {
    public PlayerInfo {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(name, "name");
    }
}
