package io.github.rohanpurohit7.mididj;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class GuitarStyleAdvisorTest {
    private static final Set<String> SCALES = Set.of(
            "Minor Pentatonic", "Blues", "Major Pentatonic", "Natural Minor",
            "Dorian", "Mixolydian", "Phrygian Dominant"
    );

    @Test
    void everyBackingStyleGetsACompleteRecommendation() {
        assertTrue(StudioBackingCatalog.styles().size() >= 30);
        for (StudioBackingCatalog.Style style : StudioBackingCatalog.styles()) {
            GuitarStyleAdvisor.Recommendation r = GuitarStyleAdvisor.recommend(style);
            assertTrue(SCALES.contains(r.scale()), style.name());
            assertFalse(r.guitarType().isBlank(), style.name());
            assertFalse(r.pickup().isBlank(), style.name());
            assertFalse(r.explanation().isBlank(), style.name());
            assertToneRange(r.tone());
        }
    }

    @Test
    void texasShuffleGetsTwangySingleCoilRecommendation() {
        StudioBackingCatalog.Style style = StudioBackingCatalog.styles().stream()
                .filter(item -> item.name().equals("Texas Shuffle"))
                .findFirst().orElseThrow();
        GuitarStyleAdvisor.Recommendation r = GuitarStyleAdvisor.recommend(style);
        assertEquals("Blues", r.scale());
        assertTrue(r.guitarType().contains("Single-Coil"));
        assertTrue(r.tone().twang() > 90);
    }

    @Test
    void spanishTrackGetsFlamencoVisualAndPhrygianScale() {
        StudioBackingCatalog.Style style = StudioBackingCatalog.styles().stream()
                .filter(item -> item.name().equals("Spanish Phrygian"))
                .findFirst().orElseThrow();
        GuitarStyleAdvisor.Recommendation r = GuitarStyleAdvisor.recommend(style);
        assertEquals("Phrygian Dominant", r.scale());
        assertTrue(r.guitarType().contains("Flamenco"));
    }

    private static void assertToneRange(StudioAudioEngine.ToneSettings tone) {
        int[] values = {tone.twang(), tone.warmth(), tone.drive(), tone.brightness(), tone.sustain(),
                tone.reverb(), tone.chorus(), tone.vibrato(), tone.humanize()};
        for (int value : values) assertTrue(value >= 0 && value <= 127);
    }
}
