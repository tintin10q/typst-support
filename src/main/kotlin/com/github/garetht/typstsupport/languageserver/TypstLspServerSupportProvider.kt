package com.github.garetht.typstsupport.languageserver

import com.github.garetht.typstsupport.TypstIcons
import com.github.garetht.typstsupport.configuration.SettingsConfigurable
import com.github.garetht.typstsupport.languageserver.downloader.Filesystem
import com.github.garetht.typstsupport.languageserver.downloader.TinymistDownloadScheduler
import com.github.garetht.typstsupport.languageserver.downloader.TinymistDownloader
import com.github.garetht.typstsupport.languageserver.locations.TinymistLocationResolver
import com.github.garetht.typstsupport.languageserver.locations.isSupportedTypstFileType
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsActions
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServer
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.platform.lsp.api.lsWidget.LspServerWidgetItem

private val LOG = logger<TypstLspServerSupportProvider>()

class TypstLspServerSupportProvider : LspServerSupportProvider {
  private val downloadScheduler by lazy {
    TinymistDownloadScheduler(
      TinymistLocationResolver(),
      TinymistDownloader(),
      Filesystem(),
      TypstLanguageServerManager()
    )
  }

  override fun fileOpened(
    project: Project,
    file: VirtualFile,
    serverStarter: LspServerSupportProvider.LspServerStarter
  ) {
    if (!file.isSupportedTypstFileType()) {
      return
    }

    TypstManager(downloadScheduler, project, serverStarter).startIfRequired()
  }

  override fun createLspServerWidgetItem(
    lspServer: LspServer,
    currentFile: VirtualFile?
  ): LspServerWidgetItem? {
    return object : LspServerWidgetItem(
      lspServer,
      currentFile,
      TypstIcons.WIDGET_ICON,
      SettingsConfigurable::class.java
    ) {
      override val widgetActionText: @NlsActions.ActionText String
        get() = "Typst (Tinymist)"
    }
  }
}
