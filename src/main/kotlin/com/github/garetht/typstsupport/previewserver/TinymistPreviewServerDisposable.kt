package com.github.garetht.typstsupport.previewserver

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project


@Service(Service.Level.APP, Service.Level.PROJECT)
class TinymistPreviewServerDisposable : Disposable {
  companion object {
    fun getInstance(): Disposable {
      return ApplicationManager.getApplication().getService(TinymistPreviewServerDisposable::class.java)
    }

    fun getInstance(project: Project): Disposable {
      return project.getService(TinymistPreviewServerDisposable::class.java)
    }
  }

  override fun dispose() {
  }
}
