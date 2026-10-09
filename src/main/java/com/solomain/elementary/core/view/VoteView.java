package com.solomain.elementary.core.view;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Текущее голосование глазами игрока. Голоса открытые: все видят, кто как проголосовал.
 *
 * @param initiatorId кто начал голосование
 * @param votes       голоса: id игрока → «за» или «против»
 * @param deadline    срок голосования
 * @param votesNeeded сколько голосов «за» нужно для перехода к ответам
 */
public record VoteView(String initiatorId, Map<String, Boolean> votes, Instant deadline, int votesNeeded) {

    public VoteView {
        Objects.requireNonNull(initiatorId, "initiatorId");
        votes = Map.copyOf(votes);
        Objects.requireNonNull(deadline, "deadline");
    }
}
