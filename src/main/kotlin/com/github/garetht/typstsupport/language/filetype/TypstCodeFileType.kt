package com.github.garetht.typstsupport.language.filetype

import com.github.garetht.typstsupport.TypstIcons
import com.github.garetht.typstsupport.language.language.TypstCodeLanguage
import com.intellij.openapi.fileTypes.LanguageFileType
import org.jetbrains.annotations.NotNull
import javax.swing.Icon

class TypstCodeFileType private constructor() : LanguageFileType(TypstCodeLanguage.INSTANCE) {
  @NotNull
  override fun getName(): String {
    return "Typst Code"
  }

  @NotNull
  override fun getDescription(): String {
    return "Typst code file"
  }

  @NotNull
  override fun getDefaultExtension(): String {
    return "typm"
  }

  override fun getIcon(): Icon {
    return TypstIcons.TYPST_FILE
  }
}
