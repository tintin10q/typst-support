package com.github.garetht.typstsupport.configuration

import com.github.garetht.typstsupport.languageserver.TypstSupportProvider
import com.github.garetht.typstsupport.languageserver.locations.Version
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.CapturingProcessHandler
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.bind
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
import java.io.File
import javax.swing.JLabel

private val LOG = logger<TypstSupportProvider>()

class TypstSettingsConfigurable(private val project: Project) :
  BoundSearchableConfigurable("Typst Support Settings", "") {

  private enum class BinaryConfiguration {
    USE_AUTOMATIC_DOWNLOAD,
    USE_CUSTOM_BINARY
  }

  // Remove custom setters - let the UI binding handle updates
  private var binaryConfig = BinaryConfiguration.USE_AUTOMATIC_DOWNLOAD
  private var customBinaryPath = ""

  private val propertiesComponent = PropertiesComponent.getInstance(project)

  init {
    loadSettings()
  }

  override fun createPanel(): DialogPanel {
    return panel {
      buttonsGroup("Binary location") {
        row {
          radioButton(
            "Use automatically downloaded binary",
            BinaryConfiguration.USE_AUTOMATIC_DOWNLOAD
          )
        }

        lateinit var customRadioButton: Cell<JBRadioButton>
        row {
          customRadioButton =
            radioButton(
              "Specify local binary location", BinaryConfiguration.USE_CUSTOM_BINARY
            )
        }
//
        lateinit var fileField: Cell<*>
        row {
          fileField =
            textFieldWithBrowseButton(
              fileChooserDescriptor =
                FileChooserDescriptorFactory.singleFile()
                  .withTitle("Select Binary File")
                  .withFileFilter { virtualFile ->
                    val extension = virtualFile.extension
                    extension == null || extension.lowercase() == "exe"
                  },
              project = project
            )
              .enabledIf(customRadioButton.selected)
              .bindText(::customBinaryPath)
              .resizableColumn()
              .align(AlignX.FILL)
              .validationOnInput {
                when (val result = validateBinary(it.text)) {
                  is ValidationResult.Failed -> error(result.message)
                  is ValidationResult.Success -> null
                }
              }
        }
      }
        .bind(
          { binaryConfig },
          { value ->
            LOG.warn("Binary config changed to $value")
            binaryConfig = value
          })
    }
  }

  override fun apply() {
    super.apply()
    LOG.warn("applying!!")
    saveSettings()
  }

  override fun reset() {
    loadSettings()
    super.reset()
  }

  private fun loadSettings() {
    val useCustomBinary = propertiesComponent.getBoolean(USE_CUSTOM_BINARY_KEY, false)
    binaryConfig =
      if (useCustomBinary) {
        BinaryConfiguration.USE_CUSTOM_BINARY
      } else {
        BinaryConfiguration.USE_AUTOMATIC_DOWNLOAD
      }

    customBinaryPath = propertiesComponent.getValue(CUSTOM_BINARY_PATH_KEY, "")
  }

  private fun saveSettings() {
    val useCustomBinary = binaryConfig == BinaryConfiguration.USE_CUSTOM_BINARY
    propertiesComponent.setValue(USE_CUSTOM_BINARY_KEY, useCustomBinary)
    propertiesComponent.setValue(CUSTOM_BINARY_PATH_KEY, customBinaryPath)
  }

  // Result sealed class to represent validation outcomes
  sealed interface ValidationResult {
    data class Success(val message: String) : ValidationResult

    data class Failed(val message: String) : ValidationResult
  }

  private fun validateBinary(binaryPath: String): ValidationResult {
    if (binaryPath.isEmpty()) {
      return ValidationResult.Failed("Binary path is empty")
    }

    val file = File(binaryPath)
    if (!file.exists()) {
      return ValidationResult.Failed("Binary file does not exist")
    }

    if (!file.canExecute()) {
      return ValidationResult.Failed("Binary file is not executable")
    }

    return try {
      val commandLine = GeneralCommandLine(binaryPath, "-V")
      commandLine.workDirectory = file.parentFile

      val processHandler = CapturingProcessHandler(commandLine)
      val result = processHandler.runProcess(10000)

      if (result.exitCode == 0) {
        val output = result.stdout.trim()
        val version = Version.parseVersion(output)
        if (version != null) {
          ValidationResult.Success("Valid tinymist binary (${version.toPathString()})")
        } else {
          ValidationResult.Failed("Invalid binary output format")
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
        ValidationResult.Failed(errorMessage)
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
      ValidationResult.Failed(message)
    }
  }


  companion object {
    private const val USE_CUSTOM_BINARY_KEY = "typst.tinymist.useCustomBinary"
    private const val CUSTOM_BINARY_PATH_KEY = "typst.tinymist.customBinaryPath"
  }
}
