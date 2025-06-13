package com.github.garetht.typstintellij.languageserver.downloader

import com.github.garetht.typstintellij.languageserver.locations.TinymistBinary
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.platform.ide.progress.withBackgroundProgress
import com.intellij.platform.util.progress.reportSequentialProgress
import kotlinx.coroutines.ensureActive
import org.apache.commons.compress.archivers.ArchiveEntry
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.archivers.zip.ZipArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path

private val LOG = logger<TinymistDownloader>()

class TinymistDownloader {
  suspend fun download(project: Project, uri: URI, path: Path) =
      withBackgroundProgress(
          project,
          title = "Typst",
          cancellable = true,
      ) {
        val readBuffer = ByteArray(4096)
        var readLen: Int
        val destination = path.parent

        LOG.warn("Downloading Tinymist from $uri")

        reportSequentialProgress(1) { reporter ->
          reporter.indeterminateStep("Checking Tinymist Language Server...")

          val archiveInputStream = createArchiveInputStream(uri)

          archiveInputStream?.use { stream ->
            var entry: ArchiveEntry?
            while (stream.nextEntry.also { entry = it } != null) {
              val currentEntry = entry!!

              // Skip directories
              if (currentEntry.isDirectory) {
                continue
              }

              // Extract to fixed path: just "tinymist" in the destination directory
              val extractedFile = destination.resolve(TinymistBinary.binaryFilename).toFile()
              val tempFile = File(extractedFile.path + ".tmp")

              LOG.warn("Extracting `${currentEntry.name}` to `${extractedFile.path}`")
              reporter.itemStep("Downloading ${currentEntry.name}...") {
                val estimatedSize = currentEntry.size.toInt()

                reportSequentialProgress(estimatedSize) { innerReporter ->
                  FileOutputStream(tempFile).use { outputStream ->
                    while (stream.read(readBuffer).also { readLen = it } != -1) {
                      ensureActive()
                      innerReporter.sizedStep(readLen)
                      outputStream.write(readBuffer, 0, readLen)
                    }
                  }
                  Files.move(tempFile.toPath(), extractedFile.toPath())
                }
              }
            }
          }
        }

        LOG.warn("Tinymist downloaded and extracted.")
      }

  private fun createArchiveInputStream(uri: URI): ArchiveInputStream<*>? {
    val inputStream = BufferedInputStream(uri.toURL().openStream())
    val uriString = uri.toString().lowercase()

    return when {
      uriString.endsWith(".zip") -> {
        ZipArchiveInputStream(inputStream)
      }
      uriString.endsWith(".tar.gz") || uriString.endsWith(".tgz") -> {
        TarArchiveInputStream(GzipCompressorInputStream(inputStream))
      }
      uriString.endsWith(".tar") -> {
        TarArchiveInputStream(inputStream)
      }
      else -> {
        LOG.error("Tinymist archive was not in a recognized format.")
        null
      }
    }
  }
}
