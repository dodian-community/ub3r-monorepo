package net.dodian.uber.game.command.admin

import net.dodian.uber.game.engine.systems.interaction.commands.*

import net.dodian.uber.game.Server
import net.dodian.uber.game.engine.config.FeatureStateService
import net.dodian.uber.game.engine.config.serverDebugMode
import net.dodian.uber.game.engine.config.serverEnv
import net.dodian.uber.game.persistence.account.Login
import net.dodian.uber.game.model.entity.UpdateFlag
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.model.entity.player.Player
import net.dodian.uber.game.netty.listener.out.CameraReset
import net.dodian.uber.game.netty.listener.out.RemoveInterfaces
import net.dodian.uber.game.netty.listener.out.SendCamera
import net.dodian.uber.game.engine.systems.world.player.PlayerRegistry
import net.dodian.uber.game.api.content.ContentFaultCircuitBreaker
import net.dodian.uber.game.api.plugin.ContentPlatformCatalog
import net.dodian.uber.game.api.plugin.ContentRouteCatalog
import net.dodian.uber.game.skill.runtime.parity.SkillDoctor

object StaffCommands : CommandContent {
    private val moderationAliases = setOf(
        "pnpc", "invis", "teleto", "kick", "teletome", "staffzone", "test_area", "busy",
        "camera", "creset", "slots", "checkbank", "checkinv", "banmac", "tradelock",
        "meeting", "alltome",
    )

    override fun definitions() =
        commands {
            command("contentfaults", "reenablecontent") {
                if (client.playerRights < 2) return@command false
                if (alias == "contentfaults") {
                    val disabled = ContentFaultCircuitBreaker.snapshot()["disabledBindings"] as List<*>
                    client.sendMessage(if (disabled.isEmpty()) "No content bindings are quarantined." else "Quarantined content: ${disabled.joinToString()}")
                    return@command true
                }
                val binding = parts.drop(1).joinToString(" ").trim()
                if (binding.isBlank()) return@command usage("::reenablecontent <binding-key>")
                client.sendMessage(
                    if (ContentFaultCircuitBreaker.reEnable(binding)) "Re-enabled content binding: $binding"
                    else "Content binding was not quarantined: $binding",
                )
                true
            }
            command("content") {
                if (client.playerRights < 2) return@command false
                when (parts.getOrNull(1)?.lowercase()) {
                    "modules" -> {
                        val snapshot = ContentPlatformCatalog.snapshot()
                        client.sendMessage("Content ${snapshot.enabledCount}/${snapshot.modules.size} enabled; fingerprint=${snapshot.fingerprint.take(12)}")
                    }
                    "module" -> {
                        val id = parts.getOrNull(2) ?: return@command usage("::content module <module-id>")
                        val module = ContentPlatformCatalog.snapshot().module(id)
                            ?: run { client.sendMessage("Unknown content module: $id"); return@command true }
                        val routes = ContentRouteCatalog.byModule(id)
                        val faults = ContentFaultCircuitBreaker.failuresForModule(id)
                        client.sendMessage("${module.id} ${module.maturity} owner=${module.owner} v=${module.version} routes=${routes.size} faults=${faults.size}")
                    }
                    "routes" -> {
                        val id = parts.getOrNull(2)?.toIntOrNull() ?: return@command usage("::content routes <id>")
                        val routes = ContentRouteCatalog.find(id)
                        client.sendMessage(if (routes.isEmpty()) "No active content routes for id=$id" else routes.take(8).joinToString(" | ") { "${it.moduleId}:${it.routeType}:${it.key}" })
                    }
                    "validate" -> {
                        val report = SkillDoctor.snapshot()
                        client.sendMessage(if (report.isClean) "Content validation passed." else "Content validation found ${report.findings.size} issue(s); see server log.")
                    }
                    else -> client.sendMessage("::content modules | module <id> | routes <id> | validate")
                }
                true
            }
            command("sync") {
                if (client.playerRights < 2) return@command false
                val name = parts.drop(1).joinToString(" ").trim()
                val target = if (name.isBlank()) client else PlayerRegistry.getPlayer(name) as? Client
                if (target == null) {
                    client.sendMessage("Player is not online. Usage: ::sync <player>")
                    return@command true
                }
                val locals =
                    (0 until target.playerListSize)
                        .mapNotNull { target.playerList.getOrNull(it) }
                        .take(12)
                        .joinToString(",") { "${it.slot}@${it.synchronizationSessionGeneration}" }
                client.sendMessage(
                    "Sync ${target.playerName}: slot=${target.slot} session=${target.synchronizationSessionGeneration} " +
                        "ready=${target.isSynchronizationReady} active=${target.isActive} loaded=${target.loaded} " +
                        "locals=${target.playerListSize} appearanceRev=${target.appearanceRevision}",
                )
                client.sendMessage("Sync locals: ${locals.ifBlank { "none" }}; ${target.connectionHealthSummary()}")
                true
            }
            command("crashserver") {
                if (!specialRights) return@command false
                if (serverEnv != "dev" || !serverDebugMode) {
                    client.sendMessage("Crash testing is available only on a dev server with server.debug=true.")
                    return@command true
                }

                recordStaffCommand(client, rawCommand)
                Server.logError("DEV CRASH TEST requested by ${client.playerName} dbId=${client.dbId}; halting without shutdown hooks.")
                Runtime.getRuntime().halt(137)
                false // Unreachable, but required by the command handler's return type.
            }
            command(
                "pnpc", "invis", "teleto", "kick", "teletome", "staffzone", "test_area", "busy",
                "camera", "creset", "slots", "checkbank", "checkinv", "banmac", "tradelock",
                "meeting", "alltome", "toggleyell", "togglepvp", "toggletrade", "toggleduel",
                "toggledrop", "toggleshop", "togglebank",
            ) {
                if (alias in moderationAliases) {
                    handleStaffModeration(this)
                } else {
                    handleWorldControl(this)
                }
            }
        }
}

private fun handleStaffModeration(context: CommandContext): Boolean {
    val client = context.client
    val command = context.rawCommand
    val cmd = context.parts
    val specialRights = context.specialRights
    if (context.alias == "pnpc") {
        if (!specialRights) {
            return false
        }
        return try {
            val npcId = cmd[1].toInt()
            if (npcId <= 8195) {
                client.isNpc = npcId >= 0
                client.playerNpc = if (npcId >= 0) npcId else -1
                client.updateFlags.setRequired(UpdateFlag.APPEARANCE, true)
            }
            client.sendMessage(if (npcId > 8195) "Maximum 8195 in npc id!" else if (npcId >= 0) "Setting npc to ${client.playerNpc}" else "Setting you normal!")
            true
        } catch (_: Exception) {
            context.usage("Wrong usage.. ::${cmd[0]} npcid")
        }
    }
    if (client.playerRights <= 0) {
        return false
    }
    when {
        context.alias == "invis" -> {
            client.invis = !client.invis
            client.sendMessage("You turn invis to ${client.invis}")
            client.transport(client.position)
            recordStaffCommand(client, command)
            return true
        }
        context.alias == "teleto" -> {
            return try {
                if (!canUseStaffTeleport(client, specialRights)) {
                    client.sendMessage("Command can't be used in the wilderness!")
                    return true
                }
                val otherName = context.playerNameTail()
                val otherIndex = PlayerRegistry.getPlayerID(otherName)
                if (otherIndex != -1) {
                    val other = PlayerRegistry.players[otherIndex] as Client
                    if (other.wildyLevel > 0 && !specialRights) {
                        client.sendMessage("That player is in the wilderness!")
                        return true
                    }
                    if (client.UsingAgility || other.UsingAgility || client.isWalkBlocked()) {
                        return true
                    }
                    client.transport(other.position.copy())
                    client.sendMessage("Teleto: You teleport to ${other.playerName}")
                    recordStaffCommand(client, command)
                } else {
                    client.sendMessage("Player $otherName is not online!")
                }
                true
            } catch (_: Exception) {
                context.usage("Try entering a name you want to tele to..")
            }
        }
        context.alias == "kick" -> {
            return try {
                val otherName = context.playerNameTail()
                val otherIndex = PlayerRegistry.getPlayerID(otherName)
                if (otherIndex != -1) {
                    val other = PlayerRegistry.players[otherIndex] as Client
                    other.disconnected = true
                    client.sendMessage("Player ${other.playerName} has been kicked!")
                    recordStaffCommand(client, command)
                } else {
                    client.sendMessage("Player $otherName is not online!")
                }
                true
            } catch (exception: Exception) {
                client.sendMessage("Try entering a name you wish to kick..")
                client.sendMessage(exception.message)
                true
            }
        }
        context.alias == "teletome" -> {
            return try {
                if (!canUseStaffTeleport(client, specialRights)) {
                    client.sendMessage("Command can't be used in the wilderness")
                    return true
                }
                val otherName = context.playerNameTail()
                val otherIndex = PlayerRegistry.getPlayerID(otherName)
                if (otherIndex != -1) {
                    val other = PlayerRegistry.players[otherIndex] as Client
                    if (other.wildyLevel > 0 && !specialRights) {
                        client.sendMessage("Can not teleport someone out of the wilderness! Contact a admin!")
                        return true
                    }
                    if (client.UsingAgility || other.UsingAgility || client.isWalkBlocked()) {
                        return true
                    }
                    other.transport(client.position.copy())
                    recordStaffCommand(client, command)
                } else {
                    client.sendMessage("Player $otherName is not online!")
                }
                true
            } catch (_: Exception) {
                context.usage("Try entering a name you want to tele to you..")
            }
        }
        context.alias == "staffzone" -> {
            if (client.inWildy()) {
                client.sendMessage("Cant use this in the wilderness!")
                return true
            }
            client.teleportTo(2936, 4688, 0)
            client.sendMessage("Welcome to the staff zone!")
            return true
        }
        context.alias == "test_area" -> {
            client.triggerTele(3260, 2784, 0, false)
            client.sendMessage("Welcome to the monster test area!")
            return true
        }
        context.alias == "busy" && client.playerRights > 1 -> {
            client.busy = !client.busy
            client.sendMessage(if (!client.busy) "You are no longer busy!" else "You are now busy!")
            return true
        }
        context.alias == "camera" -> {
            client.send(SendCamera("rotation", client.position.x, client.position.y, 100, 2, 2, ""))
            return true
        }
        context.alias == "creset" -> {
            client.send(CameraReset())
            return true
        }
        context.alias == "slots" -> {
            if (client.playerRights < 2) {
                client.sendMessage("Do not fool with yaaaaar!")
                return true
            }
            client.send(RemoveInterfaces())
            client.openInterface(671)
            Server.slots.playSlotMachine(client, -1)
            return true
        }
        context.alias == "checkbank" -> {
            net.dodian.uber.game.economy.AdminContainerInspectionService.openBank(client, context.playerNameTail())
            recordStaffCommand(client, command)
            return true
        }
        context.alias == "checkinv" -> {
            net.dodian.uber.game.economy.AdminContainerInspectionService.openInventory(client, context.playerNameTail())
            recordStaffCommand(client, command)
            return true
        }
        command.startsWith("banmac") -> {
            return try {
                val otherName = command.substring(7)
                val otherIndex = PlayerRegistry.getPlayerID(otherName)
                if (otherIndex != -1) {
                    val other = PlayerRegistry.players[otherIndex] as Client
                    Login.addUidToFile(other.UUID)
                    other.logout()
                    recordStaffCommand(client, command)
                } else {
                    client.sendMessage("Error MAC banning player. Name doesn't exist or player is offline.")
                }
                true
            } catch (_: Exception) {
                context.usage("Invalid Syntax! Use as ::banmac PlayerName")
            }
        }
        command.startsWith("tradelock") -> {
            return try {
                if (client.wildyLevel > 0) {
                    client.sendMessage("Command can't be used in the wilderness")
                    return true
                }
                val otherName = context.playerNameTail()
                val otherIndex = PlayerRegistry.getPlayerID(otherName)
                if (otherIndex != -1) {
                    val other = PlayerRegistry.players[otherIndex] as Client
                    other.tradeLocked = true
                    client.sendMessage("You have just tradelocked $otherName")
                    recordStaffCommand(client, command)
                } else {
                    client.sendMessage("The name doesnt exist.")
                }
                true
            } catch (_: Exception) {
                context.usage("Try entering a name you want to tradelock..")
            }
        }
        command.equals("meeting", true) && client.playerRights > 1 -> {
            for (i in PlayerRegistry.players.indices) {
                if (client.validClient(i)) {
                    val other = client.getClient(i)
                    if (other.playerRights > 0) {
                        other.sendMessage("All of you belong to ${client.playerName}")
                        other.triggerTele(2936, 4688, 0, false)
                    }
                }
            }
            return true
        }
        command.equals("alltome", true) && client.playerRights > 1 -> {
            for (i in PlayerRegistry.players.indices) {
                if (client.validClient(i)) {
                    val other = client.getClient(i)
                    if (other == client) continue
                    other.sendMessage("<col=cc0000>A force moved you towards a location!")
                    other.triggerTele(client.position.x, client.position.y, client.position.z, false)
                }
            }
            client.sendMessage("You teleported all online to you!")
            return true
        }
    }
    return true
}

private fun handleWorldControl(context: CommandContext): Boolean {
    val client = context.client
    if (client.playerRights <= 0) {
        return false
    }
    val toggleCommand = if (context.alias.startsWith("toggle")) context.alias.replace("_", "") else context.alias
    when {
        toggleCommand.equals("toggleyell", true) -> {
            FeatureStateService.publicChatYell.set(!FeatureStateService.publicChatYell.get())
            client.yell(if (FeatureStateService.publicChatYell.get()) "[SERVER]: Yell has been enabled!" else "[SERVER]: Yell has been disabled!")
        }
        toggleCommand.equals("togglepvp", true) -> {
            FeatureStateService.pvp.set(!FeatureStateService.pvp.get())
            client.yell(if (FeatureStateService.pvp.get()) "[SERVER]: Player Killing has been enabled!" else "[SERVER]: Player Killing  has been disabled!")
        }
        toggleCommand.equals("toggletrade", true) -> {
            FeatureStateService.trading.set(!FeatureStateService.trading.get())
            client.yell(if (FeatureStateService.trading.get()) "[SERVER]: Trading has been enabled!" else "[SERVER]: Trading has been disabled!")
        }
        toggleCommand.equals("toggleduel", true) -> {
            FeatureStateService.dueling.set(!FeatureStateService.dueling.get())
            client.yell(if (FeatureStateService.dueling.get()) "[SERVER]: Dueling has been enabled!" else "[SERVER]: Dueling has been disabled!")
        }
        toggleCommand.equals("toggledrop", true) -> {
            FeatureStateService.dropping.set(!FeatureStateService.dropping.get())
            client.yell(if (FeatureStateService.dropping.get()) "[SERVER]: Dropping items has been enabled!" else "[SERVER]: Dropping items has been disabled!")
        }
        toggleCommand.equals("toggleshop", true) -> {
            FeatureStateService.shopping.set(!FeatureStateService.shopping.get())
            client.yell(if (FeatureStateService.shopping.get()) "[SERVER]: Shops has been enabled!" else "[SERVER]: Shops has been disabled!")
        }
        toggleCommand.equals("togglebank", true) -> {
            FeatureStateService.banking.set(!FeatureStateService.banking.get())
            client.yell(if (FeatureStateService.banking.get()) "[SERVER]: The Bank has been enabled!" else "[SERVER]: The Bank has been disabled!")
            if (!FeatureStateService.banking.get()) {
                for (player in PlayerRegistry.players) {
                    val other = player as? Client ?: continue
                    if (other.IsBanking) {
                        other.send(RemoveInterfaces())
                        other.IsBanking = false
                    }
                }
            }
        }
        else -> return false
    }
    recordStaffCommand(client, context.rawCommand)
    return true
}
