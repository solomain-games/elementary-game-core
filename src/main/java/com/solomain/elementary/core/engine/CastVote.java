package com.solomain.elementary.core.engine;

import java.util.Objects;

/**
 * Проголосовать в текущем голосовании. Изменить голос нельзя.
 *
 * @param playerId кто голосует
 * @param inFavor  {@code true} — «за» переход к ответам, {@code false} — «против»
 */
public record CastVote(String playerId, boolean inFavor) implements Command {

    public CastVote {
        Objects.requireNonNull(playerId, "playerId");
    }
}
