package net.dodian.uber.net.update.resource

import java.nio.ByteBuffer

class CombinedResourceProvider(
    private vararg val providers: ResourceProvider
) : ResourceProvider {

    override fun accept(path: String) = true

    override fun get(path: String): ByteBuffer? {
        providers.forEach {
            if (it.accept(path))
                return it.get(path)
        }

        return null
    }
}