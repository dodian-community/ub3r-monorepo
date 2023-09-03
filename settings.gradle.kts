rootProject.name = "ub3r-monorepo"

include(":game-server")
include(":game-server:server-cache")
include(":game-server:server-plugins")

include(":game-client")
include(":game-client:client-launcher")