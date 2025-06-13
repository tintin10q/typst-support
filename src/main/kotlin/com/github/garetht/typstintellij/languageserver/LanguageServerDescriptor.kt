package com.github.garetht.typstintellij.languageserver

import com.github.garetht.typstintellij.languageserver.locations.isSupportedTypstFileType
import com.intellij.execution.configurations.GeneralCommandLine
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.ProjectWideLspServerDescriptor
import java.nio.file.Path

class LanguageServerDescriptor(val languageServerPath: Path, project: Project) :
    ProjectWideLspServerDescriptor(project, "") {
  override fun createCommandLine(): GeneralCommandLine =
      GeneralCommandLine(languageServerPath.toString())

  override fun isSupportedFile(file: VirtualFile): Boolean = file.isSupportedTypstFileType()
}
