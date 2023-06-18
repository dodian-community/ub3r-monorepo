package net.dodian.uber.game.libraries.commands.interfaces

import net.dodian.utilities.RightsFlag

interface ICommandDefinition<T> {
    val name: T
    val aliases: List<T>
    val description: T
    val permissions: List<RightsFlag>?
    val permissionMessage: T?
    val usage: T
    val defaultArgumentValues: Map<Int, Any>
}