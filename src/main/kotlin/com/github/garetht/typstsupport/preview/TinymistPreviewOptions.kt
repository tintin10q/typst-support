package com.github.garetht.typstsupport.preview

import java.nio.file.Path

data class TinymistPreviewOptions(
  val partialRendering: Boolean? = null,
  val invertColors: InvertColors? = null,
  val root: Path? = null,
  val inputs: Map<String, String> = emptyMap(),
  val fontPaths: List<Path> = emptyList(),
  val ignoreSystemFonts: Boolean = false,
  val packagePath: Path? = null,
  val packageCachePath: Path? = null,
  val cert: Path? = null,
  val previewMode: PreviewMode? = null,
  val host: String = "",
  val openInBrowser: Boolean = true,
  val dataPlaneHostPort: Int? = null,
  val controlPlaneHostPort: Int? = null,
) {
  fun toCommandList(): List<String> {
    val command = mutableListOf("tinymist", "preview")

    if (partialRendering == true) {
      command.add("--partial-rendering")
    }

    when (val colors = invertColors) {
      is InvertColors.Auto -> command.add("--invert-colors=auto")
      is InvertColors.Never -> command.add("--invert-colors=never")
      is InvertColors.Always -> command.add("--invert-colors=always")
      is InvertColors.ByElement -> {
        val json = buildString {
          append("{")
          append("\"rest\": \"${colors.rest}\", ")
          append("\"image\": \"${colors.image}\"")
          append("}")
        }
        command.add("--invert-colors=$json")
      }

      null -> {}
    }

    root?.let { command.addAll(listOf("--root", it.toString())) }

    inputs.forEach { (key, value) ->
      command.addAll(listOf("--input", "$key=$value"))
    }

    fontPaths.forEach { path ->
      command.addAll(listOf("--font-path", path.toString()))
    }

    if (ignoreSystemFonts) {
      command.add("--ignore-system-fonts")
    }

    packagePath?.let { command.addAll(listOf("--package-path", it.toString())) }
    packageCachePath?.let { command.addAll(listOf("--package-cache-path", it.toString())) }
    cert?.let { command.addAll(listOf("--cert", it.toString())) }

    if (previewMode != null) {
      command.addAll(listOf("--preview-mode", previewMode.value))
    }

    if (host.isNotEmpty()) {
      command.addAll(listOf("--host", host))
    }

    if (!openInBrowser) {
      command.add("--no-open")
    }

    dataPlaneHostPort?.let { command.addAll(listOf("--data-plane-host", "127.0.0.1:$it")) }
    controlPlaneHostPort?.let { command.addAll(listOf("--control-plane-host", "127.0.0.1:$it")) }

    return command
  }

  sealed class InvertColors {
    object Auto : InvertColors()
    object Never : InvertColors()
    object Always : InvertColors()
    data class ByElement(
      val rest: InvertStrategy,
      val image: InvertStrategy
    ) : InvertColors()
  }

  enum class InvertStrategy {
    Always, Never, Auto;

    override fun toString(): String = name.lowercase()
  }

  enum class PreviewMode(val value: String) {
    Document("document"),
    Slide("slide")
  }
} 
