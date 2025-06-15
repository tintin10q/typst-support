package com.github.garetht.typstsupport.configuration

import com.github.garetht.typstsupport.languageserver.locations.Version
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import java.io.File

sealed interface ExecutionValidation {
  data class Success(val version: Version) : ExecutionValidation

  data class Failed(val message: String) : ExecutionValidation

  companion object {
    fun validateBinaryExecution(binaryPath: String): ExecutionValidation {
      when (val result = PathValidation.validateBinaryFile(binaryPath)) {
        is PathValidation.Failed -> Failed(result.message)
        PathValidation.Success -> {}
      }

      val file = File(binaryPath)
      return try {
        val commandLine = GeneralCommandLine(binaryPath, "-V")
        commandLine.workDirectory = file.parentFile

        val processHandler = CapturingProcessHandler(commandLine)
        val result = processHandler.runProcess(10000)

        if (result.exitCode == 0) {
          val output = result.stdout.trim()
          val version = Version.parseVersion(output)
          if (version != null) {
            Success(version)
          } else {
            Failed("Invalid binary output format")
          }
        } else {
          val errorOutput = result.stderr.ifEmpty { result.stdout }.trim()

          val errorMessage =
            when {
              errorOutput.contains("cannot be opened because the developer cannot be verified") ||
                      errorOutput.contains("malware") ||
                      errorOutput.contains("not trusted") ||
                      result.exitCode == 126 -> {
                "macOS blocked execution - check Security & Privacy settings"
              }

              errorOutput.contains("Permission denied") -> {
                "Permission denied - make binary executable"
              }

              errorOutput.contains("No such file") -> {
                "Binary not found at specified path"
              }

              else -> {
                "Binary validation failed: ${errorOutput.take(50)}"
              }
            }
          Failed(errorMessage)
        }
      } catch (e: Exception) {
        val message =
          when {
            e.message?.contains("Permission denied") == true ->
              "Permission denied - check file permissions"

            e.message?.contains("No such file") == true -> "Binary file not found"
            e.message?.contains("cannot execute") == true ->
              "Cannot execute binary - may be blocked by macOS security"

            else -> "Failed to execute binary: ${e.message?.take(50)}"
          }
        Failed(message)
      }
    }

  }
}
