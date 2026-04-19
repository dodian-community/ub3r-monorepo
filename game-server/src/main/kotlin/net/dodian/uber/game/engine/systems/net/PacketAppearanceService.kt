package net.dodian.uber.game.engine.systems.net

import net.dodian.uber.game.model.entity.UpdateFlag
import net.dodian.uber.game.model.entity.player.Client

/**
 * Kotlin façade for the player-appearance change packet (opcode 11).
 * Moves the [Client.setLook] call and appearance-flag mutation out of the listener.
 */
object PacketAppearanceService {

    /**
     * Applies decoded look values to the player and queues an appearance update.
     *
     * @param client the player
     * @param looks  array of 13 decoded appearance byte values (gender → skin)
     */
    @JvmStatic
    fun handleAppearanceChange(client: Client, looks: IntArray) {
        client.setLook(looks)
        client.updateFlags.setRequired(UpdateFlag.APPEARANCE, true)
    }
}

