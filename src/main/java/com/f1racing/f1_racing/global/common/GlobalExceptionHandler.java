package com.f1racing.f1_racing.global.common;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {
    /**
     * 유효성 검사 예외 처리
     */

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<GlobalResponse<Map<String, String>>> handleValidationExceptions(
		MethodArgumentNotValidException ex) {
		Map<String, String> errors = new HashMap<>();
		ex.getBindingResult().getAllErrors().forEach((error) -> {
			String fieldName = ((FieldError) error).getField();
			String errorMessage = error.getDefaultMessage();
			errors.put(fieldName, errorMessage);
		});
		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(GlobalResponse.error("Validation failed: " + errors));
	}

	@ExceptionHandler(IllegalArgumentException.class)
	public ResponseEntity<GlobalResponse<Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
		return ResponseEntity
			.status(HttpStatus.BAD_REQUEST)
			.body(GlobalResponse.error(ex.getMessage()));
	}

	@ExceptionHandler(RuntimeException.class)
	public ResponseEntity<GlobalResponse<Object>> handleRuntimeException(RuntimeException ex) {
		return ResponseEntity
			.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(GlobalResponse.error("Internal server error: " + ex.getMessage()));
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<GlobalResponse<Object>> handleException(Exception ex) {
		return ResponseEntity
			.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(GlobalResponse.error("An unexpected error occurred: " + ex.getMessage()));
	}
}

