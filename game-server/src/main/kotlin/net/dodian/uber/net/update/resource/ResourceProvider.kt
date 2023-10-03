package net.dodian.uber.net.update.resource

import java.nio.ByteBuffer

interface ResourceProvider {
    fun accept(path: String): Boolean
    fun get(path: String): ByteBuffer?
}