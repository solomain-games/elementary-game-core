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

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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
     * <p>Время и случайность передаются снаружи, как и в {@link #start}: ядро не читает системные часы
     * и не создаёт генераторы само, поэтому в тестах результат полностью предсказуем.
     *
     * @param state   текущее состояние партии
     * @param command команда игрока или системы
     * @param now     текущий момент: от него отсчитывается срок хода
     * @param random  генератор случайных чисел: нужен, когда ход делается за игрока по таймеру
     * @return {@link CommandResult.Accepted} с новым состоянием или {@link CommandResult.Rejected} с причиной
     */
    public static CommandResult handle(GameState state, Command command, Instant now, RandomGenerator random) {
        Objects.requireNonNull(state, "state");
        Objects.requireNonNull(command, "command");
        Objects.requireNonNull(now, "now");
        Objects.requireNonNull(random, "random");

        return switch (command) {
            case PlayCard c -> takeTurn(state, c.playerId(), c.cardNumber(), true, now);
            case DiscardCard c -> takeTurn(state, c.playerId(), c.cardNumber(), false, now);
            case TurnTimeout c -> timeout(state, c, now, random);
            case PlayerDisconnected c -> disconnect(state, c.playerId());
            case PlayerDisconnectTimeout c -> leave(state, c.playerId(), EnumSet.of(PlayerStatus.DISCONNECTED), now);
            case PlayerLeft c ->
                    leave(state, c.playerId(), EnumSet.of(PlayerStatus.ACTIVE, PlayerStatus.DISCONNECTED), now);
            case PlayerReturned c -> returnPlayer(state, c.playerId());
        };
    }

    // ===== Ход =====

    /**
     * Ход игрока (ТЗ, 4.2–4.4): выложить или сбросить карту, добрать одну и передать ход.
     *
     * @param toTable {@code true} — выложить на стол, {@code false} — сбросить
     * @param now     текущий момент: от него отсчитывается срок следующего хода
     */
    private static CommandResult takeTurn(GameState state, String playerId, int cardNumber, boolean toTable,
                                          Instant now) {
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
        GameState after = replacePlayer(state, playerIndex, player.withHand(hand))
                .withTable(table)
                .withDiscard(discard);

        // 3. Добор в конце хода и передача хода следующему.
        after = drawFor(after, playerIndex);
        return new CommandResult.Accepted(advanceTurn(after, playerIndex, now));
    }

    /**
     * Игрок добирает одну карту: сверху основной колоды, а когда она пуста — первую из резерва (ТЗ, 4.4).
     * Если брать неоткуда, состояние не меняется.
     */
    private static GameState drawFor(GameState state, int playerIndex) {
        List<Integer> deck = new ArrayList<>(state.deck());
        List<ReserveCard> reserve = new ArrayList<>(state.reserve());

        int card;
        if (!deck.isEmpty()) {
            card = deck.removeFirst();
        } else if (!reserve.isEmpty()) {
            card = reserve.removeFirst().cardNumber();
        } else {
            return state;
        }

        Player player = state.players().get(playerIndex);
        List<Integer> hand = new ArrayList<>(player.hand());
        hand.add(card);
        return replacePlayer(state, playerIndex, player.withHand(hand))
                .withDeck(deck)
                .withReserve(reserve);
    }

    /**
     * Передаёт ход следующему игроку, который может ходить (ТЗ, 4.2).
     * Если у него нет карт, он сначала добирает одну. Если ходить некому — фаза {@link Phase#NO_MOVES_LEFT}.
     * Если в партии есть таймер и идёт игра, новому ходу назначается срок (ТЗ, 4.3).
     *
     * @param fromIndex индекс игрока, после которого ищется следующий
     * @param now       текущий момент: от него отсчитывается срок нового хода
     */
    private static GameState advanceTurn(GameState state, int fromIndex, Instant now) {
        GameState after = state.withTurnNumber(state.turnNumber() + 1);

        int nextIndex = nextPlayerIndex(after, fromIndex);
        if (nextIndex == -1) {
            after = after.withPhase(Phase.NO_MOVES_LEFT);
        } else {
            after = after.withCurrentPlayerIndex(nextIndex);
            if (after.players().get(nextIndex).hand().isEmpty()) {
                after = drawFor(after, nextIndex);
            }
        }
        return after.withTurnDeadline(deadlineFor(after, now));
    }

    /**
     * Срок текущего хода: {@code now + turnTimeout}, если таймер включён и идёт игра; иначе {@code null}.
     * Во время предыстории и после окончания ходов срока нет.
     */
    private static Instant deadlineFor(GameState state, Instant now) {
        Duration timeout = state.settings().turnTimeout();
        if (timeout == null || state.phase() != Phase.PLAYING) {
            return null;
        }
        return now.plus(timeout);
    }

    /**
     * Время хода истекло (ТЗ, 4.3): на стол выкладывается случайная карта из руки игрока.
     * Дальше всё как при обычном ходе — добор, передача хода, новый срок.
     */
    private static CommandResult timeout(GameState state, TurnTimeout command, Instant now, RandomGenerator random) {
        if (state.phase() != Phase.PLAYING) {
            return new CommandResult.Rejected(RuleViolation.WRONG_PHASE);
        }
        if (state.settings().turnTimeout() == null) {
            return new CommandResult.Rejected(RuleViolation.NO_TURN_TIMER);
        }
        Player current = state.players().get(state.currentPlayerIndex());
        if (command.turnNumber() != state.turnNumber() || !current.id().equals(command.playerId())) {
            return new CommandResult.Rejected(RuleViolation.STALE_TIMEOUT);
        }
        if (state.turnDeadline() != null && now.isBefore(state.turnDeadline())) {
            return new CommandResult.Rejected(RuleViolation.TURN_NOT_EXPIRED);
        }

        List<Integer> hand = current.hand();
        int card = hand.get(random.nextInt(hand.size()));
        return takeTurn(state, current.id(), card, true, now);
    }

    /**
     * Ищет по кругу, начиная со следующего после {@code from}, игрока, который может ходить:
     * не вышел из партии и либо имеет карты в руке, либо может добрать (колода или резерв не пусты).
     * Отключившиеся не пропускаются — их ход ждёт (ТЗ, 4.5).
     * Последним проверяется сам игрок {@code from}: если ходить может только он, ход остаётся у него.
     *
     * @return индекс следующего игрока или {@code -1}, если ходить некому
     */
    private static int nextPlayerIndex(GameState state, int from) {
        boolean canDraw = !state.deck().isEmpty() || !state.reserve().isEmpty();
        List<Player> players = state.players();
        int count = players.size();
        for (int step = 1; step <= count; step++) {
            int index = (from + step) % count;
            Player candidate = players.get(index);
            if (candidate.status() != PlayerStatus.LEFT && (canDraw || !candidate.hand().isEmpty())) {
                return index;
            }
        }
        return -1;
    }

    // ===== Отключение, выход, возвращение (ТЗ, 4.5) =====

    /**
     * Игрок потерял связь: статус {@code DISCONNECTED}, карты остаются в руке, его ход ждёт.
     */
    private static CommandResult disconnect(GameState state, String playerId) {
        int playerIndex = state.indexOfPlayer(playerId);
        Optional<RuleViolation> violation = checkPresenceCommand(state, playerIndex, EnumSet.of(PlayerStatus.ACTIVE));
        if (violation.isPresent()) {
            return new CommandResult.Rejected(violation.get());
        }

        Player player = state.players().get(playerIndex);
        return new CommandResult.Accepted(
                replacePlayer(state, playerIndex, player.withStatus(PlayerStatus.DISCONNECTED)));
    }

    /**
     * Игрок вышел (сам или не вернувшись за 2 минуты): карты уходят в конец резерва с пометкой владельца,
     * статус {@code LEFT}. Если сейчас был его ход, ход переходит к следующему.
     *
     * @param allowedStatuses статусы, из которых возможен выход этой командой
     * @param now             текущий момент: от него отсчитывается срок хода следующего игрока
     */
    private static CommandResult leave(GameState state, String playerId, Set<PlayerStatus> allowedStatuses,
                                       Instant now) {
        int playerIndex = state.indexOfPlayer(playerId);
        Optional<RuleViolation> violation = checkPresenceCommand(state, playerIndex, allowedStatuses);
        if (violation.isPresent()) {
            return new CommandResult.Rejected(violation.get());
        }

        Player player = state.players().get(playerIndex);
        List<ReserveCard> reserve = new ArrayList<>(state.reserve());
        for (int cardNumber : player.hand()) {
            reserve.add(new ReserveCard(cardNumber, playerId));
        }
        GameState after = replacePlayer(state, playerIndex, player.withHand(List.of()).withStatus(PlayerStatus.LEFT))
                .withReserve(reserve);

        boolean turnsInProgress = state.phase() == Phase.PROLOGUE || state.phase() == Phase.PLAYING;
        if (turnsInProgress && playerIndex == state.currentPlayerIndex()) {
            after = advanceTurn(after, playerIndex, now);
        }
        return new CommandResult.Accepted(after);
    }

    /**
     * Игрок вернулся: статус {@code ACTIVE}. Если он выходил, забирает из резерва свои карты,
     * которые ещё никто не взял, в том порядке, в каком они лежат в резерве.
     */
    private static CommandResult returnPlayer(GameState state, String playerId) {
        int playerIndex = state.indexOfPlayer(playerId);
        Optional<RuleViolation> violation = checkPresenceCommand(state, playerIndex,
                EnumSet.of(PlayerStatus.DISCONNECTED, PlayerStatus.LEFT));
        if (violation.isPresent()) {
            return new CommandResult.Rejected(violation.get());
        }

        // Один проход по резерву: свои карты — в руку, чужие остаются.
        Player player = state.players().get(playerIndex);
        List<Integer> hand = new ArrayList<>(player.hand());
        List<ReserveCard> remainingReserve = new ArrayList<>();
        for (ReserveCard card : state.reserve()) {
            if (card.ownerId().equals(playerId)) {
                hand.add(card.cardNumber());
            } else {
                remainingReserve.add(card);
            }
        }

        GameState after = replacePlayer(state, playerIndex, player.withHand(hand).withStatus(PlayerStatus.ACTIVE))
                .withReserve(remainingReserve);
        return new CommandResult.Accepted(after);
    }

    /**
     * Общие проверки для команд отключения, выхода и возвращения.
     *
     * @return нарушение правил или пустой {@link Optional}, если команду можно выполнить
     */
    private static Optional<RuleViolation> checkPresenceCommand(GameState state, int playerIndex,
                                                                Set<PlayerStatus> allowedStatuses) {
        if (state.phase() == Phase.FINISHED) {
            return Optional.of(RuleViolation.WRONG_PHASE);
        }
        if (playerIndex == -1) {
            return Optional.of(RuleViolation.UNKNOWN_PLAYER);
        }
        if (!allowedStatuses.contains(state.players().get(playerIndex).status())) {
            return Optional.of(RuleViolation.INVALID_PLAYER_STATUS);
        }
        return Optional.empty();
    }

    // ===== Помощники =====

    /** Копия состояния, в которой игрок с индексом {@code index} заменён на {@code player}. */
    private static GameState replacePlayer(GameState state, int index, Player player) {
        List<Player> players = new ArrayList<>(state.players());
        players.set(index, player);
        return state.withPlayers(players);
    }
}
