package com.github.garetht.typstsupport.previewserver

import com.github.garetht.typstsupport.languageserver.locations.LocationResolver
import com.github.garetht.typstsupport.languageserver.locations.TinymistLocationResolver
import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationListener
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.util.Alarm
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.io.IOException
import java.net.ServerSocket
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.time.Duration.Companion.seconds


private val LOG = logger<TinymistPreviewServerManager>()

class TinymistPreviewServerManager internal constructor(
  private val locationResolver: LocationResolver = TinymistLocationResolver()
) : PreviewServerManager, Disposable {

  private data class ServerInfo(
    val process: Process,
    val dataPlanePort: Int,
    val controlPlanePort: Int,
    val startTime: Long = System.currentTimeMillis(),
    val pid: Long? = null // Store PID for extra cleanup safety
  )

  private val servers = ConcurrentHashMap<String, ServerInfo>()
  private val portCounter = AtomicInteger(STARTING_PORT)
  private val isDisposed = AtomicBoolean(false)

  // Alarm for periodic cleanup checks
  private val cleanupAlarm = Alarm(Alarm.ThreadToUse.POOLED_THREAD, this)

  init {
    // Register multiple shutdown hooks for maximum safety
    registerShutdownHooks()

    // Start periodic cleanup check
    schedulePeriodicCleanup()
  }

  private fun registerShutdownHooks() {
    // 2. JVM shutdown hook (fallback)
    Runtime.getRuntime().addShutdownHook(Thread {
      LOG.warn("JVM Shutdown Hook: Cleaning up ${servers.size} preview servers")
      forceShutdownAllServers()
    })

    // 3. Application shutdown listener
    ApplicationManager.getApplication().invokeLater {
      ApplicationManager.getApplication()
        .addApplicationListener(object : ApplicationListener {
          override fun applicationExiting() {
            LOG.warn("Application exiting: Cleaning up preview servers")
            forceShutdownAllServers()
          }
        }, this)
    }
  }

  private fun schedulePeriodicCleanup() {
    if (isDisposed.get()) return

    cleanupAlarm.addRequest({
      cleanupDeadProcesses()
      schedulePeriodicCleanup() // Reschedule
    }, CLEANUP_INTERVAL_MS)
  }

  private fun cleanupDeadProcesses() {
    val deadServers = mutableListOf<String>()

    servers.forEach { (filename, serverInfo) ->
      if (!serverInfo.process.isAlive) {
        LOG.warn("Found dead server process for $filename, cleaning up")
        deadServers.add(filename)
      }
    }

    deadServers.forEach { filename ->
      servers.remove(filename)
    }
  }

  override fun createServer(filename: String, callback: (port: Int) -> Unit) {
    if (isDisposed.get()) {
      LOG.warn("Manager is disposed, cannot create server for $filename")
      return
    }

    val existingServer = servers[filename]
    if (existingServer != null) {
      // Double-check the process is still alive
      if (existingServer.process.isAlive) {
        callback(existingServer.dataPlanePort)
        return
      } else {
        // Clean up dead process
        servers.remove(filename)
      }
    }

    ApplicationManager.getApplication().executeOnPooledThread {
      runBlocking {
        // If we're at max servers, remove the oldest one
        if (servers.size >= MAX_SERVERS) {
          val oldestServer = servers.minByOrNull { it.value.startTime }
          oldestServer?.let { shutdownServer(it.key) }
        }

        // Try to start the server with different ports if needed
        for (attempt in 0 until MAX_START_RETRIES) {
          if (isDisposed.get()) {
            LOG.warn("Manager disposed during server start attempt")
            return@runBlocking
          }

          LOG.warn("Starting server on attempt ${attempt + 1}")
          val dataPlanePort = findAvailablePort()
          val controlPlanePort = findAvailablePort()

          try {
            val process = startServer(filename, dataPlanePort, controlPlanePort)
            val pid = try {
              process.pid()
            } catch (e: Exception) {
              LOG.warn("Could not get PID for process: ${e.message}")
              null
            }

            servers[filename] = ServerInfo(process, dataPlanePort, controlPlanePort, pid = pid)
            callback(dataPlanePort)
            return@runBlocking
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
          forceKillProcess(serverInfo, filename)
          servers.remove(filename)
        }
      }
    }
  }

  private fun forceKillProcess(serverInfo: ServerInfo, filename: String) {
    try {
      // First try graceful shutdown
      if (serverInfo.process.isAlive) {
        serverInfo.process.destroy()

        // Wait up to 3 seconds for graceful shutdown
        if (!serverInfo.process.waitFor(3, TimeUnit.SECONDS)) {
          LOG.warn("Graceful shutdown failed for $filename, using destroyForcibly")
          serverInfo.process.destroyForcibly()

          // Wait another 2 seconds for force kill
          if (!serverInfo.process.waitFor(2, TimeUnit.SECONDS)) {
            LOG.error("Force kill failed for $filename, trying system kill")

            // Last resort: system kill using PID
            serverInfo.pid?.let { pid ->
              try {
                if (isWindows()) {
                  Runtime.getRuntime().exec("taskkill /F /PID $pid")
                } else {
                  Runtime.getRuntime().exec("kill -9 $pid")
                }
                LOG.warn("Sent system kill signal to PID $pid for $filename")
              } catch (e: Exception) {
                LOG.error("System kill failed for PID $pid: ${e.message}")
              }
            }
          }
        }
      }
    } catch (e: Exception) {
      LOG.error("Error during force kill of server for $filename: ${e.message}")
    }
  }

  private fun forceShutdownAllServers() {
    if (isDisposed.getAndSet(true)) {
      return // Already disposed
    }

    val serversCopy = HashMap(servers)
    servers.clear()

    LOG.warn("Force shutdown of ${serversCopy.size} servers")

    // Kill all processes in parallel for faster shutdown
    val threads = serversCopy.map { (filename, serverInfo) ->
      Thread {
        forceKillProcess(serverInfo, filename)
      }
    }

    threads.forEach { it.start() }

    // Wait for all kills to complete (with timeout)
    threads.forEach { thread ->
      try {
        thread.join(2000) // 2 second timeout per thread
      } catch (e: InterruptedException) {
        LOG.warn("Interrupted while waiting for server shutdown")
        Thread.currentThread().interrupt()
      }
    }

    LOG.warn("Completed force shutdown of all servers")
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

  private suspend fun startServer(filename: String, dataPlanePort: Int, controlPlanePort: Int): Process {
    val options =
      TinymistPreviewOptions(
        dataPlaneHostPort = dataPlanePort,
        controlPlaneHostPort = controlPlanePort,
        openInBrowser = false // We don't want to open the browser automatically
      )

    val command = options.toCommandList(locationResolver.binaryPath()) + filename
    LOG.warn("Starting server with command: ${command.joinToString(" ")}")
    val processBuilder =
      ProcessBuilder(command)
        .directory(File(filename).parentFile)
        .redirectErrorStream(true)

    val process = processBuilder.start()

    // Use IntelliJ's application coroutine scope
    val serverStarted = withTimeout(4.seconds) {
      withContext(Dispatchers.IO) {
        val reader = process.inputStream.bufferedReader()
        try {
          while (isActive && !isDisposed.get()) { // Check if coroutine and manager are still active
            if (reader.ready()) {
              val line = reader.readLine()
              if (line?.contains("preview server listening on") == true) {
                LOG.warn("Server started: $line")
                return@withContext true
              }
            }
            delay(100) // Non-blocking delay
          }
          false
        } finally {
          reader.close()
        }
      }
    }

    if (!serverStarted) {
      process.destroyForcibly()
      throw RuntimeException("Server failed to start within timeout")
    }

    return process
  }

  private fun isWindows(): Boolean {
    return System.getProperty("os.name").lowercase().contains("windows")
  }

  override fun dispose() {
    forceShutdownAllServers()
  }

  companion object {
    private const val STARTING_PORT = 23627 // Start from default tinymist port
    private const val MAX_SERVERS = 10
    private const val MAX_START_RETRIES = 10
    private const val MAX_PORT_RETRIES = 2
    private const val CLEANUP_INTERVAL_MS = 30000 // 30 seconds

    private val instance = TinymistPreviewServerManager()
    fun getInstance(): TinymistPreviewServerManager = instance
  }
}
