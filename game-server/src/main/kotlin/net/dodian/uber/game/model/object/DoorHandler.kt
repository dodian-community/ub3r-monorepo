package net.dodian.uber.game.model.`object`

import net.dodian.uber.game.model.`object`.door.DoorDefinition
import net.dodian.uber.game.model.`object`.door.DoorDefinitionLoader
import net.dodian.uber.game.model.`object`.door.DoorDefinitionRepository
import org.slf4j.LoggerFactory

class DoorHandler @JvmOverloads constructor(
    private val loader: DoorDefinitionLoader = DoorDefinitionRepository,
) {
    init {
        loadDefinitions()
    }

    private fun loadDefinitions() {
        try {
            val definitions = loader.load()
            applyDefinitions(definitions)
            logger.info("Loaded {} doors...", definitions.size)
        } catch (exception: Exception) {
            logger.error("Failed to load door definitions.", exception)
        }
    }

    private fun applyDefinitions(definitions: List<DoorDefinition>) {
        val capacity = maxOf(DEFAULT_CAPACITY, definitions.size)
        doorX = IntArray(capacity)
        doorY = IntArray(capacity)
        doorId = IntArray(capacity)
        doorHeight = IntArray(capacity)
        doorFaceOpen = IntArray(capacity)
        doorFaceClosed = IntArray(capacity)
        doorFace = IntArray(capacity)
        doorState = IntArray(capacity)

        for (index in definitions.indices) {
            val definition = definitions[index]
            doorX[index] = definition.x
            doorY[index] = definition.y
            doorId[index] = definition.id
            doorHeight[index] = definition.height
            doorFaceOpen[index] = definition.faceOpen
            doorFaceClosed[index] = definition.faceClosed
            doorFace[index] = definition.face
            doorState[index] = definition.state
        }
    }

    companion object {
        private const val DEFAULT_CAPACITY = 100
        private val logger = LoggerFactory.getLogger(DoorHandler::class.java)

        @JvmField
        var doorX: IntArray = IntArray(DEFAULT_CAPACITY)

        @JvmField
        var doorY: IntArray = IntArray(DEFAULT_CAPACITY)

        @JvmField
        var doorId: IntArray = IntArray(DEFAULT_CAPACITY)

        @JvmField
        var doorHeight: IntArray = IntArray(DEFAULT_CAPACITY)

        @JvmField
        var doorFaceOpen: IntArray = IntArray(DEFAULT_CAPACITY)

        @JvmField
        var doorFaceClosed: IntArray = IntArray(DEFAULT_CAPACITY)

        @JvmField
        var doorFace: IntArray = IntArray(DEFAULT_CAPACITY)

        @JvmField
        var doorState: IntArray = IntArray(DEFAULT_CAPACITY)
    }
}
