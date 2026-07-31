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

const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));
const USER_AGENT = 'AmpStudioGuitarApp/6.1 asset-preparer (https://github.com/rohanpurohit7/AmpStudioGuitarApp)';

async function download(url, destination, { attempts = 7, required = true } = {}) {
  if (existsSync(destination)) return true;

  let lastError;
  for (let attempt = 1; attempt <= attempts; attempt += 1) {
    try {
      const response = await fetch(url, {
        redirect: 'follow',
        headers: {
          'User-Agent': USER_AGENT,
          Accept: 'audio/ogg,audio/*,application/octet-stream,image/*;q=0.9,*/*;q=0.5'
        }
      });

      if (response.ok) {
        await writeFile(destination, new Uint8Array(await response.arrayBuffer()));
        return true;
      }

      const retryable = response.status === 429 || response.status === 503 || response.status === 502 || response.status === 504;
      if (!retryable) throw new Error(`Download failed (${response.status}) for ${url}`);

      const retryAfter = Number(response.headers.get('retry-after'));
      const backoffMs = Number.isFinite(retryAfter) && retryAfter > 0
        ? retryAfter * 1000
        : Math.min(30000, 1800 * (2 ** (attempt - 1)) + Math.floor(Math.random() * 750));

      console.warn(`HTTP ${response.status} while downloading ${path.basename(destination)}. Retrying in ${Math.ceil(backoffMs / 1000)}s (${attempt}/${attempts})...`);
      await sleep(backoffMs);
    } catch (error) {
      lastError = error;
      if (attempt < attempts) {
        const backoffMs = Math.min(30000, 1800 * (2 ** (attempt - 1)) + Math.floor(Math.random() * 750));
        console.warn(`${error.message}. Retrying in ${Math.ceil(backoffMs / 1000)}s (${attempt}/${attempts})...`);
        await sleep(backoffMs);
      }
    }
  }

  const message = lastError?.message ?? `Unable to download ${url}`;
  if (required) throw new Error(message);
  console.warn(`Skipping optional asset: ${path.basename(destination)}. ${message}`);
  return false;
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
  await download(url, archive, { required: true });
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

const availableTracks = [];
for (const [localName, remoteName] of backingTracks) {
  const destination = path.join(backingDir, localName);
  console.log(`Preparing backing track ${localName}...`);
  const available = await download(commons(remoteName), destination, { required: false });
  if (available || existsSync(destination)) availableTracks.push(localName);
  // Commons rate-limits bursts even when requests are sequential.
  await sleep(2500);
}

await writeFile(
  path.join(backingDir, 'availability.json'),
  `${JSON.stringify({ generatedAt: new Date().toISOString(), tracks: availableTracks }, null, 2)}\n`
);

const guitarImage = path.join(imageDir, 'studio-electric-guitar.jpg');
if (!existsSync(guitarImage)) {
  await sleep(2500);
  await download(
    'https://commons.wikimedia.org/wiki/Special:Redirect/file/Fender_stratocaster_black.jpg?width=1800',
    guitarImage,
    { required: false }
  );
}

console.log(`Assets ready. ${availableTracks.length}/${backingTracks.length} built-in backing tracks are available.`);
if (availableTracks.length < backingTracks.length) {
  console.log('Run "npm run assets" again later to retry any tracks skipped because of remote rate limiting.');
}
