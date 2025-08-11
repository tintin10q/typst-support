package com.github.garetht.typstsupport.structureview

import com.github.garetht.typstsupport.languageserver.TypstLanguageServerManager
import com.github.garetht.typstsupport.languageserver.TypstLspServerSupportProvider
import com.github.garetht.typstsupport.languageserver.models.Outline
import com.github.garetht.typstsupport.languageserver.models.OutlineItem
import com.google.gson.Gson
import com.intellij.platform.lsp.api.LspServerManager
import com.intellij.psi.PsiFile
import kotlinx.coroutines.runBlocking
import org.eclipse.lsp4j.ExecuteCommandParams

internal object TypstStructureParser {
    fun parse(file: PsiFile): List<OutlineItem> = runBlocking {
        val server = TypstLanguageServerManager.waitForServer(
            LspServerManager.getInstance(file.project),
            TypstLspServerSupportProvider::class.java
        ) ?: return@runBlocking emptyList<OutlineItem>()

        val result = server.sendRequestSync {
            it.workspaceService.executeCommand(
                ExecuteCommandParams(
                    "tinymist.documentOutline",
                    listOf(file.virtualFile.path)
                )
            )
        }
        val gson = Gson()
        val outline = gson.fromJson(gson.toJson(result), Outline::class.java)
        outline.items
    }
}
