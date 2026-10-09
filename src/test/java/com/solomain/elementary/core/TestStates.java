package com.solomain.elementary.core;

import com.solomain.elementary.core.engine.Command;
import com.solomain.elementary.core.engine.CommandResult;
import com.solomain.elementary.core.engine.GameEngine;
import com.solomain.elementary.core.model.CaseDefinition;
import com.solomain.elementary.core.model.GameSettings;
import com.solomain.elementary.core.model.GameState;
import com.solomain.elementary.core.model.Phase;
import com.solomain.elementary.core.model.Player;
import com.solomain.elementary.core.model.PlayerStatus;
import com.solomain.elementary.core.model.ReserveCard;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.random.RandomGenerator;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Помощники для тестов движка: компактное создание состояний и разбор результатов.
 * Подключаются через {@code import static com.solomain.elementary.core.TestStates.*}.
 */
public final class TestStates {

    /** «Текущий момент» во всех тестах движка. */
    public static final Instant NOW = Instant.parse("2026-10-01T12:00:00Z");

    private TestStates() {
    }

    /** Дело по умолчанию для тестов движка: карты 1–30 и один вопрос. */
    public static final CaseDefinition DEFAULT_CASE = TestCases.withCards(30);

    /** Обрабатывает команду в момент {@link #NOW} с генератором с фиксированным seed. */
    public static CommandResult handle(GameState state, Command command) {
        return handle(DEFAULT_CASE, state, command);
    }

    /** То же для заданного дела. */
    public static CommandResult handle(CaseDefinition caseDefinition, GameState state, Command command) {
        return GameEngine.handle(caseDefinition, state, command, NOW, new Random(42));
    }

    /** То же в заданный момент с заданным генератором. */
    public static CommandResult handle(GameState state, Command command, Instant now, RandomGenerator random) {
        return GameEngine.handle(DEFAULT_CASE, state, command, now, random);
    }

    /** Состояние в фазе PLAYING без резерва: карта №1 на столе, сброс пуст, ход игрока с индексом {@code current}. */
    public static GameState playing(int current, List<Integer> deck, Player... players) {
        return playing(current, deck, List.of(), players);
    }

    /** Состояние в фазе PLAYING с резервом. */
    public static GameState playing(int current, List<Integer> deck, List<ReserveCard> reserve, Player... players) {
        return new GameState("test", new GameSettings("p1", null), Phase.PLAYING,
                List.of(players), List.of(1), deck, reserve, List.of(),
                current, 1, null, null,
                Map.of(), Set.of(), Map.of());
    }

    /** Игрок в игре с картами {@code hand}. */
    public static Player player(String id, Integer... hand) {
        return player(id, PlayerStatus.ACTIVE, hand);
    }

    /** Игрок с заданным статусом и картами {@code hand}. */
    public static Player player(String id, PlayerStatus status, Integer... hand) {
        return new Player(id, "Игрок " + id, status, List.of(hand));
    }

    /** Резерв из карт одного владельца. */
    public static List<ReserveCard> reserve(String ownerId, Integer... cardNumbers) {
        return List.of(cardNumbers).stream()
                .map(number -> new ReserveCard(number, ownerId))
                .toList();
    }

    /** Проверяет, что команда принята, и возвращает новое состояние. */
    public static GameState accepted(CommandResult result) {
        assertThat(result).isInstanceOf(CommandResult.Accepted.class);
        return ((CommandResult.Accepted) result).state();
    }

    /** Рука игрока по идентификатору. */
    public static List<Integer> hand(GameState state, String playerId) {
        return playerById(state, playerId).hand();
    }

    /** Игрок по идентификатору. */
    public static Player playerById(GameState state, String playerId) {
        return state.players().get(state.indexOfPlayer(playerId));
    }

    /** Все карты партии вне зависимости от места: стол, сброс, руки, колода, резерв. */
    public static List<Integer> allCards(GameState state) {
        var all = new ArrayList<Integer>();
        all.addAll(state.table());
        all.addAll(state.discard());
        state.players().forEach(player -> all.addAll(player.hand()));
        all.addAll(state.deck());
        state.reserve().forEach(card -> all.add(card.cardNumber()));
        return all;
    }
}
