package com.github.garetht.typstsupport.languageserver

import com.github.garetht.typstsupport.getMockedProject
import com.github.garetht.typstsupport.languageserver.downloader.DownloadStatus
import com.github.garetht.typstsupport.languageserver.downloader.TinymistDownloadScheduler
import com.github.garetht.typstsupport.mockIntelliJEnvironment
import com.intellij.openapi.project.Project
import com.intellij.platform.lsp.api.LspServerSupportProvider
import com.intellij.testFramework.fixtures.BasePlatformTestCase
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.nio.file.Path
import java.util.UUID

class TypstManagerTest {
  @BeforeEach
  fun setup() {
    mockIntelliJEnvironment {}
  }

  @AfterEach
  fun teardown() {
    unmockkAll()
  }

  private fun getImmediateDownloadScheduler(
    project: Project,
    expectedDownloadPath: Path
  ): TinymistDownloadScheduler {
    return mockk<TinymistDownloadScheduler>(relaxed = true) {
      every {
        obtainLanguageServerBinary(project)
      } returns DownloadStatus.Downloaded(expectedDownloadPath)
    }
  }

  private fun getImmediateDownloadScheduler(project: Project) = getImmediateDownloadScheduler(project, Path.of(""))

  @Test
  fun giventypstFilePresence_EnsureStartedIfPresent() {
    // Arrange
    val project = getMockedProject()
    val serverStarter = mockk<LspServerSupportProvider.LspServerStarter>(relaxed = true)

    val typstManager = TypstManager(getImmediateDownloadScheduler(project), project, serverStarter)

    // Act
    typstManager.startIfRequired()

    // Assert
    verify(exactly = 1) { serverStarter.ensureServerStarted(ofType(TinymistLanguageServerDescriptor::class)) }
  }

  @Test
  fun givenStarting_EnsureLanguageServerIsDownloaded() {
    // Arrange
    val project = getMockedProject()
    val serverStarter = mockk<LspServerSupportProvider.LspServerStarter>(relaxed = true)
    val typstLsDownloader = getImmediateDownloadScheduler(project)

    val typstManager = TypstManager(typstLsDownloader, project, serverStarter)

    // Act
    typstManager.startIfRequired()

    // Assert
    verify(exactly = 1) { typstLsDownloader.obtainLanguageServerBinary(project) }
  }

  @Test
  fun givenStarting_EnsureStartedWithLocationDownloadedTo() {
    // Arrange
    val project = getMockedProject()
    val serverStarter = mockk<LspServerSupportProvider.LspServerStarter>(relaxed = true)
    val mockPath = Path.of("/" + UUID.randomUUID())
    val typstLsDownloader = getImmediateDownloadScheduler(project, mockPath)

    val typstManager = TypstManager(typstLsDownloader, project, serverStarter)

    // Act
    typstManager.startIfRequired()

    // Assert
    verify { serverStarter.ensureServerStarted(match<TinymistLanguageServerDescriptor> { it.languageServerPath == mockPath }) }
  }
}
