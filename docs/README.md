# Ub3r Documentation

Welcome to the documentation for the Dodian Ub3r monorepo.

---

<details open>
<summary>Navigation Menu</summary>

<ul>
    <li><strong>Guides</strong>
        <ul>
            <li><a href="/docs/guides/getting_started.md">Getting Started Guide</a></li>
            <li><a href="/docs/guides/installing_mysql.md">Installing MariaDB / MySQL Database</a></li>
            <li><a href="/docs/guides/discord_account_creation.md">Discord OAuth2 Account Creation</a></li>
            <li><a href="/docs/guides/glossary.md">Glossary</a></li>
        </ul>
    </li>
    <li><strong>Development & Systems Architecture</strong>
        <ul>
            <li><a href="/docs/development/database.md">Ub3r Database & SQL Scripts</a></li>
            <li><a href="/docs/development/item_definitions.md">Item Definitions System</a></li>
            <li><a href="/docs/development/metrics_and_telemetry.md">Metrics, Telemetry & Real-Time Dashboard</a></li>
        </ul>
    </li>
    <li><strong>Contribution</strong>
        <ul>
            <li><a href="/docs/contribution/guidelines.md">Contribution Guidelines</a></li>
            <li><a href="/docs/contribution/issue_definitions.md">Issue Definitions</a></li>
        </ul>
    </li>
    <li><strong>Other</strong>
        <ul>
            <li><a href="/docs/other/environment_variables.md">Environment Variables Overview</a></li>
        </ul>
    </li>
</ul>

</details>

---

## Hosting Requirements

- **Host OS**: Linux, macOS, or Windows with **JDK 21 or greater**
- **Database Engine**: MariaDB 10.11+ or MySQL 8.0+
- **OSRS Cache / Definitions**: Revision 218 Cache (OSRS Data packed into 317 format: [cache-tarnish-218.zip](https://files.jire.org/cache-tarnish-218.zip)) extracted to `game-server/data/cache`
- **Memory Footprint**: ~300MB startup heap usage with Generational ZGC
- **Database Migration Scripts**: Included in [game-server/database](/game-server/database)