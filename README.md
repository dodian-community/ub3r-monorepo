[![Discord](https://discordapp.com/api/guilds/833648712633810974/widget.png)](https://discord.gg/m4CkqrakHn)

# Dodian Ub3r

This is the original Dodian Ub3r source code, which has been maintained by various people over the years. The client included in this package is not the same client that was used back in the day.

---

## Progress Notes


- **Architecture**: A transition to a robust, modular Kotlin coroutine-based action system, heavily reducing reliance on the monolithic `Client.process()` loop.
- **Scheduling Modernization**: Legacy scheduling approaches have been cleaned up, including replacing **Quartz Scheduler** with **Kotlin coroutines** for a more unified and lightweight execution model.
- **Performance**: Zero-allocation strategies in network packets, modernized caching systems, and strict tick-stamped action controllers replacing manual wall-clock checks.
- **Modernization**: Upgraded tooling to utilize Gradle and Java 17 for the backend, alongside major refactors to social, skilling, and pathfinding systems.
- **Async MySQL + Connection Pooling**: Database access has been modernized with asynchronous MySQL handling and **HikariCP** connection pooling to reduce blocking and improve scalability.
- **Netty Migration**: The networking layer has been migrated away from older stream-based I/O to **Netty** and non-blocking I/O for a more modern and efficient packet pipeline.
- **Player and NPC Delta Updating**: Update handling has been improved so only meaningful state changes are sent, reducing packet waste and unnecessary bandwidth usage.
- **UI Update Optimization**: Interface state syncing has been improved to avoid re-sending unchanged state, reducing redundant packets and wasteful resource usage.
- **Event-Driven Systems**: More gameplay systems are being moved away from constant loop-based checks and toward event-driven or action-driven execution where appropriate.
- **Stability Improvements**: Multiple duplication bugs and related exploit paths have been identified and fixed.

---

## Work in Progress

The following areas are still actively being worked on:

- **Player Following Improvements**  
  Following behavior is still being refined so movement feels correct and does not result in no-clip style pathing issues.

- **Farming**  
  Farming is still under development and will continue to be expanded and refined.

- **NPC Movement Improvements**  
  NPC movement and related behavior are still being improved. Some areas of Dodian are still years behind modern standards, and this is one of the areas being actively brought up to date.

- **Plugin System for Content**  
  A plugin-based content system is being worked on to make it easier for developers to build, extend, and maintain skills and other gameplay content without forcing everything into tightly coupled core code.

- **Newer Client with OSRS Data**  
  Work is planned or ongoing toward using a newer client with newer OSRS-packed data. The target revision is around **171**, though that may still be subject to change.

---

## Long-Term Direction

The long-term goal is to continue modernizing the codebase by reducing unnecessary main-thread work, improving update and networking efficiency, and replacing legacy blocking systems with cleaner event-driven and asynchronous designs.

There is also a strong focus on building a more modular system that is easier to maintain, easier to extend, and better suited for delivering consistent updates over time.

---

[Click here](/docs) to find more information about how to set up, host, or otherwise use this project.

---

## Feedback and Bug Reports

Suggestions, improvements, or bug reports can be submitted on [GitHub Issues](https://github.com/dodian-community/ub3r-monorepo/issues)

Or on Discord under [Bug Reports](https://discord.com/channels/833648712633810974/1137413395277164615)