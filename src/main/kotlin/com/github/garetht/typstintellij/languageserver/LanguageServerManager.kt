package com.github.garetht.typstintellij.languageserver

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.application.EDT
import com.intellij.openapi.project.Project
import com.intellij.platform.lsp.api.LspServerManager
import com.intellij.platform.lsp.api.LspServerSupportProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class LanguageServerManager {
  suspend fun initialStart(project: Project, providerClass: Class<out LspServerSupportProvider>) {
    val manager = LspServerManager.getInstance(project)
    manager.startServersIfNeeded(providerClass)
    this.repaintOnIntialize(manager, project, providerClass)
  }

  suspend fun repaintOnIntialize(
      manager: LspServerManager,
      project: Project,
      cls: Class<out LspServerSupportProvider>
  ) {
    withTimeoutOrNull(languageServerPollTimeout) { // 5 second timeout
      while (true) {
        val servers = manager.getServersForProvider(cls)
        val targetServer =
            servers.find { server -> server.providerClass.canonicalName == cls.canonicalName }

        if (targetServer != null) {
          // Found our specific server, restart highlighting
          restartCodeAnalyzer(project)
          break
        }

        delay(languageServerPollDelay)
      }
    } ?: run { restartCodeAnalyzer(project) }
  }

  private suspend fun restartCodeAnalyzer(project: Project) {
    withContext(Dispatchers.EDT) { DaemonCodeAnalyzer.getInstance(project).restart() }
  }

  companion object {
    private val languageServerPollDelay = 200.milliseconds
    private val languageServerPollTimeout = 10.seconds
  }
}
