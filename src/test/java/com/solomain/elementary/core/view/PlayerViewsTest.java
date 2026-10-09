package com.solomain.elementary.core.view;

import com.solomain.elementary.core.TestCases;
import com.solomain.elementary.core.model.CaseDefinition;
import com.solomain.elementary.core.model.GameSettings;
import com.solomain.elementary.core.model.GameState;
import com.solomain.elementary.core.model.Phase;
import com.solomain.elementary.core.model.Player;
import com.solomain.elementary.core.model.PlayerStatus;
import com.solomain.elementary.core.model.ReserveCard;
import com.solomain.elementary.core.model.Vote;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("PlayerViews — представление партии для игрока")
class PlayerViewsTest {

    // Карты 1–12; нечётные относятся к делу (см. TestCases).
    private static final CaseDefinition CASE = TestCases.withCards(12);

    /**
     * Партия в разгаре:
     * стол [1, 6]; у p1 [2, 3], у p2 [4, 5], p3 вышел; колода [7, 8]; резерв [9 от p3]; сброс [10, 11].
     * Ходит p1. Карта 12 не лежит нигде: так заодно проверяется, что в представление не попадают карты дела «просто так».
     */
    private static GameState midGame(Phase phase) {
        var players = List.of(
                new Player("p1", "Анна", PlayerStatus.ACTIVE, List.of(2, 3)),
                new Player("p2", "Борис", PlayerStatus.DISCONNECTED, List.of(4, 5)),
                new Player("p3", "Вера", PlayerStatus.LEFT, List.of()));
        return new GameState(CASE.id(), new GameSettings("p1", null), phase,
                players, List.of(1, 6), List.of(7, 8), List.of(new ReserveCard(9, "p3")), List.of(10, 11),
                0, 5, null, null,
                Map.of(), Set.of(), Map.of());
    }

    @Nested
    @DisplayName("что видно во время игры")
    class Visible {

        @Test
        @DisplayName("своя рука — лица карт в том же порядке")
        void showsOwnHand() {
            var view = PlayerViews.of(CASE, midGame(Phase.PLAYING), "p1");

            assertThat(numbers(view.hand())).containsExactly(2, 3);
        }

        @Test
        @DisplayName("стол в порядке выкладки")
        void showsTable() {
            var view = PlayerViews.of(CASE, midGame(Phase.PLAYING), "p2");

            assertThat(numbers(view.table())).containsExactly(1, 6);
        }

        @Test
        @DisplayName("все игроки по порядку: имя, статус и число карт")
        void showsPlayerSummaries() {
            var view = PlayerViews.of(CASE, midGame(Phase.PLAYING), "p1");

            assertThat(view.players()).containsExactly(
                    new PlayerSummary("p1", "Анна", PlayerStatus.ACTIVE, 2),
                    new PlayerSummary("p2", "Борис", PlayerStatus.DISCONNECTED, 2),
                    new PlayerSummary("p3", "Вера", PlayerStatus.LEFT, 0));
        }

        @Test
        @DisplayName("в колоде учитывается и резерв")
        void countsReserveInDeckSize() {
            var view = PlayerViews.of(CASE, midGame(Phase.PLAYING), "p1");

            assertThat(view.deckSize()).isEqualTo(3);
        }

        @Test
        @DisplayName("видно число карт в сбросе")
        void showsDiscardSize() {
            var view = PlayerViews.of(CASE, midGame(Phase.PLAYING), "p1");

            assertThat(view.discardSize()).isEqualTo(2);
        }

        @Test
        @DisplayName("общие поля: дело, фаза, для кого, номер хода")
        void copiesCommonFields() {
            var view = PlayerViews.of(CASE, midGame(Phase.PLAYING), "p2");

            assertThat(view.caseId()).isEqualTo(CASE.id());
            assertThat(view.phase()).isEqualTo(Phase.PLAYING);
            assertThat(view.viewerId()).isEqualTo("p2");
            assertThat(view.turnNumber()).isEqualTo(5);
            assertThat(view.turnDeadline()).isNull();
        }

        @Test
        @DisplayName("во время игры виден текущий игрок")
        void showsCurrentPlayerWhilePlaying() {
            var view = PlayerViews.of(CASE, midGame(Phase.PLAYING), "p2");

            assertThat(view.currentPlayerId()).isEqualTo("p1");
        }

        @Test
        @DisplayName("вне фазы PLAYING текущего игрока нет")
        void hidesCurrentPlayerOutsidePlaying() {
            var view = PlayerViews.of(CASE, midGame(Phase.NO_MOVES_LEFT), "p2");

            assertThat(view.currentPlayerId()).isNull();
        }

        @Test
        @DisplayName("видно текущее голосование: кто как проголосовал и сколько нужно")
        void showsVote() {
            var vote = new Vote("p1", Map.of("p1", true), Instant.parse("2026-10-01T12:00:30Z"));
            var state = midGame(Phase.PLAYING).withVote(vote);

            var view = PlayerViews.of(CASE, state, "p2");

            // в игре только p1 (p2 отключён, p3 вышел) — нужен 1 голос
            assertThat(view.vote()).isEqualTo(new VoteView("p1", Map.of("p1", true), vote.deadline(), 1));
        }

        @Test
        @DisplayName("без голосования поле пустое")
        void hasNoVoteByDefault() {
            var view = PlayerViews.of(CASE, midGame(Phase.PLAYING), "p1");

            assertThat(view.vote()).isNull();
        }
    }

    @Nested
    @DisplayName("что скрыто до финала")
    class Hidden {

        @ParameterizedTest(name = "игрок {0}, фаза {1}")
        @CsvSource({
                "p1, PROLOGUE", "p1, PLAYING", "p1, NO_MOVES_LEFT", "p1, ANSWERING",
                "p2, PLAYING", "p2, ANSWERING",
                "p3, PLAYING"})
        @DisplayName("в представление попадают только своя рука и стол")
        void neverLeaksHiddenCards(String viewerId, Phase phase) {
            var state = midGame(phase);

            var view = PlayerViews.of(CASE, state, viewerId);

            var allowed = new ArrayList<Integer>(state.table());
            allowed.addAll(handOf(state, viewerId));
            assertThat(allVisibleNumbers(view)).containsExactlyInAnyOrderElementsOf(allowed);
        }

        @ParameterizedTest(name = "фаза {0}")
        @CsvSource({"PROLOGUE", "PLAYING", "NO_MOVES_LEFT", "ANSWERING"})
        @DisplayName("до финала нет раскрытия: ни сброса, ни ответов, ни истинной картины")
        void hasNoRevealBeforeFinish(Phase phase) {
            var view = PlayerViews.of(CASE, midGame(phase), "p1");

            assertThat(view.reveal()).isNull();
        }

        @Test
        @DisplayName("у лица карты нет признака «относится к делу» — его нельзя отправить даже случайно")
        void cardFacesHaveNoRelevantField() {
            assertThat(componentNames(CardFace.ImageFace.class)).doesNotContain("relevant");
            assertThat(componentNames(CardFace.TextFace.class)).doesNotContain("relevant");
        }
    }

    @Nested
    @DisplayName("что раскрывается в финале")
    class Revealed {

        private GameState finished() {
            return midGame(Phase.FINISHED).withScores(Map.of("p1", 1, "p2", 0));
        }

        @Test
        @DisplayName("содержимое сброса")
        void revealsDiscard() {
            var view = PlayerViews.of(CASE, finished(), "p1");

            assertThat(numbers(view.reveal().discard())).containsExactly(10, 11);
        }

        @Test
        @DisplayName("какие карты дела относились к делу — все, а не только видимые")
        void revealsRelevantCards() {
            var view = PlayerViews.of(CASE, finished(), "p1");

            assertThat(view.reveal().relevantCards()).containsExactlyInAnyOrder(1, 3, 5, 7, 9, 11);
        }

        @Test
        @DisplayName("истинную картину, правильные ответы и очки")
        void revealsSolutionAnswersAndScores() {
            var view = PlayerViews.of(CASE, finished(), "p2");

            assertThat(view.reveal().solution()).isEqualTo(CASE.solution());
            assertThat(view.reveal().correctAnswers()).isEqualTo(Map.of("culprit", "yes"));
            assertThat(view.reveal().scores()).isEqualTo(Map.of("p1", 1, "p2", 0));
        }
    }

    @Test
    @DisplayName("отклоняет запрос представления для игрока не из партии")
    void rejectsUnknownViewer() {
        var state = midGame(Phase.PLAYING);

        assertThatThrownBy(() -> PlayerViews.of(CASE, state, "stranger"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("stranger");
    }

    @Test
    @DisplayName("отклоняет дело, не соответствующее партии")
    void rejectsMismatchedCase() {
        var state = midGame(Phase.PLAYING);
        var otherCase = TestCases.withCards(20);

        assertThatThrownBy(() -> PlayerViews.of(otherCase, state, "p1"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(otherCase.id());
    }

    // --- помощники ---

    private static List<Integer> numbers(List<CardFace> faces) {
        return faces.stream().map(CardFace::number).toList();
    }

    private static List<Integer> handOf(GameState state, String playerId) {
        return state.players().stream()
                .filter(player -> player.id().equals(playerId))
                .findFirst()
                .orElseThrow()
                .hand();
    }

    /** Номера всех карт, лица которых есть в представлении: рука, стол и раскрытый сброс. */
    private static List<Integer> allVisibleNumbers(PlayerView view) {
        var all = new ArrayList<Integer>();
        all.addAll(numbers(view.hand()));
        all.addAll(numbers(view.table()));
        if (view.reveal() != null) {
            all.addAll(numbers(view.reveal().discard()));
        }
        return all;
    }

    private static List<String> componentNames(Class<? extends Record> recordClass) {
        return Arrays.stream(recordClass.getRecordComponents())
                .map(component -> component.getName())
                .toList();
    }
}
