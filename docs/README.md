# Ub3r Server Documentation

This is the documentation for the RS2 Ub3r server. This once was the original server used for Dodian.com back in the
days. These days it has been quite heavily modified by many people.

---

## General Information
- Server supports being run standalone and in a Docker container using Docker Compose.
- Server supports importing SQL files from the `database`-directory located at the root of the server.

### Glossary-ish
- **Project Root:** `/` 
  - _Where you can find: `docs`, `game-client`, `game-launcher`, ..._
- **Game Server Root:** `/game-server`
  - _Where you can find: `.example.env`, `Dockerfile`, `docker-compose-exmaple.yml`, ..._

### Default Dummy Accounts
If you import the [4_dummy_development_data.sql](/game-server/database/4_dummy_development_data.sql)-file, some dummy data useful for development purposes will be added to the database.
- **[USER]** username: `Admin` & password: `abc123`
  - Which is a user with admin privileges

## Hosting Requirements

- Host OS with Java 11 or greater
- MySQL database
- Ub3r server data package
- [Database initialize scripts](/game-server/database) _(optional)_

## Setting Up Development Environment

If you want to contribute back to this repository, you should create a fork of this repository. When you've done that,
you should have your own copy of this project under your own GitHub account.

**Should work well on Windows, Mac, and Linux.**

1. Make sure you've installed IntelliJ _(or any other IDE you might want to use...)_
2. Click the green code button of your own repository _(should be visible from the first page of the repo)_
3. Copy the HTTPS or SSH URL
    * _SSH URL is used if you have configured an SSH key with your GitHub account and PC_
4. Depending on whether you're in the welcome-window or an existing project
    * if **Welcome-window:** `Get from VCS`
    * if **Existing project:** `File` -> `New` -> `Project from Version Control...`
5. Paste the URL you copied from step 3. into the `URL`-field
    * _Alternatively configure what you need to with the path to save the project to_
6. Hit the `Clone`-button and wait for it to download and ask you to open it
7. _Sadly Dodian relies on a MySQL database instance, even for development,_ \
   We're going to have to [set up a MySQL database][guide-setup_database] for the local server
8. Copy the [example.env](/game-server/example.env)-file and call it `.env` and update its properties accordingly
    * [Environment Variables Overview][guide-environment_variables] has all the properties explained
9. Download the `server_data_latest.zip` from here: https://drive.google.com/drive/folders/1z_Ua4b4Gm666U0TVzZXrBCzViPyxfBZr
10. Extract the contents of the ZIP file into a folder called `data` at the game server's root directory
11. You should now be ready to start the server and run the client using the Gradle tasks to the right (little menu called Gradle)
     * **Starting Game Server:** `ub3r-monorepo` -> `game-server` -> `Tasks` -> `application` -> `run`
     * **Starting Game Client:** `ub3r-monorepo` -> `game-client` -> `Tasks` -> `application` -> `run`

**Useful Links**

- [Generating a new SSH key and adding it to the ssh-agent](https://docs.github.com/en/authentication/connecting-to-github-with-ssh/generating-a-new-ssh-key-and-adding-it-to-the-ssh-agent)
- [Adding a new SSH key to your GitHub account](https://docs.github.com/en/authentication/connecting-to-github-with-ssh/adding-a-new-ssh-key-to-your-github-account)

_There is other nice, useful, and well documented information on GitHub's docs. However, a few useful links are provided
above._

[public-files]: https://drive.google.com/drive/folders/1z_Ua4b4Gm666U0TVzZXrBCzViPyxfBZr?usp=sharing
[guide-setup_database]: /docs/installing_mysql.md
[guide-environment_variables]: /docs/environment_variables.md