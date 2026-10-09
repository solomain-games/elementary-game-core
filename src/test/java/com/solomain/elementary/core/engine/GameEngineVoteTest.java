package com.solomain.elementary.core.engine;

import com.solomain.elementary.core.error.RuleViolation;
import com.solomain.elementary.core.model.GameState;
import com.solomain.elementary.core.model.Phase;
import com.solomain.elementary.core.model.Player;
import com.solomain.elementary.core.model.PlayerStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static com.solomain.elementary.core.TestStates.NOW;
import static com.solomain.elementary.core.TestStates.accepted;
import static com.solomain.elementary.core.TestStates.handle;
import static com.solomain.elementary.core.TestStates.player;
import static com.solomain.elementary.core.TestStates.playing;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GameEngine.handle — голосование за финал (ТЗ, 4.7)")
class GameEngineVoteTest {

    /** Партия из {@code count} игроков p1…pN в игре, у каждого по карте, ход p1. */
    private static GameState game(int count) {
        List<Player> players = new ArrayList<>();
        for (int i = 1; i <= count; i++) {
            players.add(player("p" + i, 100 + i));
        }
        return playing(0, List.of(20, 21, 22), players.toArray(Player[]::new));
    }

    /** Выполняет команды по очереди; каждая должна быть принята. */
    private static GameState run(GameState state, Command... commands) {
        for (Command command : commands) {
            state = accepted(handle(state, command));
        }
        return state;
    }

    private static boolean votePassed(GameState state) {
        return state.phase() == Phase.ANSWERING;
    }

    private static boolean voteFailed(GameState state) {
        return state.vote() == null && state.phase() != Phase.ANSWERING;
    }

    @Nested
    @DisplayName("большинство — больше половины игроков в игре")
    class Majority {

        @Test
        @DisplayName("1 игрок: голос инициатора сразу решает")
        void singlePlayerPassesImmediately() {
            var after = run(game(1), new StartVote("p1"));

            assertThat(votePassed(after)).isTrue();
        }

        @Test
        @DisplayName("2 игрока: нужны оба голоса «за»")
        void twoPlayersNeedBoth() {
            var started = run(game(2), new StartVote("p1"));
            assertThat(started.vote()).isNotNull();

            assertThat(votePassed(run(started, new CastVote("p2", true)))).isTrue();
            assertThat(voteFailed(run(started, new CastVote("p2", false)))).isTrue();
        }

        @Test
        @DisplayName("4 игрока: нужно 3 «за»")
        void fourPlayersNeedThree() {
            var twoInFavor = run(game(4), new StartVote("p1"), new CastVote("p2", true));
            assertThat(twoInFavor.vote()).isNotNull();

            assertThat(votePassed(run(twoInFavor, new CastVote("p3", true)))).isTrue();
        }

        @Test
        @DisplayName("4 игрока: два «против» — успех невозможен, голосование завершается сразу")
        void fourPlayersFailEarly() {
            var after = run(game(4), new StartVote("p1"), new CastVote("p2", false), new CastVote("p3", false));

            assertThat(voteFailed(after)).isTrue();
        }

        @Test
        @DisplayName("5 игроков: нужно 3 «за»; при двух «против» исход ещё не ясен")
        void fivePlayersNeedThree() {
            var twoAgainst = run(game(5), new StartVote("p1"), new CastVote("p2", false), new CastVote("p3", false));
            assertThat(twoAgainst.vote()).isNotNull();

            assertThat(votePassed(run(twoAgainst, new CastVote("p4", true), new CastVote("p5", true)))).isTrue();
            assertThat(voteFailed(run(twoAgainst, new CastVote("p4", false)))).isTrue();
        }
    }

    @Nested
    @DisplayName("отключённые и вышедшие")
    class Absent {

        @Test
        @DisplayName("не учитываются: 3 в игре из 4 — достаточно 2 «за»")
        void ignoresDisconnectedPlayers() {
            var state = game(4).withPlayers(List.of(
                    player("p1", 101), player("p2", 102), player("p3", 103),
                    player("p4", PlayerStatus.DISCONNECTED, 104)));

            var after = run(state, new StartVote("p1"), new CastVote("p2", true));

            assertThat(votePassed(after)).isTrue();
        }

        @Test
        @DisplayName("не могут голосовать")
        void disconnectedCannotVote() {
            var state = run(game(3), new StartVote("p1"), new PlayerDisconnected("p3"));

            var result = handle(state, new CastVote("p3", true));

            assertThat(result).isEqualTo(new CommandResult.Rejected(RuleViolation.NOT_ELIGIBLE_TO_VOTE));
        }

        @Test
        @DisplayName("отключение во время голосования пересчитывает большинство")
        void recountsWhenSomeoneDisconnects() {
            // 3 в игре — нужно 2. После отключения двух молчащих остаётся 1 — хватает голоса инициатора.
            var started = run(game(3), new StartVote("p1"));
            var oneLeft = run(started, new PlayerDisconnected("p2"));
            assertThat(oneLeft.vote()).isNotNull();

            var after = run(oneLeft, new PlayerDisconnected("p3"));

            assertThat(votePassed(after)).isTrue();
        }

        @Test
        @DisplayName("участники ответов — только те, кто в игре в момент перехода")
        void answerParticipantsAreActivePlayers() {
            var state = game(4).withPlayers(List.of(
                    player("p1", 101), player("p2", 102), player("p3", 103),
                    player("p4", PlayerStatus.DISCONNECTED, 104)));

            var after = run(state, new StartVote("p1"), new CastVote("p2", true));

            assertThat(after.answerParticipants()).containsExactlyInAnyOrder("p1", "p2", "p3");
        }
    }

    @Nested
    @DisplayName("срок голосования")
    class Deadline {

        @Test
        @DisplayName("голосование длится 30 секунд, инициатор сразу «за»")
        void startsWithDeadlineAndInitiatorVote() {
            var after = run(game(3), new StartVote("p2"));

            assertThat(after.vote().deadline()).isEqualTo(NOW.plusSeconds(30));
            assertThat(after.vote().initiatorId()).isEqualTo("p2");
            assertThat(after.vote().votes()).containsEntry("p2", true).hasSize(1);
        }

        @Test
        @DisplayName("по истечении срока не проголосовавшие — «против»")
        void silentPlayersCountAsAgainst() {
            var state = run(game(4), new StartVote("p1"), new CastVote("p2", true));
            var deadline = state.vote().deadline();

            var after = accepted(handle(state, new VoteTimeout(deadline), deadline, new Random(42)));

            assertThat(voteFailed(after)).isTrue();
        }

        @Test
        @DisplayName("сигнал раньше срока отклоняется")
        void rejectsEarlyTimeout() {
            var state = run(game(4), new StartVote("p1"));

            var result = handle(state, new VoteTimeout(state.vote().deadline()));

            assertThat(result).isEqualTo(new CommandResult.Rejected(RuleViolation.VOTE_NOT_EXPIRED));
        }

        @Test
        @DisplayName("сигнал от другого, уже завершённого голосования отклоняется")
        void rejectsStaleTimeout() {
            var state = run(game(4), new StartVote("p1"));
            var otherDeadline = NOW.minusSeconds(100);

            var result = handle(state, new VoteTimeout(otherDeadline), NOW.plusSeconds(60), new Random(42));

            assertThat(result).isEqualTo(new CommandResult.Rejected(RuleViolation.STALE_TIMEOUT));
        }

        @Test
        @DisplayName("сигнал, когда голосования уже нет, отклоняется")
        void rejectsTimeoutWithoutVote() {
            var result = handle(game(4), new VoteTimeout(NOW));

            assertThat(result).isEqualTo(new CommandResult.Rejected(RuleViolation.STALE_TIMEOUT));
        }
    }

    @Nested
    @DisplayName("переход к ответам")
    class Passing {

        @Test
        @DisplayName("фаза ANSWERING, голосование и срок хода сняты")
        void switchesToAnswering() {
            var state = game(2).withTurnDeadline(NOW.plusSeconds(10));

            var after = run(state, new StartVote("p1"), new CastVote("p2", true));

            assertThat(after.phase()).isEqualTo(Phase.ANSWERING);
            assertThat(after.vote()).isNull();
            assertThat(after.turnDeadline()).isNull();
        }

        @Test
        @DisplayName("работает и когда ходы уже закончились")
        void worksInNoMovesLeft() {
            var after = run(game(2).withPhase(Phase.NO_MOVES_LEFT), new StartVote("p1"), new CastVote("p2", true));

            assertThat(after.phase()).isEqualTo(Phase.ANSWERING);
        }

        @Test
        @DisplayName("после провала игра продолжается, и можно начать заново")
        void canRestartAfterFailure() {
            var failed = run(game(2), new StartVote("p1"), new CastVote("p2", false));
            assertThat(failed.phase()).isEqualTo(Phase.PLAYING);

            var restarted = run(failed, new StartVote("p2"));

            assertThat(restarted.vote()).isNotNull();
        }

        @Test
        @DisplayName("во время голосования ходы продолжаются")
        void turnsContinueDuringVote() {
            var started = run(game(3), new StartVote("p2"));

            var after = run(started, new PlayCard("p1", 101));

            assertThat(after.vote()).isNotNull();
            assertThat(after.table()).containsExactly(1, 101);
        }
    }

    @Nested
    @DisplayName("нарушения правил")
    class Violations {

        @Test
        @DisplayName("нельзя начать второе голосование, пока идёт первое")
        void rejectsSecondVote() {
            var state = run(game(3), new StartVote("p1"));

            assertThat(handle(state, new StartVote("p2")))
                    .isEqualTo(new CommandResult.Rejected(RuleViolation.VOTE_IN_PROGRESS));
        }

        @Test
        @DisplayName("нельзя проголосовать дважды (и изменить голос)")
        void rejectsSecondBallot() {
            var state = run(game(3), new StartVote("p1"));

            assertThat(handle(state, new CastVote("p1", false)))
                    .isEqualTo(new CommandResult.Rejected(RuleViolation.ALREADY_VOTED));
        }

        @Test
        @DisplayName("нельзя голосовать, когда голосования нет")
        void rejectsBallotWithoutVote() {
            assertThat(handle(game(3), new CastVote("p2", true)))
                    .isEqualTo(new CommandResult.Rejected(RuleViolation.NO_VOTE));
        }

        @Test
        @DisplayName("неизвестный игрок")
        void rejectsUnknownPlayer() {
            assertThat(handle(game(3), new StartVote("stranger")))
                    .isEqualTo(new CommandResult.Rejected(RuleViolation.UNKNOWN_PLAYER));
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = Phase.class, names = {"PROLOGUE", "ANSWERING", "FINISHED"})
        @DisplayName("голосование возможно только во время игры")
        void rejectsOutsideVotingPhases(Phase phase) {
            assertThat(handle(game(3).withPhase(phase), new StartVote("p1")))
                    .isEqualTo(new CommandResult.Rejected(RuleViolation.WRONG_PHASE));
        }
    }
}
