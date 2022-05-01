# Installing MySQL Database
This document will explain to you how to install MySQL in your desired environment.

**Disclaimer!** _At the time, we're only linking to external documentation on how to set up MySQL. Apart from the Docker Compose setup, where we also provide a `docker-compose-yml`-file as an example._

---

**Just a for your information:** If you don't wish to use Docker Compose or you don't want to use the volumes in the  `docker-compose.yml`-file to initialize the database, the game server itself can also import it from the [database](/game-server/database)-directory at its root.

## Recommended Database Management Tools
There are many ways to manage a MySQL database. However, the ones that we recommend are:
- [MySQL Workbench][sql-workbench] - Free and very nice and performant management tool
- [JetBrain's DataGrip][datagrip] - Paid, but is quite nice, though for MySQL you may as well use [MySQL Workbench][sql-workbench]

[sql-workbench]: https://dev.mysql.com/downloads/workbench/
[datagrip]: https://www.jetbrains.com/datagrip/

## Using Docker Compose
Docker and Docker Compose should work on Windows, Mac, and Linux.
- [Windows Installation Guide](https://docs.docker.com/desktop/windows/install/)
  - It's recommended to use WSL 2 instead of Hyper-V
- [Mac Installation Guide](https://docs.docker.com/desktop/mac/install/)
- [Linux Installation Guide](https://docs.docker.com/engine/install/)
  - You'll have to find their guide for your distro

**docker-compose.yml** _(example)_
```yaml
version: '3.7'

volumes:
  dev-db:

services:
  mysql: # A MySQL instance for the game server to use
    image: mysql:5.7
    restart: always
    environment:
      # Configured the database credentials
      # Refer to https://hub.docker.com/_/mysql for more info.
      MYSQL_DATABASE: dodiannet
      MYSQL_USER: dodian
      MYSQL_PASSWORD: dodian_local_development_123
      MYSQL_ALLOW_EMPTY_PASSWORD: 'true'
    ports:
      - '3306:3306'
    volumes:
      # Where our data will be persisted
      - dev-db:/var/lib/mysql
      # Enable the below SQL file mappings if you want to initialize the database with data.
      # Do note that these files (the way they are done in this example) need to be in the same directory as this file.
      # You can find them in the "database"-directory of the game-server module
      # !! Everything depends on the mandatory_tables.sql, unless you took care of database another way
      #- ./mandatory_tables.sql:/docker-entrypoint-initdb.d/1_mandatory_game_tables.sql
      #- ./dodian_default_data.sql:/docker-entrypoint-initdb.d/2_dodian_default_data.sql
      #- ./convenient_data.sql:/docker-entrypoint-initdb.d/3_convenient_game_data.sql
      #- ./dummy_development_data.sql:/docker-entrypoint-initdb/4_dummy_game_data.sql
```

In your command line, where `docker-compose`-command is available, navigate to wherever you saved the file above _(should be called `docker-compose.yml` for simplicity)_. Then after configuring it to your liking, you type `docker-compose up` to start the MySQL instance. If you wish to let it run in the background, use `docker-compose up -d`.

When your MySQL Docker container is running, you need to configure the database properties for the game server to connect using the credentials and IP-address for the host where this Docker container is running. If it's running on your local PC, it should just be `localhost`/`127.0.0.1`.

**URLs For References**
- [How to Create a MySql Instance with Docker Compose (medium.com)](https://medium.com/@chrischuck35/how-to-create-a-mysql-instance-with-docker-compose-1598f3cc1bee)
- [Official MySQL Docker Image (hub.docker.com)](https://hub.docker.com/_/mysql)

## Standalone Installation
You can install MySQL in standalone version as well. Usually this is done using an installer. If you wish to do this, you can Google `How to install MySQL server on <INSERT OS HERE>`, and there will be a ton of tutorials - usually one of the first few should be sufficient.