package com.github.garetht.typstsupport.previewserver

interface PreviewServerManager {
  fun createServer(filename: String, callback: (Int) -> Unit)
  fun shutdownServer(filename: String)
}
