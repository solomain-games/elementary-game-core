package com.solomain.elementary.core.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Состояние партии. Хранится в Redis и заменяется новым объектом после каждого действия.
 *
 * <p>Карты обозначаются номерами; сами карты лежат в {@link CaseDefinition}.
 *
 * @param caseId             идентификатор дела
 * @param settings           настройки партии
 * @param phase              текущая фаза
 * @param players            игроки в порядке ходов, вместе с руками
 * @param table              карты на столе в порядке выкладки
 * @param deck               основная колода; первый элемент — верхняя карта
 * @param reserve            карты отсутствующих игроков с указанием владельца; добираются после основной колоды
 * @param discard            сброс в порядке сброса
 * @param currentPlayerIndex индекс игрока в {@code players}, который сейчас ходит
 * @param turnNumber         номер хода с начала партии; защищает от устаревших сигналов таймера
 * @param turnDeadline       момент окончания текущего хода; {@code null}, если таймера нет
 * @param vote               текущее голосование за переход к ответам; {@code null}, если голосования нет
 * @param answers            ответы игроков: id игрока → (id вопроса → id выбранного варианта)
 * @param answerParticipants игроки, которые участвуют в ответах
 * @param scores             очки игроков; заполняются при переходе в {@link Phase#FINISHED}
 */
public record GameState(String caseId, GameSettings settings, Phase phase, List<Player> players, List<Integer> table,
                        List<Integer> deck, List<ReserveCard> reserve, List<Integer> discard, int currentPlayerIndex,
                        int turnNumber, Instant turnDeadline, Vote vote, Map<String, Map<String, String>> answers,
                        Set<String> answerParticipants, Map<String, Integer> scores) {
    public GameState {
        Objects.requireNonNull(caseId, "caseId");
        Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(phase, "phase");
        players = List.copyOf(players);
        table = List.copyOf(table);
        deck = List.copyOf(deck);
        reserve = List.copyOf(reserve);
        discard = List.copyOf(discard);

        if (currentPlayerIndex < 0 || currentPlayerIndex >= players.size()) {
            String message = String.format("Player index out of bounds (size: %s), got %s",
                    players.size(), currentPlayerIndex);
            throw new IllegalArgumentException(message);
        }

        if (turnNumber < 1) {
            throw new IllegalArgumentException("Turn number must be >= 1, got " + turnNumber);
        }

        answers = answers.entrySet().stream()
                .collect(Collectors.toUnmodifiableMap(
                        Map.Entry::getKey,
                        e -> Map.copyOf(e.getValue())));
        answerParticipants = Set.copyOf(answerParticipants);
        scores = Map.copyOf(scores);
    }

    // «Изменение» неизменяемого состояния: создаётся копия, в которой заменено одно поле.

    /** Копия состояния с новым значением {@code phase}. */
    public GameState withPhase(Phase newPhase) {
        return new GameState(caseId, settings, newPhase,
                players, table, deck, reserve, discard,
                currentPlayerIndex, turnNumber, turnDeadline, vote,
                answers, answerParticipants, scores);
    }

    /** Копия состояния с новым значением {@code players}. */
    public GameState withPlayers(List<Player> newPlayers) {
        return new GameState(caseId, settings, phase,
                newPlayers, table, deck, reserve, discard,
                currentPlayerIndex, turnNumber, turnDeadline, vote,
                answers, answerParticipants, scores);
    }

    /** Копия состояния с новым значением {@code table}. */
    public GameState withTable(List<Integer> newTable) {
        return new GameState(caseId, settings, phase,
                players, newTable, deck, reserve, discard,
                currentPlayerIndex, turnNumber, turnDeadline, vote,
                answers, answerParticipants, scores);
    }

    /** Копия состояния с новым значением {@code deck}. */
    public GameState withDeck(List<Integer> newDeck) {
        return new GameState(caseId, settings, phase,
                players, table, newDeck, reserve, discard,
                currentPlayerIndex, turnNumber, turnDeadline, vote,
                answers, answerParticipants, scores);
    }

    /** Копия состояния с новым значением {@code reserve}. */
    public GameState withReserve(List<ReserveCard> newReserve) {
        return new GameState(caseId, settings, phase,
                players, table, deck, newReserve, discard,
                currentPlayerIndex, turnNumber, turnDeadline, vote,
                answers, answerParticipants, scores);
    }

    /** Копия состояния с новым значением {@code discard}. */
    public GameState withDiscard(List<Integer> newDiscard) {
        return new GameState(caseId, settings, phase,
                players, table, deck, reserve, newDiscard,
                currentPlayerIndex, turnNumber, turnDeadline, vote,
                answers, answerParticipants, scores);
    }

    /** Копия состояния с новым значением {@code currentPlayerIndex}. */
    public GameState withCurrentPlayerIndex(int newCurrentPlayerIndex) {
        return new GameState(caseId, settings, phase,
                players, table, deck, reserve, discard,
                newCurrentPlayerIndex, turnNumber, turnDeadline, vote,
                answers, answerParticipants, scores);
    }

    /** Копия состояния с новым значением {@code turnNumber}. */
    public GameState withTurnNumber(int newTurnNumber) {
        return new GameState(caseId, settings, phase,
                players, table, deck, reserve, discard,
                currentPlayerIndex, newTurnNumber, turnDeadline, vote,
                answers, answerParticipants, scores);
    }

    /** Копия состояния с новым значением {@code turnDeadline}. */
    public GameState withTurnDeadline(Instant newTurnDeadline) {
        return new GameState(caseId, settings, phase,
                players, table, deck, reserve, discard,
                currentPlayerIndex, turnNumber, newTurnDeadline, vote,
                answers, answerParticipants, scores);
    }

    /** Копия состояния с новым значением {@code vote}. */
    public GameState withVote(Vote newVote) {
        return new GameState(caseId, settings, phase,
                players, table, deck, reserve, discard,
                currentPlayerIndex, turnNumber, turnDeadline, newVote,
                answers, answerParticipants, scores);
    }

    /** Копия состояния с новым значением {@code answers}. */
    public GameState withAnswers(Map<String, Map<String, String>> newAnswers) {
        return new GameState(caseId, settings, phase,
                players, table, deck, reserve, discard,
                currentPlayerIndex, turnNumber, turnDeadline, vote,
                newAnswers, answerParticipants, scores);
    }

    /** Копия состояния с новым значением {@code answerParticipants}. */
    public GameState withAnswerParticipants(Set<String> newAnswerParticipants) {
        return new GameState(caseId, settings, phase,
                players, table, deck, reserve, discard,
                currentPlayerIndex, turnNumber, turnDeadline, vote,
                answers, newAnswerParticipants, scores);
    }

    /** Копия состояния с новым значением {@code scores}. */
    public GameState withScores(Map<String, Integer> newScores) {
        return new GameState(caseId, settings, phase,
                players, table, deck, reserve, discard,
                currentPlayerIndex, turnNumber, turnDeadline, vote,
                answers, answerParticipants, newScores);
    }
}
