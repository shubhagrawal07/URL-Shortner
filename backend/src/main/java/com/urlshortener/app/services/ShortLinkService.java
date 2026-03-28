package com.urlshortener.app.services;

import com.urlshortener.app.models.dto.CreateUrlResponse;
import com.urlshortener.app.models.entity.ShortLink;
import com.urlshortener.app.models.repository.ShortLinkRepository;
import com.urlshortener.app.routes.AppProperties;
import com.urlshortener.app.utils.ShortCodeGenerator;
import com.urlshortener.app.utils.UrlNormalizer;
import java.time.Instant;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShortLinkService {

	private static final String REDIS_KEY_PREFIX = "url:short:";
	private static final int MAX_SHORT_CODE_ATTEMPTS = 20;

	private final ShortLinkRepository shortLinkRepository;
	private final UrlNormalizer urlNormalizer;
	private final AppProperties appProperties;
	private final StringRedisTemplate redisTemplate;
	private final ShortCodeGenerator shortCodeGenerator;

	public ShortLinkService(
			ShortLinkRepository shortLinkRepository,
			UrlNormalizer urlNormalizer,
			AppProperties appProperties,
			StringRedisTemplate redisTemplate,
			ShortCodeGenerator shortCodeGenerator) {
		this.shortLinkRepository = shortLinkRepository;
		this.urlNormalizer = urlNormalizer;
		this.appProperties = appProperties;
		this.redisTemplate = redisTemplate;
		this.shortCodeGenerator = shortCodeGenerator;
	}

	@Transactional
	public CreateUrlResponse create(String rawLongUrl) {
		String canonical = urlNormalizer.toCanonical(rawLongUrl);
		return shortLinkRepository
				.findByLongUrl(canonical)
				.map(link -> {
					if (link.getShortCode() != null) {
						cachePut(link.getShortCode(), canonical);
					}
					return toResponse(link);
				})
				.orElseGet(() -> insertNew(canonical));
	}

	private CreateUrlResponse insertNew(String canonical) {
		for (int attempt = 0; attempt < MAX_SHORT_CODE_ATTEMPTS; attempt++) {
			try {
				String code = shortCodeGenerator.next();
				ShortLink link = new ShortLink();
				link.setLongUrl(canonical);
				link.setCreatedAt(Instant.now());
				link.setShortCode(code);
				shortLinkRepository.save(link);
				cachePut(code, canonical);
				return toResponse(link);
			}
			catch (DataIntegrityViolationException ex) {
				Optional<ShortLink> existing = shortLinkRepository.findByLongUrl(canonical);
				if (existing.isPresent()) {
					ShortLink link = existing.get();
					if (link.getShortCode() != null) {
						cachePut(link.getShortCode(), canonical);
					}
					return toResponse(link);
				}
			}
		}
		throw new IllegalStateException(
				"Failed to allocate a unique short code after " + MAX_SHORT_CODE_ATTEMPTS + " attempts");
	}

	private CreateUrlResponse toResponse(ShortLink link) {
		String base = trimTrailingSlash(appProperties.getPublicShortLinkBase());
		String shortUrl = base + "/r/" + link.getShortCode();
		return new CreateUrlResponse(link.getShortCode(), shortUrl, link.getLongUrl());
	}

	private void cachePut(String shortCode, String longUrl) {
		String key = REDIS_KEY_PREFIX + shortCode;
		redisTemplate.opsForValue().set(key, longUrl, appProperties.getRedisCacheTtl());
	}

	private static String trimTrailingSlash(String base) {
		if (base == null || base.isEmpty()) {
			return "";
		}
		return base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
	}
}
