package com.github.garetht.typstsupport.configuration

import com.github.garetht.typstsupport.languageserver.locations.Version

sealed interface ExecutionValidation {
  data class Success(val version: Version) : ExecutionValidation
  data class Failed(val message: String) : ExecutionValidation
}

