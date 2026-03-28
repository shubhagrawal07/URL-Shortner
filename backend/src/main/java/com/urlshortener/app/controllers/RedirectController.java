package com.urlshortener.app.controllers;

import com.urlshortener.app.services.RedirectService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class RedirectController {

	private final RedirectService redirectService;

	public RedirectController(RedirectService redirectService) {
		this.redirectService = redirectService;
	}

	@GetMapping("/r/{shortCode}")
	public ResponseEntity<Void> redirect(@PathVariable String shortCode) {
		return redirectService
				.resolveRedirectUri(shortCode)
				.map(uri -> ResponseEntity.status(HttpStatus.FOUND).location(uri).<Void>build())
				.orElse(ResponseEntity.notFound().build());
	}
}
