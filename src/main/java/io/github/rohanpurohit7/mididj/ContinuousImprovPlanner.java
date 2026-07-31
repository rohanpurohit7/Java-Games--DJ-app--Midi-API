package io.github.rohanpurohit7.mididj;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/** Pure, non-UI phrase planner used by continuous improvisation and regression tests. */
public final class ContinuousImprovPlanner {
    private final Random random;
    private List<Integer> previous = List.of();
    private int generation;

    public ContinuousImprovPlanner(long seed) {
        this.random = new Random(seed);
    }

    public List<Integer> nextPhrase(int rootMidi, String scale, int minNotes, int maxNotes) {
        if (minNotes < 2 || maxNotes < minNotes) {
            throw new IllegalArgumentException("Invalid phrase length range");
        }
        List<Integer> result;
        if (previous.isEmpty() || generation % 4 == 0) {
            int count = minNotes + random.nextInt(maxNotes - minNotes + 1);
            result = GuitarTheory.generateLick(rootMidi, scale, count, random.nextLong());
        } else {
            result = GuitarTheory.variation(previous, random.nextLong());
        }
        previous = List.copyOf(result);
        generation++;
        return new ArrayList<>(result);
    }

    public int generation() { return generation; }
    public void reset() { previous = List.of(); generation = 0; }
}
