package com.github.garetht.typstintellij.languageserver

import com.github.garetht.typstintellij.languageserver.downloader.DownloadStatus
import com.github.garetht.typstintellij.languageserver.downloader.TinymistDownloadScheduler
import com.intellij.openapi.project.Project
import com.intellij.platform.lsp.api.LspServerSupportProvider


class TypstManager(
    private val lsDownloader: TinymistDownloadScheduler,
    val project: Project,
    private val serverStarter: LspServerSupportProvider.LspServerStarter
) {
  fun startIfRequired() {
    val status = lsDownloader.scheduleDownloadIfRequired(project)

    when (status) {
      is DownloadStatus.Downloaded -> {
        serverStarter.ensureServerStarted(LanguageServerDescriptor(status.path, project))
      }
      else -> {}
    }
  }
}
