package com.solomain.elementary.core.engine;

import com.solomain.elementary.core.error.RuleViolation;
import com.solomain.elementary.core.model.GameSettings;
import com.solomain.elementary.core.model.GameState;
import com.solomain.elementary.core.model.Phase;
import com.solomain.elementary.core.model.Player;
import com.solomain.elementary.core.model.PlayerStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static com.solomain.elementary.core.TestStates.NOW;
import static com.solomain.elementary.core.TestStates.accepted;
import static com.solomain.elementary.core.TestStates.allCards;
import static com.solomain.elementary.core.TestStates.hand;
import static com.solomain.elementary.core.TestStates.handle;
import static com.solomain.elementary.core.TestStates.player;
import static com.solomain.elementary.core.TestStates.playing;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GameEngine.handle — таймер хода (ТЗ, 4.3)")
class GameEngineTimerTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(60);

    /** Партия с таймером 60 с; текущий ход истекает в {@code deadline}. */
    private static GameState timed(int current, Instant deadline, List<Integer> deck, Player... players) {
        return new GameState("test", new GameSettings("p1", TIMEOUT), Phase.PLAYING,
                List.of(players), List.of(1), deck, List.of(), List.of(),
                current, 7, deadline, null,
                Map.of(), Set.of(), Map.of());
    }

    /** Момент после истечения хода. */
    private static final Instant LATER = NOW.plus(TIMEOUT).plusSeconds(1);

    @Nested
    @DisplayName("срок хода")
    class Deadline {

        @Test
        @DisplayName("после хода новый срок = текущий момент + время на ход")
        void setsDeadlineForNextTurn() {
            var state = timed(0, NOW.plus(TIMEOUT), List.of(10, 11), player("p1", 5), player("p2", 7));

            var after = accepted(handle(state, new PlayCard("p1", 5)));

            assertThat(after.turnDeadline()).isEqualTo(NOW.plus(TIMEOUT));
        }

        @Test
        @DisplayName("без таймера срока нет")
        void noDeadlineWithoutTimer() {
            var state = playing(0, List.of(10, 11), player("p1", 5), player("p2", 7));

            var after = accepted(handle(state, new PlayCard("p1", 5)));

            assertThat(after.turnDeadline()).isNull();
        }

        @Test
        @DisplayName("когда ходы закончились, срока нет")
        void clearsDeadlineWhenNoMovesLeft() {
            var state = timed(0, NOW.plus(TIMEOUT), List.of(), player("p1", 5), player("p2"));

            var after = accepted(handle(state, new PlayCard("p1", 5)));

            assertThat(after.phase()).isEqualTo(Phase.NO_MOVES_LEFT);
            assertThat(after.turnDeadline()).isNull();
        }

        @Test
        @DisplayName("если ушёл тот, чей ход, следующий получает полный срок")
        void setsDeadlineWhenCurrentPlayerLeaves() {
            var state = timed(0, NOW.plusSeconds(5), List.of(10), player("p1", 5), player("p2", 7));

            var after = accepted(handle(state, new PlayerLeft("p1")));

            assertThat(after.turnDeadline()).isEqualTo(NOW.plus(TIMEOUT));
        }
    }

    @Nested
    @DisplayName("истечение хода")
    class Timeout {

        @Test
        @DisplayName("выкладывает на стол случайную карту из руки и передаёт ход")
        void playsRandomCardFromHand() {
            var state = timed(0, NOW, List.of(10), player("p1", 5, 6, 7), player("p2", 8));

            var after = accepted(handle(state, new TurnTimeout("p1", 7), LATER, new Random(42)));

            var played = after.table().getLast();
            assertThat(after.table()).hasSize(2);
            assertThat(played).isIn(5, 6, 7);
            assertThat(hand(after, "p1")).doesNotContain(played).contains(10); // добрал в конце хода
            assertThat(after.currentPlayerIndex()).isEqualTo(1);
            assertThat(after.turnNumber()).isEqualTo(8);
            assertThat(allCards(after)).containsExactlyInAnyOrderElementsOf(allCards(state));
        }

        @Test
        @DisplayName("выбор карты зависит только от генератора: одинаковый seed — одинаковая карта")
        void isDeterministicForSameSeed() {
            var state = timed(0, NOW, List.of(10), player("p1", 5, 6, 7), player("p2", 8));

            var first = accepted(handle(state, new TurnTimeout("p1", 7), LATER, new Random(1)));
            var second = accepted(handle(state, new TurnTimeout("p1", 7), LATER, new Random(1)));

            assertThat(first).isEqualTo(second);
        }

        @Test
        @DisplayName("срабатывает и для отключённого игрока: таймер хода не ждёт")
        void appliesToDisconnectedPlayer() {
            var state = timed(0, NOW, List.of(10), player("p1", PlayerStatus.DISCONNECTED, 5), player("p2", 8));

            var after = accepted(handle(state, new TurnTimeout("p1", 7), LATER, new Random(42)));

            assertThat(after.table()).containsExactly(1, 5);
        }

        @Test
        @DisplayName("срабатывает ровно в момент окончания срока")
        void firesExactlyAtDeadline() {
            var deadline = NOW;
            var state = timed(0, deadline, List.of(10), player("p1", 5), player("p2", 8));

            var result = handle(state, new TurnTimeout("p1", 7), deadline, new Random(42));

            assertThat(result).isInstanceOf(CommandResult.Accepted.class);
        }
    }

    @Nested
    @DisplayName("отклонённые сигналы")
    class Rejected {

        @Test
        @DisplayName("устаревший номер хода: игрок успел сходить")
        void rejectsStaleTurnNumber() {
            var state = timed(0, NOW, List.of(10), player("p1", 5), player("p2", 8));

            var result = handle(state, new TurnTimeout("p1", 6), LATER, new Random(42));

            assertThat(result).isEqualTo(new CommandResult.Rejected(RuleViolation.STALE_TIMEOUT));
        }

        @Test
        @DisplayName("сигнал для другого игрока, чем тот, чей ход")
        void rejectsWrongPlayer() {
            var state = timed(0, NOW, List.of(10), player("p1", 5), player("p2", 8));

            var result = handle(state, new TurnTimeout("p2", 7), LATER, new Random(42));

            assertThat(result).isEqualTo(new CommandResult.Rejected(RuleViolation.STALE_TIMEOUT));
        }

        @Test
        @DisplayName("сигнал раньше срока")
        void rejectsBeforeDeadline() {
            var state = timed(0, NOW.plusSeconds(30), List.of(10), player("p1", 5), player("p2", 8));

            var result = handle(state, new TurnTimeout("p1", 7), NOW, new Random(42));

            assertThat(result).isEqualTo(new CommandResult.Rejected(RuleViolation.TURN_NOT_EXPIRED));
        }

        @Test
        @DisplayName("в партии без таймера")
        void rejectsWithoutTimer() {
            var state = playing(0, List.of(10), player("p1", 5), player("p2", 8));

            var result = handle(state, new TurnTimeout("p1", state.turnNumber()));

            assertThat(result).isEqualTo(new CommandResult.Rejected(RuleViolation.NO_TURN_TIMER));
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = Phase.class, names = {"PROLOGUE", "NO_MOVES_LEFT", "ANSWERING", "FINISHED"})
        @DisplayName("вне фазы PLAYING")
        void rejectsOutsidePlaying(Phase phase) {
            var state = timed(0, NOW, List.of(10), player("p1", 5), player("p2", 8)).withPhase(phase);

            var result = handle(state, new TurnTimeout("p1", 7), LATER, new Random(42));

            assertThat(result).isEqualTo(new CommandResult.Rejected(RuleViolation.WRONG_PHASE));
        }
    }
}
