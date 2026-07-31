import { copyFile, mkdir, readdir, rename, stat, writeFile } from 'node:fs/promises';
import { existsSync } from 'node:fs';
import { spawnSync } from 'node:child_process';
import path from 'node:path';
import { path7za } from '7zip-bin';

const root = process.cwd();
const publicDir = path.join(root, 'public');
const soundfontDir = path.join(publicDir, 'soundfonts');
const imageDir = path.join(publicDir, 'assets');
const sf2Target = path.join(soundfontDir, 'freepats-clean-electric-guitar.sf2');
const archivePath = path.join(soundfontDir, 'freepats-clean-electric-guitar.7z');
const guitarImage = path.join(imageDir, 'studio-electric-guitar.jpg');

await mkdir(soundfontDir, { recursive: true });
await mkdir(imageDir, { recursive: true });

const workletSource = path.join(root, 'node_modules', 'spessasynth_lib', 'dist', 'spessasynth_processor.min.js');
if (existsSync(workletSource)) {
  await copyFile(workletSource, path.join(publicDir, 'spessasynth_processor.min.js'));
}

async function download(url, destination) {
  const response = await fetch(url, { redirect: 'follow' });
  if (!response.ok) throw new Error(`Download failed (${response.status}) for ${url}`);
  await writeFile(destination, new Uint8Array(await response.arrayBuffer()));
}

async function findSf2(directory) {
  for (const entry of await readdir(directory)) {
    const full = path.join(directory, entry);
    const details = await stat(full);
    if (details.isDirectory()) {
      const nested = await findSf2(full);
      if (nested) return nested;
    } else if (entry.toLowerCase().endsWith('.sf2')) return full;
  }
  return null;
}

if (!existsSync(sf2Target)) {
  const url = 'https://freepats.zenvoid.org/ElectricGuitar/FSBS-EGuitar/EGuitarFSBS-bridge-clean-small-SF2-20220911.7z';
  console.log('Downloading CC0 FreePats clean electric guitar SoundFont...');
  await download(url, archivePath);
  const extraction = spawnSync(path7za, ['x', archivePath, `-o${soundfontDir}`, '-y'], { stdio: 'inherit' });
  if (extraction.status !== 0) throw new Error('Unable to extract the FreePats archive.');
  const extracted = await findSf2(soundfontDir);
  if (!extracted) throw new Error('The FreePats archive did not contain an SF2 file.');
  if (extracted !== sf2Target) await rename(extracted, sf2Target);
}

if (!existsSync(guitarImage)) {
  console.log('Downloading licensed guitar photograph...');
  await download(
    'https://commons.wikimedia.org/wiki/Special:Redirect/file/Fender_stratocaster_black.jpg?width=1800',
    guitarImage
  );
}

console.log('Web audio and image assets are ready.');
