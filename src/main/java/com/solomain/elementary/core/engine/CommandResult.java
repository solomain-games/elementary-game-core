package com.solomain.elementary.core.engine;

import com.solomain.elementary.core.error.RuleViolation;
import com.solomain.elementary.core.model.GameState;

import java.util.Objects;

/**
 * Результат обработки команды: либо новое состояние, либо нарушение правил.
 *
 * <p>Нарушение правил — обычная игровая ситуация (сходил не в свою очередь, нажал дважды),
 * поэтому оно возвращается как значение, а не бросается исключением. Вызывающий код обязан
 * разобрать оба варианта:
 *
 * <pre>{@code
 * switch (GameEngine.handle(state, command)) {
 *     case CommandResult.Accepted(GameState newState) -> save(newState);
 *     case CommandResult.Rejected(RuleViolation violation) -> sendError(violation);
 * }
 * }</pre>
 */
public sealed interface CommandResult {

    /**
     * Команда выполнена.
     *
     * @param state новое состояние партии
     */
    record Accepted(GameState state) implements CommandResult {
        public Accepted {
            Objects.requireNonNull(state, "state");
        }
    }

    /**
     * Команда отклонена; состояние партии не изменилось.
     *
     * @param violation какое правило нарушено
     */
    record Rejected(RuleViolation violation) implements CommandResult {
        public Rejected {
            Objects.requireNonNull(violation, "violation");
        }
    }
}
