# Getting Started

If you want to contribute back to this repository, you should create a fork of this repository. When you've done that,
you should have your own copy of this project under your own GitHub account.

**Should work well on Windows, Mac, and Linux.**

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

## Create a Local Development Environment
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

### Useful Links

Some links that are particularly useful, and directly related to this guide.

- [Generating a new SSH key and adding it to the ssh-agent](https://docs.github.com/en/authentication/connecting-to-github-with-ssh/generating-a-new-ssh-key-and-adding-it-to-the-ssh-agent)
- [Adding a new SSH key to your GitHub account](https://docs.github.com/en/authentication/connecting-to-github-with-ssh/adding-a-new-ssh-key-to-your-github-account)

_GitHub's docs are however a very nice and organized source of information when it comes to Git and GitHub beyond the above links._

[public-files]: https://drive.google.com/drive/folders/1z_Ua4b4Gm666U0TVzZXrBCzViPyxfBZr?usp=sharing
[guide-setup_database]: /docs/guides/installing_mysql.md
[guide-environment_variables]: /docs/other/environment_variables.md