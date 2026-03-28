package com.urlshortener.app.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.urlshortener.app.models.dto.CreateUrlResponse;
import com.urlshortener.app.models.entity.ShortLink;
import com.urlshortener.app.models.repository.ShortLinkRepository;
import com.urlshortener.app.routes.AppProperties;
import com.urlshortener.app.utils.UrlNormalizer;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

	private ShortLinkService shortLinkService;

	@BeforeEach
	void setUp() {
		when(redisTemplate.opsForValue()).thenReturn(valueOperations);
		AppProperties props = new AppProperties();
		props.setPublicShortLinkBase("http://localhost:8080");
		props.setRedisCacheTtl(Duration.ofHours(24));
		shortLinkService = new ShortLinkService(shortLinkRepository, urlNormalizer, props, redisTemplate);
	}

	@Test
	void createPersistsBase62CodeAndCaches() {
		when(urlNormalizer.toCanonical("https://example.com/a")).thenReturn("https://example.com/a");
		when(shortLinkRepository.findByLongUrl("https://example.com/a")).thenReturn(Optional.empty());
		when(shortLinkRepository.save(any(ShortLink.class)))
				.thenAnswer(invocation -> {
					ShortLink link = invocation.getArgument(0);
					if (link.getId() == null) {
						link.setId(1L);
					}
					return link;
				});

		CreateUrlResponse response = shortLinkService.create("https://example.com/a");

		assertThat(response.shortCode()).isEqualTo("1");
		assertThat(response.shortUrl()).isEqualTo("http://localhost:8080/r/1");
		assertThat(response.longUrl()).isEqualTo("https://example.com/a");

		ArgumentCaptor<ShortLink> captor = ArgumentCaptor.forClass(ShortLink.class);
		verify(shortLinkRepository, times(2)).save(captor.capture());
		assertThat(captor.getAllValues().get(1).getShortCode()).isEqualTo("1");

		verify(valueOperations).set(eq("url:short:1"), eq("https://example.com/a"), any(Duration.class));
	}
}
