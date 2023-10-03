package net.dodian.uber.net

data class NetworkPorts(
    val http: Int = 8080,
    val jaggrab: Int = 43595,
    val service: Int = 43594
)

const val IDLE_TIME = 15