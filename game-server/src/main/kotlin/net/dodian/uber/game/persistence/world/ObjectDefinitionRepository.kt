package net.dodian.uber.game.persistence.world

import net.dodian.uber.game.model.objects.WorldObject
import net.dodian.uber.game.persistence.db.DbTables
import net.dodian.uber.game.persistence.repository.DbAsyncRepository

object ObjectDefinitionRepository {
    @JvmStatic
    fun loadObjects(): List<WorldObject> =
        DbAsyncRepository.withConnection { connection ->
            connection
                .createStatement()
                .use { statement ->
                    statement.executeQuery("SELECT id, x, y, type FROM ${DbTables.GAME_OBJECT_DEFINITIONS}").use { results ->
                        val objects = ArrayList<WorldObject>()
                        while (results.next()) {
                            objects += WorldObject(results.getInt("id"), results.getInt("x"), results.getInt("y"), 0, results.getInt("type"))
                        }
                        objects
                    }
                }
        }
}
