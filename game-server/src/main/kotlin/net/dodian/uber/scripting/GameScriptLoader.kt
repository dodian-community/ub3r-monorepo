package net.dodian.uber.scripting

import io.github.classgraph.ClassGraph

object GameScriptLoader {

    fun load(type: Class<GameScript>, lenient: Boolean = false): List<GameScript> {
        val gameScripts = mutableListOf<GameScript>()

        ClassGraph().enableAllInfo().scan().use { scan ->
            val infoList = scan.getSubclasses(type).directOnly()
            infoList.forEach { info ->
                val clazz = info.loadClass(type)
                val ctor = clazz.getConstructor()
                try {
                    val instance = ctor.newInstance()
                    gameScripts += instance
                } catch (t: Throwable) {
                    if (!lenient) throw (t.cause ?: t)
                    t.printStackTrace()
                }
            }
        }

        return gameScripts
    }
}