package com.healthmine.testforge.config

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.context.request.WebRequest
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime

@ControllerAdvice
class GlobalExceptionHandler {

    @ExceptionHandler(Exception::class)
    fun handleAllExceptions(ex: Exception, request: WebRequest): ResponseEntity<ErrorResponse> {
        val errorDetails = ErrorResponse(
            timestamp = LocalDateTime.now(),
            message = ex.localizedMessage ?: "An error occurred",
            details = request.getDescription(false)
        )
        return ResponseEntity(errorDetails, HttpStatus.INTERNAL_SERVER_ERROR)
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationExceptions(ex: MethodArgumentNotValidException): ResponseEntity<ValidationErrorResponse> {
        val errors = ex.bindingResult.fieldErrors.associate { it.field to it.defaultMessage }
        val errorDetails = ValidationErrorResponse(
            timestamp = LocalDateTime.now(),
            message = "Validation failed",
            errors = errors
        )
        return ResponseEntity(errorDetails, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatchException(
        ex: MethodArgumentTypeMismatchException,
        request: WebRequest
    ): ResponseEntity<ErrorResponse> {
        val errorDetails = ErrorResponse(
            timestamp = LocalDateTime.now(),
            message = "Parameter '${ex.name}' should be of type '${ex.requiredType?.simpleName}'",
            details = request.getDescription(false)
        )
        return ResponseEntity(errorDetails, HttpStatus.BAD_REQUEST)
    }

    @ExceptionHandler(ResponseStatusException::class)
    fun handleResponseStatusException(ex: ResponseStatusException): ResponseEntity<ErrorResponse> {
        val errorDetails = ErrorResponse(
            timestamp = LocalDateTime.now(),
            message = ex.reason ?: "Error occurred",
            details = ""
        )
        return ResponseEntity(errorDetails, ex.statusCode)
    }

    data class ErrorResponse(
        val timestamp: LocalDateTime,
        val message: String,
        val details: String
    )

    data class ValidationErrorResponse(
        val timestamp: LocalDateTime,
        val message: String,
        val errors: Map<String, String?>
    )
}
