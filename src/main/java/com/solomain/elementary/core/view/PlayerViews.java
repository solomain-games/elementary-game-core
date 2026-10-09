package com.solomain.elementary.core.view;

import com.solomain.elementary.core.engine.VoteRules;
import com.solomain.elementary.core.model.Card;
import com.solomain.elementary.core.model.CaseDefinition;
import com.solomain.elementary.core.model.GameState;
import com.solomain.elementary.core.model.Phase;
import com.solomain.elementary.core.model.Player;
import com.solomain.elementary.core.model.Question;
import com.solomain.elementary.core.model.Vote;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Строит {@link PlayerView} — состояние партии глазами конкретного игрока (ТЗ, 4.6, ADR 0005).
 *
 * <p>Главное правило: в представление попадают лица только тех карт, которые игрок имеет право
 * видеть, — своей руки и стола. От колоды, резерва, сброса и чужих рук берутся лишь размеры.
 * Всё скрытое раскрывается только в фазе {@link Phase#FINISHED}.
 */
public final class PlayerViews {

    private PlayerViews() {
    }

    /**
     * Представление партии для игрока.
     *
     * @param caseDefinition дело, которое играется в партии
     * @param state          состояние партии
     * @param viewerId       идентификатор игрока, для которого строится представление
     * @return представление, которое можно целиком отправить в браузер этого игрока
     * @throws IllegalArgumentException если игрока нет в партии или дело не соответствует партии
     */
    public static PlayerView of(CaseDefinition caseDefinition, GameState state, String viewerId) {
        Objects.requireNonNull(caseDefinition, "caseDefinition");
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(viewerId, "viewerId");
        if (!caseDefinition.id().equals(state.caseId())) {
            throw new IllegalArgumentException(
                    "case " + caseDefinition.id() + " does not match game case " + state.caseId());
        }

        int viewerIndex = state.indexOfPlayer(viewerId);
        if (viewerIndex == -1) {
            throw new IllegalArgumentException("there is no player in the game with id " + viewerId);
        }
        Player viewer = state.players().get(viewerIndex);

        List<PlayerSummary> players = state.players().stream()
                .map(player -> new PlayerSummary(player.id(), player.name(), player.status(), player.hand().size()))
                .toList();

        String currentPlayerId = state.phase() == Phase.PLAYING
                ? state.players().get(state.currentPlayerIndex()).id()
                : null;

        return new PlayerView(
                state.caseId(),
                state.phase(),
                viewerId,
                faces(caseDefinition, viewer.hand()),
                faces(caseDefinition, state.table()),
                players,
                currentPlayerId,
                state.deck().size() + state.reserve().size(),
                state.discard().size(),
                state.turnNumber(),
                state.turnDeadline(),
                voteView(state),
                state.phase() == Phase.FINISHED ? reveal(caseDefinition, state) : null);
    }

    /**
     * Текущее голосование; {@code null}, если его нет.
     */
    private static VoteView voteView(GameState state) {
        Vote vote = state.vote();
        if (vote == null) {
            return null;
        }
        return new VoteView(vote.initiatorId(), vote.votes(), vote.deadline(), VoteRules.votesNeeded(state));
    }

    /**
     * Раскрытие в финале (ТЗ, 4.8): сброс, карты «по делу», истинная картина, ответы и очки.
     */
    private static Reveal reveal(CaseDefinition caseDefinition, GameState state) {
        Set<Integer> relevantCards = caseDefinition.cards().stream()
                .filter(Card::relevant)
                .map(Card::number)
                .collect(Collectors.toSet());

        Map<String, String> correctAnswers = caseDefinition.questions().stream()
                .collect(Collectors.toMap(Question::id, Question::correctOptionId));

        return new Reveal(
                faces(caseDefinition, state.discard()),
                relevantCards,
                caseDefinition.solution(),
                correctAnswers,
                state.scores());
    }

    /**
     * Лица карт по их номерам, в том же порядке.
     */
    private static List<CardFace> faces(CaseDefinition caseDefinition, List<Integer> cardNumbers) {
        return cardNumbers.stream()
                .map(caseDefinition::card)
                .map(CardFace::of)
                .toList();
    }
}
