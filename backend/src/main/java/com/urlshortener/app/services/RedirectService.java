package com.urlshortener.app.services;

import com.urlshortener.app.models.entity.ShortLink;
import com.urlshortener.app.models.repository.ShortLinkRepository;
import com.urlshortener.app.routes.AppProperties;
import java.net.URI;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RedirectService {

	private static final String REDIS_KEY_PREFIX = "url:short:";
	private static final Pattern SHORT_CODE_PATTERN = Pattern.compile("^[0-9A-Za-z]+$");

	private final ShortLinkRepository shortLinkRepository;
	private final StringRedisTemplate redisTemplate;
	private final AppProperties appProperties;

	public RedirectService(
			ShortLinkRepository shortLinkRepository,
			StringRedisTemplate redisTemplate,
			AppProperties appProperties) {
		this.shortLinkRepository = shortLinkRepository;
		this.redisTemplate = redisTemplate;
		this.appProperties = appProperties;
	}

	public Optional<URI> resolveRedirectUri(String shortCode) {
		if (shortCode == null || shortCode.isBlank() || !SHORT_CODE_PATTERN.matcher(shortCode).matches()) {
			return Optional.empty();
		}
		return resolveLongUrl(shortCode).flatMap(url -> Optional.ofNullable(toSafeRedirectUri(url)));
	}

	private Optional<String> resolveLongUrl(String shortCode) {
		String key = REDIS_KEY_PREFIX + shortCode;
		String cached = redisTemplate.opsForValue().get(key);
		if (cached != null) {
			return Optional.of(cached);
		}
		Optional<ShortLink> link = shortLinkRepository.findByShortCode(shortCode);
		if (link.isEmpty()) {
			return Optional.empty();
		}
		String longUrl = link.get().getLongUrl();
		redisTemplate.opsForValue().set(key, longUrl, appProperties.getRedisCacheTtl());
		return Optional.of(longUrl);
	}

	private URI toSafeRedirectUri(String longUrl) {
		try {
			URI uri = URI.create(longUrl);
			String scheme = uri.getScheme();
			if (scheme == null) {
				return null;
			}
			scheme = scheme.toLowerCase(Locale.ROOT);
			if (!"http".equals(scheme) && !"https".equals(scheme)) {
				return null;
			}
			return uri;
		}
		catch (IllegalArgumentException ex) {
			return null;
		}
	}
}
