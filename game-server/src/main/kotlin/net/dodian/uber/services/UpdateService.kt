package net.dodian.uber.services

import com.github.michaelbull.logging.InlineLogger
import net.dodian.uber.net.update.*
import org.apollo.cache.IndexedFileSystem
import java.nio.file.Paths
import java.util.concurrent.Executors

private val logger = InlineLogger()

class UpdateService : Service {
    private val requestTypes = 3
    private val threadsPerType = Runtime.getRuntime().availableProcessors()
    private val service = Executors.newFixedThreadPool(requestTypes * threadsPerType)
    private val workers = mutableListOf<RequestWorker<*, *>>()

    val dispatcher = UpdateDispatcher()

    override fun start() {
        val base = Paths.get("./data/cache")

        for (i in 0 until threadsPerType) {
            workers.add(JagGrabRequestWorker(dispatcher, IndexedFileSystem(base, true)))
            workers.add(OnDemandRequestWorker(dispatcher, IndexedFileSystem(base, true)))
            workers.add(HttpRequestWorker(dispatcher, IndexedFileSystem(base, true)))
        }

        workers.forEach { service.submit(it) }
    }
}