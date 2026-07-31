const OPEN_STRINGS = [40, 45, 50, 55, 59, 64];

export const SCALES = {
  'Minor Pentatonic': [0, 3, 5, 7, 10],
  Blues: [0, 3, 5, 6, 7, 10],
  'Major Pentatonic': [0, 2, 4, 7, 9],
  Dorian: [0, 2, 3, 5, 7, 9, 10],
  Mixolydian: [0, 2, 4, 5, 7, 9, 10],
  'Natural Minor': [0, 2, 3, 5, 7, 8, 10]
};

const GROOVES = {
  'slow-blues': [
    { wait: 0, duration: 1.4 }, { wait: .6, duration: .55 }, { wait: .2, duration: .65 },
    { wait: .9, duration: 1.7 }, { wait: .8, duration: .5 }, { wait: .25, duration: 1.35 }
  ],
  'laid-back': [
    { wait: .25, duration: .8 }, { wait: .45, duration: .55 }, { wait: .3, duration: 1.1 },
    { wait: .75, duration: .6 }, { wait: .35, duration: 1.4 }
  ],
  'neo-soul': [
    { wait: .5, duration: .55 }, { wait: .2, duration: .5 }, { wait: .45, duration: .9 },
    { wait: .75, duration: .55 }, { wait: .2, duration: 1.25 }
  ],
  funk: [
    { wait: .25, duration: .28 }, { wait: .25, duration: .32 }, { wait: .5, duration: .25 },
    { wait: .25, duration: .35 }, { wait: .75, duration: .45 }
  ],
  swing: [
    { wait: 0, duration: .65 }, { wait: .33, duration: .32 }, { wait: .67, duration: .6 },
    { wait: .33, duration: .35 }, { wait: .67, duration: .9 }
  ],
  'soul-waltz': [
    { wait: 0, duration: 1.15 }, { wait: .7, duration: .55 }, { wait: .3, duration: .85 },
    { wait: .8, duration: 1.4 }
  ],
  default: [
    { wait: .25, duration: .65 }, { wait: .25, duration: .5 }, { wait: .5, duration: .85 },
    { wait: .75, duration: .55 }, { wait: .25, duration: 1.15 }
  ]
};

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
    return position.fret >= minFret && intervals.includes(degree) && position.midi >= 52 && position.midi <= 76;
  });
}

function seeded(seed) {
  let state = seed >>> 0;
  return () => {
    state = (state * 1664525 + 1013904223) >>> 0;
    return state / 4294967296;
  };
}

function chooseNear(candidates, previous, desiredDegree, rootMidi, random) {
  const ranked = candidates.map((position) => {
    const movement = previous
      ? Math.abs(position.fret - previous.fret) + Math.abs(position.stringIndex - previous.stringIndex) * 2.1
      : Math.abs(position.fret - 7);
    const degree = ((position.midi - rootMidi) % 12 + 12) % 12;
    const harmonic = desiredDegree == null ? 0 : Math.min(Math.abs(degree - desiredDegree), 12 - Math.abs(degree - desiredDegree)) * 1.8;
    return { position, score: movement + harmonic + random() * 1.4 };
  }).sort((a, b) => a.score - b.score);
  return ranked[Math.floor(random() * Math.min(3, ranked.length))].position;
}

function chooseTechnique(previous, current, isEnding, random) {
  if (!previous) return 'pick';
  const move = current.midi - previous.midi;
  const sameString = current.stringIndex === previous.stringIndex;
  if (isEnding && random() > .25) return Math.abs(move) <= 2 ? 'vibrato' : 'bend';
  if (sameString && move > 0 && move <= 3) return 'hammer-on';
  if (sameString && move < 0 && move >= -3) return 'pull-off';
  if (sameString && Math.abs(move) >= 3 && Math.abs(move) <= 5) return 'slide';
  if (move === 2 && random() > .62) return 'bend';
  if (random() > .82) return 'vibrato';
  return 'pick';
}

function patternFor(feel = 'default') {
  if (GROOVES[feel]) return GROOVES[feel];
  if (feel.includes('funk')) return GROOVES.funk;
  if (feel.includes('soul') || feel.includes('rnb')) return GROOVES['neo-soul'];
  if (feel.includes('waltz')) return GROOVES['soul-waltz'];
  if (feel.includes('swing') || feel.includes('boogie')) return GROOVES.swing;
  if (feel.includes('slow')) return GROOVES['slow-blues'];
  return GROOVES.default;
}

export function generateLick({ rootMidi = 57, scaleName = 'Blues', length = 8, seed = Date.now(), feel = 'slow-blues', mode = 'minor' } = {}) {
  const random = seeded(seed);
  const intervals = SCALES[scaleName] ?? SCALES.Blues;
  const candidates = candidatePositions(rootMidi, intervals);
  const rhythm = patternFor(feel);
  const count = Math.max(5, Math.min(length, 10));
  const positions = [];
  let previous = null;

  // Strong phrase points resolve to root, third or fifth; passing notes fill the spaces.
  const third = mode === 'major' ? 4 : 3;
  const targets = [0, null, 7, null, third, null, 7, 0, null, 0];

  for (let index = 0; index < count; index += 1) {
    const desired = targets[index % targets.length];
    let position = chooseNear(candidates, previous, desired, rootMidi, random);
    if (previous && position.id === previous.id) {
      position = chooseNear(candidates.filter((candidate) => candidate.id !== previous.id), previous, desired, rootMidi, random);
    }
    positions.push(position);
    previous = position;
  }

  return positions.map((position, index) => {
    const previousPosition = positions[index - 1];
    const nextPosition = positions[index + 1];
    const isEnding = index === positions.length - 1 || index === Math.floor(positions.length / 2) - 1;
    const technique = chooseTechnique(previousPosition, position, isEnding, random);
    const rhythmCell = rhythm[index % rhythm.length];
    const target = ['hammer-on', 'pull-off', 'slide'].includes(technique)
      ? nextPosition ?? position
      : technique === 'bend' ? { ...position, midi: position.midi + 2 } : position;

    const accent = index === 0 || isEnding || index % 4 === 0;
    return {
      ...position,
      technique,
      targetMidi: target.midi,
      targetPositionId: target.id,
      waitBeats: Math.max(0, rhythmCell.wait + (random() - .5) * .06),
      durationBeats: rhythmCell.duration * (technique === 'vibrato' || technique === 'bend' ? 1.25 : 1),
      velocity: Math.round((accent ? 102 : 84) + (random() - .5) * 12),
      phraseEnd: isEnding
    };
  });
}

export function scalePitchClasses(rootMidi, scaleName) {
  return new Set((SCALES[scaleName] ?? SCALES.Blues).map((interval) => (rootMidi + interval) % 12));
}
