package com.github.garetht.typstintellij.editor

import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.fileEditor.FileEditor
import com.intellij.openapi.fileEditor.FileEditorState
import com.intellij.openapi.fileEditor.FileEditorStateLevel
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.UserDataHolderBase
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.ui.jcef.JBCefBrowser
import java.awt.BorderLayout
import java.awt.event.InputEvent
import java.beans.PropertyChangeListener
import java.util.Timer
import javax.swing.JComponent
import javax.swing.JPanel

private val LOG = logger<TypstPreviewEditor>()

class TypstPreviewEditor(private val project: Project, private val file: VirtualFile) :
    UserDataHolderBase(), FileEditor {

  private val panel = JPanel(BorderLayout())
  private val browser = JBCefBrowser.createBuilder()
    .setOffScreenRendering(false)
    .setUrl("http://127.0.0.1:23625/")
    .setMouseWheelEventEnable(false)
    .build()


  // Trackpad detection and debouncing
  private var lastEventTime = 0L
  private val eventTimes = mutableListOf<Long>()
  private var accumulatedDeltaX = 0.0
  private var accumulatedDeltaY = 0.0
  private var debounceTimer: Timer? = null

  init {
    panel.add(browser.component, BorderLayout.CENTER)

    // Inject CSS to disable scrolling in the browser
    browser.cefBrowser.executeJavaScript("""
//    document.documentElement.style.overflow = 'hidden';
//    document.body.style.overflow = 'hidden';
    
    window.addEventListener('wheel', function(e) {
      e.preventDefault();
      e.stopPropagation();
    }, { passive: false });
    
    window.addEventListener('scroll', function(e) {
      e.preventDefault();
      e.stopPropagation();
    }, { passive: false });
  """.trimIndent(), "", 0)


    browser.component.addMouseWheelListener { e ->
      // Check if Shift key is pressed
      val isShiftPressed = (e.modifiersEx and InputEvent.SHIFT_DOWN_MASK) != 0

      // Calculate the scroll delta
      val scrollDelta = e.wheelRotation * e.scrollAmount * 3.0

      // Convert to horizontal scroll if Shift is pressed, otherwise vertical
      val deltaX = if (isShiftPressed) {
        scrollDelta
      } else {
        0.0
      }
      val deltaY = if (isShiftPressed) {
        0.0
      } else {
        scrollDelta
      }

      handleMouseWheelScroll(deltaX, deltaY)
    }
  }

  private fun handleMouseWheelScroll(deltaX: Double, deltaY: Double) {
    LOG.warn("Handling mouse wheel scroll: deltaX=$deltaX, deltaY=$deltaY")
    // Direct transmission for mouse wheel events
    transmitScrollToPage(deltaX, deltaY)
  }

  private fun transmitScrollToPage(deltaX: Double, deltaY: Double) {
    val scrollScript = """
    (function() {
      // Create and dispatch a wheel event
      var wheelEvent = new WheelEvent('wheel', {
        deltaX: $deltaX,
        deltaY: $deltaY,
        deltaZ: 0,
        deltaMode: WheelEvent.DOM_DELTA_PIXEL,
        bubbles: true,
        cancelable: false,
        view: window
      });
      
      // Dispatch to the document
      document.dispatchEvent(wheelEvent);
      
      // Alternative: Manually scroll the page
      window.scrollBy($deltaX, $deltaY);
    })();
  """.trimIndent()

    browser.cefBrowser.executeJavaScript(scrollScript, "", 0)
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
