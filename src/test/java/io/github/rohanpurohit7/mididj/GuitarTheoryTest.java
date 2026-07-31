package io.github.rohanpurohit7.mididj;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class GuitarTheoryTest {

    @Test
    void minorPentatonicHasExpectedIntervals() {
        assertArrayEquals(new int[]{0, 3, 5, 7, 10}, GuitarTheory.intervals("Minor Pentatonic"));
    }

    @Test
    void generatedLickHasRequestedLength() {
        List<Integer> lick = GuitarTheory.generateLick(57, "Minor Pentatonic", 8, 42L);
        assertEquals(8, lick.size());
    }

    @Test
    void generatedMinorPentatonicLickUsesOnlyScaleTones() {
        List<Integer> lick = GuitarTheory.generateLick(57, "Minor Pentatonic", 12, 42L);
        assertTrue(GuitarTheory.allNotesBelongToScale(lick, 9, "Minor Pentatonic"));
    }

    @Test
    void fretboardOverlayIncludesRootsAndMultipleStrings() {
        List<GuitarTheory.FretNote> overlay = GuitarTheory.scaleOverlay(9, "Blues", 12);
        assertFalse(overlay.isEmpty());
        assertTrue(overlay.stream().anyMatch(note -> note.scaleDegree() == 0));
        assertEquals(6, overlay.stream().map(GuitarTheory.FretNote::stringIndex).distinct().count());
    }

    @Test
    void variationPreservesLengthAndChangesPhrase() {
        List<Integer> source = List.of(57, 60, 62, 64, 67, 69, 72, 69);
        List<Integer> variation = GuitarTheory.variation(source, 7L);
        assertEquals(source.size(), variation.size());
        assertNotEquals(source, variation);
    }

    @Test
    void unsupportedScaleFailsFast() {
        assertThrows(IllegalArgumentException.class, () -> GuitarTheory.intervals("Unknown Scale"));
    }
}
