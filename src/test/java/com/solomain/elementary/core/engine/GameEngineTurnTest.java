package com.solomain.elementary.core.engine;

import com.solomain.elementary.core.error.RuleViolation;
import com.solomain.elementary.core.model.Phase;
import com.solomain.elementary.core.model.PlayerStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;

import static com.solomain.elementary.core.TestStates.accepted;
import static com.solomain.elementary.core.TestStates.allCards;
import static com.solomain.elementary.core.TestStates.hand;
import static com.solomain.elementary.core.TestStates.player;
import static com.solomain.elementary.core.TestStates.playing;
import static com.solomain.elementary.core.TestStates.reserve;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GameEngine.handle — ход игрока")
class GameEngineTurnTest {

    @Nested
    @DisplayName("действие")
    class Action {

        @Test
        @DisplayName("«выложить» переносит карту из руки на стол")
        void playMovesCardToTable() {
            var state = playing(0, List.of(10, 11), player("p1", 5, 6), player("p2", 7));

            var after = accepted(GameEngine.handle(state, new PlayCard("p1", 5)));

            assertThat(after.table()).containsExactly(1, 5);
            assertThat(after.discard()).isEmpty();
            assertThat(hand(after, "p1")).doesNotContain(5);
        }

        @Test
        @DisplayName("«сбросить» переносит карту из руки в сброс, стол не меняется")
        void discardMovesCardToDiscard() {
            var state = playing(0, List.of(10, 11), player("p1", 5, 6), player("p2", 7));

            var after = accepted(GameEngine.handle(state, new DiscardCard("p1", 5)));

            assertThat(after.discard()).containsExactly(5);
            assertThat(after.table()).containsExactly(1);
            assertThat(hand(after, "p1")).doesNotContain(5);
        }

        @Test
        @DisplayName("убирает из руки именно карту с этим номером, а не карту по индексу")
        void removesCardByNumberNotByIndex() {
            // Ловушка List.remove(int): remove(1) удалит элемент с индексом 1, а не число 1.
            var state = playing(0, List.of(), player("p1", 9, 2, 4), player("p2", 7));

            var after = accepted(GameEngine.handle(state, new PlayCard("p1", 2)));

            assertThat(hand(after, "p1")).containsExactly(9, 4);
        }

        @Test
        @DisplayName("после хода ни одна карта не потерялась и не задвоилась")
        void keepsEveryCardExactlyOnce() {
            var state = playing(0, List.of(10, 11), reserve("p3", 12),
                    player("p1", 5, 6), player("p2", 7, 8));

            var after = accepted(GameEngine.handle(state, new DiscardCard("p1", 6)));

            assertThat(allCards(after)).containsExactlyInAnyOrderElementsOf(allCards(state));
        }
    }

    @Nested
    @DisplayName("добор")
    class Draw {

        @Test
        @DisplayName("берёт верхнюю карту основной колоды")
        void drawsTopCardOfDeck() {
            var state = playing(0, List.of(10, 11), player("p1", 5), player("p2", 7));

            var after = accepted(GameEngine.handle(state, new PlayCard("p1", 5)));

            assertThat(hand(after, "p1")).containsExactly(10);
            assertThat(after.deck()).containsExactly(11);
        }

        @Test
        @DisplayName("когда основная колода пуста, берёт из резерва")
        void drawsFromReserveWhenDeckIsEmpty() {
            var state = playing(0, List.of(), reserve("p3", 12), player("p1", 5), player("p2", 7));

            var after = accepted(GameEngine.handle(state, new PlayCard("p1", 5)));

            assertThat(hand(after, "p1")).containsExactly(12);
            assertThat(after.reserve()).isEmpty();
        }

        @Test
        @DisplayName("когда колода и резерв пусты, ничего не берёт")
        void drawsNothingWhenNothingLeft() {
            var state = playing(0, List.of(), player("p1", 5, 6), player("p2", 7));

            var after = accepted(GameEngine.handle(state, new PlayCard("p1", 5)));

            assertThat(hand(after, "p1")).containsExactly(6);
        }
    }

    @Nested
    @DisplayName("переход хода")
    class NextTurn {

        @Test
        @DisplayName("передаёт ход следующему игроку")
        void passesTurnToNextPlayer() {
            var state = playing(0, List.of(10, 11), player("p1", 5), player("p2", 7), player("p3", 8));

            var after = accepted(GameEngine.handle(state, new PlayCard("p1", 5)));

            assertThat(after.currentPlayerIndex()).isEqualTo(1);
        }

        @Test
        @DisplayName("после последнего игрока ход возвращается к первому")
        void wrapsAroundToFirstPlayer() {
            var state = playing(2, List.of(10, 11), player("p1", 5), player("p2", 7), player("p3", 8));

            var after = accepted(GameEngine.handle(state, new PlayCard("p3", 8)));

            assertThat(after.currentPlayerIndex()).isZero();
        }

        @Test
        @DisplayName("пропускает вышедших игроков")
        void skipsLeftPlayers() {
            var state = playing(0, List.of(10, 11),
                    player("p1", 5),
                    player("p2", PlayerStatus.LEFT),
                    player("p3", 8));

            var after = accepted(GameEngine.handle(state, new PlayCard("p1", 5)));

            assertThat(after.currentPlayerIndex()).isEqualTo(2);
        }

        @Test
        @DisplayName("не пропускает отключившихся: их ход ждёт возвращения")
        void doesNotSkipDisconnectedPlayers() {
            var state = playing(0, List.of(10, 11),
                    player("p1", 5),
                    player("p2", PlayerStatus.DISCONNECTED, 7),
                    player("p3", 8));

            var after = accepted(GameEngine.handle(state, new PlayCard("p1", 5)));

            assertThat(after.currentPlayerIndex()).isEqualTo(1);
        }

        @Test
        @DisplayName("пропускает игроков без карт, когда добирать уже нечего")
        void skipsPlayersWithEmptyHand() {
            var state = playing(0, List.of(), player("p1", 5, 6), player("p2"), player("p3", 8));

            var after = accepted(GameEngine.handle(state, new PlayCard("p1", 5)));

            assertThat(after.currentPlayerIndex()).isEqualTo(2);
        }

        @Test
        @DisplayName("единственный игрок ходит снова")
        void singlePlayerKeepsTurn() {
            var state = playing(0, List.of(10), player("p1", 5, 6));

            var after = accepted(GameEngine.handle(state, new PlayCard("p1", 5)));

            assertThat(after.currentPlayerIndex()).isZero();
        }

        @Test
        @DisplayName("увеличивает номер хода")
        void incrementsTurnNumber() {
            var state = playing(0, List.of(10), player("p1", 5), player("p2", 7));

            var after = accepted(GameEngine.handle(state, new PlayCard("p1", 5)));

            assertThat(after.turnNumber()).isEqualTo(state.turnNumber() + 1);
        }

        @Test
        @DisplayName("когда колода, резерв и руки пусты — фаза NO_MOVES_LEFT")
        void switchesToNoMovesLeftWhenAllCardsPlayed() {
            var state = playing(0, List.of(), player("p1", 5), player("p2"));

            var after = accepted(GameEngine.handle(state, new PlayCard("p1", 5)));

            assertThat(after.phase()).isEqualTo(Phase.NO_MOVES_LEFT);
        }

        @Test
        @DisplayName("пока у кого-то есть карты, игра продолжается")
        void staysPlayingWhileSomeoneHasCards() {
            var state = playing(0, List.of(), player("p1", 5), player("p2", 7));

            var after = accepted(GameEngine.handle(state, new PlayCard("p1", 5)));

            assertThat(after.phase()).isEqualTo(Phase.PLAYING);
            assertThat(after.currentPlayerIndex()).isEqualTo(1);
        }
        @Test
        @DisplayName("игрок без карт в начале своего хода добирает из резерва и ходит")
        void playerWithEmptyHandDrawsAtStartOfTurn() {
            // p2 без карт, колода пуста, в резерве карты вышедшего p3
            var state = playing(0, List.of(), reserve("p3", 12, 13),
                    player("p1", 5), player("p2"), player("p3", PlayerStatus.LEFT));

            var after = accepted(GameEngine.handle(state, new PlayCard("p1", 5)));

            // p1 добрал 12 в конце своего хода, p2 добрал 13 в начале своего
            assertThat(after.currentPlayerIndex()).isEqualTo(1);
            assertThat(hand(after, "p2")).containsExactly(13);
            assertThat(after.reserve()).isEmpty();
            assertThat(after.phase()).isEqualTo(Phase.PLAYING);
        }

        @Test
        @DisplayName("ходы не заканчиваются, пока в резерве есть карты")
        void doesNotEndWhileReserveHasCards() {
            var state = playing(0, List.of(), reserve("p3", 12),
                    player("p1", 5), player("p2"), player("p3", PlayerStatus.LEFT));

            var after = accepted(GameEngine.handle(state, new DiscardCard("p1", 5)));

            assertThat(after.phase()).isEqualTo(Phase.PLAYING);
            assertThat(hand(after, "p1")).containsExactly(12);
        }

    }

    @Nested
    @DisplayName("нарушения правил")
    class Violations {

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = Phase.class, names = {"PROLOGUE", "NO_MOVES_LEFT", "ANSWERING", "FINISHED"})
        @DisplayName("ход возможен только в фазе PLAYING")
        void rejectsTurnOutsidePlayingPhase(Phase phase) {
            var state = playing(0, List.of(10), player("p1", 5), player("p2", 7)).withPhase(phase);

            var result = GameEngine.handle(state, new PlayCard("p1", 5));

            assertThat(result).isEqualTo(new CommandResult.Rejected(RuleViolation.WRONG_PHASE));
        }

        @Test
        @DisplayName("отклоняет команду неизвестного игрока")
        void rejectsUnknownPlayer() {
            var state = playing(0, List.of(10), player("p1", 5), player("p2", 7));

            var result = GameEngine.handle(state, new PlayCard("stranger", 5));

            assertThat(result).isEqualTo(new CommandResult.Rejected(RuleViolation.UNKNOWN_PLAYER));
        }

        @Test
        @DisplayName("отклоняет ход не в свою очередь")
        void rejectsNotYourTurn() {
            var state = playing(0, List.of(10), player("p1", 5), player("p2", 7));

            var result = GameEngine.handle(state, new PlayCard("p2", 7));

            assertThat(result).isEqualTo(new CommandResult.Rejected(RuleViolation.NOT_YOUR_TURN));
        }

        @Test
        @DisplayName("отклоняет карту, которой нет в руке (в том числе чужую)")
        void rejectsCardNotInHand() {
            var state = playing(0, List.of(10), player("p1", 5), player("p2", 7));

            var result = GameEngine.handle(state, new DiscardCard("p1", 7));

            assertThat(result).isEqualTo(new CommandResult.Rejected(RuleViolation.CARD_NOT_IN_HAND));
        }
    }
}
