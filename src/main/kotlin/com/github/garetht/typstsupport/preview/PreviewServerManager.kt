package com.github.garetht.typstsupport.preview

import com.intellij.openapi.diagnostic.logger


interface PreviewServerManager {
  fun createServer(filename: String)
  fun shutdownServer(filename: String)
}
