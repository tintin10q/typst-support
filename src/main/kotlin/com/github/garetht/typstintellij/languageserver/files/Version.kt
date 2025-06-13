package com.github.garetht.typstintellij.languageserver.files

data class Version(val major: Int, val minor: Int, val patch: Int) {
  fun toPathString(): String {
    return "v$major.$minor.$patch"
  }
}
