package com.github.garetht.typstsupport.configuration

import com.github.garetht.typstsupport.configuration.PathValidation.Companion.validateBinaryFile
import com.github.garetht.typstsupport.languageserver.TypstSupportProvider
import com.intellij.icons.AllIcons
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.TextFieldWithBrowseButton
import com.intellij.ui.JBColor
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.dsl.builder.AlignX
import com.intellij.ui.dsl.builder.Cell
import com.intellij.ui.dsl.builder.bind
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
import javax.swing.JLabel
import javax.swing.SwingConstants

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
  private lateinit var fileField: Cell<TextFieldWithBrowseButton>
  private lateinit var customRadioButton: Cell<JBRadioButton>
  private lateinit var testResultLabel: Cell<JLabel>

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

        row {
          customRadioButton =
            radioButton(
              "Specify local binary location", BinaryConfiguration.USE_CUSTOM_BINARY
            )
              .onChanged {
                if (it.isSelected) {
                  this@TypstSettingsConfigurable.resetTestResultLabel()
                }
              }
        }

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
                validateBinaryFile(it.text).toValidationInfo(this)
              }
        }

        row {
          button("Test Binary") {
            testBinaryExecution(fileField.component.text)
          }
            .enabledIf(customRadioButton.selected)

          testResultLabel = label("")
            .visibleIf(customRadioButton.selected)
          testResultLabel.component.horizontalAlignment = SwingConstants.LEFT
        }

      }
        .bind(
          { binaryConfig },
          { value ->
            binaryConfig = value
          })
    }
  }

  private fun resetTestResultLabel() {
    testResultLabel.component.text = ""
    testResultLabel.component.icon = null
  }

  private fun testBinaryExecution(binaryPath: String) {
    // Clear previous result
    this.resetTestResultLabel()

    // Run validation in background thread to avoid blocking UI
    ApplicationManager.getApplication().executeOnPooledThread {
      try {
        val result = ExecutionValidation.validateBinaryExecution(binaryPath)
        // Update UI on EDT
        ApplicationManager.getApplication().invokeLater({
          updateTestResult(result)
        }, ModalityState.any())
      } catch (e: Exception) {
        ApplicationManager.getApplication().invokeLater({
          updateTestResult(ExecutionValidation.Failed("Error during validation: ${e.message}"))
        }, ModalityState.any())
      }
    }
  }

  private fun updateTestResult(result: ExecutionValidation) {
    when (result) {
      is ExecutionValidation.Success -> {
        testResultLabel.component.icon = AllIcons.General.InspectionsOK
        testResultLabel.component.text = "Success: ${result.version.toConsoleString()}"
        testResultLabel.component.foreground = JBColor.GREEN
      }

      is ExecutionValidation.Failed -> {
        testResultLabel.component.icon = AllIcons.General.Error
        testResultLabel.component.text = "Failed: ${result.message}"
        testResultLabel.component.foreground = JBColor.RED
      }
    }
  }


  override fun apply() {
    if (customRadioButton.selected()) {
      val exception = validateBinaryFile(fileField.component.text)
        .toConfigurationException()
      if (exception != null) {
        throw exception
      }
    }

    super.apply()
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

  companion object {
    private const val USE_CUSTOM_BINARY_KEY = "typst.tinymist.useCustomBinary"
    private const val CUSTOM_BINARY_PATH_KEY = "typst.tinymist.customBinaryPath"
  }
}
