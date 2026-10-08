package com.solomain.elementary.core.model;

import java.util.Map;
import java.util.Objects;

/**
 * Голосование за переход к ответам.
 *
 * @param initiatorId идентификатор игрока, начавшего голосование
 * @param votes       голоса: id игрока → {@code true} «за», {@code false} «против»
 */
public record Vote(String initiatorId, Map<String, Boolean> votes) {
    public Vote {
        Objects.requireNonNull(initiatorId, "initiatorId");
        votes = Map.copyOf(votes);
    }
}
