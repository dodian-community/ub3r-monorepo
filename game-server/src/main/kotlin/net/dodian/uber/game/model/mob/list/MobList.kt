package net.dodian.uber.game.model.mob.list

import net.dodian.uber.game.model.client.Client
import net.dodian.uber.game.model.entity.Entity

sealed class MobList<T : Entity>(
    private val entities: MutableList<Client> = mutableListOf()
) : MutableList<Entity>