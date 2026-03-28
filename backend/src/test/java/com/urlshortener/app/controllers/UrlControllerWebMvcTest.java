package com.urlshortener.app.controllers;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.urlshortener.app.middlewares.GlobalExceptionHandler;
import com.urlshortener.app.models.dto.CreateUrlRequest;
import com.urlshortener.app.models.dto.CreateUrlResponse;
import com.urlshortener.app.models.repository.ShortLinkRepository;
import com.urlshortener.app.routes.AppProperties;
import com.urlshortener.app.services.ShortLinkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(controllers = UrlController.class)
@Import(GlobalExceptionHandler.class)
class UrlControllerWebMvcTest {

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@MockBean
	private ShortLinkService shortLinkService;

	@MockBean
	private ShortLinkRepository shortLinkRepository;

	@MockBean
	private AppProperties appProperties;

	@BeforeEach
	void stubAppProperties() {
		when(appProperties.getCorsAllowedOrigins()).thenReturn("http://localhost:3000");
		when(appProperties.getPublicShortLinkBase()).thenReturn("http://localhost:8080");
	}

	@Test
	void postCreateReturnsPayload() throws Exception {
		when(shortLinkService.create(anyString()))
				.thenReturn(new CreateUrlResponse("1", "http://localhost:8080/r/1", "https://example.com/a"));

		mockMvc.perform(post("/api/v1/urls")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(new CreateUrlRequest("https://example.com/a"))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.shortCode").value("1"))
				.andExpect(jsonPath("$.shortUrl").value("http://localhost:8080/r/1"));
	}

	@Test
	void postCreateValidatesBody() throws Exception {
		mockMvc.perform(post("/api/v1/urls")
						.contentType(MediaType.APPLICATION_JSON)
						.content("{}"))
				.andExpect(status().isBadRequest());
	}
}
