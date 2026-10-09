package com.solomain.elementary.core.model;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Голосование за переход к ответам (ТЗ, 4.7).
 *
 * @param initiatorId идентификатор игрока, начавшего голосование (его голос «за» учтён сразу)
 * @param votes       голоса: id игрока → {@code true} «за», {@code false} «против»
 * @param deadline    срок голосования: по его истечении не проголосовавшие считаются «против»
 */
public record Vote(String initiatorId, Map<String, Boolean> votes, Instant deadline) {

    public Vote {
        Objects.requireNonNull(initiatorId, "initiatorId");
        votes = Map.copyOf(votes);
        Objects.requireNonNull(deadline, "deadline");
    }

    /** Копия голосования с добавленным голосом игрока. */
    public Vote withVote(String playerId, boolean inFavor) {
        var newVotes = new HashMap<>(votes);
        newVotes.put(playerId, inFavor);
        return new Vote(initiatorId, newVotes, deadline);
    }
}
