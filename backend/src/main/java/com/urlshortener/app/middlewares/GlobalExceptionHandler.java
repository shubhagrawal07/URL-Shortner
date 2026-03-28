package com.urlshortener.app.middlewares;

import com.urlshortener.app.models.dto.ErrorResponse;
import com.urlshortener.app.models.dto.UrlValidationException;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(UrlValidationException.class)
	public ResponseEntity<ErrorResponse> handleUrlValidation(UrlValidationException ex) {
		return ResponseEntity.badRequest().body(new ErrorResponse(ex.getMessage(), null));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
		List<ErrorResponse.FieldError> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
				.map(fe -> new ErrorResponse.FieldError(fe.getField(), fe.getDefaultMessage()))
				.toList();
		return ResponseEntity.badRequest().body(new ErrorResponse("Validation failed", fieldErrors));
	}
}
