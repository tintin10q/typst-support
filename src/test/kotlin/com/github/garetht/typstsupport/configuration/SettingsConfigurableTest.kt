package com.github.garetht.typstsupport.configuration

import com.github.garetht.typstsupport.languageserver.locations.Version
import com.intellij.icons.AllIcons
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.options.ConfigurationException
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import com.intellij.testFramework.runInEdtAndWait
import com.intellij.util.ui.UIUtil
import io.mockk.mockk
import io.mockk.unmockkAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class SettingsConfigurableTest : BasePlatformTestCase() {
  @BeforeEach
  override fun setUp() {
    super.setUp()
  }

  @AfterEach
  fun `Remove all mocks`() {
    unmockkAll()
  }

  @Nested
  inner class PanelInitialization {
    @Test
    fun `panel initializes with correct default state`() {
      val settings = SettingsState()
      val configurable = TypstSettingsConfigurable(
        mockk(),
        mockk(),
        settings
      )
      configurable.createPanel()
      assertNotNull(configurable.fileField)
      assertNotNull(configurable.customRadioButton)
      assertNotNull(configurable.testResultLabel)
      assertEquals("", configurable.fileField.component.text)
      assertEquals(BinarySource.USE_AUTOMATIC_DOWNLOAD, settings.state.binarySource)
    }

    @Test
    fun `panel initializes with custom binary path if previously set`() {
      val settings = SettingsState()
      settings.state.customBinaryPath = "/path/to/binary"
      settings.state.binarySource = BinarySource.USE_CUSTOM_BINARY

      val configurable = TypstSettingsConfigurable(
        mockk(),
        mockk(),
        settings
      )
      configurable.createPanel()

      assertEquals("/path/to/binary", configurable.fileField.component.text)
      assertTrue(configurable.customRadioButton.component.isSelected)
    }
  }

  @Nested
  inner class BinarySourceSelection {
    @Test
    fun `switching to custom binary resets validation and label`() {
      val settings = SettingsState()
      val configurable = TypstSettingsConfigurable(
        mockk(),
        mockk(),
        settings
      )

      configurable.createPanel()

      configurable.testResultLabel.component.text = "Success!"
      configurable.testResultLabel.component.icon = AllIcons.General.InspectionsOK

      configurable.customRadioButton.component.doClick()

      assertEquals("", configurable.testResultLabel.component.text)
      assertNull(configurable.testResultLabel.component.icon)
    }

    @Test
    fun `switching to automatic download disables file field`() {
      val settings = SettingsState()
      val configurable = TypstSettingsConfigurable(
        mockk(),
        mockk(),
        settings
      )

      configurable.createPanel()
      configurable.customRadioButton.component.doClick()
      assertTrue(configurable.fileField.component.isEnabled)

      configurable.automaticRadioButton.component.doClick()
      assertFalse(configurable.fileField.component.isEnabled)
    }
  }

  @Nested
  inner class FilePathValidation {
    @Test
    fun `custom binary path validation fails when path is incorrect`() {
      val settings = SettingsState()
      val failureMessage = "Path not a binary"
      val configurable = TypstSettingsConfigurable(
        object : PathValidator {
          override fun validateBinaryFile(binaryPath: String): PathValidation =
            PathValidation.Success
        }, object : ExecutionValidator {
          override fun validateBinaryExecution(binaryPath: String): ExecutionValidation =
            ExecutionValidation.Failed(failureMessage)
        },
        settings
      )

      configurable.createPanel()
      configurable.customRadioButton.component.doClick()
      configurable.fileField.component.textField.text = ""

      configurable.testBinaryButton.component.doClick()

      Thread.sleep(750)
      ApplicationManager.getApplication().invokeLater {
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
        UIUtil.dispatchAllInvocationEvents()

        assertEquals(
          "Failed: $failureMessage",
          configurable.testResultLabel.component.text
        )
      }
    }

    @Test
    fun `custom binary path validation succeeds with correct path`() {
      val settings = SettingsState()
      val version = Version(1, 2, 3)
      val configurable = TypstSettingsConfigurable(
        object : PathValidator {
          override fun validateBinaryFile(binaryPath: String): PathValidation =
            PathValidation.Success
        }, object : ExecutionValidator {
          override fun validateBinaryExecution(binaryPath: String): ExecutionValidation =
            ExecutionValidation.Success(version)
        },
        settings
      )

      configurable.createPanel()
      configurable.customRadioButton.component.doClick()
      configurable.fileField.component.textField.text = "/valid/path"

      configurable.testBinaryButton.component.doClick()

      ApplicationManager.getApplication().invokeLater {
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
        UIUtil.dispatchAllInvocationEvents()

        assertEquals(
          "Success: ${version.toConsoleString()}",
          configurable.testResultLabel.component.text
        )
        assertEquals(AllIcons.General.InspectionsOK, configurable.testResultLabel.component.icon)
      }
    }

    @Nested
    inner class SettingsApplication {
      @Test
      fun `apply throws ConfigurationException when custom binary is not validated`() {
        val settings = SettingsState()
        val configurable = TypstSettingsConfigurable(
          object : PathValidator {
            override fun validateBinaryFile(binaryPath: String): PathValidation = PathValidation.Success
          },
          object : ExecutionValidator {
            override fun validateBinaryExecution(binaryPath: String): ExecutionValidation =
              ExecutionValidation.Success(Version(1, 2, 3))
          },
          settings
        )

        configurable.createPanel()
        configurable.customRadioButton.component.doClick()
        configurable.fileField.component.textField.text = "/path/to/binary"

        val exception = assertThrows<ConfigurationException> {
          configurable.apply()
        }
        assertEquals("The binary should be tested with Test Binary before applying", exception.message)
      }

      @Test
      fun `apply succeeds when custom binary is validated`() {
        val settings = SettingsState()
        val configurable = TypstSettingsConfigurable(
          object : PathValidator {
            override fun validateBinaryFile(binaryPath: String): PathValidation =
              PathValidation.Success
          }, object : ExecutionValidator {
            override fun validateBinaryExecution(binaryPath: String): ExecutionValidation =
              ExecutionValidation.Success(Version(1, 2, 3))
          },
          settings
        )

        configurable.createPanel()
        configurable.customRadioButton.component.doClick()
        configurable.fileField.component.textField.text = "/path/to/binary"

        configurable.testBinaryButton.component.doClick()

        configurable.apply()

        Thread.sleep(750)
        ApplicationManager.getApplication().invokeLater {
          PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
          UIUtil.dispatchAllInvocationEvents()
          assertEquals(BinarySource.USE_CUSTOM_BINARY, settings.state.binarySource)
          assertEquals("/path/to/binary", settings.state.customBinaryPath)
        }
      }

      @Test
      fun `apply succeeds with automatic download selected`() {
        val settings = SettingsState()
        val configurable = TypstSettingsConfigurable(
          mockk(),
          mockk(),
          settings
        )

        configurable.createPanel()
        configurable.automaticRadioButton.component.doClick()

        Thread.sleep(750)
        ApplicationManager.getApplication().invokeLater {
          PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
          UIUtil.dispatchAllInvocationEvents()

          configurable.apply()
          assertEquals(BinarySource.USE_AUTOMATIC_DOWNLOAD, settings.state.binarySource)
        }
      }
    }

    @Nested
    inner class ErrorHandling {
      @Test
      fun `handles validation error during binary test`() {
        val settings = SettingsState()
        val errorMessage = "Unexpected error during validation"
        val configurable = TypstSettingsConfigurable(
          object : PathValidator {
            override fun validateBinaryFile(binaryPath: String): PathValidation =
              PathValidation.Success
          }, object : ExecutionValidator {
            override fun validateBinaryExecution(binaryPath: String): ExecutionValidation =
              ExecutionValidation.Failed(errorMessage)
          },
          settings
        )

        configurable.createPanel()
        configurable.customRadioButton.component.doClick()
        configurable.fileField.component.textField.text = "/path/to/binary"

        configurable.testBinaryButton.component.doClick()

        Thread.sleep(750)
        ApplicationManager.getApplication().invokeLater {
          PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
          UIUtil.dispatchAllInvocationEvents()
          assertEquals(
            "Failed: $errorMessage",
            configurable.testResultLabel.component.text
          )
          assertEquals(AllIcons.General.Error, configurable.testResultLabel.component.icon)
        }
      }
    }

    @Test
    fun `handles validation error when applying invalidly`() {
      val settings = SettingsState()
      val errorMessage = "Invalid binary path"
      val configurable = TypstSettingsConfigurable(
        object : PathValidator {
          override fun validateBinaryFile(binaryPath: String): PathValidation =
            PathValidation.Failed(errorMessage)
        }, mockk(),
        settings
      )

      configurable.createPanel()
      configurable.customRadioButton.component.doClick()
      configurable.fileField.component.textField.text = "/invalid/path"

      val exception = assertThrows<ConfigurationException> {
        configurable.apply()
      }

      Thread.sleep(750)
      LOG.warn(errorMessage)
      LOG.warn(exception.message)
      ApplicationManager.getApplication().invokeLater {
        PlatformTestUtil.dispatchAllEventsInIdeEventQueue()
        UIUtil.dispatchAllInvocationEvents()

        assertEquals(errorMessage, exception.message)
      }
    }
  }
}
