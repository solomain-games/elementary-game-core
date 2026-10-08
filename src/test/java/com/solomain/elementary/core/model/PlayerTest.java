package com.solomain.elementary.core.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("Player")
class PlayerTest {

    private static final String ID = "p1";
    private static final String NAME = "Анна";
    private static final List<Integer> HAND = List.of(2, 5, 7);

    @Test
    @DisplayName("создаётся с корректными данными")
    void createsWithValidData() {
        var player = new Player(ID, NAME, PlayerStatus.ACTIVE, HAND);

        assertThat(player.id()).isEqualTo(ID);
        assertThat(player.name()).isEqualTo(NAME);
        assertThat(player.status()).isEqualTo(PlayerStatus.ACTIVE);
        assertThat(player.hand()).containsExactly(2, 5, 7);
    }

    @Test
    @DisplayName("принимает пустую руку")
    void acceptsEmptyHand() {
        var player = new Player(ID, NAME, PlayerStatus.LEFT, List.of());

        assertThat(player.hand()).isEmpty();
    }

    @Test
    @DisplayName("отклоняет отсутствующий идентификатор")
    void rejectsMissingId() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Player(null, NAME, PlayerStatus.ACTIVE, HAND))
                .withMessage("id");
    }

    @Test
    @DisplayName("отклоняет отсутствующее имя")
    void rejectsMissingName() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Player(ID, null, PlayerStatus.ACTIVE, HAND))
                .withMessage("name");
    }

    @Test
    @DisplayName("отклоняет отсутствующий статус")
    void rejectsMissingStatus() {
        assertThatNullPointerException()
                .isThrownBy(() -> new Player(ID, NAME, null, HAND))
                .withMessage("status");
    }

    @Test
    @DisplayName("отклоняет пустое место (null) в руке")
    void rejectsNullCardInHand() {
        var handWithNull = Arrays.asList(2, null, 7);

        assertThatNullPointerException()
                .isThrownBy(() -> new Player(ID, NAME, PlayerStatus.ACTIVE, handWithNull));
    }

    @Test
    @DisplayName("не зависит от изменений исходного списка карт")
    void copiesHand() {
        var hand = new ArrayList<>(HAND);
        var player = new Player(ID, NAME, PlayerStatus.ACTIVE, hand);

        hand.add(9);

        assertThat(player.hand()).containsExactly(2, 5, 7);
    }

    @Test
    @DisplayName("не позволяет изменить руку через hand()")
    void exposesUnmodifiableHand() {
        var player = new Player(ID, NAME, PlayerStatus.ACTIVE, HAND);

        assertThatThrownBy(() -> player.hand().add(9))
                .isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    @DisplayName("withHand меняет только руку")
    void withHandReplacesOnlyHand() {
        var player = new Player(ID, NAME, PlayerStatus.ACTIVE, HAND);

        var changed = player.withHand(List.of(4));

        assertThat(changed.hand()).containsExactly(4);
        assertThat(changed).usingRecursiveComparison().ignoringFields("hand").isEqualTo(player);
        assertThat(player.hand()).containsExactly(2, 5, 7); // исходный объект не изменился
    }

    @Test
    @DisplayName("withStatus меняет только статус")
    void withStatusReplacesOnlyStatus() {
        var player = new Player(ID, NAME, PlayerStatus.ACTIVE, HAND);

        var changed = player.withStatus(PlayerStatus.DISCONNECTED);

        assertThat(changed.status()).isEqualTo(PlayerStatus.DISCONNECTED);
        assertThat(changed).usingRecursiveComparison().ignoringFields("status").isEqualTo(player);
        assertThat(player.status()).isEqualTo(PlayerStatus.ACTIVE);
    }
}
