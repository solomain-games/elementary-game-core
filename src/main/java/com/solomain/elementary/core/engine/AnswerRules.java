package com.solomain.elementary.core.engine;

import com.solomain.elementary.core.error.RuleViolation;
import com.solomain.elementary.core.model.AnswerOption;
import com.solomain.elementary.core.model.CaseDefinition;
import com.solomain.elementary.core.model.GameState;
import com.solomain.elementary.core.model.Phase;
import com.solomain.elementary.core.model.Player;
import com.solomain.elementary.core.model.PlayerStatus;
import com.solomain.elementary.core.model.Question;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Правила финала: ответы на вопросы и подсчёт очков (ТЗ, 4.8).
 *
 * <ul>
 *   <li>Отвечают участники — игроки, которые были в игре в момент перехода к ответам.
 *       Вернувшийся до показа результатов тоже становится участником.</li>
 *   <li>Каждый отправляет ответы на все вопросы сразу, один раз.</li>
 *   <li>Результаты подводятся, когда ответили все участники, кроме вышедших,
 *       или когда хозяин нажал «Показать результаты».</li>
 *   <li>Каждый правильный ответ — {@value #POINTS_PER_QUESTION} очко; не ответивший получает 0.</li>
 * </ul>
 */
final class AnswerRules {

    /** Очков за каждый правильный ответ (все вопросы стоят одинаково). */
    static final int POINTS_PER_QUESTION = 1;

    private AnswerRules() {
    }

    /** Игрок отправляет ответы на все вопросы. */
    static CommandResult submit(CaseDefinition caseDefinition, GameState state, String playerId,
                                Map<String, String> answers) {
        if (state.phase() != Phase.ANSWERING) {
            return new CommandResult.Rejected(RuleViolation.WRONG_PHASE);
        }
        if (state.indexOfPlayer(playerId) == -1) {
            return new CommandResult.Rejected(RuleViolation.UNKNOWN_PLAYER);
        }
        if (!state.answerParticipants().contains(playerId)) {
            return new CommandResult.Rejected(RuleViolation.NOT_ANSWER_PARTICIPANT);
        }
        if (state.answers().containsKey(playerId)) {
            return new CommandResult.Rejected(RuleViolation.ALREADY_ANSWERED);
        }
        if (!isComplete(caseDefinition, answers)) {
            return new CommandResult.Rejected(RuleViolation.INVALID_ANSWERS);
        }

        Map<String, Map<String, String>> allAnswers = new HashMap<>(state.answers());
        allAnswers.put(playerId, answers);
        return new CommandResult.Accepted(finishIfEveryoneAnswered(caseDefinition, state.withAnswers(allAnswers)));
    }

    /** Хозяин завершает ответы, не дожидаясь всех. */
    static CommandResult forceResults(CaseDefinition caseDefinition, GameState state, String playerId) {
        if (state.phase() != Phase.ANSWERING) {
            return new CommandResult.Rejected(RuleViolation.WRONG_PHASE);
        }
        if (state.indexOfPlayer(playerId) == -1) {
            return new CommandResult.Rejected(RuleViolation.UNKNOWN_PLAYER);
        }
        if (!state.settings().hostId().equals(playerId)) {
            return new CommandResult.Rejected(RuleViolation.NOT_HOST);
        }
        return new CommandResult.Accepted(finish(caseDefinition, state));
    }

    /**
     * Перепроверка после изменения состава: вернувшиеся становятся участниками,
     * а если ждать больше некого (остальные вышли) — результаты подводятся.
     */
    static GameState reevaluate(CaseDefinition caseDefinition, GameState state) {
        if (state.phase() != Phase.ANSWERING) {
            return state;
        }
        Set<String> participants = new HashSet<>(state.answerParticipants());
        state.players().stream()
                .filter(player -> player.status() == PlayerStatus.ACTIVE)
                .forEach(player -> participants.add(player.id()));
        return finishIfEveryoneAnswered(caseDefinition, state.withAnswerParticipants(participants));
    }

    /**
     * Ответы полные и корректные: ровно по одному на каждый вопрос дела, и каждый — существующий вариант.
     */
    private static boolean isComplete(CaseDefinition caseDefinition, Map<String, String> answers) {
        if (answers.size() != caseDefinition.questions().size()) {
            return false;
        }
        for (Question question : caseDefinition.questions()) {
            String chosen = answers.get(question.id());
            boolean optionExists = question.options().stream()
                    .map(AnswerOption::id)
                    .anyMatch(id -> id.equals(chosen));
            if (!optionExists) {
                return false;
            }
        }
        return true;
    }

    /** Подводит итоги, если ответили все участники, кроме вышедших. */
    private static GameState finishIfEveryoneAnswered(CaseDefinition caseDefinition, GameState state) {
        boolean someoneToWaitFor = state.answerParticipants().stream()
                .filter(id -> !state.answers().containsKey(id))
                .anyMatch(id -> statusOf(state, id) != PlayerStatus.LEFT);
        return someoneToWaitFor ? state : finish(caseDefinition, state);
    }

    /** Подсчёт очков всех участников и переход в {@link Phase#FINISHED}. */
    private static GameState finish(CaseDefinition caseDefinition, GameState state) {
        Map<String, Integer> scores = state.answerParticipants().stream()
                .collect(Collectors.toMap(
                        id -> id,
                        id -> score(caseDefinition, state.answers().getOrDefault(id, Map.of()))));
        return state
                .withScores(scores)
                .withPhase(Phase.FINISHED)
                .withVote(null)
                .withTurnDeadline(null);
    }

    /** Очки за ответы: число правильных, умноженное на {@link #POINTS_PER_QUESTION}. */
    private static int score(CaseDefinition caseDefinition, Map<String, String> answers) {
        int correct = 0;
        for (Question question : caseDefinition.questions()) {
            if (question.correctOptionId().equals(answers.get(question.id()))) {
                correct++;
            }
        }
        return correct * POINTS_PER_QUESTION;
    }

    private static PlayerStatus statusOf(GameState state, String playerId) {
        int index = state.indexOfPlayer(playerId);
        return index == -1 ? PlayerStatus.LEFT : state.players().get(index).status();
    }
}
