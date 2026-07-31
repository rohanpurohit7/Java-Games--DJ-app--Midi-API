export const BACKING_TRACKS = [
  {
    id: 'funk-soul', title: 'Funk Soul', artist: 'Wiki Learning Tec de Monterrey', genre: 'Soul Funk',
    key: 'A', mode: 'minor', bpm: 96, meter: '4/4', feel: 'neo-soul',
    audioUrl: '/backing-tracks/funk-soul.ogg', guitar: 'electric-clean',
    license: 'CC BY-SA 4.0', source: 'https://commons.wikimedia.org/wiki/File:Funk_Soul.ogg',
    chords: ['Am9', 'Dm9', 'G13', 'Cmaj9']
  },
  {
    id: 'e-blues', title: 'E Blues', artist: 'Michael Huber', genre: 'Slow Piano Blues',
    key: 'E', mode: 'minor', bpm: 72, meter: '4/4', feel: 'slow-blues',
    audioUrl: '/backing-tracks/e-blues.ogg', guitar: 'electric-jazz',
    license: 'CC BY-SA 3.0', source: 'https://commons.wikimedia.org/wiki/File:E_Blues_by_Michael_Huber.ogg',
    chords: ['E7', 'A7', 'E7', 'B7']
  },
  {
    id: 'blues-accompaniment', title: 'Blues Accompaniment', artist: 'Jason M. C. Han', genre: 'Blues Practice Loop',
    key: 'A', mode: 'minor', bpm: 80, meter: '4/4', feel: 'laid-back',
    audioUrl: '/backing-tracks/blues-accompaniment.ogg', guitar: 'electric-clean',
    license: 'CC BY-SA 4.0', source: 'https://commons.wikimedia.org/wiki/File:Blues_accompanyment.ogg',
    chords: ['A7', 'D7', 'A7', 'E7']
  },
  {
    id: 'jazz-at-the-park', title: 'Jazz at the Park', artist: 'Manwithmetalpig', genre: 'Contemporary Jazz',
    key: 'C', mode: 'major', bpm: 92, meter: '4/4', feel: 'jazz-rnb',
    audioUrl: '/backing-tracks/jazz-at-the-park.ogg', guitar: 'electric-jazz',
    license: 'CC0 1.0', source: 'https://commons.wikimedia.org/wiki/File:Jazz_at_the_park.ogg',
    chords: ['Cmaj7', 'Am7', 'Dm9', 'G13']
  },
  {
    id: 'jazz-guitar', title: 'Jazz Guitar', artist: 'Serolillo', genre: 'Jazz Guitar Loop',
    key: 'C', mode: 'major', bpm: 88, meter: '4/4', feel: 'laid-back',
    audioUrl: '/backing-tracks/jazz-guitar.ogg', guitar: 'electric-jazz',
    license: 'CC BY 2.5', source: 'https://commons.wikimedia.org/wiki/File:Jazz-Guitar.ogg',
    chords: ['Cmaj7', 'A7', 'Dm7', 'G7']
  },
  {
    id: 'static', title: 'Static', artist: 'Mise', genre: 'Atmospheric Soul Groove',
    key: 'D', mode: 'minor', bpm: 84, meter: '4/4', feel: 'soul-trap',
    audioUrl: '/backing-tracks/static.ogg', guitar: 'electric-clean',
    license: 'CC BY 4.0', source: 'https://commons.wikimedia.org/wiki/File:Mise_-_07_-_Static.ogg',
    chords: ['Dm9', 'Bbmaj7', 'Fmaj7', 'Cadd9']
  },
  {
    id: 'jazz-piano', title: 'Jazz Piano in E-flat', artist: 'Serolillo', genre: 'Jazz Piano Loop',
    key: 'D#', mode: 'major', bpm: 78, meter: '4/4', feel: 'jazz-rnb',
    audioUrl: '/backing-tracks/jazz-piano.ogg', guitar: 'electric-jazz',
    license: 'CC BY 2.5', source: 'https://commons.wikimedia.org/wiki/File:Jazz_Piano.ogg',
    chords: ['Ebmaj7', 'Cm7', 'Fm7', 'Bb7']
  },
  {
    id: 'shumi-piano', title: 'Shumi Piano', artist: 'Parchokhalq', genre: 'Soulful Piano',
    key: 'A', mode: 'minor', bpm: 68, meter: '4/4', feel: 'slow-blues',
    audioUrl: '/backing-tracks/shumi-piano.ogg', guitar: 'nylon-spanish',
    license: 'CC0 1.0', source: 'https://commons.wikimedia.org/wiki/File:Shumi-Marista_Piano_Instrumental.ogg',
    chords: ['Am', 'Fmaj7', 'C', 'G']
  }
];

export const GUITAR_INSTRUMENTS = [
  {
    id: 'electric-clean', name: 'Clean Electric', family: 'Electric',
    soundfontUrl: '/soundfonts/freepats-electric-clean.sf2', imageUrl: '/assets/studio-electric-guitar.jpg',
    description: 'Bright sampled electric guitar with clean amplifier processing.'
  },
  {
    id: 'electric-jazz', name: 'Jazz Electric', family: 'Electric',
    soundfontUrl: '/soundfonts/freepats-electric-jazz.sf2', imageUrl: '/assets/studio-electric-guitar.jpg',
    description: 'Warmer clean electric bank voiced for jazz and soul phrasing.'
  },
  {
    id: 'nylon-spanish', name: 'Spanish Nylon', family: 'Nylon',
    soundfontUrl: '/soundfonts/freepats-nylon-spanish.sf2', imageUrl: '/assets/studio-electric-guitar.jpg',
    description: 'CC0 Spanish classical nylon-string guitar samples.'
  }
];

export function suggestedScale(track) {
  if (track.feel.includes('funk')) return track.mode === 'major' ? 'Mixolydian' : 'Dorian';
  if (track.feel.includes('jazz') || track.feel.includes('soul')) return track.mode === 'major' ? 'Major Pentatonic' : 'Dorian';
  return track.mode === 'major' ? 'Major Pentatonic' : 'Blues';
}
