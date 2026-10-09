package com.solomain.elementary.core.engine;

import java.time.Instant;
import java.util.Objects;

/**
 * Срок голосования истёк. Отправляет таймер game-service.
 *
 * <p>Не проголосовавшие считаются «против», и голосование завершается. Срок служит
 * идентификатором голосования: если он не совпадает со сроком текущего голосования
 * (то уже завершилось, и началось новое), сигнал устарел и отклоняется.
 *
 * @param deadline срок голосования, для которого запускался таймер
 */
public record VoteTimeout(Instant deadline) implements Command {

    public VoteTimeout {
        Objects.requireNonNull(deadline, "deadline");
    }
}
