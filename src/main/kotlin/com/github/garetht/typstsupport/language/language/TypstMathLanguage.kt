package com.github.garetht.typstsupport.language.language

import com.intellij.lang.Language

class TypstMathLanguage : Language("TypstMath") {
  companion object {
    @JvmStatic val INSTANCE: TypstMathLanguage = TypstMathLanguage()
  }
}
