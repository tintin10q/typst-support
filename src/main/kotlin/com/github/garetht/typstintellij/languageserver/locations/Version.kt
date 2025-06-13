package com.github.garetht.typstintellij.languageserver.locations

data class Version(val major: Int, val minor: Int, val patch: Int) {
  fun toPathString(): String {
    return "v$major.$minor.$patch"
  }
}
