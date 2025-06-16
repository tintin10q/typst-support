package com.github.garetht.typstsupport.configuration

interface PathValidator {
  fun validateBinaryFile(binaryPath: String): PathValidation
}
