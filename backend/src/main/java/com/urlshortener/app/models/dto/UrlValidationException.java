package com.urlshortener.app.models.dto;

public class UrlValidationException extends RuntimeException {

	public UrlValidationException(String message) {
		super(message);
	}
}
