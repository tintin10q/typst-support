package com.github.garetht.typstsupport.languageserver

import com.github.garetht.typstsupport.configuration.PathValidator.Companion.isValidPath
import com.github.garetht.typstsupport.configuration.SettingsState
import com.github.garetht.typstsupport.languageserver.locations.isSupportedTypstFileType
import com.google.gson.JsonObject
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.BaseProjectDirectories.Companion.getBaseDirectories
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerDescriptor
import com.intellij.platform.lsp.api.customization.LspFormattingSupport
import java.nio.file.Path

private val LOG = logger<TinymistLanguageServerDescriptor>()

class TinymistLanguageServerDescriptor(val languageServerPath: Path, project: Project) :
  LspServerDescriptor(
    project,
    "Tinymist",
    // filtering for .isValidPath allows us to get around some Jupyter strangeness, where
    // a file called Remote Server is said to be one of the base directories, causing the
    // language server to crash
    *project.getBaseDirectories().filter { it.path.isValidPath() }.toTypedArray()
  ) {

  init {
    LOG.info("Language server project base dirs: ${project.getBaseDirectories().map { it.path }}")
  }

  val settings = SettingsState.getInstance()

  override fun createCommandLine(): GeneralCommandLine =
    GeneralCommandLine(languageServerPath.toString())

  override fun isSupportedFile(file: VirtualFile): Boolean = file.isSupportedTypstFileType()

  override val lspFormattingSupport: LspFormattingSupport = object : LspFormattingSupport() {
    override fun shouldFormatThisFileExclusivelyByServer(
      file: VirtualFile,
      ideCanFormatThisFileItself: Boolean,
      serverExplicitlyWantsToFormatThisFile: Boolean
    ): Boolean = file.isSupportedTypstFileType() || serverExplicitlyWantsToFormatThisFile
  }

  override fun createInitializationOptions(): JsonObject? = JsonObject().apply {
    addProperty(
      "formatterMode",
      settings.state.formatter.toString()
    )
  }
}
