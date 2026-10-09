package com.solomain.elementary.core.engine;

import com.solomain.elementary.core.error.RuleViolation;
import com.solomain.elementary.core.model.Phase;
import com.solomain.elementary.core.model.PlayerStatus;
import com.solomain.elementary.core.model.ReserveCard;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static com.solomain.elementary.core.TestStates.accepted;
import static com.solomain.elementary.core.TestStates.allCards;
import static com.solomain.elementary.core.TestStates.hand;
import static com.solomain.elementary.core.TestStates.handle;
import static com.solomain.elementary.core.TestStates.player;
import static com.solomain.elementary.core.TestStates.playerById;
import static com.solomain.elementary.core.TestStates.playing;
import static com.solomain.elementary.core.TestStates.reserve;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GameEngine.handle — отключение, выход, возвращение (ТЗ, 4.5)")
class GameEnginePresenceTest {

    @Nested
    @DisplayName("отключение")
    class Disconnect {

        @Test
        @DisplayName("меняет статус на DISCONNECTED, карты остаются в руке")
        void marksPlayerDisconnected() {
            var state = playing(0, List.of(10), player("p1", 5), player("p2", 7, 8));

            var after = accepted(handle(state, new PlayerDisconnected("p2")));

            assertThat(playerById(after, "p2").status()).isEqualTo(PlayerStatus.DISCONNECTED);
            assertThat(hand(after, "p2")).containsExactly(7, 8);
            assertThat(after.reserve()).isEmpty();
        }

        @Test
        @DisplayName("если отключился тот, чей ход, ход ждёт его")
        void keepsTurnOfDisconnectedPlayer() {
            var state = playing(1, List.of(10), player("p1", 5), player("p2", 7));

            var after = accepted(handle(state, new PlayerDisconnected("p2")));

            assertThat(after.currentPlayerIndex()).isEqualTo(1);
            assertThat(after.turnNumber()).isEqualTo(state.turnNumber());
        }

        @Test
        @DisplayName("отклоняет повторное отключение")
        void rejectsAlreadyDisconnected() {
            var state = playing(0, List.of(10), player("p1", 5), player("p2", PlayerStatus.DISCONNECTED, 7));

            var result = handle(state, new PlayerDisconnected("p2"));

            assertThat(result).isEqualTo(new CommandResult.Rejected(RuleViolation.INVALID_PLAYER_STATUS));
        }
    }

    @Nested
    @DisplayName("выход и истечение ожидания")
    class Leave {

        @Test
        @DisplayName("выход: карты уходят в резерв с пометкой владельца, статус LEFT")
        void movesCardsToReserveOnLeave() {
            var state = playing(0, List.of(10), player("p1", 5), player("p2", 7, 8));

            var after = accepted(handle(state, new PlayerLeft("p2")));

            var p2 = playerById(after, "p2");
            assertThat(p2.status()).isEqualTo(PlayerStatus.LEFT);
            assertThat(p2.hand()).isEmpty();
            assertThat(after.reserve()).containsExactly(new ReserveCard(7, "p2"), new ReserveCard(8, "p2"));
        }

        @Test
        @DisplayName("карты добавляются в конец резерва, после уже лежащих там")
        void appendsToExistingReserve() {
            var state = playing(0, List.of(10), reserve("p3", 12),
                    player("p1", 5), player("p2", 7), player("p3", PlayerStatus.LEFT));

            var after = accepted(handle(state, new PlayerLeft("p2")));

            assertThat(after.reserve()).containsExactly(new ReserveCard(12, "p3"), new ReserveCard(7, "p2"));
        }

        @Test
        @DisplayName("истечение 2 минут для отключённого действует как выход")
        void timeoutActsAsLeave() {
            var state = playing(0, List.of(10), player("p1", 5), player("p2", PlayerStatus.DISCONNECTED, 7));

            var after = accepted(handle(state, new PlayerDisconnectTimeout("p2")));

            assertThat(playerById(after, "p2").status()).isEqualTo(PlayerStatus.LEFT);
            assertThat(after.reserve()).containsExactly(new ReserveCard(7, "p2"));
        }

        @Test
        @DisplayName("устаревший сигнал таймера: игрок уже вернулся — отклоняется")
        void rejectsTimeoutForActivePlayer() {
            var state = playing(0, List.of(10), player("p1", 5), player("p2", 7));

            var result = handle(state, new PlayerDisconnectTimeout("p2"));

            assertThat(result).isEqualTo(new CommandResult.Rejected(RuleViolation.INVALID_PLAYER_STATUS));
        }

        @Test
        @DisplayName("отклоняет выход уже вышедшего")
        void rejectsLeaveOfLeftPlayer() {
            var state = playing(0, List.of(10), player("p1", 5), player("p2", PlayerStatus.LEFT));

            var result = handle(state, new PlayerLeft("p2"));

            assertThat(result).isEqualTo(new CommandResult.Rejected(RuleViolation.INVALID_PLAYER_STATUS));
        }

        @Test
        @DisplayName("если ушёл тот, чей ход, ход сразу переходит дальше")
        void passesTurnWhenCurrentPlayerLeaves() {
            var state = playing(1, List.of(10), player("p1", 5), player("p2", 7), player("p3", 8));

            var after = accepted(handle(state, new PlayerLeft("p2")));

            assertThat(after.currentPlayerIndex()).isEqualTo(2);
            assertThat(after.turnNumber()).isEqualTo(state.turnNumber() + 1);
        }

        @Test
        @DisplayName("если ушёл не тот, чей ход, очередь не меняется")
        void keepsTurnWhenOtherPlayerLeaves() {
            var state = playing(0, List.of(10), player("p1", 5), player("p2", 7), player("p3", 8));

            var after = accepted(handle(state, new PlayerLeft("p3")));

            assertThat(after.currentPlayerIndex()).isZero();
            assertThat(after.turnNumber()).isEqualTo(state.turnNumber());
        }

        @Test
        @DisplayName("карты ушедшего возвращаются в игру: игрок без карт добирает их в начале хода")
        void reserveCardsComeBackIntoPlay() {
            // У p2 нет карт, колода пуста. p1 выходит в свой ход — его карты в резерве,
            // ход переходит к p2, и он добирает из резерва, а не пропускается.
            var state = playing(0, List.of(), player("p1", 5, 6), player("p2"));

            var after = accepted(handle(state, new PlayerLeft("p1")));

            assertThat(after.currentPlayerIndex()).isEqualTo(1);
            assertThat(hand(after, "p2")).containsExactly(5);
            assertThat(after.reserve()).containsExactly(new ReserveCard(6, "p1"));
            assertThat(after.phase()).isEqualTo(Phase.PLAYING);
        }

        @Test
        @DisplayName("двое ушли одновременно — карты обоих в резерве, ходит оставшийся")
        void twoPlayersLeave() {
            var state = playing(0, List.of(10), player("p1", 5), player("p2", 7), player("p3", 8));

            var afterFirst = accepted(handle(state, new PlayerLeft("p1")));
            var afterSecond = accepted(handle(afterFirst, new PlayerLeft("p2")));

            assertThat(afterSecond.reserve()).containsExactly(new ReserveCard(5, "p1"), new ReserveCard(7, "p2"));
            assertThat(afterSecond.players().get(afterSecond.currentPlayerIndex()).id()).isEqualTo("p3");
            assertThat(allCards(afterSecond)).containsExactlyInAnyOrderElementsOf(allCards(state));
        }
    }

    @Nested
    @DisplayName("возвращение")
    class Return {

        @Test
        @DisplayName("отключённый возвращается со своими картами")
        void returnsDisconnectedPlayer() {
            var state = playing(0, List.of(10), player("p1", 5), player("p2", PlayerStatus.DISCONNECTED, 7, 8));

            var after = accepted(handle(state, new PlayerReturned("p2")));

            assertThat(playerById(after, "p2").status()).isEqualTo(PlayerStatus.ACTIVE);
            assertThat(hand(after, "p2")).containsExactly(7, 8);
        }

        @Test
        @DisplayName("вышедший забирает из резерва только свои карты")
        void takesBackOwnReserveCards() {
            var reserve = List.of(new ReserveCard(7, "p2"), new ReserveCard(12, "p3"), new ReserveCard(8, "p2"));
            var state = playing(0, List.of(10), reserve,
                    player("p1", 5), player("p2", PlayerStatus.LEFT), player("p3", PlayerStatus.LEFT));

            var after = accepted(handle(state, new PlayerReturned("p2")));

            assertThat(playerById(after, "p2").status()).isEqualTo(PlayerStatus.ACTIVE);
            assertThat(hand(after, "p2")).containsExactly(7, 8);
            assertThat(after.reserve()).containsExactly(new ReserveCard(12, "p3"));
        }

        @Test
        @DisplayName("если часть его карт уже разобрали, получает только оставшиеся")
        void takesBackOnlyRemainingCards() {
            // p2 вышел с картами 7 и 8; карту 7 уже добрал p1 — в резерве осталась только 8
            var state = playing(0, List.of(), List.of(new ReserveCard(8, "p2")),
                    player("p1", 5, 7), player("p2", PlayerStatus.LEFT));

            var after = accepted(handle(state, new PlayerReturned("p2")));

            assertThat(hand(after, "p2")).containsExactly(8);
            assertThat(hand(after, "p1")).containsExactly(5, 7);
            assertThat(after.reserve()).isEmpty();
        }

        @Test
        @DisplayName("вернувшийся снова участвует в очерёдности ходов")
        void returnedPlayerGetsTurnsAgain() {
            var state = playing(0, List.of(10, 11), reserve("p2", 7),
                    player("p1", 5), player("p2", PlayerStatus.LEFT), player("p3", 8));

            var returned = accepted(handle(state, new PlayerReturned("p2")));
            var afterTurn = accepted(handle(returned, new PlayCard("p1", 5)));

            assertThat(afterTurn.currentPlayerIndex()).isEqualTo(1);
        }

        @Test
        @DisplayName("отклоняет возвращение того, кто никуда не уходил")
        void rejectsReturnOfActivePlayer() {
            var state = playing(0, List.of(10), player("p1", 5), player("p2", 7));

            var result = handle(state, new PlayerReturned("p2"));

            assertThat(result).isEqualTo(new CommandResult.Rejected(RuleViolation.INVALID_PLAYER_STATUS));
        }
    }

    @Nested
    @DisplayName("общие правила")
    class Common {

        @Test
        @DisplayName("отклоняет команды неизвестного игрока")
        void rejectsUnknownPlayer() {
            var state = playing(0, List.of(10), player("p1", 5));

            assertThat(handle(state, new PlayerLeft("stranger")))
                    .isEqualTo(new CommandResult.Rejected(RuleViolation.UNKNOWN_PLAYER));
            assertThat(handle(state, new PlayerReturned("stranger")))
                    .isEqualTo(new CommandResult.Rejected(RuleViolation.UNKNOWN_PLAYER));
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = Phase.class, names = {"PROLOGUE", "PLAYING", "NO_MOVES_LEFT", "ANSWERING"})
        @DisplayName("выход возможен в любой фазе до финала")
        void allowsLeaveBeforeFinish(Phase phase) {
            var state = playing(0, List.of(10), player("p1", 5), player("p2", 7)).withPhase(phase);

            var result = handle(state, new PlayerLeft("p2"));

            assertThat(result).isInstanceOf(CommandResult.Accepted.class);
        }

        @Test
        @DisplayName("после финала изменения состава отклоняются")
        void rejectsAfterFinish() {
            var state = playing(0, List.of(10), player("p1", 5), player("p2", 7)).withPhase(Phase.FINISHED);

            var result = handle(state, new PlayerLeft("p2"));

            assertThat(result).isEqualTo(new CommandResult.Rejected(RuleViolation.WRONG_PHASE));
        }
    }
}
