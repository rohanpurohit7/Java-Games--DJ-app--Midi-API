package io.github.rohanpurohit7.mididj;

import java.util.Locale;

/** Explainable, deterministic advisor for scale, guitar archetype, pickup and tone. */
public final class GuitarStyleAdvisor {
    public record Recommendation(
            String scale,
            String guitarType,
            String pickup,
            StudioAudioEngine.ToneSettings tone,
            String explanation
    ) {}

    private GuitarStyleAdvisor() {}

    public static Recommendation recommend(StudioBackingCatalog.Style style) {
        String family = style.family().toLowerCase(Locale.ROOT);
        String name = style.name().toLowerCase(Locale.ROOT);

        if (family.contains("blues")) {
            if (name.contains("texas") || name.contains("shuffle")) {
                return rec("Blues", "Single-Coil S-Style", "Neck + Middle",
                        104, 62, 66, 92, 78, 34, 8, 24, 78,
                        "Shuffle rhythm favors bright single-coil attack, blues-scale phrasing and restrained spring ambience.");
            }
            if (name.contains("minor")) {
                return rec("Blues", "Semi-Hollow Humbucker", "Neck + Bridge",
                        66, 92, 48, 64, 92, 54, 14, 28, 82,
                        "Minor blues benefits from a warm semi-hollow voice, expressive sustain and blues-scale color.");
            }
            return rec("Minor Pentatonic", "Single-Coil S-Style", "Neck",
                    84, 76, 42, 78, 86, 44, 10, 22, 84,
                    "Classic blues works well with minor pentatonic language and a rounded neck-pickup voice.");
        }
        if (family.contains("soul") || family.contains("r&b") || family.contains("gospel")) {
            return rec("Major Pentatonic", "Semi-Hollow Soul Guitar", "Neck",
                    42, 104, 24, 60, 98, 68, 26, 18, 92,
                    "Soul and gospel favor warm body, soft transients, vocal sustain and spacious reverb.");
        }
        if (family.contains("funk")) {
            return rec("Dorian", "Single-Coil S-Style", "Bridge + Middle",
                    112, 46, 18, 104, 54, 20, 12, 12, 96,
                    "Funk needs tight sustain, sharp attack, bridge-middle bite and Dorian color.");
        }
        if (family.contains("jazz")) {
            return rec("Mixolydian", "Jazz Hollowbody", "Neck",
                    20, 116, 12, 44, 108, 48, 22, 12, 72,
                    "Jazz harmony benefits from a dark neck pickup, rounded body and Mixolydian dominant color.");
        }
        if (family.contains("fusion")) {
            return rec("Dorian", "Double-Cut Fusion Guitar", "Neck Humbucker",
                    30, 94, 52, 72, 110, 58, 32, 26, 78,
                    "Fusion favors Dorian phrasing, smooth sustain and a focused neck-humbucker lead voice.");
        }
        if (family.contains("latin") || family.contains("world")) {
            if (name.contains("spanish") || name.contains("andalus") || name.contains("phrygian")) {
                return rec("Phrygian Dominant", "Flamenco Negra Nylon", "Soundhole / Bridge Blend",
                        92, 74, 8, 98, 58, 36, 4, 8, 88,
                        "Spanish harmony calls for Phrygian-dominant color and a fast, dry flamenco response.");
            }
            if (name.contains("santana") || name.contains("latin rock")) {
                return rec("Natural Minor", "Sustain Double-Cut", "Neck Humbucker",
                        34, 96, 58, 68, 118, 70, 18, 34, 80,
                        "Latin rock favors singing sustain, neck-humbucker warmth and natural-minor target tones.");
            }
            return rec("Major Pentatonic", "Warm Cedar Nylon", "Soundhole",
                    48, 104, 10, 62, 78, 48, 8, 10, 86,
                    "World and acoustic grooves benefit from a warm nylon voice and open pentatonic phrasing.");
        }
        if (family.contains("country")) {
            return rec("Major Pentatonic", "T-Style Twang Guitar", "Bridge",
                    120, 44, 28, 112, 58, 26, 6, 16, 90,
                    "Country rhythm rewards bridge-pickup twang, bright attack and major-pentatonic double stops.");
        }
        if (family.contains("reggae")) {
            return rec("Mixolydian", "Single-Coil T-Style", "Bridge + Neck",
                    78, 66, 14, 84, 52, 46, 18, 10, 92,
                    "Reggae favors clean, short chord chops with a lightly scooped single-coil tone.");
        }
        if (family.contains("ambient")) {
            return rec("Natural Minor", "Modern Double-Cut", "Neck",
                    24, 90, 34, 56, 118, 116, 72, 38, 64,
                    "Ambient tracks favor long sustain, broad reverb, chorus and natural-minor melodic space.");
        }
        if (family.contains("rock")) {
            return rec("Minor Pentatonic", "Dual-Humbucker Single-Cut", "Bridge Humbucker",
                    36, 82, 78, 86, 102, 42, 10, 22, 76,
                    "Rock backing calls for a bridge humbucker, moderate drive and minor-pentatonic phrasing.");
        }
        return rec("Minor Pentatonic", "Versatile Double-Cut", "Neck + Bridge",
                58, 82, 38, 72, 88, 48, 16, 18, 80,
                "Balanced settings provide a versatile starting point for this backing track.");
    }

    private static Recommendation rec(String scale, String guitar, String pickup,
                                      int twang, int warmth, int drive, int brightness, int sustain,
                                      int reverb, int chorus, int vibrato, int humanize, String why) {
        return new Recommendation(scale, guitar, pickup,
                new StudioAudioEngine.ToneSettings(twang, warmth, drive, brightness, sustain,
                        reverb, chorus, vibrato, humanize), why);
    }
}
