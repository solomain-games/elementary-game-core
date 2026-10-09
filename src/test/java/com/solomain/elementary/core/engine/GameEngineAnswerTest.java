package com.solomain.elementary.core.engine;

import com.solomain.elementary.core.TestCases;
import com.solomain.elementary.core.error.RuleViolation;
import com.solomain.elementary.core.model.CaseDefinition;
import com.solomain.elementary.core.model.GameState;
import com.solomain.elementary.core.model.Phase;
import com.solomain.elementary.core.model.PlayerStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.solomain.elementary.core.TestStates.accepted;
import static com.solomain.elementary.core.TestStates.handle;
import static com.solomain.elementary.core.TestStates.player;
import static com.solomain.elementary.core.TestStates.playing;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GameEngine.handle — финал: ответы и очки (ТЗ, 4.8)")
class GameEngineAnswerTest {

    /** Три вопроса; правильно: q1 → a, q2 → b, q3 → c. */
    private static final CaseDefinition QUIZ = TestCases.quiz();

    private static final Map<String, String> ALL_CORRECT = Map.of("q1", "a", "q2", "b", "q3", "c");
    private static final Map<String, String> ONE_CORRECT = Map.of("q1", "a", "q2", "a", "q3", "a");
    private static final Map<String, String> NONE_CORRECT = Map.of("q1", "b", "q2", "a", "q3", "b");

    /** Фаза ответов; участники p1, p2, p3; p4 отключился до перехода и не участвует. Хозяин — p1. */
    private static GameState answering() {
        return playing(0, List.of(),
                player("p1"), player("p2"), player("p3"), player("p4", PlayerStatus.DISCONNECTED))
                .withPhase(Phase.ANSWERING)
                .withAnswerParticipants(Set.of("p1", "p2", "p3"));
    }

    private static GameState run(GameState state, Command... commands) {
        for (Command command : commands) {
            state = accepted(handle(QUIZ, state, command));
        }
        return state;
    }

    private static CommandResult.Rejected rejected(RuleViolation violation) {
        return new CommandResult.Rejected(violation);
    }

    @Nested
    @DisplayName("ответы")
    class Answers {

        @Test
        @DisplayName("ответы сохраняются, пока ответили не все — фаза не меняется")
        void storesAnswersAndWaits() {
            var after = run(answering(), new SubmitAnswers("p1", ALL_CORRECT));

            assertThat(after.phase()).isEqualTo(Phase.ANSWERING);
            assertThat(after.answers()).containsEntry("p1", ALL_CORRECT);
            assertThat(after.scores()).isEmpty();
        }

        @Test
        @DisplayName("когда ответили все участники — подсчёт очков и FINISHED")
        void finishesWhenEveryoneAnswered() {
            var after = run(answering(),
                    new SubmitAnswers("p1", ALL_CORRECT),
                    new SubmitAnswers("p2", ONE_CORRECT),
                    new SubmitAnswers("p3", NONE_CORRECT));

            assertThat(after.phase()).isEqualTo(Phase.FINISHED);
            assertThat(after.scores()).isEqualTo(Map.of("p1", 3, "p2", 1, "p3", 0));
        }

        @Test
        @DisplayName("не участник (отсутствовал при переходе) ответить не может")
        void rejectsNonParticipant() {
            assertThat(handle(QUIZ, answering(), new SubmitAnswers("p4", ALL_CORRECT)))
                    .isEqualTo(rejected(RuleViolation.NOT_ANSWER_PARTICIPANT));
        }

        @Test
        @DisplayName("ответить можно только один раз")
        void rejectsSecondSubmission() {
            var state = run(answering(), new SubmitAnswers("p1", NONE_CORRECT));

            assertThat(handle(QUIZ, state, new SubmitAnswers("p1", ALL_CORRECT)))
                    .isEqualTo(rejected(RuleViolation.ALREADY_ANSWERED));
        }

        @Test
        @DisplayName("нужно ответить на все вопросы")
        void rejectsMissingAnswer() {
            var partial = Map.of("q1", "a", "q2", "b");

            assertThat(handle(QUIZ, answering(), new SubmitAnswers("p1", partial)))
                    .isEqualTo(rejected(RuleViolation.INVALID_ANSWERS));
        }

        @Test
        @DisplayName("ответ на несуществующий вопрос отклоняется")
        void rejectsUnknownQuestion() {
            var withExtra = Map.of("q1", "a", "q2", "b", "q99", "a");

            assertThat(handle(QUIZ, answering(), new SubmitAnswers("p1", withExtra)))
                    .isEqualTo(rejected(RuleViolation.INVALID_ANSWERS));
        }

        @Test
        @DisplayName("несуществующий вариант отклоняется")
        void rejectsUnknownOption() {
            var badOption = Map.of("q1", "a", "q2", "b", "q3", "z");

            assertThat(handle(QUIZ, answering(), new SubmitAnswers("p1", badOption)))
                    .isEqualTo(rejected(RuleViolation.INVALID_ANSWERS));
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(value = Phase.class, names = {"PROLOGUE", "PLAYING", "NO_MOVES_LEFT", "FINISHED"})
        @DisplayName("отвечать можно только в фазе ANSWERING")
        void rejectsOutsideAnswering(Phase phase) {
            assertThat(handle(QUIZ, answering().withPhase(phase), new SubmitAnswers("p1", ALL_CORRECT)))
                    .isEqualTo(rejected(RuleViolation.WRONG_PHASE));
        }
    }

    @Nested
    @DisplayName("кнопка хозяина «Показать результаты»")
    class Force {

        @Test
        @DisplayName("завершает ответы; не ответившие получают 0")
        void finishesWithoutWaiting() {
            var after = run(answering(),
                    new SubmitAnswers("p2", ALL_CORRECT),
                    new ForceResults("p1"));

            assertThat(after.phase()).isEqualTo(Phase.FINISHED);
            assertThat(after.scores()).isEqualTo(Map.of("p1", 0, "p2", 3, "p3", 0));
        }

        @Test
        @DisplayName("доступна только хозяину")
        void onlyHost() {
            assertThat(handle(QUIZ, answering(), new ForceResults("p2")))
                    .isEqualTo(rejected(RuleViolation.NOT_HOST));
        }

        @Test
        @DisplayName("только в фазе ANSWERING")
        void onlyWhileAnswering() {
            assertThat(handle(QUIZ, answering().withPhase(Phase.PLAYING), new ForceResults("p1")))
                    .isEqualTo(rejected(RuleViolation.WRONG_PHASE));
        }
    }

    @Nested
    @DisplayName("изменение состава во время ответов")
    class Presence {

        @Test
        @DisplayName("вернувшийся до показа результатов становится участником и может ответить")
        void returnedPlayerCanAnswer() {
            var returned = run(answering(), new PlayerReturned("p4"));
            assertThat(returned.answerParticipants()).contains("p4");

            var after = run(returned, new SubmitAnswers("p4", ALL_CORRECT));

            assertThat(after.answers()).containsKey("p4");
        }

        @Test
        @DisplayName("вышедший до перехода к ответам тоже может ответить, если вернулся до итогов")
        void returnedFromLeftCanAnswer() {
            var state = answering().withPlayers(List.of(
                    player("p1"), player("p2"), player("p3"), player("p4", PlayerStatus.LEFT)));

            var returned = run(state, new PlayerReturned("p4"));
            var after = run(returned, new SubmitAnswers("p4", ALL_CORRECT));

            assertThat(returned.answerParticipants()).contains("p4");
            assertThat(after.answers()).containsKey("p4");
        }

        @Test
        @DisplayName("вышедших не ждём: если остальные уже ответили — сразу итоги")
        void doesNotWaitForLeftPlayers() {
            var twoAnswered = run(answering(),
                    new SubmitAnswers("p1", ALL_CORRECT),
                    new SubmitAnswers("p2", ONE_CORRECT));
            assertThat(twoAnswered.phase()).isEqualTo(Phase.ANSWERING);

            var after = run(twoAnswered, new PlayerLeft("p3"));

            assertThat(after.phase()).isEqualTo(Phase.FINISHED);
            assertThat(after.scores()).isEqualTo(Map.of("p1", 3, "p2", 1, "p3", 0));
        }

        @Test
        @DisplayName("отключившегося участника ждём: он может вернуться")
        void waitsForDisconnectedParticipant() {
            var after = run(answering(),
                    new SubmitAnswers("p1", ALL_CORRECT),
                    new SubmitAnswers("p2", ONE_CORRECT),
                    new PlayerDisconnected("p3"));

            assertThat(after.phase()).isEqualTo(Phase.ANSWERING);
        }
    }

    @Test
    @DisplayName("полный путь: голосование → ответы → итоги")
    void fullPathFromVoteToResults() {
        var state = playing(0, List.of(20), player("p1", 5), player("p2", 6));

        var after = run(state,
                new StartVote("p1"),
                new CastVote("p2", true),
                new SubmitAnswers("p1", ALL_CORRECT),
                new SubmitAnswers("p2", NONE_CORRECT));

        assertThat(after.phase()).isEqualTo(Phase.FINISHED);
        assertThat(after.scores()).isEqualTo(Map.of("p1", 3, "p2", 0));
    }
}
