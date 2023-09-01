package net.dodian.uber.game.model.mob.list

import net.dodian.uber.game.model.client.Client

// TODO: could map to player index as a valid player session is required for a client
class ClientList(
    private val clients: MutableList<Client> = mutableListOf()
) : MutableList<Client> by clients
