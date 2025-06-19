package com.github.garetht.typstsupport.languageserver.models

data class StartPreviewResponse(
  val staticServerPort: Int? = null,
  val staticServerAddr: String? = null,
  val dataPlanePort: Int? = null,
  val isPrimary: Boolean
)
