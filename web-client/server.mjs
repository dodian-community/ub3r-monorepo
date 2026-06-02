import fs from "node:fs";
import fsp from "node:fs/promises";
import childProcess from "node:child_process";
import https from "node:https";
import net from "node:net";
import path from "node:path";
import { fileURLToPath } from "node:url";
import { WebSocketServer } from "ws";

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);
const publicDir = path.join(__dirname, "public");
const envFilePath = path.join(__dirname, ".env");

loadDotEnv(envFilePath);

const bindHost = process.env.WEB_BIND_HOST || "0.0.0.0";
const httpsPort = Number.parseInt(process.env.WEB_HTTPS_PORT || "8443", 10);
const publicHost = process.env.WEB_PUBLIC_HOST || "localhost";
const gameUpstreamHost = process.env.GAME_UPSTREAM_HOST || "127.0.0.1";
const gameUpstreamPort = Number.parseInt(process.env.GAME_UPSTREAM_PORT || "43594", 10);
const tlsCertPath = resolveRuntimePath(process.env.TLS_CERT_PATH || ".runtime-certs/localhost.crt");
const tlsKeyPath = resolveRuntimePath(process.env.TLS_KEY_PATH || ".runtime-certs/localhost.key");
const tlsAutoGenerate = String(process.env.TLS_AUTO_GENERATE || "true").toLowerCase() === "true";

if (!tlsCertPath || !tlsKeyPath) {
  throw new Error("TLS_CERT_PATH and TLS_KEY_PATH are required.");
}

ensureTlsFiles();

if (!fs.existsSync(tlsCertPath) || !fs.existsSync(tlsKeyPath)) {
  throw new Error(`TLS files not found. cert=${tlsCertPath} key=${tlsKeyPath}`);
}

const tlsOptions = {
  cert: fs.readFileSync(tlsCertPath),
  key: fs.readFileSync(tlsKeyPath)
};

const requiredAssets = [
  path.join(publicDir, "index.html"),
  path.join(publicDir, "client.js"),
  path.join(publicDir, "cache-manifest.txt"),
  path.join(publicDir, "client", "mystic-updatedclient.jar"),
  path.join(publicDir, "cache", "main_file_cache.dat"),
  path.join(publicDir, "cache", "main_file_cache.idx0")
];

const mimeTypes = new Map([
  [".css", "text/css; charset=utf-8"],
  [".html", "text/html; charset=utf-8"],
  [".js", "text/javascript; charset=utf-8"],
  [".json", "application/json; charset=utf-8"],
  [".jar", "application/java-archive"],
  [".dat", "application/octet-stream"],
  [".idx", "application/octet-stream"],
  [".txt", "text/plain; charset=utf-8"]
]);

function contentTypeFor(filePath) {
  return mimeTypes.get(path.extname(filePath).toLowerCase()) || "application/octet-stream";
}

function loadDotEnv(filePath) {
  if (!fs.existsSync(filePath)) {
    return;
  }

  const lines = fs.readFileSync(filePath, "utf8").split(/\r?\n/);
  for (const rawLine of lines) {
    const line = rawLine.trim();
    if (!line || line.startsWith("#")) {
      continue;
    }
    const separatorIndex = line.indexOf("=");
    if (separatorIndex <= 0) {
      continue;
    }
    const key = line.slice(0, separatorIndex).trim();
    let value = line.slice(separatorIndex + 1).trim();
    if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
      value = value.slice(1, -1);
    }
    if (process.env[key] == null || process.env[key] === "") {
      process.env[key] = value;
    }
  }
}

function resolveRuntimePath(filePath) {
  if (!filePath) {
    return filePath;
  }
  if (path.isAbsolute(filePath)) {
    return filePath;
  }
  return path.resolve(__dirname, filePath);
}

function ensureTlsFiles() {
  if (fs.existsSync(tlsCertPath) && fs.existsSync(tlsKeyPath)) {
    return;
  }
  if (!tlsAutoGenerate) {
    return;
  }

  fs.mkdirSync(path.dirname(tlsCertPath), { recursive: true });
  fs.mkdirSync(path.dirname(tlsKeyPath), { recursive: true });
  try {
    childProcess.execFileSync(
      "openssl",
      [
        "req",
        "-x509",
        "-nodes",
        "-newkey",
        "rsa:2048",
        "-keyout",
        tlsKeyPath,
        "-out",
        tlsCertPath,
        "-subj",
        `/CN=${publicHost}`,
        "-days",
        "30"
      ],
      { stdio: "ignore" }
    );
  } catch (error) {
    console.error(`[web-client] ERROR: Failed to automatically generate self-signed TLS certificates using 'openssl'.`);
    if (error && error.code === "ENOENT") {
      console.error(`[web-client] ERROR: 'openssl' executable was not found in your system's PATH.\n  - Windows users: please install OpenSSL (e.g. via winget or git-bash) and add it to your system PATH.\n  - Or disable TLS_AUTO_GENERATE in your .env and place pre-generated certs at:\n    Cert: ${tlsCertPath}\n    Key: ${tlsKeyPath}`);
    } else {
      console.error("[web-client] ERROR:", error.message || error);
    }
    process.exit(1);
  }
}

function validateGeneratedAssets() {
  const missingAssets = requiredAssets.filter((assetPath) => !fs.existsSync(assetPath));
  if (missingAssets.length > 0) {
    throw new Error(
      `Missing generated web-client assets:\n${missingAssets.join("\n")}\nRun the web-client build before starting the service.`
    );
  }
}

function resolveStaticPath(urlPathname) {
  const normalized = urlPathname === "/" ? "/index.html" : urlPathname;
  const candidate = path.normalize(path.join(publicDir, normalized));
  if (!candidate.startsWith(publicDir)) {
    return null;
  }
  return candidate;
}

async function serveStatic(req, res) {
  const url = new URL(req.url, `https://${req.headers.host || "localhost"}`);

  if (url.pathname === "/healthz") {
    res.writeHead(200, { "content-type": "application/json; charset=utf-8" });
    res.end(JSON.stringify({ ok: true }));
    return;
  }

  const filePath = resolveStaticPath(url.pathname);
  if (!filePath) {
    res.writeHead(403);
    res.end("Forbidden");
    return;
  }

  try {
    const stats = await fsp.stat(filePath);
    if (!stats.isFile()) {
      res.writeHead(404);
      res.end("Not found");
      return;
    }

    const rangeHeader = req.headers.range;
    if (rangeHeader) {
      const match = rangeHeader.match(/^bytes=(\d+)-(\d+)?$/);
      if (match) {
        const start = parseInt(match[1], 10);
        const end = match[2] ? parseInt(match[2], 10) : stats.size - 1;

        if (start >= stats.size || end >= stats.size || start > end) {
          res.writeHead(416, {
            "content-range": `bytes */${stats.size}`,
            "accept-ranges": "bytes",
            "cache-control": "no-cache"
          });
          res.end();
          return;
        }

        res.writeHead(206, {
          "content-range": `bytes ${start}-${end}/${stats.size}`,
          "accept-ranges": "bytes",
          "content-length": end - start + 1,
          "content-type": contentTypeFor(filePath),
          "cache-control": filePath.endsWith(".html") ? "no-cache" : "public, max-age=300"
        });
        fs.createReadStream(filePath, { start, end }).pipe(res);
        return;
      }
    }

    res.writeHead(200, {
      "content-length": stats.size,
      "content-type": contentTypeFor(filePath),
      "accept-ranges": "bytes",
      "cache-control": filePath.endsWith(".html") ? "no-cache" : "public, max-age=300"
    });
    fs.createReadStream(filePath).pipe(res);
  } catch (error) {
    if (error && error.code === "ENOENT") {
      res.writeHead(404);
      res.end("Not found");
      return;
    }
    res.writeHead(500);
    res.end("Internal server error");
  }
}

function upstreamForRoute(pathname) {
  if (pathname === "/gamews/game") {
    return { host: gameUpstreamHost, port: gameUpstreamPort };
  }
  return null;
}

validateGeneratedAssets();
console.log(`[web-client] bind host ${bindHost}`);
console.log(`[web-client] public dir ${publicDir}`);
console.log(`[web-client] public host ${publicHost}`);
console.log(`[web-client] tls cert ${tlsCertPath}`);

const server = https.createServer(tlsOptions, (req, res) => {
  serveStatic(req, res).catch((error) => {
    console.error("[web-client] request failure", error);
    res.writeHead(500);
    res.end("Internal server error");
  });
});

const wss = new WebSocketServer({ noServer: true, perMessageDeflate: false });

wss.on("connection", (ws, req, upstream) => {
  const socket = net.createConnection(upstream.port, upstream.host);
  socket.setNoDelay(true);

  const closeBoth = () => {
    if (ws.readyState < 2) {
      ws.close();
    }
    if (!socket.destroyed) {
      socket.destroy();
    }
  };

  socket.on("connect", () => {
    console.log(`[web-client] bridge ${req.url} -> ${upstream.host}:${upstream.port}`);
  });

  socket.on("data", (chunk) => {
    if (ws.readyState === 1) {
      ws.send(chunk, { binary: true });
    }
  });

  socket.on("error", (error) => {
    console.error(`[web-client] upstream error ${req.url}`, error);
    closeBoth();
  });

  socket.on("close", () => {
    closeBoth();
  });

  ws.on("message", (data, isBinary) => {
    const payload = isBinary ? data : Buffer.from(data);
    if (!socket.destroyed) {
      socket.write(payload);
    }
  });

  ws.on("close", () => {
    if (!socket.destroyed) {
      socket.end();
    }
  });

  ws.on("error", (error) => {
    console.error(`[web-client] websocket error ${req.url}`, error);
    closeBoth();
  });
});

server.on("upgrade", (req, socket, head) => {
  const url = new URL(req.url, `https://${req.headers.host || "localhost"}`);
  const upstream = upstreamForRoute(url.pathname);
  if (!upstream) {
    socket.write("HTTP/1.1 404 Not Found\r\n\r\n");
    socket.destroy();
    return;
  }

  wss.handleUpgrade(req, socket, head, (ws) => {
    wss.emit("connection", ws, req, upstream);
  });
});

server.listen(httpsPort, bindHost, () => {
  console.log(`[web-client] listening on https://${bindHost}:${httpsPort}`);
  console.log(`[web-client] game upstream ${gameUpstreamHost}:${gameUpstreamPort}`);
});
