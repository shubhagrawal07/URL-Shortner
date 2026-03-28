package com.urlshortener.app.models.dto;

import com.urlshortener.app.utils.UrlNormalizer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateUrlRequest(
		@NotBlank(message = "longUrl is required")
		@Size(max = UrlNormalizer.MAX_URL_LENGTH, message = "longUrl is too long")
		String longUrl) {}
