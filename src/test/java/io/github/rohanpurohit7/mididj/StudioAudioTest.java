package io.github.rohanpurohit7.mididj;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;

class StudioAudioTest {
    @Test
    void catalogContainsThirtyDistinctSoulfulStyles() {
        var styles = StudioBackingCatalog.styles();
        assertEquals(30, styles.size());
        assertEquals(30, new HashSet<>(styles.stream().map(StudioBackingCatalog.Style::name).toList()).size());
        assertTrue(styles.stream().allMatch(style -> style.bpm() >= 55 && style.bpm() <= 180));
        assertTrue(styles.stream().allMatch(style -> style.progression().length >= 4));
    }

    @Test
    void everyBackingStyleHasLosslessStemIdentifier() {
        assertTrue(StudioBackingCatalog.styles().stream()
                .allMatch(style -> style.stemBaseName() != null && !style.stemBaseName().isBlank()));
    }

    @Test
    void toneSettingsClampMidiControllerRange() {
        var settings = new StudioAudioEngine.ToneSettings(-20, 200, 64, 80, 90, 100, 20, 10, 70);
        assertEquals(0, settings.twang());
        assertEquals(127, settings.warmth());
        assertEquals(64, settings.drive());
    }
}
