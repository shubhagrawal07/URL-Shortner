package com.urlshortener.app.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.urlshortener.app.models.dto.UrlValidationException;
import org.junit.jupiter.api.Test;

class UrlNormalizerTest {

	private final UrlNormalizer normalizer = new UrlNormalizer();

	@Test
	void normalizesSchemeAndHost() {
		assertThat(normalizer.toCanonical("HTTPS://Example.COM/path"))
				.isEqualTo("https://example.com/path");
	}

	@Test
	void rejectsNonHttpScheme() {
		assertThatThrownBy(() -> normalizer.toCanonical("ftp://example.com/"))
				.isInstanceOf(UrlValidationException.class);
	}

	@Test
	void rejectsRelativeUrl() {
		assertThatThrownBy(() -> normalizer.toCanonical("/only/path"))
				.isInstanceOf(UrlValidationException.class);
	}
}
