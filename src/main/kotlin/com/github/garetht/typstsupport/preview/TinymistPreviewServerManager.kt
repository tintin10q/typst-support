package com.github.garetht.typstsupport.preview

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.io.IOException
import java.net.ServerSocket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds

private val LOG = logger<TinymistPreviewServerManager>()

class TinymistPreviewServerManager : PreviewServerManager {
  private data class ServerInfo(
    val process: Process,
    val httpPort: Int,
    val wsPort: Int,
    val startTime: Long = System.currentTimeMillis()
  )

  private val servers = ConcurrentHashMap<String, ServerInfo>()

  private val portCounter = AtomicInteger(STARTING_PORT)

  override fun createServer(filename: String) {
    ApplicationManager.getApplication().executeOnPooledThread {
      runBlocking {
        // If we're at max servers, remove the oldest one
        if (servers.size >= MAX_SERVERS) {
          val oldestServer = servers.minByOrNull { it.value.startTime }
          oldestServer?.let { shutdownServer(it.key) }
        }

        // Try to start the server with different ports if needed
        for (attempt in 0 until MAX_START_RETRIES) {
          val dataPlanePort = findAvailablePort()
          val controlPlanePort = findAvailablePort()

          try {
            val process = startServer(filename, dataPlanePort, controlPlanePort)
            servers[filename] = ServerInfo(process, dataPlanePort, controlPlanePort)
          } catch (e: Exception) {
            LOG.warn(
              "Failed to start server for $filename on attempt ${attempt + 1}: ${e.message}"
            )
            // Don't retry if we've hit max attempts
            if (attempt == MAX_START_RETRIES - 1) {
              throw e
            }
          }
        }
      }
    }
  }

  override fun shutdownServer(filename: String) {
    ApplicationManager.getApplication().executeOnPooledThread {
      runBlocking {
        servers[filename]?.let { serverInfo ->
          try {
            serverInfo.process.destroy()
            if (!serverInfo.process.waitFor(5, TimeUnit.SECONDS)) {
              serverInfo.process.destroyForcibly()
            }
            servers.remove(filename)
          } catch (e: Exception) {
            LOG.error("Error shutting down server for $filename: ${e.message}")
          } finally {
            serverInfo.process.destroyForcibly()
          }
        }
      }
    }
  }

  private fun findAvailablePort(): Int {
    var attempts = 0
    while (attempts < MAX_PORT_RETRIES) {
      val port = portCounter.getAndIncrement()
      try {
        ServerSocket(port).use {
          return port
        }
      } catch (e: IOException) {
        // Port is in use, try the next one
        attempts++
        continue
      }
    }
    return portCounter.get() - 1
  }

  private suspend fun startServer(filename: String, httpPort: Int, wsPort: Int): Process {
    val options =
      TinymistPreviewOptions(
        dataPlaneHostPort = httpPort,
        controlPlaneHostPort = wsPort,
        openInBrowser = false // We don't want to open the browser automatically
      )

    val processBuilder =
      ProcessBuilder(options.toCommandList() + filename)
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

        withContext(Dispatchers.IO) { Thread.sleep(100) }
      }
    }
      ?: throw RuntimeException("Server failed to start within timeout")

    return process
  }

  private fun isPortInUse(port: Int): Boolean {
    return try {
      ServerSocket(port).use { false }
    } catch (e: IOException) {
      true
    }
  }

  private companion object {
    private const val STARTING_PORT = 23625 // Start from default tinymist port
    private const val MAX_SERVERS = 10
    private const val MAX_START_RETRIES = 3
    private const val MAX_PORT_RETRIES = 10
  }
}
