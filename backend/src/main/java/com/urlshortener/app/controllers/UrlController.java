package com.urlshortener.app.controllers;

import com.urlshortener.app.models.dto.CreateUrlRequest;
import com.urlshortener.app.models.dto.CreateUrlResponse;
import com.urlshortener.app.models.entity.ShortLink;
import com.urlshortener.app.models.repository.ShortLinkRepository;
import com.urlshortener.app.routes.ApiRoutes;
import com.urlshortener.app.routes.AppProperties;
import com.urlshortener.app.services.ShortLinkService;
import jakarta.validation.Valid;
import java.util.regex.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(ApiRoutes.API_V1 + "/urls")
public class UrlController {

	private static final Pattern SHORT_CODE_PATTERN = Pattern.compile("^[0-9A-Za-z]+$");

	private final ShortLinkService shortLinkService;
	private final ShortLinkRepository shortLinkRepository;
	private final AppProperties appProperties;

	public UrlController(
			ShortLinkService shortLinkService,
			ShortLinkRepository shortLinkRepository,
			AppProperties appProperties) {
		this.shortLinkService = shortLinkService;
		this.shortLinkRepository = shortLinkRepository;
		this.appProperties = appProperties;
	}

	@PostMapping
	public ResponseEntity<CreateUrlResponse> create(@Valid @RequestBody CreateUrlRequest request) {
		return ResponseEntity.ok(shortLinkService.create(request.longUrl()));
	}

	@GetMapping("/{shortCode}")
	public ResponseEntity<CreateUrlResponse> getByCode(@PathVariable String shortCode) {
		if (!SHORT_CODE_PATTERN.matcher(shortCode).matches()) {
			return ResponseEntity.notFound().build();
		}
		return shortLinkRepository
				.findByShortCode(shortCode)
				.map(this::toResponse)
				.map(ResponseEntity::ok)
				.orElseGet(() -> ResponseEntity.notFound().build());
	}

	private CreateUrlResponse toResponse(ShortLink link) {
		String base = appProperties.getPublicShortLinkBase();
		if (base.endsWith("/")) {
			base = base.substring(0, base.length() - 1);
		}
		String shortUrl = base + "/r/" + link.getShortCode();
		return new CreateUrlResponse(link.getShortCode(), shortUrl, link.getLongUrl());
	}
}
