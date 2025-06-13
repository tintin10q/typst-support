package com.github.garetht.typstintellij.languageserver.files

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import java.nio.file.Path
import net.harawata.appdirs.AppDirsFactory
import java.net.URI

private const val TYPST_INTELLIJ_ID = "com.github.garetht.typstintellij"
private val version = Version(0, 13, 12)

class TinymistLocationResolver {
  private val jnaNoClassPathKey = "jna.noclasspath"
  private var jnaNoClassPath: String? = null

  private val binary = TinymistBinary(
    version = version,
    osName = OsName.fromString(System.getProperty("os.name")),
    osArchitecture = OsArchitecture.fromString(System.getProperty("os.arch")),
  )

  private fun pushJnaNoClassPathFalse() {
    jnaNoClassPath = System.getProperty(jnaNoClassPathKey)
    System.setProperty(jnaNoClassPathKey, "false")
  }

  private fun popJnaNoClassPath() {
    jnaNoClassPath?.let { System.setProperty(jnaNoClassPathKey, it) }
        ?: run { System.clearProperty(jnaNoClassPathKey) }
  }

  fun url(): URI = binary.downloadUrl

  fun path(): Path {
    PluginManagerCore.getPlugin(PluginId.getId(TYPST_INTELLIJ_ID))!!.run {
      pushJnaNoClassPathFalse()

      val appDirs = AppDirsFactory.getInstance()
      val path =
          Path.of(appDirs.getUserDataDir("Typst", null, "com.github.garetht.typstintellij"))
              .resolve("language-server")
              .resolve(binary.versionPath)
              .resolve(TinymistBinary.binaryFilename)

      popJnaNoClassPath()

      return path
    }
  }
}
