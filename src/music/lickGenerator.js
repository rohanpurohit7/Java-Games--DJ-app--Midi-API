const OPEN_STRINGS = [40, 45, 50, 55, 59, 64];

export const SCALES = {
  'Minor Pentatonic': [0, 3, 5, 7, 10],
  Blues: [0, 3, 5, 6, 7, 10],
  'Major Pentatonic': [0, 2, 4, 7, 9],
  Dorian: [0, 2, 3, 5, 7, 9, 10],
  Mixolydian: [0, 2, 4, 5, 7, 9, 10],
  'Natural Minor': [0, 2, 3, 5, 7, 8, 10]
};

const TECHNIQUES = ['pick', 'hammer-on', 'pull-off', 'bend', 'vibrato', 'slide'];

export function noteName(midi) {
  const names = ['C', 'C#', 'D', 'D#', 'E', 'F', 'F#', 'G', 'G#', 'A', 'A#', 'B'];
  return `${names[midi % 12]}${Math.floor(midi / 12) - 1}`;
}

export function buildFretboard(maxFret = 15) {
  return OPEN_STRINGS.flatMap((openMidi, stringIndex) =>
    Array.from({ length: maxFret + 1 }, (_, fret) => ({
      id: `${stringIndex}-${fret}`,
      stringIndex,
      stringNumber: 6 - stringIndex,
      fret,
      midi: openMidi + fret,
      name: noteName(openMidi + fret)
    }))
  );
}

function candidatePositions(root, intervals, minFret = 3, maxFret = 15) {
  return buildFretboard(maxFret).filter((position) => {
    const degree = ((position.midi - root) % 12 + 12) % 12;
    return position.fret >= minFret && intervals.includes(degree);
  });
}

function chooseNear(candidates, previous, random) {
  const ranked = candidates
    .map((position) => ({
      position,
      distance: previous
        ? Math.abs(position.fret - previous.fret) + Math.abs(position.stringIndex - previous.stringIndex) * 1.7
        : Math.abs(position.fret - 7)
    }))
    .sort((a, b) => a.distance - b.distance);
  const pool = ranked.slice(0, Math.min(7, ranked.length));
  return pool[Math.floor(random() * pool.length)].position;
}

function selectTechnique(previous, current, next, random) {
  if (!previous) return 'pick';
  const semitoneMove = current.midi - previous.midi;
  const sameString = current.stringIndex === previous.stringIndex;
  if (sameString && semitoneMove > 0 && semitoneMove <= 3) return 'hammer-on';
  if (sameString && semitoneMove < 0 && semitoneMove >= -3) return 'pull-off';
  if (sameString && Math.abs(semitoneMove) >= 3 && Math.abs(semitoneMove) <= 5) return 'slide';
  if (next && current.stringIndex === next.stringIndex && next.midi - current.midi === 2 && random() > 0.48) return 'bend';
  if (random() > 0.7) return 'vibrato';
  return TECHNIQUES[Math.floor(random() * 2)];
}

export function generateLick({ rootMidi = 57, scaleName = 'Blues', length = 12, seed = Date.now() } = {}) {
  let state = seed >>> 0;
  const random = () => {
    state = (state * 1664525 + 1013904223) >>> 0;
    return state / 4294967296;
  };

  const intervals = SCALES[scaleName] ?? SCALES.Blues;
  const candidates = candidatePositions(rootMidi, intervals);
  const positions = [];
  let previous = null;

  for (let index = 0; index < length; index += 1) {
    let position = chooseNear(candidates, previous, random);
    if (previous && position.id === previous.id) {
      const alternatives = candidates.filter((candidate) => candidate.id !== previous.id);
      position = chooseNear(alternatives, previous, random);
    }
    positions.push(position);
    previous = position;
  }

  return positions.map((position, index) => {
    const previousPosition = positions[index - 1];
    const nextPosition = positions[index + 1];
    const technique = selectTechnique(previousPosition, position, nextPosition, random);
    const target = technique === 'hammer-on' || technique === 'pull-off' || technique === 'slide'
      ? nextPosition ?? position
      : technique === 'bend'
        ? { ...position, midi: position.midi + 2 }
        : position;

    return {
      ...position,
      technique,
      targetMidi: target.midi,
      targetPositionId: target.id,
      velocity: 82 + Math.floor(random() * 28),
      length: technique === 'vibrato' || technique === 'bend' ? 1.35 : 0.88 + random() * 0.18
    };
  });
}

export function scalePitchClasses(rootMidi, scaleName) {
  return new Set((SCALES[scaleName] ?? SCALES.Blues).map((interval) => (rootMidi + interval) % 12));
}
