package com.solomain.elementary.core.engine;

/**
 * Предыстория закончилась, игра начинается (ТЗ, 4.1, п. 2).
 *
 * <p>Отправляет game-service, когда все нажали «Готов» или хозяин нажал «Начать» (задача Г8).
 * Ядро только переводит партию из {@code PROLOGUE} в {@code PLAYING} и запускает срок первого хода.
 */
public record BeginPlay() implements Command {
}
