package com.github.garetht.typstsupport.languageserver

import com.intellij.openapi.project.Project

interface LanguageServerManager {
  suspend fun initialStart(project: Project)
}
