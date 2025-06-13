package com.github.garetht.typstintellij.languageserver

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.platform.lsp.api.LspServerSupportProvider

class TypstLspServerSupportProvider : LspServerSupportProvider {
    override fun fileOpened(project: Project,
                            virtualFile: VirtualFile,
                            lspServerStarter: LspServerSupportProvider.LspServerStarter
    ) {
    }
}
