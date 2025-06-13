package com.github.garetht.typstintellij.languageserver.files

import com.intellij.openapi.vfs.VirtualFile


private val supportedTypstExtensions = setOf("typ", "typm", "typc")

fun VirtualFile.isSupportedTypstFileType(): Boolean {
  return supportedTypstExtensions.contains(extension)
}
