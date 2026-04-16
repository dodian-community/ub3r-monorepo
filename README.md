[![Discord](https://discordapp.com/api/guilds/833648712633810974/widget.png)](https://discord.gg/m4CkqrakHn)

# Dodian Ub3r

This is the original Dodian Ub3r source code, which has been maintained by various people over the years. The client included in this package is not the same client that was used back in the day.

---

## Progress Since Initial Commit

Since the initial commit (`1df002f0`), this project has evolved significantly. Key improvements include:

- **Architecture**: A transition to a robust, modular Kotlin coroutine-based action system, heavily reducing reliance on the monolithic `Client.process()` loop.
- **Performance**: Zero-allocation strategies in network packets, modernized caching systems, and strict tick-stamped action controllers replacing manual wall-clock checks.
- **Modernization**: Upgraded tooling to utilize Gradle and Java 17 for the backend, alongside major refactors to social, skilling, and pathfinding systems.

---

## Planned Improvements

While the project has already undergone major modernization, several significant improvements are still planned or actively in progress:

- **Async MySQL + Connection Pooling**  
  Introduce a proper pooled MySQL access layer with asynchronous execution to reduce blocking and improve scalability.

- **Netty Migration**  
  Replace the older stream-based networking layer with **Netty** and non-blocking I/O for a more modern and efficient packet pipeline.

- **Player and NPC Delta Updating**  
  Improve update efficiency by only sending meaningful state changes, reducing packet waste and unnecessary bandwidth usage.

- **UI Update Optimization**  
  Avoid re-sending interface state that has not changed, cutting down on redundant resource usage and packet spam.

- **Event-Driven Skilling**  
  Continue moving skill systems away from constant loop-based checks and toward event/action-driven execution. For example, mining should not depend on the main thread continuously checking whether a player is still mining.

- **Following Improvements**  
  Improve player-following behavior so movement feels correct and does not result in no-clip style pathing issues. This is still a work in progress.

- **Farming**  
  Farming is still under development and will continue to be expanded.

---

## Long-Term Direction

The long-term goal is to continue modernizing the codebase by reducing unnecessary main-thread work, improving update and networking efficiency, and replacing legacy blocking systems with cleaner event-driven and asynchronous designs.

---

[Click here](/docs) to find more information about how to set up, host, or otherwise use this project.