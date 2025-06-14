package com.github.garetht.typstsupport.languageserver

import com.github.garetht.typstsupport.TypstIcons
import com.github.garetht.typstsupport.configuration.TypstConfigurable
import com.github.garetht.typstsupport.languageserver.downloader.TinymistDownloadScheduler
import com.github.garetht.typstsupport.languageserver.downloader.TinymistDownloader
import com.github.garetht.typstsupport.languageserver.downloader.TypstPluginFileSystem
import com.github.garetht.typstsupport.languageserver.locations.TinymistLocationResolver
import com.github.garetht.typstsupport.languageserver.locations.isSupportedTypstFileType
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.NlsActions
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServer
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.platform.lsp.api.lsWidget.LspServerWidgetItem

private val LOG = logger<TypstSupportProvider>()

class TypstSupportProvider : LspServerSupportProvider {
  private val downloadScheduler by lazy {
    TinymistDownloadScheduler(
        TinymistLocationResolver(),
        TinymistDownloader(),
        TypstPluginFileSystem(),
        LanguageServerManager(),
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
    LOG.warn(lspServer.javaClass.canonicalName)
    LOG.warn(lspServer.providerClass.canonicalName)
    return object : LspServerWidgetItem(
      lspServer,
      currentFile,
      TypstIcons.WIDGET_ICON,
      TypstConfigurable::class.java
    ) {
      override val widgetActionText: @NlsActions.ActionText String
        get() = "Typst (Tinymist)"
    }
  }
}
