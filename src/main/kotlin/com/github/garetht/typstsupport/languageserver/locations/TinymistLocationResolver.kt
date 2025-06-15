package com.github.garetht.typstsupport.languageserver.locations

import com.intellij.ide.plugins.PluginManagerCore
import com.intellij.openapi.extensions.PluginId
import net.harawata.appdirs.AppDirsFactory
import java.net.URI
import java.nio.file.Path

private const val TYPST_SUPPORT_ID = "com.github.garetht.typstsupport"
private val version = Version(0, 13, 12)

class TinymistLocationResolver {
  private val jnaNoClassPathKey = "jna.noclasspath"
  private var jnaNoClassPath: String? = null

  private val binary =
      TinymistBinary(
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
    PluginManagerCore.getPlugin(PluginId.getId(TYPST_SUPPORT_ID))!!.run {
      pushJnaNoClassPathFalse()

      val appDirs = AppDirsFactory.getInstance()
      val path =
          Path.of(appDirs.getUserDataDir("TypstSupport", null, "com.github.garetht.typstsupport"))
              .resolve("language-server")
              .resolve(binary.versionPath)
              .resolve(TinymistBinary.binaryFilename)

      popJnaNoClassPath()
      return path
    }
  }
}
