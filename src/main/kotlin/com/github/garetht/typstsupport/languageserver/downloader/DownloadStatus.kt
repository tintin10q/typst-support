package com.github.garetht.typstsupport.languageserver.downloader

import java.nio.file.Path

sealed interface DownloadStatus {
  data class Downloaded(val path: Path) : DownloadStatus

  data object Scheduled : DownloadStatus

  data object Downloading : DownloadStatus

  data object Failed : DownloadStatus
}
