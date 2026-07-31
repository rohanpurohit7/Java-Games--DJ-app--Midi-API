package io.github.rohanpurohit7.mididj;

import java.nio.file.Path;
import java.util.List;

/** Catalog of soulful backing arrangements. Lossless stems are optional and user-supplied. */
public final class StudioBackingCatalog {
    public record Style(
            String name,
            String family,
            int bpm,
            int chordProgram,
            int bassProgram,
            int leadProgram,
            int[] progression,
            int groove,
            String stemBaseName
    ) {
        @Override public String toString() { return name + " · " + bpm + " BPM"; }

        public Path resolveStem(Path libraryRoot) {
            if (libraryRoot == null || stemBaseName == null || stemBaseName.isBlank()) return null;
            Path wav = libraryRoot.resolve(stemBaseName + ".wav");
            if (wav.toFile().isFile()) return wav;
            Path aiff = libraryRoot.resolve(stemBaseName + ".aiff");
            return aiff.toFile().isFile() ? aiff : null;
        }
    }

    private StudioBackingCatalog() {}

    public static List<Style> styles() {
        return List.of(
                s("Slow Chicago Blues", "Blues", 72, 26, 33, 29, p(0,5,0,0,5,5,0,0,7,5,0,7), 0, "slow-chicago-blues"),
                s("Texas Shuffle", "Blues", 112, 27, 34, 30, p(0,0,5,0,7,5,0,7), 1, "texas-shuffle"),
                s("Soulful Minor Blues", "Blues", 78, 17, 33, 29, p(0,5,0,0,8,5,0,7), 2, "soulful-minor-blues"),
                s("Memphis Soul", "Soul", 96, 17, 34, 27, p(0,5,9,7), 3, "memphis-soul"),
                s("Neo Soul Pocket", "Soul", 86, 5, 36, 27, p(0,9,5,7), 4, "neo-soul-pocket"),
                s("Gospel Turnaround", "Gospel", 82, 16, 33, 26, p(0,5,9,7,0,5,7,0), 5, "gospel-turnaround"),
                s("Funk One-Chord Jam", "Funk", 104, 4, 36, 27, p(0,0,0,0), 6, "funk-one-chord"),
                s("Deep Funk Vamp", "Funk", 98, 17, 36, 28, p(0,3,5,3), 7, "deep-funk-vamp"),
                s("Classic Rock Ballad", "Rock", 76, 30, 34, 29, p(0,8,5,7), 8, "classic-rock-ballad"),
                s("Southern Rock Jam", "Rock", 108, 25, 34, 30, p(0,5,8,7), 9, "southern-rock-jam"),
                s("Arena Rock Drive", "Rock", 126, 30, 35, 30, p(0,7,5,8), 10, "arena-rock-drive"),
                s("Indie Dream Rock", "Rock", 94, 88, 38, 29, p(0,8,3,10), 11, "indie-dream-rock"),
                s("Jazz Blues Lounge", "Jazz", 92, 4, 32, 26, p(0,5,0,7,5,0,7,0), 12, "jazz-blues-lounge"),
                s("Dorian Jazz Fusion", "Fusion", 118, 5, 36, 30, p(0,3,5,0), 13, "dorian-jazz-fusion"),
                s("Smooth Fusion", "Fusion", 102, 89, 38, 29, p(0,9,5,7), 14, "smooth-fusion"),
                s("Latin Rock Montuno", "Latin", 112, 4, 32, 29, p(0,5,7,5), 15, "latin-rock-montuno"),
                s("Santana Style Minor", "Latin", 96, 17, 33, 29, p(0,8,7,0), 16, "santana-minor"),
                s("Bossa Guitar Lounge", "Latin", 128, 24, 32, 26, p(0,9,5,7), 17, "bossa-guitar-lounge"),
                s("Spanish Phrygian", "World", 104, 24, 32, 24, p(0,1,10,8), 18, "spanish-phrygian"),
                s("Andalusian Soul", "World", 88, 24, 33, 29, p(0,10,8,7), 19, "andalusian-soul"),
                s("Desert Rock", "World", 100, 25, 34, 29, p(0,1,5,0), 20, "desert-rock"),
                s("Indian Fusion Drone", "World", 90, 104, 43, 29, p(0,0,5,0), 21, "indian-fusion-drone"),
                s("African Highlife", "World", 116, 25, 36, 27, p(0,5,9,7), 22, "african-highlife"),
                s("Reggae One Drop", "Reggae", 76, 27, 34, 28, p(0,5,7,5), 23, "reggae-one-drop"),
                s("Dub Minor Space", "Reggae", 72, 89, 38, 29, p(0,8,5,7), 24, "dub-minor-space"),
                s("Country Twang", "Country", 118, 25, 33, 26, p(0,5,0,7), 25, "country-twang"),
                s("Nashville Slow Burn", "Country", 82, 25, 34, 26, p(0,8,5,7), 26, "nashville-slow-burn"),
                s("Ambient Cinematic Minor", "Ambient", 68, 89, 38, 29, p(0,8,3,10), 27, "ambient-cinematic-minor"),
                s("Post Rock Crescendo", "Ambient", 84, 88, 39, 29, p(0,5,8,3), 28, "post-rock-crescendo"),
                s("Midnight R&B", "R&B", 74, 5, 36, 27, p(0,9,3,7), 29, "midnight-rnb")
        );
    }

    private static Style s(String name, String family, int bpm, int chordProgram, int bassProgram,
                           int leadProgram, int[] progression, int groove, String stem) {
        return new Style(name, family, bpm, chordProgram, bassProgram, leadProgram, progression, groove, stem);
    }

    private static int[] p(int... values) { return values; }
}
