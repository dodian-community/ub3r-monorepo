package net.dodian.content.plugin.testing

import com.github.michaelbull.logging.InlineLogger
import net.dodian.plugin.DodianPlugin

private val logger = InlineLogger()

class TestingPlugin(
    override val name: String = "Testing Plugin",
    override val authors: List<String> = listOf("Nozemi"),
    override val version: String = "1.0.0"
) : DodianPlugin() {

    override fun start() {
        logger.info { "Oh hello there!" }
    }
}