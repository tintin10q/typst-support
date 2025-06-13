package com.github.garetht.typstintellij.languageserver.downloader

import com.github.garetht.typstintellij.languageserver.LanguageServerManager
import com.github.garetht.typstintellij.languageserver.TypstSupportProvider
import com.github.garetht.typstintellij.languageserver.downloader.LOG
import com.github.garetht.typstintellij.languageserver.files.TinymistLocationResolver
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import java.net.URI
import java.nio.file.Path
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.jvm.java

private val LOG = logger<TinymistDownloadScheduler>()

class TinymistDownloadScheduler(
    private val resolver: TinymistLocationResolver,
    private val downloader: TinymistDownloader,
    private val fileSystem: TypstPluginFileSystem,
    private val languageServerManager: LanguageServerManager
) {

    companion object {
        private val isDownloading = AtomicBoolean()
        fun resetDownloadingStatus() = isDownloading.set(false)
        private const val DOWNLOAD_CANCELLED_MSG = "You have cancelled the Typst Language Server download.\n\n To retry, restart the IDE."
    }

    private suspend fun prepAndDownload(project: Project, url: URI, path: Path) {
        fileSystem.createDirectories(path.parent)
        downloader.download(project, url, path)
        fileSystem.setExecutable(path)
    }

    fun scheduleDownloadIfRequired(project: Project): DownloadStatus {
        val path = resolver.path()
        val url = resolver.url()
        if (isDownloading.get()) {
            return DownloadStatus.Downloading
        }

        if (fileSystem.exists(path)) {
            return DownloadStatus.Downloaded(path)
        } else {
            isDownloading.set(true)
            ApplicationManager.getApplication().executeOnPooledThread {
                runBlocking {
                    try {
                        prepAndDownload(project, url, path)
                        languageServerManager.start(project, TypstSupportProvider::class.java)
                        isDownloading.set(false)
                    } catch(_: CancellationException) {
                        NotificationGroupManager.getInstance()
                            .getNotificationGroup("Typst")
                            .createNotification(DOWNLOAD_CANCELLED_MSG, NotificationType.WARNING)
                            .notify(project)
                    }
                }
            }
            return DownloadStatus.Scheduled
        }
    }

}
