package com.github.garetht.typstintellij.editor

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.event.DocumentEvent
import com.intellij.openapi.editor.event.DocumentListener
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.JComponent

class TypstSplitEditor(project: Project, file: VirtualFile) :
    TextEditorWithPreview(
        TextEditorProvider.getInstance().createEditor(project, file) as TextEditor,
        TypstPreviewEditor(project, file),
        "TypstSplitEditor",
    ) {

  init {
    // Add document listener for live HTML preview updates
    myEditor.editor.document.addDocumentListener(object : DocumentListener {
      override fun documentChanged(event: DocumentEvent) {
        // Debounce updates to avoid too frequent refreshes
        ApplicationManager.getApplication().invokeLater {
          (myPreview as TypstPreviewEditor).updatePreview(myEditor.editor.document.text)
        }
      }
    })
  }

  override fun getComponent(): JComponent {
    return super.getComponent()
  }
}
