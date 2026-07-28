package net.dodian.uber.game.api.plugin.testkit

import net.dodian.uber.game.api.content.ContentAttributeKey
import net.dodian.uber.game.api.content.ContentAttributes

/** Deterministic in-memory attribute store shared by isolated content-module tests. */
class FakeContentAttributes : ContentAttributes {
    private val values = mutableMapOf<String, Any>()

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> get(key: ContentAttributeKey<T>): T? = values[key.id] as? T

    override fun <T : Any> put(key: ContentAttributeKey<T>, value: T) {
        values[key.id] = value
    }

    override fun remove(key: ContentAttributeKey<*>) {
        values.remove(key.id)
    }
}
