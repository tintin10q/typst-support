package com.github.garetht.typstintellij.languageserver

import com.intellij.openapi.project.Project
import com.intellij.platform.lsp.api.LspServerManager
import com.intellij.platform.lsp.api.LspServerSupportProvider

class LanguageServerManager {
    fun start(project: Project, cls: Class<out LspServerSupportProvider>) {
        LspServerManager.getInstance(project)
            .startServersIfNeeded(cls)
    }
}
