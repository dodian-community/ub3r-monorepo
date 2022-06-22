# Ub3r Documentation

This is the documentation for the RS2 Ub3r server. This once was the original server used for Dodian.com back in the
days. These days it has been quite heavily modified by many people.

**Disclaimer!** _The docs are a work in progress, you can report information that is missing, outdated, or incorrect in our [Discord server](https://discord.gg/m4CkqrakHn)!_

---

<details>
<summary>Navigation Menu</summary>

<ul>
    <li><a href="/docs/contribution">Guides</a>
        <ul>
            <li><a href="/docs/guides/getting_started.md">Getting Started</a></li>
            <li><a href="/docs/guides/installing_mysql.md">Installing MySQL Database</a></li>
            <li style="margin-top: 5px"><a href="/docs/guides/glossary.md">Glossary</a></li>
        </ul>
    </li>
    <li><a href="/docs/contribution">Contribution</a>
        <ul>
            <li><a href="/docs/contribution/guidelines.md">Contribution Guidelines</a></li>
            <li><a href="/docs/contribution/issue_definitions.md">Issue Definitions</a></li>
        </ul>
    </li>
    <li><a href="/docs/development">Development</a>
        <ul>
            <li><a href="/docs/development/database.md">Ub3r Database</a></li>
        </ul>
    </li>
    <li><a href="/docs/other">Other</a>
        <ul>
            <li><a href="/docs/other/environment_variables.md">Environment Variables</a></li>
        </ul>
    </li>
</ul>

</details>

---

## General Information
- Server supports being run standalone and in a Docker container using Docker Compose.
- Server supports importing SQL files from the `database`-directory located at the root of the server.


### Default Dummy Accounts
If you import the [4_dummy_development_data.sql](/game-server/database/4_dummy_development_data.sql)-file, some dummy data useful for development purposes will be added to the database.
- **[USER]** username: `Admin` & password: `abc123`
  - Which is a user with admin privileges

## Hosting Requirements
- Host OS with Java 11 or greater
- MySQL database
- Ub3r server data package
- [Database initialize scripts](/game-server/database) _(optional)_