package net.dodian.uber.net.message

abstract class Message(
    var isTerminated: Boolean = false
) {
    fun terminate() {
        isTerminated = true
    }
}