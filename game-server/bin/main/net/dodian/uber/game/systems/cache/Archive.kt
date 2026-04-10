package net.dodian.uber.game.systems.cache

class Archive private constructor(
    private val data: ByteArray,
    private val extracted: Boolean,
    private val files: Map<Int, ArchiveFile>,
) {
    fun readFile(name: String): ByteArray? {
        val file = files[CacheUtils.hash(name)] ?: return null
        val slice = data.copyOfRange(file.offset, file.offset + file.compressedSize)
        return if (extracted) {
            slice
        } else {
            CacheUtils.unpackBzip2(slice)
        }
    }

    companion object {
        fun decode(data: ByteArray): Archive? {
            if (data.size < 8) {
                return null
            }

            val extractedSize = CacheUtils.readMedium(data, 0)
            val compressedSize = CacheUtils.readMedium(data, 3)
            val extracted = compressedSize != extractedSize
            val payload =
                if (extracted) {
                    if (data.size < 6 + compressedSize) {
                        return null
                    }
                    CacheUtils.unpackBzip2(data.copyOfRange(6, 6 + compressedSize))
                } else {
                    data
                }

            val startOffset = if (extracted) 0 else 6
            if (payload.size < startOffset + 2) {
                return null
            }

            val fileCount = CacheUtils.readUnsignedShort(payload, startOffset)
            val tableOffset = startOffset + 2
            val dataOffset = tableOffset + fileCount * ENTRY_HEADER_SIZE
            if (payload.size < dataOffset) {
                return null
            }

            val files = HashMap<Int, ArchiveFile>(fileCount)
            var offset = dataOffset
            repeat(fileCount) { index ->
                val entryOffset = tableOffset + index * ENTRY_HEADER_SIZE
                val hash = readInt(payload, entryOffset)
                val extractedLength = CacheUtils.readMedium(payload, entryOffset + 4)
                val compressedLength = CacheUtils.readMedium(payload, entryOffset + 7)
                if (offset + compressedLength > payload.size) {
                    return null
                }
                files[hash] = ArchiveFile(hash, extractedLength, compressedLength, offset)
                offset += compressedLength
            }

            return Archive(payload, extracted, files)
        }

        private fun readInt(data: ByteArray, offset: Int): Int {
            if (offset + 3 >= data.size) {
                return 0
            }
            return ((data[offset].toInt() and 0xFF) shl 24) or
                ((data[offset + 1].toInt() and 0xFF) shl 16) or
                ((data[offset + 2].toInt() and 0xFF) shl 8) or
                (data[offset + 3].toInt() and 0xFF)
        }

        private const val ENTRY_HEADER_SIZE = 10
    }
}

data class ArchiveFile(
    val hash: Int,
    val extractedSize: Int,
    val compressedSize: Int,
    val offset: Int,
)

