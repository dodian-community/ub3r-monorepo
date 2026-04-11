# Web API

## Overview
The Ub3r server includes an embedded HTTP server used for external monitoring, highscores, and integration with the Dodian.net forums. It is built using the **Spark Java** framework and executes on a dedicated thread outside the game loop.

## Technology Stack
- **Spark Java**: A micro-framework for creating web applications in Java/Kotlin.
- **Jackson**: Used for JSON serialization/deserialization.
- **Port**: Typically configured in environment variables (defaulting to 80 or 4443).

## Core Endpoints

### 1. Server Status (`/api/server-status`)
- **Method**: `GET`
- **Description**: Returns a JSON object containing the server's uptime and a list of all currently online players.
- **Data Model**:
    ```json
    {
      "launchedAt": "2026-04-10T12:00:00",
      "playersOnline": [
        { "id": 1, "username": "Admin" },
        { "id": 2, "username": "Player1" }
      ]
    }
    ```

## Threading & Safety
The Web API runs on its own thread pool managed by Spark. 
- **Read Safety**: When the API requests the list of online players, it reads from the `PlayerRegistry.playersOnline` map. This map is a `ConcurrentHashMap`, making it thread-safe for the API to read while the Game Thread is adding or removing players.
- **Mutations**: The Web API should **never** directly modify player state (e.g., giving items). Instead, it should submit a task to the `GameThreadIngress` to ensure the mutation happens on the Game Thread.

## Future Improvements
- **Highscores**: An endpoint to fetch player experience and levels directly from the database or in-memory cache.
- **Admin CP**: Authenticated endpoints for remote server management (reloading NPCs, kicking players, global messages).
