# Mystic Web Client

This is the web-client portal for the Dodian game client, enabling players to launch and play the game natively inside modern web browsers using the CheerpJ runtime.

---

## Setup & Configuration

1. **Install Dependencies**:
   Navigate to the `web-client` folder and install packages:
   ```bash
   cd web-client
   npm install
   ```

2. **Manually Place Cache Files**:
   Copy your game cache files (containing `main_file_cache.dat`, index files, sprites, etc.) into the following directory:
   
   📁 **`web-client/public/cache/`**

   Ensure the following files are present directly inside `public/cache/`:
   - `main_file_cache.dat`
   - `main_file_cache.idx0` through `main_file_cache.idx5`
   - `obj.dat` and `obj.idx`
   - `sprites.dat` and `sprites.idx`
   - `tradable.dat`
   - `version.txt`

3. **Configure Environment Variables**:
   Copy `.env.example` to `.env` and configure the upstream game server host/port if necessary:
   ```bash
   cp .env.example .env
   ```

---

## How to Build and Start

### 1. Build the Java Client
Before preparing web assets, compile the client code in the repository root:
```bash
# From the monorepo root directory:
./gradlew :mystic-updatedclient:jar
```

### 2. Build the Web Client Assets
Navigate to the `web-client` directory and compile the bundle:
```bash
cd web-client
npm run build
```

### 3. Start the Web Client
Start the Node.js HTTPS server:
```bash
npm run start
```
The server will start listening at `https://localhost:8443`.
