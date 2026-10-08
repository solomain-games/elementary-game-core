package com.solomain.elementary.core.model;

/**
 * Статус игрока в партии.
 */
public enum PlayerStatus {
    /** В игре. */
    ACTIVE,
    /** Потерял связь; ждём возвращения до 2 минут. */
    DISCONNECTED,
    /** Вышел сам или не вернулся после отключения; его ходы пропускаются. */
    LEFT
}
