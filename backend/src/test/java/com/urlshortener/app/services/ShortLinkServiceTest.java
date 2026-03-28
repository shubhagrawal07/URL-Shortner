package com.urlshortener.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.urlshortener.app.models.dto.CreateUrlResponse;
import com.urlshortener.app.models.entity.ShortLink;
import com.urlshortener.app.models.repository.ShortLinkRepository;
import com.urlshortener.app.routes.AppProperties;
import com.urlshortener.app.utils.ShortCodeGenerator;
import com.urlshortener.app.utils.UrlNormalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

@ExtendWith(MockitoExtension.class)
class ShortLinkServiceTest {

	@Mock
	private ShortLinkRepository shortLinkRepository;

	@Mock
	private UrlNormalizer urlNormalizer;

	@Mock
	private StringRedisTemplate redisTemplate;

	@Mock
	private ValueOperations<String, String> valueOperations;

	@Mock
	private ShortCodeGenerator shortCodeGenerator;

	private ShortLinkService shortLinkService;

	@BeforeEach
	void setUp() {
		lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		AppProperties props = new AppProperties();
		props.setPublicShortLinkBase("http://localhost:8080");
		props.setRedisCacheTtl(Duration.ofHours(24));
		shortLinkService = new ShortLinkService(
				shortLinkRepository, urlNormalizer, props, redisTemplate, shortCodeGenerator);
	}

	@Test
	void createPersistsRandomCodeAndCaches() {
		when(urlNormalizer.toCanonical("https://example.com/a")).thenReturn("https://example.com/a");
		when(shortLinkRepository.findByLongUrl("https://example.com/a")).thenReturn(Optional.empty());
		when(shortCodeGenerator.next()).thenReturn("Ab3xY9z");
		when(shortLinkRepository.save(any(ShortLink.class)))
				.thenAnswer(invocation -> {
					ShortLink link = invocation.getArgument(0);
					if (link.getId() == null) {
						link.setId(1L);
					}
					return link;
				});

		CreateUrlResponse response = shortLinkService.create("https://example.com/a");

		assertThat(response.shortCode()).isEqualTo("Ab3xY9z");
		assertThat(response.shortUrl()).isEqualTo("http://localhost:8080/r/Ab3xY9z");
		assertThat(response.longUrl()).isEqualTo("https://example.com/a");

		ArgumentCaptor<ShortLink> captor = ArgumentCaptor.forClass(ShortLink.class);
		verify(shortLinkRepository, times(1)).save(captor.capture());
		assertThat(captor.getValue().getShortCode()).isEqualTo("Ab3xY9z");

		verify(valueOperations).set(eq("url:short:Ab3xY9z"), eq("https://example.com/a"), any(Duration.class));
	}

	@Test
	void createRetriesOnShortCodeCollision() {
		when(urlNormalizer.toCanonical("https://example.com/a")).thenReturn("https://example.com/a");
		when(shortLinkRepository.findByLongUrl("https://example.com/a")).thenReturn(Optional.empty());
		AtomicInteger saveCount = new AtomicInteger();
		when(shortLinkRepository.save(any(ShortLink.class)))
				.thenAnswer(invocation -> {
					if (saveCount.getAndIncrement() == 0) {
						throw new DataIntegrityViolationException("duplicate short_code");
					}
					ShortLink link = invocation.getArgument(0);
					if (link.getId() == null) {
						link.setId(2L);
					}
					return link;
				});
		when(shortCodeGenerator.next()).thenReturn("AAAAAAA", "BBBBBBB");

		CreateUrlResponse response = shortLinkService.create("https://example.com/a");

		assertThat(response.shortCode()).isEqualTo("BBBBBBB");
		verify(shortLinkRepository, times(2)).save(any(ShortLink.class));
		verify(shortCodeGenerator, times(2)).next();
		verify(valueOperations).set(eq("url:short:BBBBBBB"), eq("https://example.com/a"), any(Duration.class));
	}

	@Test
	void createFailsAfterMaxShortCodeAttempts() {
		when(urlNormalizer.toCanonical("https://example.com/z")).thenReturn("https://example.com/z");
		when(shortLinkRepository.findByLongUrl("https://example.com/z")).thenReturn(Optional.empty());
		when(shortLinkRepository.save(any(ShortLink.class)))
				.thenThrow(new DataIntegrityViolationException("duplicate short_code"));
		when(shortCodeGenerator.next()).thenReturn("ccccccc");

		assertThatThrownBy(() -> shortLinkService.create("https://example.com/z"))
				.isInstanceOf(IllegalStateException.class)
				.hasMessageContaining("Failed to allocate a unique short code");

		verify(shortLinkRepository, times(20)).save(any(ShortLink.class));
		verify(shortCodeGenerator, times(20)).next();
	}
}
