package io.github.rohanpurohit7.mididj;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

/** Pure, headless guitar-theory functions used by tests and UI features. */
public final class GuitarTheory {
    private static final int[] OPEN_STRINGS = {40, 45, 50, 55, 59, 64}; // low E to high E

    private GuitarTheory() { }

    public record FretNote(int stringIndex, int fret, int midiNote, int scaleDegree) { }

    public static int[] intervals(String scaleName) {
        return switch (scaleName) {
            case "Minor Pentatonic" -> new int[]{0, 3, 5, 7, 10};
            case "Blues" -> new int[]{0, 3, 5, 6, 7, 10};
            case "Major Pentatonic" -> new int[]{0, 2, 4, 7, 9};
            case "Natural Minor" -> new int[]{0, 2, 3, 5, 7, 8, 10};
            case "Dorian" -> new int[]{0, 2, 3, 5, 7, 9, 10};
            case "Mixolydian" -> new int[]{0, 2, 4, 5, 7, 9, 10};
            case "Phrygian Dominant" -> new int[]{0, 1, 4, 5, 7, 8, 10};
            default -> throw new IllegalArgumentException("Unsupported scale: " + scaleName);
        };
    }

    public static List<FretNote> scaleOverlay(int rootPitchClass, String scaleName, int maxFret) {
        if (rootPitchClass < 0 || rootPitchClass > 11) {
            throw new IllegalArgumentException("Root pitch class must be 0-11");
        }
        if (maxFret < 0 || maxFret > 24) {
            throw new IllegalArgumentException("maxFret must be 0-24");
        }
        int[] scale = intervals(scaleName);
        List<FretNote> result = new ArrayList<>();
        for (int string = 0; string < OPEN_STRINGS.length; string++) {
            for (int fret = 0; fret <= maxFret; fret++) {
                int midi = OPEN_STRINGS[string] + fret;
                int relative = Math.floorMod(midi - rootPitchClass, 12);
                for (int degree = 0; degree < scale.length; degree++) {
                    if (scale[degree] == relative) {
                        result.add(new FretNote(string, fret, midi, degree));
                        break;
                    }
                }
            }
        }
        return List.copyOf(result);
    }

    public static List<Integer> generateLick(int rootMidi, String scaleName, int length, long seed) {
        if (length < 4 || length > 64) {
            throw new IllegalArgumentException("Lick length must be 4-64 notes");
        }
        int[] scale = intervals(scaleName);
        Random random = new Random(seed);
        List<Integer> notes = new ArrayList<>(length);
        int degree = random.nextInt(scale.length);
        int octave = 0;
        for (int i = 0; i < length; i++) {
            int movement = random.nextInt(5) - 2;
            degree += movement;
            while (degree < 0) {
                degree += scale.length;
                octave--;
            }
            while (degree >= scale.length) {
                degree -= scale.length;
                octave++;
            }
            octave = Math.max(-1, Math.min(1, octave));
            notes.add(rootMidi + scale[degree] + octave * 12);
        }
        return List.copyOf(notes);
    }

    public static List<Integer> variation(List<Integer> source, long seed) {
        if (source == null || source.size() < 4) {
            throw new IllegalArgumentException("A source lick needs at least four notes");
        }
        List<Integer> result = new ArrayList<>(source);
        Random random = new Random(seed);
        switch (random.nextInt(4)) {
            case 0 -> Collections.reverse(result);
            case 1 -> Collections.rotate(result, Math.max(1, result.size() / 3));
            case 2 -> {
                for (int i = 1; i < result.size(); i += 2) {
                    result.set(i, result.get(i) + (random.nextBoolean() ? 12 : -12));
                }
            }
            default -> {
                int midpoint = result.size() / 2;
                Collections.reverse(result.subList(midpoint, result.size()));
            }
        }
        return List.copyOf(result);
    }

    public static boolean allNotesBelongToScale(List<Integer> notes, int rootPitchClass, String scaleName) {
        int[] scale = intervals(scaleName);
        return notes.stream().allMatch(note -> {
            int relative = Math.floorMod(note - rootPitchClass, 12);
            return Arrays.stream(scale).anyMatch(interval -> interval == relative);
        });
    }
}
