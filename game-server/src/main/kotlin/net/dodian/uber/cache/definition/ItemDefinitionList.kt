package net.dodian.uber.cache.definition

data class ItemDefinitionList(
    private val items: MutableMap<Int, ItemDefinition> = mutableMapOf()
) : MutableMap<Int, ItemDefinition> by items