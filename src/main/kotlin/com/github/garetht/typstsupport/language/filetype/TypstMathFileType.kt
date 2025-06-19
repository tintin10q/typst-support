package com.github.garetht.typstsupport.language.filetype

import com.github.garetht.typstsupport.TypstIcons
import com.github.garetht.typstsupport.language.language.TypstMathLanguage
import com.intellij.openapi.fileTypes.LanguageFileType
import org.jetbrains.annotations.NotNull
import javax.swing.Icon

class TypstMathFileType private constructor() : LanguageFileType(TypstMathLanguage.INSTANCE) {
  @NotNull
  override fun getName(): String {
    return "Typst Math"
  }

  @NotNull
  override fun getDescription(): String {
    return "Typst math file"
  }

  @NotNull
  override fun getDefaultExtension(): String {
    return "typm"
  }

  override fun getIcon(): Icon {
    return TypstIcons.TYPST_FILE
  }
}
