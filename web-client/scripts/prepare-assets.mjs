import fs from "node:fs/promises";
import path from "node:path";
import { fileURLToPath } from "node:url";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const repoRoot = path.resolve(__dirname, "..", "..");
const publicDir = path.resolve(__dirname, "..", "public");
const cacheTargetDir = path.join(publicDir, "cache");
const clientTargetDir = path.join(publicDir, "client");

const cacheSourceDir = process.env.MYSTIC_CACHE_SOURCE || "/Users/alexismaneely/Dodian-Exorth317";
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
    const entries = await fs.readdir(currentDir, { withFileTypes: true });
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

async function validateCacheSource() {
  const cacheFiles = await listFiles(cacheSourceDir);
  for (const requiredFile of requiredCacheFiles) {
    if (!cacheFiles.includes(requiredFile)) {
      throw new Error(`Missing required cache file: ${requiredFile}`);
    }
  }
  return cacheFiles;
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
  await fs.rm(cacheTargetDir, { recursive: true, force: true });
  await ensureDir(cacheTargetDir);

  const cacheFiles = await validateCacheSource();
  for (const relativeFile of cacheFiles) {
    const sourceFile = path.join(cacheSourceDir, relativeFile);
    const targetFile = path.join(cacheTargetDir, relativeFile);
    await ensureDir(path.dirname(targetFile));
    await fs.copyFile(sourceFile, targetFile);
  }
  await fs.writeFile(
    path.join(publicDir, "cache-manifest.json"),
    JSON.stringify({ files: cacheFiles, generatedAt: new Date().toISOString() }, null, 2) + "\n"
  );
  await fs.writeFile(path.join(publicDir, "cache-manifest.txt"), cacheFiles.join("\n") + "\n");

  const jarSource = await resolveJarSource();
  const jarTarget = path.join(clientTargetDir, "mystic-updatedclient.jar");
  await fs.copyFile(jarSource, jarTarget);

  console.log(`[prepare-assets] copied cache from ${cacheSourceDir}`);
  console.log(`[prepare-assets] copied jar from ${jarSource}`);
}

main().catch((error) => {
  console.error("[prepare-assets] failed", error);
  process.exitCode = 1;
});
