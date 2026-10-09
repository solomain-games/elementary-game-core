package com.solomain.elementary.core.engine;

import java.util.Objects;

/**
 * Начать голосование за переход к ответам (ТЗ, 4.7). Голос инициатора «за» учитывается сразу.
 *
 * @param playerId кто начинает голосование
 */
public record StartVote(String playerId) implements Command {

    public StartVote {
        Objects.requireNonNull(playerId, "playerId");
    }
}
