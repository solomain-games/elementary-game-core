package com.solomain.elementary.core.engine;

/**
 * Команда игрока или системы, которую обрабатывает {@link GameEngine#handle}.
 *
 * <p>Команды игроков содержат {@code playerId} — от чьего имени выполняется команда (его определяет
 * game-service по токену, а не по данным из сообщения). Системные команды (сигналы таймеров) игрока
 * могут и не иметь.
 *
 * <p>Интерфейс запечатан: список команд закрыт, и {@code switch} по командам проверяется
 * компилятором на полноту. Новые команды (выход, голосование, ответы) добавляются в {@code permits}
 * в следующих задачах.
 */
public sealed interface Command
        permits PlayCard, DiscardCard, TurnTimeout,
        PlayerDisconnected, PlayerDisconnectTimeout, PlayerLeft, PlayerReturned,
        StartVote, CastVote, VoteTimeout,
        SubmitAnswers, ForceResults {
}
