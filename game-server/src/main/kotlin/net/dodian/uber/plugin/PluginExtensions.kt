package net.dodian.uber.plugin

import net.dodian.server.scripting.ScriptPlugin
import net.dodian.uber.context
import net.dodian.uber.game.model.entity.player.Player

fun ScriptPlugin.context() = context

fun ScriptPlugin.onLogin(action: Player.() -> Unit) {

}