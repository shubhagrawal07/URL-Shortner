package com.urlshortener.app.models.dto;

import java.util.List;

public record ErrorResponse(String message, List<FieldError> fieldErrors) {

	public record FieldError(String field, String message) {}
}
