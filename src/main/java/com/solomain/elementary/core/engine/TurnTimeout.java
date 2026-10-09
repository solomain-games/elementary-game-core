package com.solomain.elementary.core.engine;

import java.util.Objects;

/**
 * Время хода истекло (ТЗ, 4.3). Отправляет таймер game-service.
 *
 * <p>Ядро само время не отсчитывает: оно хранит срок хода в {@code GameState.turnDeadline},
 * а game-service присылает эту команду, когда срок наступил. Тогда на стол выкладывается
 * случайная карта из руки игрока, и ход переходит дальше.
 *
 * <p>Сигнал может оказаться устаревшим (игрок успел сходить, пока сигнал шёл), поэтому команда
 * несёт номер хода и игрока, для которых таймер запускался. Если они не совпадают с текущими,
 * команда отклоняется.
 *
 * @param playerId   чей ход истёк
 * @param turnNumber номер хода, для которого запускался таймер
 */
public record TurnTimeout(String playerId, int turnNumber) implements Command {

    public TurnTimeout {
        Objects.requireNonNull(playerId, "playerId");
    }
}
