rootProject.name = "dodian"

include(":dodian-common")
include(":dodian-common:dodian-cache")
include(":dodian-common:dodian-library")

include(":dodian-tools")
include(":dodian-tools:dodian-cache-dumper")

include(":dodian-backend")
include(":dodian-backend:dodian-scripting")
include(":dodian-backend:dodian-server")
include(":dodian-backend:dodian-webapi")
include(":dodian-backend:dodian-content")
include(":dodian-backend:dodian-plugins")
include(":dodian-backend:dodian-plugins:plugin-testing")

include(":dodian-frontend")
include(":dodian-frontend:dodian-client")
include(":dodian-frontend:dodian-launcher")