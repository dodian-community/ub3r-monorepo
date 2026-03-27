package net.dodian.uber.game.model.`object`.door

import java.sql.ResultSet
import net.dodian.utilities.DbTables
import net.dodian.utilities.dbConnection

data class DoorDefinition(
    val x: Int,
    val y: Int,
    val id: Int,
    val height: Int,
    val faceOpen: Int,
    val faceClosed: Int,
    val face: Int,
    val state: Int,
)

class DoorDefinitionSetBuilder {
    private val definitions = ArrayList<DoorDefinition>()

    fun door(
        x: Int,
        y: Int,
        id: Int,
        height: Int,
        faceOpen: Int,
        faceClosed: Int,
        face: Int,
        state: Int,
    ) {
        definitions += DoorDefinition(
            x = x,
            y = y,
            id = id,
            height = height,
            faceOpen = faceOpen,
            faceClosed = faceClosed,
            face = face,
            state = state,
        )
    }

    fun build(): List<DoorDefinition> = definitions.toList()
}

fun doorDefinitions(block: DoorDefinitionSetBuilder.() -> Unit): List<DoorDefinition> {
    val builder = DoorDefinitionSetBuilder()
    builder.block()
    return builder.build()
}

fun interface DoorDefinitionLoader {
    fun load(): List<DoorDefinition>
}

object DoorDefinitionRepository : DoorDefinitionLoader {
    private val QUERY =
        "SELECT doorX, doorY, doorId, doorFaceOpen, doorFaceClosed, doorFace, doorState, doorHeight " +
            "FROM ${DbTables.GAME_DOOR_DEFINITIONS}"

    override fun load(): List<DoorDefinition> {
        val loaded = ArrayList<DoorDefinition>()
        dbConnection.use { connection ->
            connection.prepareStatement(QUERY).use { statement ->
                statement.executeQuery().use { rows ->
                    while (rows.next()) {
                        loaded += rows.toDoorDefinition()
                    }
                }
            }
        }
        return loaded
    }
}

private fun ResultSet.toDoorDefinition(): DoorDefinition =
    DoorDefinition(
        x = getInt("doorX"),
        y = getInt("doorY"),
        id = getInt("doorId"),
        height = getInt("doorHeight"),
        faceOpen = getInt("doorFaceOpen"),
        faceClosed = getInt("doorFaceClosed"),
        face = getInt("doorFace"),
        state = getInt("doorState"),
    )
