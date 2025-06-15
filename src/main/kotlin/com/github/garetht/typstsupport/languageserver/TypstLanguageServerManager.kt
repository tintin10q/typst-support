package com.github.garetht.typstsupport.languageserver

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.EDT
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.platform.lsp.api.LspServerManager
import com.intellij.platform.lsp.api.LspServerState
import com.intellij.platform.lsp.api.LspServerSupportProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

private val LOG = logger<TypstLanguageServerManager>()

class TypstLanguageServerManager {
  suspend fun initialStart(project: Project) {
    val providerClass = TypstSupportProvider::class.java
    val manager = LspServerManager.getInstance(project)
    manager.stopAndRestartIfNeeded(providerClass)
    repaintOnIntialize(manager, project, providerClass)
  }

  companion object {
    private val languageServerPollDelay = 300.milliseconds
    private val languageServerPollTimeout = 15.seconds

    suspend fun repaintOnIntialize(
      manager: LspServerManager,
      project: Project,
      cls: Class<out LspServerSupportProvider>
    ) {
      withTimeoutOrNull(languageServerPollTimeout) {
        while (true) {
          val servers = manager.getServersForProvider(cls)
          val targetServer =
            servers.find { server -> server.providerClass.canonicalName == cls.canonicalName }

          if (targetServer?.state == LspServerState.Running) {
            // the server has started up, restart the code analyzer
            restartCodeAnalyzer(project)
            break
          }

          delay(languageServerPollDelay)
        }
      } ?: TypstLanguageServerManager.run { restartCodeAnalyzer(project) }
    }

    private suspend fun restartCodeAnalyzer(project: Project) {
      LOG.warn("Restarting code analyzer for ${project.name}")
      withContext(Dispatchers.EDT) { DaemonCodeAnalyzer.getInstance(project).restart() }
    }
  }
}
