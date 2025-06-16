package com.github.garetht.typstsupport.preview

import com.intellij.openapi.diagnostic.logger
import java.io.File
import java.net.ServerSocket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.seconds

private val LOG = logger<PreviewServerManager>()

class PreviewServerManager {
    private data class ServerInfo(
        val process: Process,
        val httpPort: Int,
        val wsPort: Int,
        val startTime: Long = System.currentTimeMillis()
    )

    private val servers = ConcurrentHashMap<String, ServerInfo>()
    private val portCounter = AtomicInteger(23625) // Start from default tinymist port
    private val maxServers = 10
    private val maxRetries = 5

    suspend fun createServer(filename: String): Boolean {
        // If we're at max servers, remove the oldest one
        if (servers.size >= maxServers) {
            val oldestServer = servers.minByOrNull { it.value.startTime }
            oldestServer?.let { shutdownServer(it.key) }
        }

        // Try to start the server with different ports if needed
        for (attempt in 0 until maxRetries) {
            val httpPort = findAvailablePort()
            val wsPort = findAvailablePort()
            
            try {
                val process = startServer(filename, httpPort, wsPort)
                servers[filename] = ServerInfo(process, httpPort, wsPort)
                return true
            } catch (e: Exception) {
                LOG.warn("Failed to start server for $filename on attempt ${attempt + 1}: ${e.message}")
                // Don't retry if we've hit max attempts
                if (attempt == maxRetries - 1) {
                    throw e
                }
            }
        }
        return false
    }

    fun shutdownServer(filename: String) {
        servers[filename]?.let { serverInfo ->
            try {
                serverInfo.process.destroy()
                if (!serverInfo.process.waitFor(5, java.util.concurrent.TimeUnit.SECONDS)) {
                    serverInfo.process.destroyForcibly()
                }
                servers.remove(filename)
            } catch (e: Exception) {
                LOG.error("Error shutting down server for $filename: ${e.message}")
            }
        }
    }

    private fun findAvailablePort(): Int {
        while (true) {
            val port = portCounter.getAndIncrement()
            try {
                ServerSocket(port).use { return port }
            } catch (e: Exception) {
                // Port is in use, try next one
                continue
            }
        }
    }

    private suspend fun startServer(filename: String, httpPort: Int, wsPort: Int): Process {
        val options = TinymistPreviewOptions(
            dataPlaneHostPort = httpPort,
            controlPlaneHostPort = wsPort,
            openInBrowser = false // We don't want to open the browser automatically
        )

        val processBuilder = ProcessBuilder(options.toCommandList() + filename)
            .directory(File(filename).parentFile)
            .redirectErrorStream(true)

        val process = processBuilder.start()
        
        // Wait for server to start up
        withTimeoutOrNull(5.seconds) {
            while (true) {
                if (!process.isAlive) {
                    throw RuntimeException("Server process died unexpectedly")
                }
                
                // Check if ports are in use
                if (isPortInUse(httpPort) && isPortInUse(wsPort)) {
                    break
                }
                
                withContext(Dispatchers.IO) {
                    Thread.sleep(100)
                }
            }
        } ?: throw RuntimeException("Server failed to start within timeout")

        return process
    }

    private fun isPortInUse(port: Int): Boolean {
        return try {
            ServerSocket(port).use { false }
        } catch (e: Exception) {
            true
        }
    }
} 
