package com.solomain.elementary.core.engine;

/**
 * Команда игрока или системы, которую обрабатывает {@link GameEngine#handle}.
 *
 * <p>Интерфейс запечатан: список команд закрыт, и {@code switch} по командам проверяется
 * компилятором на полноту. Новые команды (выход, голосование, ответы) добавляются в {@code permits}
 * в следующих задачах.
 */
public sealed interface Command permits PlayCard, DiscardCard {

    /** Идентификатор игрока, от имени которого выполняется команда. */
    String playerId();
}
