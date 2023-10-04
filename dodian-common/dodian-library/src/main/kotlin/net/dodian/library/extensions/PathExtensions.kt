package net.dodian.library.extensions

import com.github.michaelbull.logging.InlineLogger
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import kotlin.io.path.Path
import kotlin.io.path.deleteIfExists
import kotlin.io.path.isDirectory

private val logger = InlineLogger()

fun Path.unzip(destination: Path = Path("./"), deleteAfter: Boolean = false) {
    if (!destination.isDirectory()) {
        logger.debug { "Destination is not a directory: $destination" }
        return
    }

    val directory = destination.toFile()
    val buffer = ByteArray(1024)
    val zipInputStream = ZipInputStream(FileInputStream(this.toString()))
    var zipEntry: ZipEntry? = zipInputStream.nextEntry
    while (zipEntry != null) {
        val file = newFile(directory, zipEntry) ?: return
        if (zipEntry.isDirectory) {
            if (!file.isDirectory && !file.mkdirs()) {
                logger.error { "Failed to create directory: ${file.absolutePath}" }
                return
            }
        } else {
            val parent = file.parentFile
            if (!parent.isDirectory && !parent.mkdirs()) {
                logger.error { "Failed to create directory: ${parent.absolutePath}" }
                return
            }

            val outputStream = FileOutputStream(file)
            var len: Int
            while (zipInputStream.read(buffer).also { len = it } > 0) {
                outputStream.write(buffer, 0, len)
            }
            outputStream.close()
        }

        zipEntry = zipInputStream.nextEntry
    }
    zipInputStream.close()

    if (deleteAfter)
        this.deleteIfExists()
}

private fun newFile(destination: File, entry: ZipEntry): File? {
    val destinationFile = File(destination, entry.name)

    val destinationDirectory = destination.canonicalPath
    val destinationFilePath = destinationFile.canonicalPath

    if (!destinationFilePath.startsWith(destinationDirectory + File.separator)) {
        logger.error { "Entry is outside of the target directory: ${entry.name}" }
        return null
    }

    return destinationFile
}
