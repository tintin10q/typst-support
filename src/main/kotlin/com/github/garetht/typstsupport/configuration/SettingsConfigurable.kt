package com.github.garetht.typstsupport.configuration

import com.github.garetht.typstsupport.configuration.PathValidation.Companion.validateBinaryFile
import com.github.garetht.typstsupport.languageserver.TypstLanguageServerManager
import com.github.garetht.typstsupport.languageserver.TypstSupportProvider
import com.github.garetht.typstsupport.notifier.Notifier
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.components.service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileChooser.FileChooserDescriptorFactory
import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.options.ConfigurationException
import com.intellij.openapi.project.ProjectManager
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
import kotlinx.coroutines.runBlocking
import javax.swing.JLabel
import javax.swing.SwingConstants

private val LOG = logger<TypstSupportProvider>()

class TypstSettingsConfigurable :
  BoundSearchableConfigurable("Typst Support Settings", "") {

  // our persisted settings
  private val settings = SettingsState.getInstance()

  // UI Elements
  private lateinit var fileField: Cell<TextFieldWithBrowseButton>
  private lateinit var customRadioButton: Cell<JBRadioButton>
  private lateinit var testResultLabel: Cell<JLabel>

  // Component-internal state
  private var binaryExecutionValidated: Boolean = false

  override fun createPanel(): DialogPanel {
    return panel {
      buttonsGroup("Binary location") {
        row {
          radioButton(
            "Use automatically downloaded binary",
            BinarySource.USE_AUTOMATIC_DOWNLOAD
          )
        }

        row {
          customRadioButton =
            radioButton(
              "Specify local binary location", BinarySource.USE_CUSTOM_BINARY
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
            )
              .enabledIf(customRadioButton.selected)
              .bindText(settings.state::customBinaryPath)
              .resizableColumn()
              .align(AlignX.FILL)
              .validationOnInput {
                validateBinaryFile(it.text).toValidationInfo(this)
              }
              .onChanged {
                binaryExecutionValidated = false
                resetTestResultLabel()
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
        .bind(settings.state::binarySource)
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

        binaryExecutionValidated = when (result) {
          is ExecutionValidation.Failed -> false
          is ExecutionValidation.Success -> true
        }
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

      if (!binaryExecutionValidated) {
        throw ConfigurationException("The binary should be tested with Test Binary before applying")
      }
    }

    super.apply()

    ApplicationManager.getApplication().invokeLater {
      val projectManager = ApplicationManager.getApplication().service<ProjectManager>()
      val openProjects = projectManager.openProjects
      val manager = TypstLanguageServerManager()
      openProjects.forEach { project ->
        ApplicationManager.getApplication().executeOnPooledThread {
          runBlocking {
            if (project.isDisposed) {
              return@runBlocking
            }
            manager.initialStart(project)
            Notifier.info(project, "Restarting Tinymist server...")
          }
        }
      }
    }
  }
}
