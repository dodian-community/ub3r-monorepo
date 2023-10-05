package net.dodian.plugin

import org.pf4j.Plugin
import org.pf4j.PluginWrapper

abstract class DodianPlugin() : Plugin() {
    abstract val name: String
    abstract val authors: List<String>
    abstract val version: String
    open val description: String? = null

    //abstract fun reload()
    //abstract fun save()
    //abstract fun load()
}