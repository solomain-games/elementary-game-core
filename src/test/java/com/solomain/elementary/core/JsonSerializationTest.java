package com.solomain.elementary.core;

import com.solomain.elementary.core.engine.GameEngine;
import com.solomain.elementary.core.engine.PlayerInfo;
import com.solomain.elementary.core.model.GameSettings;
import com.solomain.elementary.core.model.GameState;
import com.solomain.elementary.core.model.Phase;
import com.solomain.elementary.core.model.PlayerStatus;
import com.solomain.elementary.core.model.ReserveCard;
import com.solomain.elementary.core.model.Vote;
import com.solomain.elementary.core.view.PlayerViews;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import tools.jackson.databind.json.JsonMapper;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static com.solomain.elementary.core.TestStates.player;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Сериализация в JSON.
 *
 * <p>game-service хранит {@link GameState} в Redis в виде JSON и отправляет игрокам
 * {@link com.solomain.elementary.core.view.PlayerView} тоже в JSON. Здесь проверяется, что
 * состояние переживает путь «объект → JSON → объект» без потерь, а в JSON для игрока нет секретов.
 */
@DisplayName("JSON: состояние и представление")
class JsonSerializationTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    /** Состояние, в котором заполнены все поля, включая необязательные. */
    private static GameState fullState() {
        var players = List.of(
                player("p1", 5, 6),
                player("p2", PlayerStatus.DISCONNECTED, 7),
                player("p3", PlayerStatus.LEFT));
        return new GameState(
                "demo",
                new GameSettings("p1", Duration.ofSeconds(60)),
                Phase.PLAYING,
                players,
                List.of(1, 2),
                List.of(10, 11),
                List.of(new ReserveCard(12, "p3")),
                List.of(3),
                0,
                7,
                Instant.parse("2026-10-01T12:01:00Z"),
                new Vote("p1", Map.of("p1", true, "p2", false), Instant.parse("2026-10-01T12:00:30Z")),
                Map.of("p1", Map.of("q1", "a")),
                Set.of("p1", "p2"),
                Map.of("p1", 3));
    }

    @Test
    @DisplayName("состояние со всеми полями: объект → JSON → объект без потерь")
    void roundTripsFullState() {
        var state = fullState();

        var json = JSON.writeValueAsString(state);
        var restored = JSON.readValue(json, GameState.class);

        assertThat(restored).isEqualTo(state);
    }

    @Test
    @DisplayName("необязательные поля null (без таймера и голосования) тоже переживают сохранение")
    void roundTripsNullFields() {
        var state = fullState().withTurnDeadline(null).withVote(null);

        var restored = JSON.readValue(JSON.writeValueAsString(state), GameState.class);

        assertThat(restored).isEqualTo(state);
        assertThat(restored.vote()).isNull();
    }

    @Test
    @DisplayName("стартовое состояние из GameEngine.start тоже сохраняется")
    void roundTripsStartedGame() {
        var players = List.of(
                new PlayerInfo("p1", "Анна"),
                new PlayerInfo("p2", "Борис"));
        var state = GameEngine.start(
                TestCases.withCards(20), players, new GameSettings("p1", null), new Random(42));

        var restored = JSON.readValue(JSON.writeValueAsString(state), GameState.class);

        assertThat(restored).isEqualTo(state);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Phase.class, names = {"PROLOGUE", "PLAYING", "NO_MOVES_LEFT", "ANSWERING"})
    @DisplayName("в JSON для игрока до финала нет ни «относится к делу», ни правильных ответов")
    void viewJsonHasNoSecretsBeforeFinish(Phase phase) {
        var caseDef = TestCases.withCards(20);
        var state = new GameState(caseDef.id(), new GameSettings("p1", null), phase,
                List.of(player("p1", 2, 3), player("p2", 4)), List.of(1), List.of(5, 6), List.of(), List.of(7),
                0, 1, null, null, Map.of(), Set.of("p1", "p2"), Map.of());

        var json = JSON.writeValueAsString(PlayerViews.of(caseDef, state, "p1"));

        assertThat(json)
                .doesNotContain("relevant")
                .doesNotContain("correct");
    }
}
