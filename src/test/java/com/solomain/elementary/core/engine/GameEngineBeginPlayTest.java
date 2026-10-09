package com.solomain.elementary.core.engine;

import com.solomain.elementary.core.error.RuleViolation;
import com.solomain.elementary.core.model.GameSettings;
import com.solomain.elementary.core.model.GameState;
import com.solomain.elementary.core.model.Phase;
import com.solomain.elementary.core.model.Player;
import com.solomain.elementary.core.model.PlayerStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.solomain.elementary.core.TestStates.NOW;
import static com.solomain.elementary.core.TestStates.accepted;
import static com.solomain.elementary.core.TestStates.handle;
import static com.solomain.elementary.core.TestStates.player;
import static com.solomain.elementary.core.TestStates.playing;
import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GameEngine.handle — начало игры после предыстории")
class GameEngineBeginPlayTest {

    private static GameState prologue(int current, Player... players) {
        return playing(current, List.of(10, 11), players).withPhase(Phase.PROLOGUE);
    }

    @Test
    @DisplayName("переводит партию в PLAYING, ход не меняется")
    void startsPlaying() {
        var state = prologue(1, player("p1", 5), player("p2", 6));

        var after = accepted(handle(state, new BeginPlay()));

        assertThat(after.phase()).isEqualTo(Phase.PLAYING);
        assertThat(after.currentPlayerIndex()).isEqualTo(1);
        assertThat(after.turnNumber()).isEqualTo(state.turnNumber());
    }

    @Test
    @DisplayName("с таймером запускает срок первого хода")
    void startsTurnTimer() {
        var state = new GameState("test", new GameSettings("p1", Duration.ofSeconds(60)), Phase.PROLOGUE,
                List.of(player("p1", 5), player("p2", 6)), List.of(1), List.of(10), List.of(), List.of(),
                0, 1, null, null, Map.of(), Set.of(), Map.of());

        var after = accepted(handle(state, new BeginPlay()));

        assertThat(after.turnDeadline()).isEqualTo(NOW.plusSeconds(60));
    }

    @Test
    @DisplayName("если первый игрок вышел во время предыстории, ходит следующий")
    void skipsFirstPlayerWhoLeft() {
        var state = prologue(0, player("p1", PlayerStatus.LEFT), player("p2", 6));

        var after = accepted(handle(state, new BeginPlay()));

        assertThat(after.currentPlayerIndex()).isEqualTo(1);
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(value = Phase.class, names = {"PLAYING", "NO_MOVES_LEFT", "ANSWERING", "FINISHED"})
    @DisplayName("возможно только из предыстории")
    void onlyFromPrologue(Phase phase) {
        var state = prologue(0, player("p1", 5)).withPhase(phase);

        assertThat(handle(state, new BeginPlay()))
                .isEqualTo(new CommandResult.Rejected(RuleViolation.WRONG_PHASE));
    }
}
