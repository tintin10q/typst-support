package com.github.garetht.typstsupport.configuration

enum class TypstFormatter {
  TYPSTFMT,
  TYPSTYLE;

  override fun toString(): String = when (this) {
    TYPSTFMT -> "typstfmt"
    TYPSTYLE -> "typstyle"
  }
}
