package com.github.garetht.typstintellij.editor

import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.jcef.JBCefBrowser
import java.awt.BorderLayout
import java.beans.PropertyChangeListener
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.SwingUtilities

class TypstPreviewEditor(private val project: Project, private val file: VirtualFile) :
    UserDataHolderBase(), FileEditor {

  private val panel = JPanel(BorderLayout())
  private val browser = JBCefBrowser()

  init {
    panel.add(browser.component, BorderLayout.CENTER)
    updatePreview()
  }

  private fun updatePreview() {
    val content = file.inputStream.readBytes().toString(Charsets.UTF_8)
    val html = convertToHtml(content)
    browser.loadHTML(html)
  }

  fun updatePreview(content: String) {
    SwingUtilities.invokeLater {
      val html = convertToHtml(content)
      browser.loadHTML(html)
    }
  }

  private fun convertToHtml(content: String): String {
    // Your custom conversion logic here
    return """
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <style>
                    body { 
                        font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
                        line-height: 1.6;
                        margin: 20px;
                        background: white;
                    }
                    .content {
                        max-width: 800px;
                        margin: 0 auto;
                    }
                    pre {
                        background: #f8f8f8;
                        border: 1px solid #ddd;
                        border-radius: 4px;
                        padding: 16px;
                        overflow-x: auto;
                    }
                </style>
            </head>
            <body>
                <div class="content">
                    ${processContent(content)}
                </div>
            </body>
            </html>
        """
        .trimIndent()
  }

  private fun processContent(content: String): String {
    // Implement your specific content processing logic
    // For example, if it's a custom markup language:
    return content
  }

  override fun getComponent(): JComponent = panel

  override fun getPreferredFocusedComponent(): JComponent? = browser.component

  override fun getName(): String = "Preview"

  override fun setState(state: FileEditorState) {}

  override fun getState(level: FileEditorStateLevel): FileEditorState = FileEditorState.INSTANCE

  override fun isModified(): Boolean = false

  override fun isValid(): Boolean = true

  override fun addPropertyChangeListener(p0: PropertyChangeListener) {}

  override fun removePropertyChangeListener(p0: PropertyChangeListener) {}

  override fun dispose() {
    browser.dispose()
  }

  override fun getFile(): VirtualFile = file
}
