package com.github.garetht.typstsupport.languageserver.downloader

import com.github.garetht.typstsupport.languageserver.TypstLanguageServerManager
import com.github.garetht.typstsupport.languageserver.locations.TinymistLocationResolver
import com.github.garetht.typstsupport.notifier.Notifier
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import java.net.URI
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean

class TinymistDownloadScheduler(
  private val resolver: TinymistLocationResolver,
  private val downloader: TinymistDownloader,
  private val fileSystem: Filesystem,
  private val languageServerManager: TypstLanguageServerManager
) {

  companion object {
    private val isDownloading = AtomicBoolean()

    fun resetDownloadingStatus() = isDownloading.set(false)

    private const val DOWNLOAD_CANCELLED_MSG =
      "The Typst Language Server download was cancelled.\n\n To retry, restart the IDE."
  }

  private suspend fun prepAndDownload(project: Project, url: URI, path: Path) {
    fileSystem.createDirectories(path.parent)
    downloader.download(project, url, path)
    fileSystem.setExecutable(path)
  }

  fun obtainLanguageServerBinary(project: Project): DownloadStatus {
    val path = resolver.binaryPath()
    if (isDownloading.get()) {
      return DownloadStatus.Downloading
    }

    // the path can exist because the user has specified it
    if (fileSystem.exists(path)) {
      return DownloadStatus.Downloaded(path)
    } else {
      isDownloading.set(true)
      ApplicationManager.getApplication().executeOnPooledThread {
        runBlocking {
          try {
            val url = resolver.downloadUrl()
            prepAndDownload(project, url, path)
            isDownloading.set(false)
            languageServerManager.initialStart(project)
          } catch (ce: CancellationException) {
            Notifier.warn(DOWNLOAD_CANCELLED_MSG)
            throw ce
          }
        }
      }
      return DownloadStatus.Scheduled
    }
  }
}
