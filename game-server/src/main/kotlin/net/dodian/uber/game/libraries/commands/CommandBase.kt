package net.dodian.uber.game.libraries.commands

import net.dodian.uber.game.libraries.commands.interfaces.ICommand
import net.dodian.utilities.RightsFlag
import kotlin.reflect.KParameter

abstract class CommandBase<R : Any>(
    override val name: String,
    override val aliases: List<String>,
    override val description: String,
    override val usage: String,
    override val permissions: List<RightsFlag>?,
    override val permissionMessage: String?,
    override val parameters: Map<String, KParameter> = mapOf(),
    override val defaultArgumentValues: Map<Int, Any> = mapOf()
) : ICommand<R>