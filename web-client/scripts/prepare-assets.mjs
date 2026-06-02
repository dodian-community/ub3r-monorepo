import fs from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const repoRoot = path.resolve(__dirname, "..", "..");
const publicDir = path.resolve(__dirname, "..", "public");
const cacheTargetDir = path.join(publicDir, "cache");
const clientTargetDir = path.join(publicDir, "client");

const explicitJarSource = process.env.MYSTIC_JAR_SOURCE;
const requiredCacheFiles = [
  "main_file_cache.dat",
  "main_file_cache.idx0",
  "main_file_cache.idx1",
  "main_file_cache.idx2",
  "main_file_cache.idx3",
  "main_file_cache.idx4",
  "main_file_cache.idx5",
  "obj.dat",
  "obj.idx",
  "sprites.dat",
  "sprites.idx",
  "tradable.dat",
  "version.txt"
];

async function ensureDir(dirPath) {
  await fs.mkdir(dirPath, { recursive: true });
}

async function listFiles(rootDir) {
  const result = [];

  async function walk(currentDir) {
    const entries = await fs.readdir(currentDir, { withFileTypes: true }).catch(() => []);
    for (const entry of entries) {
      if (entry.name.startsWith(".")) {
        continue;
      }
      const absolutePath = path.join(currentDir, entry.name);
      if (entry.isDirectory()) {
        await walk(absolutePath);
      } else {
        result.push(path.relative(rootDir, absolutePath).replaceAll(path.sep, "/"));
      }
    }
  }

  await walk(rootDir);
  return result.sort();
}

async function resolveJarSource() {
  if (explicitJarSource) {
    return explicitJarSource;
  }

  const libsDir = path.join(repoRoot, "mystic-updatedclient", "build", "libs");
  const entries = await fs.readdir(libsDir, { withFileTypes: true });
  const jars = entries
    .filter((entry) => entry.isFile() && entry.name.endsWith(".jar"))
    .map((entry) => path.join(libsDir, entry.name))
    .sort();

  if (jars.length === 0) {
    throw new Error(`No jar found in ${libsDir}. Build :mystic-updatedclient first.`);
  }

  return jars[jars.length - 1];
}

async function main() {
  await ensureDir(publicDir);
  await ensureDir(clientTargetDir);

  if (!(await fs.stat(cacheTargetDir).catch(() => null))?.isDirectory()) {
    throw new Error(
      `Cache directory not found at ${cacheTargetDir}.\n` +
      `Please manually create this directory and place your cache files in it.`
    );
  }

  const allFiles = await listFiles(cacheTargetDir);
  // Exclude packed_sprites files from manifest indexing
  const cacheFiles = allFiles.filter((file) => !file.startsWith("packed_sprites/"));

  // Validate that required files exist
  for (const requiredFile of requiredCacheFiles) {
    if (!cacheFiles.includes(requiredFile)) {
      throw new Error(
        `Missing required cache file in public/cache: ${requiredFile}\n` +
        `Please ensure all cache files are placed in ${cacheTargetDir}.`
      );
    }
  }

  await fs.writeFile(
    path.join(publicDir, "cache-manifest.json"),
    JSON.stringify({ files: cacheFiles, generatedAt: new Date().toISOString() }, null, 2) + "\n"
  );
  await fs.writeFile(path.join(publicDir, "cache-manifest.txt"), cacheFiles.join("\n") + "\n");

  const jarSource = await resolveJarSource();
  const jarTarget = path.join(clientTargetDir, "mystic-updatedclient.jar");
  await fs.copyFile(jarSource, jarTarget);

  console.log(`[prepare-assets] validated manual cache in ${cacheTargetDir} (excluded packed_sprites)`);
  console.log(`[prepare-assets] copied jar from ${jarSource}`);
}

main().catch((error) => {
  console.error("[prepare-assets] failed", error);
  process.exitCode = 1;
});
