package com.solomain.elementary.core.view;

import com.solomain.elementary.core.model.ImageCard;
import com.solomain.elementary.core.model.LocalizedText;
import com.solomain.elementary.core.model.TextAlign;
import com.solomain.elementary.core.model.TextCard;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("CardFace")
class CardFaceTest {

    private static final LocalizedText TEXT = new LocalizedText(Map.of("ru", "Записка"));

    @Test
    @DisplayName("карта-картинка превращается в ImageFace со всеми полями, кроме relevant")
    void mapsImageCard() {
        var card = new ImageCard(3, true, "cards/03.webp", TEXT);

        var face = CardFace.of(card);

        assertThat(face).isEqualTo(new CardFace.ImageFace(3, "cards/03.webp", TEXT));
    }

    @Test
    @DisplayName("текстовая карта превращается в TextFace со всеми полями, кроме relevant")
    void mapsTextCard() {
        var card = new TextCard(4, false, TEXT, "backgrounds/paper.webp", TextAlign.CENTER);

        var face = CardFace.of(card);

        assertThat(face).isEqualTo(new CardFace.TextFace(4, "backgrounds/paper.webp", TEXT, TextAlign.CENTER));
    }
}
