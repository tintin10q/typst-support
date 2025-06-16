package com.github.garetht.typstsupport.configuration

import com.github.garetht.typstsupport.languageserver.locations.Version
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.execution.process.ProcessOutput
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.mockkObject
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertIs

class ExecutionValidationTest {
    @Nested
    inner class ValidateBinaryExecution {
        @TempDir
        lateinit var tempDir: File

        private lateinit var mockProcessExecutor: ProcessExecutor
        private lateinit var mockCommandLine: GeneralCommandLine
        private lateinit var mockProcessOutput: ProcessOutput

        @BeforeEach
        fun setUp() {
            mockProcessExecutor = mockk()
            mockCommandLine = mockk()
            mockProcessOutput = mockk()
            ExecutionValidation.processExecutor = mockProcessExecutor
            mockkObject(PathValidation)
            mockkObject(Version)
        }

        @AfterEach
        fun tearDown() {
            unmockkAll()
        }

        private fun setupCommandLineMock(exitCode: Int, stdout: String = "", stderr: String = "") {
            every { mockProcessOutput.exitCode } returns exitCode
            every { mockProcessOutput.stdout } returns stdout
            every { mockProcessOutput.stderr } returns stderr
            every { mockProcessExecutor.executeProcess(any()) } returns mockProcessOutput
        }

        @Test
        fun `should fail with empty path`() {
            every { PathValidation.validateBinaryFile("") } returns PathValidation.Failed("Path cannot be empty")
            val result = ExecutionValidation.validateBinaryExecution("")
            assertIs<ExecutionValidation.Failed>(result)
            assertEquals("Path cannot be empty", (result as ExecutionValidation.Failed).message)
        }

        @Test
        fun `should fail with blank path`() {
            every { PathValidation.validateBinaryFile("   ") } returns PathValidation.Failed("Path cannot be empty")
            val result = ExecutionValidation.validateBinaryExecution("   ")
            assertIs<ExecutionValidation.Failed>(result)
            assertEquals("Path cannot be empty", (result as ExecutionValidation.Failed).message)
        }

        @Test
        fun `should fail with non-existent file`() {
            every { PathValidation.validateBinaryFile("/nonexistent/path") } returns PathValidation.Failed("Binary file not found")
            val result = ExecutionValidation.validateBinaryExecution("/nonexistent/path")
            assertIs<ExecutionValidation.Failed>(result)
            assertEquals("Binary file not found", (result as ExecutionValidation.Failed).message)
        }

        @Test
        fun `should fail with non-executable file`() {
            val file = File(tempDir, "test")
            file.createNewFile()
            file.setExecutable(false)
            every { PathValidation.validateBinaryFile(file.absolutePath) } returns PathValidation.Failed("Permission denied - check file permissions")
            val result = ExecutionValidation.validateBinaryExecution(file.absolutePath)
            assertIs<ExecutionValidation.Failed>(result)
            assertEquals("Permission denied - check file permissions", (result as ExecutionValidation.Failed).message)
        }

        @Test
        fun `should fail with permission denied`() {
            val file = File(tempDir, "test")
            file.createNewFile()
            file.setExecutable(false)
            every { PathValidation.validateBinaryFile(file.absolutePath) } returns PathValidation.Success
            setupCommandLineMock(1, stderr = "Permission denied")
            val result = ExecutionValidation.validateBinaryExecution(file.absolutePath)
            assertIs<ExecutionValidation.Failed>(result)
            assertEquals("Permission denied - make binary executable", (result as ExecutionValidation.Failed).message)
        }

        @Test
        fun `should fail with no such file error`() {
            every { PathValidation.validateBinaryFile("/nonexistent/path") } returns PathValidation.Success
            setupCommandLineMock(1, stderr = "No such file or directory")
            val result = ExecutionValidation.validateBinaryExecution("/nonexistent/path")
            assertIs<ExecutionValidation.Failed>(result)
            assertEquals("Binary not found at specified path", (result as ExecutionValidation.Failed).message)
        }

        @Test
        fun `should fail with generic error`() {
            every { PathValidation.validateBinaryFile("/some/path") } returns PathValidation.Success
            setupCommandLineMock(1, stderr = "Some error occurred")
            val result = ExecutionValidation.validateBinaryExecution("/some/path")
            assertIs<ExecutionValidation.Failed>(result)
            assertEquals("Binary validation failed: Some error occurred", (result as ExecutionValidation.Failed).message)
        }

        @Test
        fun `should fail with invalid version output`() {
            val file = File(tempDir, "test")
            file.createNewFile()
            file.setExecutable(true)
            every { PathValidation.validateBinaryFile(file.absolutePath) } returns PathValidation.Success
            setupCommandLineMock(0, stdout = "invalid version")
            val result = ExecutionValidation.validateBinaryExecution(file.absolutePath)
            assertIs<ExecutionValidation.Failed>(result)
            assertEquals("Invalid binary output format", (result as ExecutionValidation.Failed).message)
        }

        @ParameterizedTest
        @ValueSource(strings = ["typst 0.1.0", "typst 1.0.0", "typst 10.20.30", "typst 0.0.1"])
        fun `should handle various valid version outputs`(version: String) {
            val file = File(tempDir, "test")
            file.createNewFile()
            file.setExecutable(true)
            every { PathValidation.validateBinaryFile(file.absolutePath) } returns PathValidation.Success
            val expectedVersion = Version.parseVersion(version) ?: when (version) {
                "typst 0.1.0" -> Version(0, 1, 0)
                "typst 1.0.0" -> Version(1, 0, 0)
                "typst 10.20.30" -> Version(10, 20, 30)
                "typst 0.0.1" -> Version(0, 0, 1)
                else -> null
            }
            every { Version.parseVersion(version) } returns expectedVersion
            setupCommandLineMock(0, stdout = version)
            val result = ExecutionValidation.validateBinaryExecution(file.absolutePath)
            assertIs<ExecutionValidation.Success>(result)
            assertEquals(expectedVersion, (result as ExecutionValidation.Success).version)
        }
    }
}
