package com.solomain.elementary.core.model;

import java.time.Duration;
import java.util.Objects;

/**
 * Настройки партии, заданные при запуске и не меняющиеся до её конца.
 *
 * @param hostId      идентификатор хозяина комнаты (может нажать «Показать результаты»)
 * @param turnTimeout ограничение времени на ход; {@code null} — без таймера
 */
public record GameSettings(String hostId, Duration turnTimeout) {
    public GameSettings {
        Objects.requireNonNull(hostId);

        if (turnTimeout != null && !turnTimeout.isPositive()) {
            throw new IllegalArgumentException("turn timeout must be > 0, got " + turnTimeout);
        }
    }
}
