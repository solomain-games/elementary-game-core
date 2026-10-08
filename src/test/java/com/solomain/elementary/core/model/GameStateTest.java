package com.solomain.elementary.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GameState")
class GameStateTest {

    /**
     * Типовое состояние: двое игроков, карта №1 на столе, ход первого игрока, без таймера и голосования.
     */
    private static GameState sampleState() {
        var players = List.of(
                new Player("p1", "Анна", PlayerStatus.ACTIVE, List.of(2, 3)),
                new Player("p2", "Борис", PlayerStatus.ACTIVE, List.of(4, 5)));
        return new GameState(
                "demo",
                new GameSettings("p1", null),
                Phase.PLAYING,
                players,
                List.of(1),
                List.of(6, 7, 8),
                List.of(),
                List.of(),
                0,
                1,
                null,
                null,
                Map.of(),
                Set.of(),
                Map.of());
    }

    @Test
    @DisplayName("создаётся без таймера и без голосования")
    void allowsNoDeadlineAndNoVote() {
        var state = sampleState();

        assertThat(state.turnDeadline()).isNull();
        assertThat(state.vote()).isNull();
    }

    @ParameterizedTest(name = "индекс {0}")
    @ValueSource(ints = {-1, 2})
    @DisplayName("отклоняет индекс текущего игрока вне списка игроков")
    void rejectsCurrentPlayerIndexOutOfBounds(int index) {
        var state = sampleState();

        assertThatThrownBy(() -> state.withCurrentPlayerIndex(index))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining(String.valueOf(index));
    }

    @Test
    @DisplayName("принимает индекс последнего игрока — граничное значение")
    void acceptsLastPlayerIndex() {
        var state = sampleState().withCurrentPlayerIndex(1);

        assertThat(state.currentPlayerIndex()).isEqualTo(1);
    }

    @Test
    @DisplayName("отклоняет партию без игроков")
    void rejectsEmptyPlayers() {
        var state = sampleState();

        assertThatThrownBy(() -> state.withPlayers(List.of()))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("отклоняет номер хода меньше 1")
    void rejectsTurnNumberBelowOne() {
        var state = sampleState();

        assertThatThrownBy(() -> state.withTurnNumber(0))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0");
    }

    @Test
    @DisplayName("не позволяет изменить колоду через deck()")
    void exposesUnmodifiableCollections() {
        var state = sampleState();

        assertThatThrownBy(() -> state.deck().add(9))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("делает глубокую копию ответов")
    void copiesAnswersDeeply() {
        var annaAnswers = new HashMap<String, String>();
        annaAnswers.put("culprit", "guest");
        var answers = new HashMap<String, Map<String, String>>();
        answers.put("p1", annaAnswers);

        var state = sampleState().withAnswers(answers);
        annaAnswers.put("motive", "debt");

        assertThat(state.answers().get("p1")).containsOnlyKeys("culprit");
    }

    // with-методы: проверяем, что меняется ровно одно поле, а исходный объект остаётся прежним.
    // Особенно важно для полей одного типа (table, deck, discard — все List<Integer>):
    // если в with-методе перепутать аргументы, компилятор этого не заметит.

    @Test
    @DisplayName("withDeck меняет только колоду")
    void withDeckReplacesOnlyDeck() {
        var state = sampleState();

        var changed = state.withDeck(List.of(9));

        assertThat(changed.deck()).containsExactly(9);
        assertThat(changed).usingRecursiveComparison().ignoringFields("deck").isEqualTo(state);
        assertThat(state.deck()).containsExactly(6, 7, 8);
    }

    @Test
    @DisplayName("withTable меняет только стол")
    void withTableReplacesOnlyTable() {
        var state = sampleState();

        var changed = state.withTable(List.of(1, 2));

        assertThat(changed.table()).containsExactly(1, 2);
        assertThat(changed).usingRecursiveComparison().ignoringFields("table").isEqualTo(state);
    }

    @Test
    @DisplayName("withDiscard меняет только сброс")
    void withDiscardReplacesOnlyDiscard() {
        var state = sampleState();

        var changed = state.withDiscard(List.of(3));

        assertThat(changed.discard()).containsExactly(3);
        assertThat(changed).usingRecursiveComparison().ignoringFields("discard").isEqualTo(state);
    }

    @Test
    @DisplayName("withReserve меняет только резерв")
    void withReserveReplacesOnlyReserve() {
        var state = sampleState();

        var changed = state.withReserve(List.of(new ReserveCard(4, "p2")));

        assertThat(changed.reserve()).containsExactly(new ReserveCard(4, "p2"));
        assertThat(changed).usingRecursiveComparison().ignoringFields("reserve").isEqualTo(state);
    }

    @Test
    @DisplayName("withPhase меняет только фазу")
    void withPhaseReplacesOnlyPhase() {
        var state = sampleState();

        var changed = state.withPhase(Phase.ANSWERING);

        assertThat(changed.phase()).isEqualTo(Phase.ANSWERING);
        assertThat(changed).usingRecursiveComparison().ignoringFields("phase").isEqualTo(state);
    }
}
