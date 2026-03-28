package com.urlshortener.app;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIf;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestTemplate;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.utility.DockerImageName;
import java.net.http.HttpClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
@EnabledIf("com.urlshortener.app.UrlShortenerIntegrationTest#dockerAvailable")
class UrlShortenerIntegrationTest {

	static boolean dockerAvailable() {
		try {
			return DockerClientFactory.instance().isDockerAvailable();
		}
		catch (Throwable t) {
			return false;
		}
	}

	@Container
	static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
			.withDatabaseName("urlshortener")
			.withUsername("urlshortener")
			.withPassword("urlshortener");

	@Container
	static final GenericContainer<?> redis =
			new GenericContainer<>(DockerImageName.parse("redis:7-alpine")).withExposedPorts(6379);

	@DynamicPropertySource
	static void registerProperties(DynamicPropertyRegistry registry) {
		registry.add("spring.datasource.url", postgres::getJdbcUrl);
		registry.add("spring.datasource.username", postgres::getUsername);
		registry.add("spring.datasource.password", postgres::getPassword);
		registry.add("spring.data.redis.host", redis::getHost);
		registry.add("spring.data.redis.port", () -> String.valueOf(redis.getMappedPort(6379)));
	}

	@Autowired
	private TestRestTemplate restTemplate;

	@org.springframework.boot.test.web.server.LocalServerPort
	private int port;

	private RestTemplate noRedirectClient;

	@BeforeEach
	void setUpClients() {
		HttpClient httpClient = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		noRedirectClient = new RestTemplate(requestFactory);
	}

	@Test
	void createNormalizesUrlAndRedirectReturnsFound() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Map<String, String>> entity =
				new HttpEntity<>(Map.of("longUrl", "HTTPS://Example.COM/foo?x=1"), headers);

		ResponseEntity<JsonNode> created =
				restTemplate.postForEntity("http://localhost:" + port + "/api/v1/urls", entity, JsonNode.class);

		assertThat(created.getStatusCode()).isEqualTo(HttpStatus.OK);
		assertThat(created.getBody()).isNotNull();
		String code = created.getBody().get("shortCode").asText();
		assertThat(code).isNotBlank();

		ResponseEntity<Void> redirect = noRedirectClient.exchange(
				"http://localhost:" + port + "/r/" + code, HttpMethod.GET, HttpEntity.EMPTY, Void.class);

		assertThat(redirect.getStatusCode()).isEqualTo(HttpStatus.FOUND);
		assertThat(redirect.getHeaders().getLocation()).isNotNull();
		assertThat(redirect.getHeaders().getLocation().toString()).isEqualTo("https://example.com/foo?x=1");
	}

	@Test
	void duplicateLongUrlIsIdempotent() {
		HttpHeaders headers = new HttpHeaders();
		headers.setContentType(MediaType.APPLICATION_JSON);
		HttpEntity<Map<String, String>> entity =
				new HttpEntity<>(Map.of("longUrl", "https://dup.example.com/z"), headers);

		ResponseEntity<JsonNode> first =
				restTemplate.postForEntity("http://localhost:" + port + "/api/v1/urls", entity, JsonNode.class);
		ResponseEntity<JsonNode> second =
				restTemplate.postForEntity("http://localhost:" + port + "/api/v1/urls", entity, JsonNode.class);

		assertThat(first.getBody()).isNotNull();
		assertThat(second.getBody()).isNotNull();
		assertThat(second.getBody().get("shortCode").asText())
				.isEqualTo(first.getBody().get("shortCode").asText());
	}
}
