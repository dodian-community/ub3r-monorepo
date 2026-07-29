package net.dodian.uber.game.engine.systems.skills

import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.skills.slayer.SlayerTaskMessageService

object SlayerTaskMessageBridge { @JvmStatic fun sendCurrentTask(client: Client) = SlayerTaskMessageService.sendCurrentTask(client.asSkillPlayer()) }
