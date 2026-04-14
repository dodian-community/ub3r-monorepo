package net.dodian.uber.game.api.content

import net.dodian.uber.game.engine.systems.dialogue.DialogueService
import net.dodian.uber.game.model.entity.player.Client
import net.dodian.uber.game.shop.ShopId

/**
 * Content-facing player interaction facade for frequent gameplay actions.
 */
object ContentPlayerActions {
	@JvmStatic
	fun message(player: Client, text: String) {
		if (text.isNotBlank()) {
			player.sendMessage(text)
		}
	}

	@JvmStatic
	fun openShop(player: Client, shopId: Int) {
		player.openUpShopRouted(shopId)
	}

	@JvmStatic
	fun openShop(player: Client, shopId: ShopId) {
		openShop(player, shopId.id)
	}

	@JvmStatic
	fun openBank(player: Client) {
		player.openUpBankRouted()
	}

	@JvmStatic
	fun startNpcDialogue(player: Client, dialogueId: Int, npcId: Int) {
		player.startNpcDialogue(dialogueId, npcId)
	}

	@JvmStatic
	@JvmOverloads
	fun teleport(
		player: Client,
		x: Int,
		y: Int,
		z: Int,
		premiumOnly: Boolean = false,
		emote: Int? = null,
	) {
		if (emote == null) {
			player.triggerTele(x, y, z, premiumOnly)
		} else {
			player.triggerTele(x, y, z, premiumOnly, emote)
		}
	}

	@JvmStatic
	fun npcChat(
		player: Client,
		npcId: Int,
		emote: Int,
		vararg lines: String,
	) {
		DialogueService.showNpcChat(player, npcId, emote, lines.toList().toTypedArray())
	}

	@JvmStatic
	fun playerChat(
		player: Client,
		emote: Int,
		vararg lines: String,
	) {
		DialogueService.showPlayerChat(player, lines.toList().toTypedArray(), emote)
	}
}

