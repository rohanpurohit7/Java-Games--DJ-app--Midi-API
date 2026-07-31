import { copyFile, mkdir, readdir, rename, stat, writeFile } from 'node:fs/promises';
import { existsSync } from 'node:fs';
import { spawnSync } from 'node:child_process';
import path from 'node:path';
import { path7za } from '7zip-bin';

const root = process.cwd();
const publicDir = path.join(root, 'public');
const soundfontDir = path.join(publicDir, 'soundfonts');
const imageDir = path.join(publicDir, 'assets');
const backingDir = path.join(publicDir, 'backing-tracks');

await mkdir(soundfontDir, { recursive: true });
await mkdir(imageDir, { recursive: true });
await mkdir(backingDir, { recursive: true });

const workletSource = path.join(root, 'node_modules', 'spessasynth_lib', 'dist', 'spessasynth_processor.min.js');
if (existsSync(workletSource)) {
  await copyFile(workletSource, path.join(publicDir, 'spessasynth_processor.min.js'));
}

async function download(url, destination) {
  if (existsSync(destination)) return;
  const response = await fetch(url, {
    redirect: 'follow',
    headers: { 'User-Agent': 'AmpStudioGuitarApp asset preparation' }
  });
  if (!response.ok) throw new Error(`Download failed (${response.status}) for ${url}`);
  await writeFile(destination, new Uint8Array(await response.arrayBuffer()));
}

async function findSf2(directory, ignored = new Set()) {
  for (const entry of await readdir(directory)) {
    const full = path.join(directory, entry);
    const details = await stat(full);
    if (details.isDirectory()) {
      const nested = await findSf2(full, ignored);
      if (nested) return nested;
    } else if (entry.toLowerCase().endsWith('.sf2') && !ignored.has(full)) return full;
  }
  return null;
}

async function installSoundFont({ id, url }) {
  const target = path.join(soundfontDir, `${id}.sf2`);
  if (existsSync(target)) return;
  const archive = path.join(soundfontDir, `${id}.7z`);
  const before = new Set();
  if (existsSync(soundfontDir)) {
    for (const entry of await readdir(soundfontDir)) before.add(path.join(soundfontDir, entry));
  }
  console.log(`Downloading ${id} SoundFont...`);
  await download(url, archive);
  const extraction = spawnSync(path7za, ['x', archive, `-o${soundfontDir}`, '-y'], { stdio: 'inherit' });
  if (extraction.status !== 0) throw new Error(`Unable to extract ${id}.`);
  const extracted = await findSf2(soundfontDir, before);
  if (!extracted) throw new Error(`${id} archive did not contain an SF2 file.`);
  if (extracted !== target) await rename(extracted, target);
}

const soundfonts = [
  {
    id: 'freepats-electric-clean',
    url: 'https://freepats.zenvoid.org/ElectricGuitar/FSBS-EGuitar/EGuitarFSBS-bridge-clean-small-SF2-20220911.7z'
  },
  {
    id: 'freepats-electric-jazz',
    url: 'https://freepats.zenvoid.org/ElectricGuitar/FSBS-EGuitar/EGuitarFSBS-bridge-jazz-small-SF2-20220911.7z'
  },
  {
    id: 'freepats-nylon-spanish',
    url: 'https://freepats.zenvoid.org/Guitar/SpanishClassicalGuitar/SpanishClassicalGuitar-SF2-20190618.7z'
  }
];

for (const soundfont of soundfonts) await installSoundFont(soundfont);

const commons = (filename) => `https://commons.wikimedia.org/wiki/Special:Redirect/file/${encodeURIComponent(filename)}`;
const backingTracks = [
  ['funk-soul.ogg', 'Funk Soul.ogg'],
  ['e-blues.ogg', 'E Blues by Michael Huber.ogg'],
  ['blues-accompaniment.ogg', 'Blues accompanyment.ogg'],
  ['jazz-at-the-park.ogg', 'Jazz at the park.ogg'],
  ['jazz-guitar.ogg', 'Jazz-Guitar.ogg'],
  ['static.ogg', 'Mise - 07 - Static.ogg'],
  ['jazz-piano.ogg', 'Jazz Piano.ogg'],
  ['shumi-piano.ogg', 'Shumi-Marista Piano Instrumental.ogg']
];

for (const [localName, remoteName] of backingTracks) {
  console.log(`Preparing backing track ${localName}...`);
  await download(commons(remoteName), path.join(backingDir, localName));
}

const guitarImage = path.join(imageDir, 'studio-electric-guitar.jpg');
if (!existsSync(guitarImage)) {
  await download(
    'https://commons.wikimedia.org/wiki/Special:Redirect/file/Fender_stratocaster_black.jpg?width=1800',
    guitarImage
  );
}

console.log('Backing tracks, guitar SoundFonts, audio worklet, and images are ready.');
