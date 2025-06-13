package com.github.garetht.typstintellij.languageserver.files

enum class OsName {
  Mac,
  Windows,
  Linux;

  fun toOsPath(): String {
    return when (this) {
      Mac -> "apple-darwin"
      Windows -> "pc-windows-msvc"
      Linux -> "unknown-linux-gnu"
    }
  }

  fun toExtensionPath(): String {
    return when (this) {
      Mac,
      Linux -> ".tar.gz"
      Windows -> ".zip"
    }
  }

  companion object {
    fun fromString(name: String?): OsName {
      val name = name.orEmpty().lowercase().trim()
      return when {
        "mac" in name -> Mac
        "windows" in name -> Windows
        else -> Linux
      }
    }
  }
}
