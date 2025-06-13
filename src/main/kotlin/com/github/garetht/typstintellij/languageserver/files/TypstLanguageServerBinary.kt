package com.github.garetht.typstintellij.languageserver.files

import java.net.URI
import kotlin.io.path.Path

data class TypstLanguageServerBinary(
    private val version: Version,
    private val osName: OsName,
    private val osArchitecture: OsArchitecture
) {
  val versionPath = Path(version.toPathString())
  val filename
    get() =
        Path(
            "tinymist-${this.osArchitecture.toArchPath()}-${this.osName.toOsPath()}.${this.osName.toExtensionPath()}")

  val downloadUrl
    get() = URI("https://github.com/Myriad-Dreamin/tinymist/releases/download/$versionPath/$filename")
}
