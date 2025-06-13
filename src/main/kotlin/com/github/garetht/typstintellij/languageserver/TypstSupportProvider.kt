package com.github.garetht.typstintellij.languageserver

import com.github.garetht.typstintellij.languageserver.downloader.TinymistDownloadScheduler
import com.github.garetht.typstintellij.languageserver.downloader.TinymistDownloader
import com.github.garetht.typstintellij.languageserver.downloader.TypstPluginFileSystem
import com.github.garetht.typstintellij.languageserver.files.TinymistLocationResolver
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerSupportProvider

class TypstSupportProvider : LspServerSupportProvider {
    override fun fileOpened(project: Project,
                            file: VirtualFile,
                            serverStarter: LspServerSupportProvider.LspServerStarter
    ) {
      val typstManager = TypstManager(
        TinymistDownloadScheduler(
          TinymistLocationResolver(),
          TinymistDownloader(),
          TypstPluginFileSystem(),
          LanguageServerManager(),
        ),
        project,
        serverStarter
      )
      typstManager.startIfRequired()
    }
}
