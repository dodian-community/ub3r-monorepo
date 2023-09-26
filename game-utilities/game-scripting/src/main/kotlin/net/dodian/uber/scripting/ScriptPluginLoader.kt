package net.dodian.uber.scripting

import io.github.classgraph.ClassGraph

object ScriptPluginLoader {

    fun <T : ScriptPlugin> load(type: Class<T>, lenient: Boolean = false): List<T> {
        val plugins = mutableListOf<T>()

        ClassGraph().enableAllInfo().scan().use { scan ->
            val infoList = scan.getSubclasses(type).directOnly()

            infoList.forEach { info ->
                val clazz = info.loadClass(type)
                val ctor = clazz.getConstructor()
                try {
                    val instance = ctor.newInstance()
                    plugins += instance
                } catch (t: Throwable) {
                    if (!lenient) throw (t.cause ?: t)
                    t.printStackTrace()
                }
            }
        }

        return plugins
    }
}