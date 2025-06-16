package com.github.garetht.typstsupport.editor

import com.github.garetht.typstsupport.previewserver.TinymistPreviewServerManager
import com.intellij.openapi.fileEditor.TextEditor
import com.intellij.openapi.fileEditor.TextEditorWithPreview
import com.intellij.openapi.fileEditor.impl.text.TextEditorProvider
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import javax.swing.JComponent

class TypstSplitEditor(project: Project, file: VirtualFile) :
    TextEditorWithPreview(
        TextEditorProvider.getInstance().createEditor(project, file) as TextEditor,
        TypstPreviewEditor(file, TinymistPreviewServerManager(project)),
        "TypstSplitEditor",
    ) {

  override fun getComponent(): JComponent {
    return super.getComponent()
  }
}
