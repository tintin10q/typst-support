package com.github.garetht.typstsupport.languageserver.models

data class OutlineItem(
  val title: String,
  val span: String? = null,
  val position: DocumentPosition? = null,
  val children: List<OutlineItem> = emptyList()
)
