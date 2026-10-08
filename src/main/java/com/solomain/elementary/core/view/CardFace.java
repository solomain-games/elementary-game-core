package com.solomain.elementary.core.view;

import com.solomain.elementary.core.model.Card;
import com.solomain.elementary.core.model.ImageCard;
import com.solomain.elementary.core.model.LocalizedText;
import com.solomain.elementary.core.model.TextAlign;
import com.solomain.elementary.core.model.TextCard;

import java.util.Objects;

/**
 * «Лицо» карты: всё, что игрок видит на карте, но без признака «относится к делу».
 *
 * <p>Отдельный тип нужен для защиты от читов: {@link Card} содержит {@code relevant},
 * и если отправить её клиенту целиком, ответ можно подсмотреть в инструментах разработчика.
 * У {@code CardFace} такого поля просто нет.
 */
public sealed interface CardFace {

    /** Номер карты. */
    int number();

    /** Лицо карты-картинки. */
    record ImageFace(int number, String image, LocalizedText caption) implements CardFace {
        public ImageFace {
            Objects.requireNonNull(image, "image");
        }
    }

    /** Лицо текстовой карты. */
    record TextFace(int number, String background, LocalizedText text, TextAlign align) implements CardFace {
        public TextFace {
            Objects.requireNonNull(background, "background");
            Objects.requireNonNull(text, "text");
            Objects.requireNonNull(align, "align");
        }
    }

    /** Лицо карты: копирует всё, кроме признака {@code relevant}. */
    static CardFace of(Card card) {
        return switch (card) {
            case ImageCard c -> new ImageFace(c.number(), c.image(), c.caption());
            case TextCard c -> new TextFace(c.number(), c.background(), c.text(), c.align());
        };
    }
}
