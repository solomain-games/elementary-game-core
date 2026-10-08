package com.solomain.elementary.core.view;

import com.solomain.elementary.core.model.Phase;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Состояние партии глазами одного игрока: только то, что ему разрешено видеть (ТЗ, 4.6, ADR 0005).
 *
 * <p>Этот объект целиком отправляется в браузер игрока, поэтому в нём не должно быть
 * ни чужих рук, ни содержимого колоды, ни признака «относится к делу» до финала.
 *
 * @param caseId          идентификатор дела
 * @param phase           фаза партии
 * @param viewerId        для кого построено представление
 * @param hand            своя рука
 * @param table           карты на столе
 * @param players         все игроки в порядке ходов, включая самого игрока
 * @param currentPlayerId чей ход; {@code null}, если фаза не {@code PLAYING}
 * @param deckSize        сколько карт осталось добрать: основная колода и резерв вместе
 * @param discardSize     сколько карт в сбросе
 * @param turnNumber      номер хода
 * @param turnDeadline    когда истекает ход; {@code null} без таймера
 * @param reveal          раскрытие в финале; {@code null} до фазы {@code FINISHED}
 */
public record PlayerView(String caseId,
                         Phase phase,
                         String viewerId,
                         List<CardFace> hand,
                         List<CardFace> table,
                         List<PlayerSummary> players,
                         String currentPlayerId,
                         int deckSize,
                         int discardSize,
                         int turnNumber,
                         Instant turnDeadline,
                         Reveal reveal) {

    public PlayerView {
        Objects.requireNonNull(caseId, "caseId");
        Objects.requireNonNull(phase, "phase");
        Objects.requireNonNull(viewerId, "viewerId");
        hand = List.copyOf(hand);
        table = List.copyOf(table);
        players = List.copyOf(players);
    }
}
