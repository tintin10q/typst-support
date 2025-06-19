package com.github.garetht.typstsupport.languageserver

import com.github.garetht.typstsupport.getMockedProject
import com.intellij.openapi.vfs.VirtualFile
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.nio.file.Path
import java.util.UUID
import kotlin.test.assertEquals
import kotlin.test.assertTrue


class LanguageServerDescriptorTest {
  @ParameterizedTest
  @ValueSource(strings = ["typ", "typc", "typm"])
  fun shouldSupportTypstFileTypes(fileName: String) {
    // Arrange
    val descriptor = TinymistLSPDescriptor(
      Path.of(""),
      getMockedProject()
    )

    val mockFile = mockk<VirtualFile>(relaxed = true) {
      every { extension } returns fileName
    }

    // Act
    val isFileSupported = descriptor.isSupportedFile(mockFile)

    // Assert
    assertTrue(isFileSupported)
  }

  @ParameterizedTest
  @ValueSource(strings = ["txt", "java", "py", "js", "json"])
  fun shouldNotSupportOtherExtensions(fileName: String) {
    // Arrange
    val descriptor = TinymistLSPDescriptor(
      Path.of(""),
      getMockedProject()
    )

    val mockFile = mockk<VirtualFile>(relaxed = true) {
      every { extension } returns fileName
    }

    // Act
    val isFileSupported = descriptor.isSupportedFile(mockFile)

    // Assert
    assertFalse(isFileSupported)
  }

  @Test
  fun shouldCreateCommandLineFromPath() {
    // Arrange
    val path = Path.of("/" + UUID.randomUUID())
    val descriptor = TinymistLSPDescriptor(
      path,
      getMockedProject()
    )

    // Act
    val command = descriptor.createCommandLine()

    // Assert
    assertEquals(path.toString(), command.commandLineString)
  }
}
