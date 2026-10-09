package com.solomain.elementary.core;

import com.solomain.elementary.core.engine.BeginPlay;
import com.solomain.elementary.core.engine.CastVote;
import com.solomain.elementary.core.engine.Command;
import com.solomain.elementary.core.engine.CommandResult;
import com.solomain.elementary.core.engine.DiscardCard;
import com.solomain.elementary.core.engine.GameEngine;
import com.solomain.elementary.core.engine.PlayCard;
import com.solomain.elementary.core.engine.PlayerDisconnected;
import com.solomain.elementary.core.engine.PlayerInfo;
import com.solomain.elementary.core.engine.PlayerLeft;
import com.solomain.elementary.core.engine.PlayerReturned;
import com.solomain.elementary.core.engine.StartVote;
import com.solomain.elementary.core.engine.SubmitAnswers;
import com.solomain.elementary.core.model.CaseDefinition;
import com.solomain.elementary.core.model.GameSettings;
import com.solomain.elementary.core.model.GameState;
import com.solomain.elementary.core.model.Phase;
import com.solomain.elementary.core.model.Player;
import com.solomain.elementary.core.model.PlayerStatus;
import com.solomain.elementary.core.view.PlayerViews;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.solomain.elementary.core.TestStates.NOW;
import static com.solomain.elementary.core.TestStates.allCards;
import static com.solomain.elementary.core.TestStates.playerById;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Сквозной сценарий: партия на 4 игрока от раздачи до итогов.
 *
 * <p>После каждой команды проверяются инварианты, которые должны выполняться всегда:
 * ни одна карта не потерялась и не задвоилась, состояние сохраняется в JSON и восстанавливается
 * без потерь, представление строится для каждого игрока.
 */
@DisplayName("Сценарий целой партии на 4 игрока")
class GameScenarioTest {

    private static final CaseDefinition CASE = TestCases.quiz(); // карты 1–30, три вопроса
    private static final List<Integer> ALL_CARDS = IntStream.rangeClosed(1, 30).boxed().toList();
    private static final JsonMapper JSON = JsonMapper.builder().build();

    private final Random random = new Random(2026);
    private GameState state;

    @Test
    @DisplayName("раздача → ходы → отключение и возвращение → выход и возвращение → голосование → ответы → итоги")
    void playsWholeGame() {
        // --- подготовка и предыстория ---
        state = GameEngine.start(CASE,
                List.of(new PlayerInfo("p1", "Анна"), new PlayerInfo("p2", "Борис"),
                        new PlayerInfo("p3", "Вера"), new PlayerInfo("p4", "Глеб")),
                new GameSettings("p1", null), random);
        checkInvariants();
        assertThat(state.phase()).isEqualTo(Phase.PROLOGUE);

        apply(new BeginPlay());
        assertThat(state.phase()).isEqualTo(Phase.PLAYING);

        // --- несколько обычных ходов ---
        takeTurns(4);

        // --- отключение и возвращение: карты остаются в руке ---
        String waiting = playerAfterCurrent(2);
        List<Integer> handBefore = playerById(state, waiting).hand();
        apply(new PlayerDisconnected(waiting));
        takeTurns(1);
        apply(new PlayerReturned(waiting));
        assertThat(playerById(state, waiting).status()).isEqualTo(PlayerStatus.ACTIVE);
        assertThat(playerById(state, waiting).hand()).isEqualTo(handBefore);

        // --- выход: карты уходят в резерв; позже игрок возвращается за оставшимися ---
        String leaver = playerAfterCurrent(1);
        int leaverCards = playerById(state, leaver).hand().size();
        apply(new PlayerLeft(leaver));
        assertThat(state.reserve()).hasSizeGreaterThanOrEqualTo(leaverCards);
        takeTurns(3);
        apply(new PlayerReturned(leaver));
        assertThat(playerById(state, leaver).status()).isEqualTo(PlayerStatus.ACTIVE);

        // --- ходим, пока не кончатся карты ---
        int guard = 0;
        while (state.phase() == Phase.PLAYING) {
            takeTurns(1);
            assertThat(++guard).as("партия должна закончиться за конечное число ходов").isLessThan(100);
        }
        assertThat(state.phase()).isEqualTo(Phase.NO_MOVES_LEFT);
        assertThat(state.deck()).isEmpty();
        assertThat(state.reserve()).isEmpty();
        assertThat(state.players()).allSatisfy(player -> assertThat(player.hand()).isEmpty());
        assertThat(concat(state.table(), state.discard())).containsExactlyInAnyOrderElementsOf(ALL_CARDS);

        // --- голосование: все за ---
        apply(new StartVote("p1"));
        apply(new CastVote("p2", true));
        apply(new CastVote("p3", true));
        assertThat(state.phase()).isEqualTo(Phase.ANSWERING);
        assertThat(state.answerParticipants()).containsExactlyInAnyOrder("p1", "p2", "p3", "p4");

        // --- ответы ---
        apply(new SubmitAnswers("p1", Map.of("q1", "a", "q2", "b", "q3", "c"))); // 3 верных
        apply(new SubmitAnswers("p2", Map.of("q1", "a", "q2", "b", "q3", "a"))); // 2 верных
        apply(new SubmitAnswers("p3", Map.of("q1", "a", "q2", "a", "q3", "a"))); // 1 верный
        assertThat(state.phase()).isEqualTo(Phase.ANSWERING);
        apply(new SubmitAnswers("p4", Map.of("q1", "b", "q2", "a", "q3", "b"))); // 0 верных

        // --- итоги ---
        assertThat(state.phase()).isEqualTo(Phase.FINISHED);
        assertThat(state.scores()).isEqualTo(Map.of("p1", 3, "p2", 2, "p3", 1, "p4", 0));
        var view = PlayerViews.of(CASE, state, "p4");
        assertThat(view.reveal()).isNotNull();
        assertThat(view.reveal().discard()).hasSize(state.discard().size());
    }

    // --- шаги сценария ---

    /** Выполняет команду (она должна быть принята) и проверяет инварианты. */
    private void apply(Command command) {
        var result = GameEngine.handle(CASE, state, command, NOW, random);
        assertThat(result).as("команда %s", command).isInstanceOf(CommandResult.Accepted.class);
        state = ((CommandResult.Accepted) result).state();
        checkInvariants();
    }

    /** Текущий игрок делает {@code count} ходов подряд: чётные ходы — выложить, нечётные — сбросить первую карту. */
    private void takeTurns(int count) {
        for (int i = 0; i < count && state.phase() == Phase.PLAYING; i++) {
            Player current = state.players().get(state.currentPlayerIndex());
            int card = current.hand().getFirst();
            apply(state.turnNumber() % 2 == 0
                    ? new PlayCard(current.id(), card)
                    : new DiscardCard(current.id(), card));
        }
    }

    /** Игрок через {@code offset} мест после текущего. */
    private String playerAfterCurrent(int offset) {
        int index = (state.currentPlayerIndex() + offset) % state.players().size();
        return state.players().get(index).id();
    }

    /** Инварианты, которые обязаны выполняться после любой команды. */
    private void checkInvariants() {
        assertThat(allCards(state)).as("каждая карта ровно в одном месте").containsExactlyInAnyOrderElementsOf(ALL_CARDS);

        var restored = JSON.readValue(JSON.writeValueAsString(state), GameState.class);
        assertThat(restored).as("состояние переживает JSON").isEqualTo(state);

        state.players().forEach(player -> PlayerViews.of(CASE, state, player.id()));
    }

    private static List<Integer> concat(List<Integer> first, List<Integer> second) {
        return Stream.concat(first.stream(), second.stream()).toList();
    }
}
