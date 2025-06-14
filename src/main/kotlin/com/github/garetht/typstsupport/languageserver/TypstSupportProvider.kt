package com.github.garetht.typstsupport.languageserver

import com.github.garetht.typstsupport.languageserver.downloader.TinymistDownloadScheduler
import com.github.garetht.typstsupport.languageserver.downloader.TinymistDownloader
import com.github.garetht.typstsupport.languageserver.downloader.TypstPluginFileSystem
import com.github.garetht.typstsupport.languageserver.locations.TinymistLocationResolver
import com.github.garetht.typstsupport.languageserver.locations.isSupportedTypstFileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerSupportProvider

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
}
