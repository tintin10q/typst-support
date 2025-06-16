package com.github.garetht.typstsupport.configuration

interface ExecutionValidator {
  fun validateBinaryExecution(binaryPath: String): ExecutionValidation
}
