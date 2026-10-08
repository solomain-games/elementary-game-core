package com.solomain.elementary.core.engine;

import com.solomain.elementary.core.model.Card;
import com.solomain.elementary.core.model.CaseDefinition;
import com.solomain.elementary.core.model.GameSettings;
import com.solomain.elementary.core.model.GameState;
import com.solomain.elementary.core.model.Phase;
import com.solomain.elementary.core.model.Player;
import com.solomain.elementary.core.model.PlayerStatus;

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
}
