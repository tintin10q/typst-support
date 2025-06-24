package com.github.garetht.typstsupport.languageserver

import com.github.garetht.typstsupport.languageserver.models.Outline
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import com.intellij.platform.lsp.api.Lsp4jClient
import com.intellij.platform.lsp.api.LspServerNotificationsHandler
import org.eclipse.lsp4j.jsonrpc.services.JsonNotification

private val LOG = logger<TypstLspClient>()

class TypstLspClient(
  project: Project,
  serverNotificationsHandler: LspServerNotificationsHandler,
) : Lsp4jClient(serverNotificationsHandler) {
  @JsonNotification("tinymist/documentOutline")
  fun handleDocumentOutline(_: Outline) {
  }
}
