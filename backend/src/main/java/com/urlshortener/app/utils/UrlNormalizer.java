package com.urlshortener.app.utils;

import com.urlshortener.app.models.dto.UrlValidationException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class UrlNormalizer {

	public static final int MAX_URL_LENGTH = 4096;

	public String toCanonical(String raw) {
		if (raw == null || raw.isBlank()) {
			throw new UrlValidationException("URL must not be empty");
		}
		String trimmed = raw.trim();
		if (trimmed.length() > MAX_URL_LENGTH) {
			throw new UrlValidationException("URL exceeds maximum length of " + MAX_URL_LENGTH);
		}
		URI parsed;
		try {
			parsed = new URI(trimmed);
		}
		catch (URISyntaxException e) {
			throw new UrlValidationException("Invalid URL syntax");
		}
		if (!parsed.isAbsolute()) {
			throw new UrlValidationException("URL must be absolute (include scheme and host)");
		}
		String scheme = parsed.getScheme();
		if (scheme == null) {
			throw new UrlValidationException("URL must use http or https");
		}
		scheme = scheme.toLowerCase(Locale.ROOT);
		if (!"http".equals(scheme) && !"https".equals(scheme)) {
			throw new UrlValidationException("Only http and https URLs are allowed");
		}
		if (parsed.getUserInfo() != null) {
			throw new UrlValidationException("URLs with user info are not allowed");
		}
		String host = parsed.getHost();
		if (host == null || host.isBlank()) {
			throw new UrlValidationException("URL must include a host");
		}
		host = host.toLowerCase(Locale.ROOT);
		int port = parsed.getPort();
		String path = parsed.getRawPath();
		if (path == null || path.isEmpty()) {
			path = "/";
		}
		String query = parsed.getRawQuery();
		try {
			URI canonical = new URI(scheme, null, host, port, path, query, null);
			return canonical.toASCIIString();
		}
		catch (URISyntaxException e) {
			throw new UrlValidationException("Could not normalize URL");
		}
	}
}
