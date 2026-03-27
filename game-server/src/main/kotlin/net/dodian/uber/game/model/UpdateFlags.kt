package net.dodian.uber.game.model

import java.util.EnumSet
import net.dodian.uber.game.model.entity.Entity

class UpdateFlags {
    private var flags: Int = 0

    val isUpdateRequired: Boolean
        get() = flags != 0

    fun isRequired(flag: UpdateFlag): Boolean = (flags and flag.getMask()) != 0

    fun setRequired(flag: UpdateFlag, required: Boolean) {
        flags = if (required) flags or flag.getMask() else flags and flag.getMask().inv()
    }

    fun set(flag: UpdateFlag) {
        flags = flags or flag.getMask()
    }

    fun clear(flag: UpdateFlag) {
        flags = flags and flag.getMask().inv()
    }

    fun toggle(flag: UpdateFlag) {
        flags = flags xor flag.getMask()
    }

    fun clear() {
        flags = 0
    }

    fun getValue(): Int = flags

    fun get(flag: UpdateFlag): Boolean = isRequired(flag)

    fun keySet(): Set<UpdateFlag> {
        val result = EnumSet.noneOf(UpdateFlag::class.java)
        for (flag in UpdateFlag.values()) {
            if (isRequired(flag)) {
                result.add(flag)
            }
        }
        return result
    }

    fun getMask(type: Entity.Type): Int {
        var mask = 0
        for (flag in UpdateFlag.values()) {
            if (!isRequired(flag)) {
                continue
            }
            try {
                mask = mask or flag.getMask(type)
            } catch (_: IllegalStateException) {
                // Skip flags that are not valid for the requested entity type.
            }
        }
        return mask
    }

    override fun toString(): String {
        val builder = StringBuilder("UpdateFlags[")
        var first = true
        for (flag in UpdateFlag.values()) {
            if (!isRequired(flag)) {
                continue
            }
            if (!first) {
                builder.append(", ")
            }
            builder.append(flag.name)
            first = false
        }
        builder.append(']')
        return builder.toString()
    }
}
