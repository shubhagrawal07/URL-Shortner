package com.urlshortener.app.services;

import com.urlshortener.app.models.dto.CreateUrlResponse;
import com.urlshortener.app.models.entity.ShortLink;
import com.urlshortener.app.models.repository.ShortLinkRepository;
import com.urlshortener.app.routes.AppProperties;
import com.urlshortener.app.utils.Base62;
import com.urlshortener.app.utils.UrlNormalizer;
import java.time.Instant;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ShortLinkService {

	private static final String REDIS_KEY_PREFIX = "url:short:";

	private final ShortLinkRepository shortLinkRepository;
	private final UrlNormalizer urlNormalizer;
	private final AppProperties appProperties;
	private final StringRedisTemplate redisTemplate;

	public ShortLinkService(
			ShortLinkRepository shortLinkRepository,
			UrlNormalizer urlNormalizer,
			AppProperties appProperties,
			StringRedisTemplate redisTemplate) {
		this.shortLinkRepository = shortLinkRepository;
		this.urlNormalizer = urlNormalizer;
		this.appProperties = appProperties;
		this.redisTemplate = redisTemplate;
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
		try {
			ShortLink link = new ShortLink();
			link.setLongUrl(canonical);
			link.setCreatedAt(Instant.now());
			shortLinkRepository.save(link);
			shortLinkRepository.flush();
			String code = Base62.encode(link.getId());
			link.setShortCode(code);
			shortLinkRepository.save(link);
			cachePut(code, canonical);
			return toResponse(link);
		}
		catch (DataIntegrityViolationException ex) {
			return shortLinkRepository
					.findByLongUrl(canonical)
					.map(this::toResponse)
					.orElseThrow(() -> ex);
		}
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
