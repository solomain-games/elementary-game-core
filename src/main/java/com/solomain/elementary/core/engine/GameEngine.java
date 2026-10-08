package com.solomain.elementary.core.engine;

import com.solomain.elementary.core.error.RuleViolation;
import com.solomain.elementary.core.model.Card;
import com.solomain.elementary.core.model.CaseDefinition;
import com.solomain.elementary.core.model.GameSettings;
import com.solomain.elementary.core.model.GameState;
import com.solomain.elementary.core.model.Phase;
import com.solomain.elementary.core.model.Player;
import com.solomain.elementary.core.model.PlayerStatus;
import com.solomain.elementary.core.model.ReserveCard;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.random.RandomGenerator;
import java.util.stream.Collectors;

/**
 * Движок правил игры.
 *
 * <p>Все методы — чистые функции: принимают дело и текущее состояние, возвращают новое состояние
 * и ничего не меняют снаружи. Случайность передаётся параметром, поэтому при одинаковом генераторе
 * результат всегда одинаковый.
 */
public final class GameEngine {
    private GameEngine() {
        // только статические методы, экземпляры не нужны
    }

    /**
     * Готовит партию к началу (ТЗ, 4.1).
     *
     * <ol>
     *   <li>Карта №1 выкладывается на стол.</li>
     *   <li>Остальные карты перемешиваются и образуют колоду.</li>
     *   <li>Порядок ходов определяется случайно и до конца партии не меняется.</li>
     *   <li>Карты раздаются по кругу, по одной каждому игроку, сверху колоды —
     *       до {@link #cardsPerPlayer(int) нужного числа} или пока колода не кончится.</li>
     * </ol>
     *
     * <p>Партия начинается в фазе {@link Phase#PROLOGUE}: ход первого игрока в новом порядке,
     * номер хода 1, таймер не запущен.
     *
     * @param caseDefinition дело
     * @param players        игроки в порядке входа в комнату, от 1 до 8, без повторов {@code id}
     * @param settings       настройки партии
     * @param random         генератор случайных чисел; в тестах — с фиксированным seed
     * @return начальное состояние партии
     * @throws IllegalArgumentException если игроков меньше 1 или больше 8 либо их {@code id} повторяются
     */
    public static GameState start(CaseDefinition caseDefinition,
                                  List<PlayerInfo> players,
                                  GameSettings settings,
                                  RandomGenerator random) {
        Objects.requireNonNull(caseDefinition, "caseDefinition");
        Objects.requireNonNull(players, "players");
        Objects.requireNonNull(settings, "settings");
        Objects.requireNonNull(random, "random");

        if (players.isEmpty() || players.size() > 8) {
            throw new IllegalArgumentException("Game can have 1 to 8 players max, got " + players.size());
        }

        List<Player> playerList = new ArrayList<>();
        Set<String> playerIds = new HashSet<>();
        for (PlayerInfo playerInfo : players) {
            if (!playerIds.add(playerInfo.id())) {
                throw new IllegalArgumentException("Duplicate player id " + playerInfo.id());
            }
            playerList.add(new Player(playerInfo.id(), playerInfo.name(), PlayerStatus.ACTIVE, List.of()));
        }
        Collections.shuffle(playerList, random);

        List<Integer> deck = caseDefinition.cards().stream()
                .map(Card::number)
                .filter(number -> number != 1)
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(deck, random);

        // Раздача по кругу: по одной карте каждому, пока у всех не будет perPlayer карт или не кончится колода
        int perPlayer = cardsPerPlayer(playerList.size());
        List<List<Integer>> hands = new ArrayList<>();
        for (int i = 0; i < playerList.size(); i++) {
            hands.add(new ArrayList<>());
        }
        for (int round = 0; round < perPlayer && !deck.isEmpty(); round++) {
            for (List<Integer> hand : hands) {
                if (deck.isEmpty()) {
                    break;
                }
                hand.add(deck.removeFirst()); // верх колоды — начало списка
            }
        }

        List<Player> dealtPlayers = new ArrayList<>();
        for (int i = 0; i < playerList.size(); i++) {
            dealtPlayers.add(playerList.get(i).withHand(hands.get(i)));
        }

        return new GameState(
                caseDefinition.id(),
                settings,
                Phase.PROLOGUE,
                dealtPlayers,
                List.of(1),
                deck,
                List.of(),
                List.of(),
                0,
                1,
                null,
                null,
                Map.of(),
                Set.of(),
                Map.of()
        );
    }

    /**
     * Сколько карт раздаётся каждому игроку (ТЗ, 4.1, п. 6).
     *
     * <ul>
     *   <li>1–6 игроков — по 3 карты;</li>
     *   <li>7–8 игроков — по 2 карты.</li>
     * </ul>
     *
     * @param playerCount число игроков
     * @return число карт на руку
     */
    private static int cardsPerPlayer(int playerCount) {
        return playerCount < 7 ? 3 : 2;
    }

    /**
     * Обрабатывает команду и возвращает результат: новое состояние или нарушение правил.
     *
     * <p>Исходное состояние не меняется. Если команда отклонена, вызывающему коду достаточно
     * продолжать работать со старым состоянием.
     *
     * @param state   текущее состояние партии
     * @param command команда игрока
     * @return {@link CommandResult.Accepted} с новым состоянием или {@link CommandResult.Rejected} с причиной
     */
    public static CommandResult handle(GameState state, Command command) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(command, "command");

        return switch (command) {
            case PlayCard c -> takeTurn(state, c.playerId(), c.cardNumber(), true);
            case DiscardCard c -> takeTurn(state, c.playerId(), c.cardNumber(), false);
        };
    }

    /**
     * Ход игрока (ТЗ, 4.2–4.4): выложить или сбросить карту, добрать одну и передать ход.
     *
     * @param toTable {@code true} — выложить на стол, {@code false} — сбросить
     */
    private static CommandResult takeTurn(GameState state, String playerId, int cardNumber, boolean toTable) {
        // 1. Проверки правил. Порядок важен: от общего (фаза) к частному (карта).
        if (state.phase() != Phase.PLAYING) {
            return new CommandResult.Rejected(RuleViolation.WRONG_PHASE);
        }
        int playerIndex = state.indexOfPlayer(playerId);
        if (playerIndex == -1) {
            return new CommandResult.Rejected(RuleViolation.UNKNOWN_PLAYER);
        }
        if (playerIndex != state.currentPlayerIndex()) {
            return new CommandResult.Rejected(RuleViolation.NOT_YOUR_TURN);
        }
        Player player = state.players().get(playerIndex);
        if (!player.hand().contains(cardNumber)) {
            return new CommandResult.Rejected(RuleViolation.CARD_NOT_IN_HAND);
        }

        // 2. Карта из руки — на стол или в сброс.
        // Списки в state неизменяемые, поэтому работаем с изменяемыми копиями.
        List<Integer> hand = new ArrayList<>(player.hand());
        hand.remove(Integer.valueOf(cardNumber)); // по значению, а не по индексу!
        List<Integer> table = new ArrayList<>(state.table());
        List<Integer> discard = new ArrayList<>(state.discard());
        if (toTable) {
            table.add(cardNumber);
        } else {
            discard.add(cardNumber);
        }

        // 3. Добор: сначала основная колода, когда она пуста — резерв (ТЗ, 4.4).
        List<Integer> deck = new ArrayList<>(state.deck());
        List<ReserveCard> reserve = new ArrayList<>(state.reserve());
        if (!deck.isEmpty()) {
            hand.add(deck.removeFirst());
        } else if (!reserve.isEmpty()) {
            hand.add(reserve.removeFirst().cardNumber());
        }

        List<Player> players = new ArrayList<>(state.players());
        players.set(playerIndex, player.withHand(hand));

        // 4. Передача хода. Если ходить некому — карты кончились, фаза NO_MOVES_LEFT.
        GameState after = state
                .withPlayers(players)
                .withTable(table)
                .withDiscard(discard)
                .withDeck(deck)
                .withReserve(reserve)
                .withTurnNumber(state.turnNumber() + 1);

        int nextIndex = nextPlayerIndex(players, playerIndex);
        if (nextIndex == -1) {
            after = after.withPhase(Phase.NO_MOVES_LEFT);
        } else {
            after = after.withCurrentPlayerIndex(nextIndex);
        }
        return new CommandResult.Accepted(after);
    }

    /**
     * Ищет по кругу, начиная со следующего после {@code from}, игрока, который может ходить:
     * не вышел из партии и имеет карты в руке. Отключившиеся не пропускаются — их ход ждёт (ТЗ, 4.5).
     * Последним проверяется сам игрок {@code from}: если ходить может только он, ход остаётся у него.
     *
     * @return индекс следующего игрока или {@code -1}, если ходить некому
     */
    private static int nextPlayerIndex(List<Player> players, int from) {
        int count = players.size();
        for (int step = 1; step <= count; step++) {
            int index = (from + step) % count;
            Player candidate = players.get(index);
            if (candidate.status() != PlayerStatus.LEFT && !candidate.hand().isEmpty()) {
                return index;
            }
        }
        return -1;
    }
}
