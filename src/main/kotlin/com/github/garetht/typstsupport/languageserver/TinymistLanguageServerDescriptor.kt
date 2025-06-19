package com.github.garetht.typstsupport.languageserver

import com.github.garetht.typstsupport.configuration.SettingsState
import com.github.garetht.typstsupport.languageserver.locations.isSupportedTypstFileType
import com.google.gson.JsonObject
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor
import com.intellij.platform.lsp.api.customization.LspFormattingSupport
import java.nio.file.Path

private val LOG = logger<TinymistLSPDescriptor>()

class TinymistLSPDescriptor(val languageServerPath: Path, project: Project) :
  ProjectWideLspServerDescriptor(project, "") {

  val settings = SettingsState.getInstance()

  override fun createCommandLine(): GeneralCommandLine =
    GeneralCommandLine(languageServerPath.toString())

  override fun isSupportedFile(file: VirtualFile): Boolean = file.isSupportedTypstFileType()

  override val lspFormattingSupport: LspFormattingSupport? = object : LspFormattingSupport() {
    override fun shouldFormatThisFileExclusivelyByServer(
      file: VirtualFile,
      ideCanFormatThisFileItself: Boolean,
      serverExplicitlyWantsToFormatThisFile: Boolean
    ): Boolean {
      return file.isSupportedTypstFileType() || serverExplicitlyWantsToFormatThisFile
    }
  }

  override fun createInitializationOptions(): JsonObject? = JsonObject().apply {
    addProperty(
      "formatterMode",
      settings.state.formatter.toString()
    )
  }
}
