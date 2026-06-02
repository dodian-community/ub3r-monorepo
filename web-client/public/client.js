const statusEl = document.getElementById("status");
const displayEl = document.getElementById("display");
const consoleEl = document.getElementById("console");

const bridgeBase = "/gamews";
const clientWidth = 765;
const clientHeight = 503;
const jarPath = "/app/client/mystic-updatedclient.jar";

const originalConsole = {
  error: console.error.bind(console),
  info: console.info.bind(console),
  log: console.log.bind(console),
  warn: console.warn.bind(console)
};

function formatLogPart(value) {
  if (value instanceof Error) {
    return value.stack || value.message;
  }
  if (typeof value === "string") {
    return value;
  }
  try {
    return JSON.stringify(value);
  } catch {
    return String(value);
  }
}

function appendLog(level, ...parts) {
  const line = `[${new Date().toISOString()}] ${parts.map(formatLogPart).join(" ")}`;
  const row = document.createElement("div");
  row.className = `log-${level}`;
  row.textContent = line;
  consoleEl.appendChild(row);
  consoleEl.scrollTop = consoleEl.scrollHeight;
}

console.log = (...parts) => {
  originalConsole.log(...parts);
  appendLog("info", ...parts);
};
console.info = (...parts) => {
  originalConsole.info(...parts);
  appendLog("info", ...parts);
};
console.warn = (...parts) => {
  originalConsole.warn(...parts);
  appendLog("warn", ...parts);
};
console.error = (...parts) => {
  originalConsole.error(...parts);
  appendLog("error", ...parts);
};

window.addEventListener("error", (event) => {
  console.error("window error", event.message, event.error || "");
});

window.addEventListener("unhandledrejection", (event) => {
  console.error("unhandled rejection", event.reason || "");
});

function setStatus(message, isError = false) {
  statusEl.textContent = message;
  statusEl.classList.toggle("is-error", isError);
  console.log("status", message);
}

function toWsUrl(route) {
  const protocol = window.location.protocol === "https:" ? "wss:" : "ws:";
  return `${protocol}//${window.location.host}${route}`;
}

function routeFor(url) {
  return `${bridgeBase}/game`;
}

const bridge = (() => {
  let nextId = 1;
  const sockets = new Map();

  function bytesToBase64(bytes) {
    let binary = "";
    for (let index = 0; index < bytes.length; index += 1) {
      binary += String.fromCharCode(bytes[index] & 255);
    }
    return btoa(binary);
  }

  function base64ToBytes(base64) {
    const binary = atob(String(base64));
    const bytes = new Uint8Array(binary.length);
    for (let index = 0; index < binary.length; index += 1) {
      bytes[index] = binary.charCodeAt(index);
    }
    return bytes;
  }

  function resolveRead(socket, payload) {
    const waiter = socket.waiters.shift();
    if (waiter) {
      waiter(payload);
      return true;
    }
    return false;
  }

  function drain(socket) {
    while (socket.queue.length > 0 && socket.bulkWaiters.length > 0) {
      const waiter = socket.bulkWaiters.shift();
      const count = Math.min(waiter.maxBytes, socket.queue.length);
      waiter.resolve(bytesToBase64(socket.queue.splice(0, count)));
    }
    while (socket.queue.length > 0 && socket.waiters.length > 0) {
      resolveRead(socket, socket.queue.shift());
    }
  }

  function closeSocket(socket) {
    socket.closed = true;
    while (socket.waiters.length > 0) {
      resolveRead(socket, -1);
    }
    while (socket.bulkWaiters.length > 0) {
      socket.bulkWaiters.shift().resolve("");
    }
  }

  return {
    open(url) {
      return new Promise((resolve, reject) => {
        const route = routeFor(url);
        const wsUrl = toWsUrl(route);
        console.log("bridge open", url, "->", wsUrl);
        const ws = new WebSocket(wsUrl);
        ws.binaryType = "arraybuffer";

        const socket = {
          id: nextId++,
          ws,
          queue: [],
          waiters: [],
          bulkWaiters: [],
          closed: false,
          route
        };

        sockets.set(socket.id, socket);

        ws.addEventListener(
          "open",
          () => {
            console.log("bridge open ok", route, "socket", socket.id);
            resolve(socket.id);
          },
          { once: true }
        );
        ws.addEventListener(
          "error",
          () => {
            console.error("bridge open failed", route);
            reject(new Error(`Bridge open failed for ${route}`));
          },
          { once: true }
        );
        ws.addEventListener("message", (event) => {
          const bytes = new Uint8Array(event.data);
          for (const byte of bytes) {
            socket.queue.push(byte);
          }
          drain(socket);
        });
        ws.addEventListener("close", (event) => {
          console.warn("bridge closed", route, "code", event.code, "clean", event.wasClean);
          sockets.delete(socket.id);
          closeSocket(socket);
        });
      });
    },
    readByte(id) {
      const socket = sockets.get(Number(id));
      if (!socket) {
        return -1;
      }
      if (socket.queue.length > 0) {
        return socket.queue.shift();
      }
      if (socket.closed) {
        return -1;
      }
      return new Promise((resolve) => socket.waiters.push(resolve));
    },
    readBase64(id, maxBytes) {
      const socket = sockets.get(Number(id));
      if (!socket) {
        return "";
      }
      const limit = Math.max(1, Math.min(Number(maxBytes) || 1, 65536));
      if (socket.queue.length > 0) {
        const count = Math.min(limit, socket.queue.length);
        return bytesToBase64(socket.queue.splice(0, count));
      }
      if (socket.closed) {
        return "";
      }
      return new Promise((resolve) => socket.bulkWaiters.push({ maxBytes: limit, resolve }));
    },
    available(id) {
      const socket = sockets.get(Number(id));
      return socket ? socket.queue.length : 0;
    },
    writeBase64(id, base64) {
      const socket = sockets.get(Number(id));
      if (!socket || socket.closed || socket.ws.readyState !== WebSocket.OPEN) {
        console.warn("bridge write skipped", id, "socket not open");
        return;
      }
      socket.ws.send(base64ToBytes(base64));
    },
    close(id) {
      const socket = sockets.get(Number(id));
      if (!socket) {
        return;
      }
      console.log("bridge close requested", socket.route, "socket", socket.id);
      sockets.delete(Number(id));
      closeSocket(socket);
      try {
        socket.ws.close();
      } catch (error) {
        console.error("bridge close failed", error);
      }
    }
  };
})();

function nativeMethods() {
  const open = async (_lib, url) => bridge.open(url);
  const readByte = async (_lib, id) => bridge.readByte(id);
  const readBase64 = async (_lib, id, maxBytes) => bridge.readBase64(id, maxBytes);
  const available = async (_lib, id) => bridge.available(id);
  const writeBase64 = async (_lib, id, base64) => bridge.writeBase64(id, base64);
  const close = async (_lib, id) => bridge.close(id);

  const getLocalStorage = async (_lib, key) => {
    return localStorage.getItem(key) || "";
  };
  const setLocalStorage = async (_lib, key, value) => {
    localStorage.setItem(key, value);
  };

  return {
    Java_com_runescape_web_CheerpJWs_open: open,
    Java_com_runescape_web_CheerpJWs_readByte: readByte,
    Java_com_runescape_web_CheerpJWs_readBase64: readBase64,
    Java_com_runescape_web_CheerpJWs_available: available,
    Java_com_runescape_web_CheerpJWs_writeBase64: writeBase64,
    Java_com_runescape_web_CheerpJWs_close: close,
    Java_com_runescape_web_CheerpJWs_getLocalStorage: getLocalStorage,
    Java_com_runescape_web_CheerpJWs_setLocalStorage: setLocalStorage,
    Java_CheerpJWs_open: open,
    Java_CheerpJWs_readByte: readByte,
    Java_CheerpJWs_readBase64: readBase64,
    Java_CheerpJWs_available: available,
    Java_CheerpJWs_writeBase64: writeBase64,
    Java_CheerpJWs_close: close,
    Java_CheerpJWs_getLocalStorage: getLocalStorage,
    Java_CheerpJWs_setLocalStorage: setLocalStorage
  };
}

async function main() {
  try {
    console.log("boot", "initializing CheerpJ runtime");
    await cheerpjInit({
      natives: nativeMethods(),
      status: "none",
      version: 11
    });
    console.log("boot", "CheerpJ initialized");
    cheerpjCreateDisplay(clientWidth, clientHeight, displayEl);
    console.log("boot", "display created", `${clientWidth}x${clientHeight}`);
    console.log("boot", "running main", jarPath);
    const exitCode = await cheerpjRunMain(
      "com.runescape.web.CheerpJClientLauncher",
      jarPath,
      `server=${window.location.hostname}`,
      "bridge=/gamews"
    );
    const isError = exitCode !== 0;
    if (isError) {
      console.error("java client exited with code", exitCode);
    } else {
      console.log("java client exited cleanly", exitCode);
    }
    setStatus(`Java client exited with code ${exitCode}`, isError);
  } catch (error) {
    console.error("CheerpJ startup failed", error);
    setStatus(error && error.message ? error.message : "CheerpJ startup failed", true);
  }
}

main();
