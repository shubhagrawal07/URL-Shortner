package com.urlshortener.app.routes;

import java.util.Arrays;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

	private final AppProperties appProperties;

	public WebConfig(AppProperties appProperties) {
		this.appProperties = appProperties;
	}

	@Override
	public void addCorsMappings(CorsRegistry registry) {
		String raw = appProperties.getCorsAllowedOrigins();
		if (raw == null || raw.isBlank()) {
			return;
		}
		String[] origins = Arrays.stream(raw.split(","))
				.map(String::trim)
				.filter(s -> !s.isEmpty())
				.toArray(String[]::new);
		if (origins.length == 0) {
			return;
		}
		registry.addMapping("/api/**")
				.allowedOrigins(origins)
				.allowedMethods("GET", "POST", "OPTIONS")
				.allowedHeaders("*")
				.maxAge(3600);
	}
}
