package com.github.garetht.typstsupport.configuration

import com.intellij.openapi.options.BoundSearchableConfigurable
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.ui.dsl.builder.panel

class TypstConfigurable(private val project: Project) : BoundSearchableConfigurable(
  "Typst Settings",
  "typst.settings"
) {
  override fun createPanel(): DialogPanel {
    return panel {
      row {
        label("Hello! This is a simple settings panel.")
      }
      row {
        label("Success - your configurable is working!")
      }
    }
  }
}
