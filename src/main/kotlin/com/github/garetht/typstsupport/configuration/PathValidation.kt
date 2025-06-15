package com.github.garetht.typstsupport.configuration

import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.ui.ValidationInfo
import com.intellij.ui.layout.ValidationInfoBuilder
import java.io.File

// Result sealed class to represent validation outcomes
sealed interface PathValidation {
  data object Success : PathValidation

  data class Failed(val message: String) : PathValidation

  fun toValidationInfo(context: ValidationInfoBuilder): ValidationInfo? = when (this) {
    is Failed -> context.error(message)
    Success -> null
  }

  fun toConfigurationException(): ConfigurationException? = when (this) {
    is Failed -> ConfigurationException(message)
    Success -> null
  }

  companion object {
    fun validateBinaryFile(binaryPath: String): PathValidation {
      if (binaryPath.isEmpty()) {
        return Failed("Binary path is empty")
      }

      val file = File(binaryPath)
      if (!file.exists()) {
        return Failed("Binary file does not exist")
      }

      if (!file.canExecute()) {
        return Failed("Binary file is not executable")
      }

      return Success
    }
  }
}
