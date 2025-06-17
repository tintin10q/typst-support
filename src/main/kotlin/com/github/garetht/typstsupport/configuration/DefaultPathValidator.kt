package com.github.garetht.typstsupport.configuration

import java.io.File

class DefaultPathValidator : PathValidator {
  override fun validateBinaryFile(binaryPath: String): PathValidation {
    if (binaryPath.isEmpty()) {
      return PathValidation.Failed("Binary path is empty")
    }

    val file = File(binaryPath)
    if (!file.exists()) {
      return PathValidation.Failed("Binary file does not exist")
    }

    if (!file.canExecute()) {
      return PathValidation.Failed("Binary file is not executable")
    }

    return PathValidation.Success
  }
}
