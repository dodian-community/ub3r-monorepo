package net.dodian.library.zip

import com.github.michaelbull.logging.InlineLogger
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import kotlin.io.path.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.isDirectory


private val logger = InlineLogger()

class ZipArchive(
    private val data: ByteArrayOutputStream = ByteArrayOutputStream(),
    private val zip: ZipOutputStream = ZipOutputStream(data)
) {
    fun withFile(file: File): ZipArchive {
        file.add()
        return this
    }

    fun withFiles(vararg files: File): ZipArchive {
        files.forEach { it.add() }
        return this
    }

    fun withFiles(files: List<File>): ZipArchive {
        withFiles(*files.toTypedArray())
        return this
    }

    fun saveTo(path: Path) {
        data.writeTo(FileOutputStream(path.toString()))
        finish()
    }

    private fun finish() {
        data.close()
        zip.close()
    }

    private fun File.add() {
        val inStream = FileInputStream(this)
        val zipEntry = ZipEntry(this.name)
        zip.putNextEntry(zipEntry)
        val bytes = ByteArray(1024)
        var length: Int
        while (inStream.read(bytes).also { length = it } >= 0) {
            zip.write(bytes, 0, length)
        }
        inStream.close()
    }
}