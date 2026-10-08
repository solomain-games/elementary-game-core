package com.solomain.elementary.core.engine;

import com.solomain.elementary.core.TestCases;
import com.solomain.elementary.core.model.CaseDefinition;
import com.solomain.elementary.core.model.GameSettings;
import com.solomain.elementary.core.model.GameState;
import com.solomain.elementary.core.model.Phase;
import com.solomain.elementary.core.model.Player;
import com.solomain.elementary.core.model.PlayerStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GameEngine.start — подготовка партии")
class GameEngineStartTest {

    private static final GameSettings SETTINGS = new GameSettings("p1", null);
    private static final CaseDefinition CASE_40_CARDS = TestCases.withCards(40);

    // --- стол и колода ---

    @Test
    @DisplayName("выкладывает на стол только карту №1")
    void placesStartingCardOnTable() {
        var state = start(CASE_40_CARDS, 4, 42);

        assertThat(state.table()).containsExactly(1);
        assertThat(state.deck()).doesNotContain(1);
        assertThat(allHands(state)).doesNotContain(1);
    }

    @Test
    @DisplayName("находит карту №1, даже если в деле она не первая по списку")
    void findsStartingCardRegardlessOfOrder() {
        var reversed = new CaseDefinition(CASE_40_CARDS.id(), CASE_40_CARDS.cards().reversed(),
                CASE_40_CARDS.questions(), CASE_40_CARDS.solution());

        var state = start(reversed, 4, 42);

        assertThat(state.table()).containsExactly(1);
        assertThat(state.deck()).doesNotContain(1);
    }

    @ParameterizedTest(name = "{0} игроков")
    @ValueSource(ints = {1, 4, 8})
    @DisplayName("каждая карта оказывается ровно в одном месте: на столе, в руке или в колоде")
    void keepsEveryCardExactlyOnce(int playerCount) {
        var state = start(CASE_40_CARDS, playerCount, 42);

        var all = new ArrayList<Integer>();
        all.addAll(state.table());
        all.addAll(allHands(state));
        all.addAll(state.deck());

        var expected = IntStream.rangeClosed(1, 40).boxed().toList();
        assertThat(all).containsExactlyInAnyOrderElementsOf(expected); // заодно ловит дубли
    }

    @Test
    @DisplayName("перемешивает колоду")
    void shufflesDeck() {
        var state = start(CASE_40_CARDS, 1, 42);

        var sortedDeck = state.deck().stream().sorted().toList();
        assertThat(state.deck()).isNotEqualTo(sortedDeck);
    }

    // --- раздача ---

    @ParameterizedTest(name = "{0} игроков — по {1} карты")
    @CsvSource({"1, 3", "6, 3", "7, 2", "8, 2"})
    @DisplayName("раздаёт карты в зависимости от числа игроков")
    void dealsCardsByPlayerCount(int playerCount, int expectedHandSize) {
        var state = start(CASE_40_CARDS, playerCount, 42);

        assertThat(state.players())
                .allSatisfy(player -> assertThat(player.hand()).hasSize(expectedHandSize));
        assertThat(state.deck()).hasSize(39 - playerCount * expectedHandSize);
    }

    @Test
    @DisplayName("при нехватке карт раздаёт по кругу, пока колода не кончится")
    void dealsRoundRobinWhenDeckIsShort() {
        // кроме карты №1 ещё 3 карты на двоих: первому в порядке ходов 2, второму 1
        var state = start(TestCases.withCards(4), 2, 42);

        assertThat(state.players().get(0).hand()).hasSize(2);
        assertThat(state.players().get(1).hand()).hasSize(1);
        assertThat(state.deck()).isEmpty();
    }

    // --- игроки ---

    @Test
    @DisplayName("порядок ходов — перестановка исходных игроков, все в игре")
    void ordersPlayersAsPermutation() {
        var state = start(CASE_40_CARDS, 6, 42);

        assertThat(state.players()).extracting(Player::id)
                .containsExactlyInAnyOrder("p1", "p2", "p3", "p4", "p5", "p6");
        assertThat(state.players()).extracting(Player::status)
                .containsOnly(PlayerStatus.ACTIVE);
    }

    @Test
    @DisplayName("сохраняет имена игроков")
    void keepsPlayerNames() {
        var state = start(CASE_40_CARDS, 3, 42);

        assertThat(state.players())
                .allSatisfy(player -> assertThat(player.name()).isEqualTo("Игрок " + player.id().substring(1)));
    }

    // --- случайность ---

    @Test
    @DisplayName("одинаковый seed — одинаковая партия")
    void isDeterministicForSameSeed() {
        var first = start(CASE_40_CARDS, 5, 42);
        var second = start(CASE_40_CARDS, 5, 42);

        assertThat(first).isEqualTo(second); // records сравниваются по всем полям
    }

    @Test
    @DisplayName("разный seed — разная партия")
    void differsForDifferentSeeds() {
        var first = start(CASE_40_CARDS, 5, 1);
        var second = start(CASE_40_CARDS, 5, 2);

        assertThat(first).isNotEqualTo(second);
    }

    // --- начальные значения ---

    @Test
    @DisplayName("начинает с предыстории, хода первого игрока, без таймера и голосования")
    void setsInitialValues() {
        var state = start(CASE_40_CARDS, 4, 42);

        assertThat(state.caseId()).isEqualTo(CASE_40_CARDS.id());
        assertThat(state.settings()).isEqualTo(SETTINGS);
        assertThat(state.phase()).isEqualTo(Phase.PROLOGUE);
        assertThat(state.currentPlayerIndex()).isZero();
        assertThat(state.turnNumber()).isEqualTo(1);
        assertThat(state.turnDeadline()).isNull();
        assertThat(state.vote()).isNull();
        assertThat(state.reserve()).isEmpty();
        assertThat(state.discard()).isEmpty();
        assertThat(state.answers()).isEmpty();
        assertThat(state.answerParticipants()).isEmpty();
        assertThat(state.scores()).isEmpty();
    }

    // --- некорректный вход ---

    @ParameterizedTest(name = "{0} игроков")
    @ValueSource(ints = {0, 9})
    @DisplayName("отклоняет число игроков вне диапазона 1–8")
    void rejectsInvalidPlayerCount(int playerCount) {
        var players = players(playerCount);

        assertThatThrownBy(() -> GameEngine.start(CASE_40_CARDS, players, SETTINGS, new Random(42)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(String.valueOf(playerCount));
    }

    @Test
    @DisplayName("отклоняет повторяющийся идентификатор игрока")
    void rejectsDuplicatePlayerId() {
        var players = List.of(
                new PlayerInfo("p1", "Анна"),
                new PlayerInfo("p2", "Борис"),
                new PlayerInfo("p1", "Анна ещё раз"));

        assertThatThrownBy(() -> GameEngine.start(CASE_40_CARDS, players, SETTINGS, new Random(42)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("p1");
    }

    // --- помощники ---

    private static GameState start(CaseDefinition caseDef, int playerCount, long seed) {
        return GameEngine.start(caseDef, players(playerCount), SETTINGS, new Random(seed));
    }

    private static List<PlayerInfo> players(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(i -> new PlayerInfo("p" + i, "Игрок " + i))
                .toList();
    }

    private static List<Integer> allHands(GameState state) {
        return state.players().stream()
                .flatMap(player -> player.hand().stream())
                .toList();
    }
}
