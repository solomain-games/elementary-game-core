package com.solomain.elementary.core.engine;

import com.solomain.elementary.core.error.RuleViolation;
import com.solomain.elementary.core.model.GameState;
import com.solomain.elementary.core.model.Phase;
import com.solomain.elementary.core.model.Player;
import com.solomain.elementary.core.model.PlayerStatus;
import com.solomain.elementary.core.model.Vote;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Правила голосования за переход к ответам (ТЗ, 4.7).
 *
 * <ul>
 *   <li>Голосовать могут только игроки со статусом «в игре»; отключённые и вышедшие не учитываются.</li>
 *   <li>Решение — большинство: больше половины тех, кто сейчас в игре.</li>
 *   <li>Состав пересчитывается при каждом изменении: если кто-то отключился, вышел или вернулся,
 *       голосование тут же перепроверяется.</li>
 *   <li>Голосование завершается, как только успех достигнут или стал невозможен,
 *       либо по истечении срока — тогда не проголосовавшие считаются «против».</li>
 *   <li>Ходы во время голосования продолжаются. Голос изменить нельзя.</li>
 * </ul>
 *
 * <p>Команды голосования обрабатываются только через {@link GameEngine#handle}: методы для них
 * видны лишь внутри пакета {@code engine}. Публичен только {@link #votesNeeded} — он нужен
 * представлению игрока.
 */
public final class VoteRules {

    /** Сколько длится голосование. */
    static final Duration VOTE_DURATION = Duration.ofSeconds(30);

    private VoteRules() {
    }

    /**
     * Сколько голосов «за» нужно для перехода к ответам: больше половины игроков в игре.
     */
    public static int votesNeeded(GameState state) {
        return eligibleVoters(state).size() / 2 + 1;
    }

    /** Начать голосование. Голос инициатора «за» учитывается сразу. */
    static CommandResult start(GameState state, String playerId, Instant now) {
        if (!isVotingPhase(state)) {
            return new CommandResult.Rejected(RuleViolation.WRONG_PHASE);
        }
        int playerIndex = state.indexOfPlayer(playerId);
        if (playerIndex == -1) {
            return new CommandResult.Rejected(RuleViolation.UNKNOWN_PLAYER);
        }
        if (state.players().get(playerIndex).status() != PlayerStatus.ACTIVE) {
            return new CommandResult.Rejected(RuleViolation.NOT_ELIGIBLE_TO_VOTE);
        }
        if (state.vote() != null) {
            return new CommandResult.Rejected(RuleViolation.VOTE_IN_PROGRESS);
        }

        Vote vote = new Vote(playerId, Map.of(playerId, true), now.plus(VOTE_DURATION));
        return new CommandResult.Accepted(resolve(state.withVote(vote), false));
    }

    /** Проголосовать в текущем голосовании. */
    static CommandResult cast(GameState state, String playerId, boolean inFavor) {
        if (!isVotingPhase(state)) {
            return new CommandResult.Rejected(RuleViolation.WRONG_PHASE);
        }
        Vote vote = state.vote();
        if (vote == null) {
            return new CommandResult.Rejected(RuleViolation.NO_VOTE);
        }
        int playerIndex = state.indexOfPlayer(playerId);
        if (playerIndex == -1) {
            return new CommandResult.Rejected(RuleViolation.UNKNOWN_PLAYER);
        }
        if (state.players().get(playerIndex).status() != PlayerStatus.ACTIVE) {
            return new CommandResult.Rejected(RuleViolation.NOT_ELIGIBLE_TO_VOTE);
        }
        if (vote.votes().containsKey(playerId)) {
            return new CommandResult.Rejected(RuleViolation.ALREADY_VOTED);
        }

        return new CommandResult.Accepted(resolve(state.withVote(vote.withVote(playerId, inFavor)), false));
    }

    /** Срок голосования истёк: не проголосовавшие считаются «против». */
    static CommandResult timeout(GameState state, Instant deadline, Instant now) {
        Vote vote = state.vote();
        if (vote == null || !vote.deadline().equals(deadline)) {
            return new CommandResult.Rejected(RuleViolation.STALE_TIMEOUT);
        }
        if (now.isBefore(vote.deadline())) {
            return new CommandResult.Rejected(RuleViolation.VOTE_NOT_EXPIRED);
        }
        return new CommandResult.Accepted(resolve(state, true));
    }

    /**
     * Перепроверить голосование после изменения состава (отключение, выход, возвращение).
     * Если голосования нет, состояние не меняется.
     */
    static GameState reevaluate(GameState state) {
        if (state.vote() == null || !isVotingPhase(state)) {
            return state;
        }
        return resolve(state, false);
    }

    /**
     * Подводит итог, если он уже ясен.
     *
     * @param expired истёк ли срок: тогда не проголосовавшие считаются «против»
     * @return состояние с пройденным голосованием (фаза {@code ANSWERING}), с проваленным
     *         (голосование снято, игра продолжается) или без изменений, если исход ещё не ясен
     */
    private static GameState resolve(GameState state, boolean expired) {
        Vote vote = state.vote();
        List<String> eligible = eligibleVoters(state);
        int needed = eligible.size() / 2 + 1;

        long inFavor = eligible.stream()
                .filter(id -> Boolean.TRUE.equals(vote.votes().get(id)))
                .count();
        long notVotedYet = expired ? 0 : eligible.stream()
                .filter(id -> !vote.votes().containsKey(id))
                .count();

        if (inFavor >= needed) {
            // Прошло: переходим к ответам. Участники ответов — те, кто в игре сейчас (ТЗ, 4.8).
            return state
                    .withVote(null)
                    .withPhase(Phase.ANSWERING)
                    .withTurnDeadline(null)
                    .withAnswerParticipants(Set.copyOf(eligible));
        }
        if (inFavor + notVotedYet < needed) {
            // Провалилось: даже если все оставшиеся скажут «за», большинства не будет.
            return state.withVote(null);
        }
        return state;
    }

    /** Идентификаторы игроков, которые сейчас в игре и могут голосовать. */
    private static List<String> eligibleVoters(GameState state) {
        return state.players().stream()
                .filter(player -> player.status() == PlayerStatus.ACTIVE)
                .map(Player::id)
                .toList();
    }

    private static boolean isVotingPhase(GameState state) {
        return state.phase() == Phase.PLAYING || state.phase() == Phase.NO_MOVES_LEFT;
    }
}
