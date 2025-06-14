package com.github.garetht.typstsupport.languageserver.locations

import java.net.URI
import kotlin.io.path.Path

data class TinymistBinary(
    private val version: Version,
    private val osName: OsName,
    private val osArchitecture: OsArchitecture
) {
  val versionPath = Path(version.toPathString())
  val compressedFilename
    get() =
        Path(
            "tinymist-${this.osArchitecture.toArchPath()}-${this.osName.toOsPath()}.${this.osName.toExtensionPath()}")

  val downloadUrl
    get() =
        URI(
            "https://github.com/Myriad-Dreamin/tinymist/releases/download/$versionPath/$compressedFilename")

  companion object {
    val binaryFilename = Path("tinymist")
  }
}
