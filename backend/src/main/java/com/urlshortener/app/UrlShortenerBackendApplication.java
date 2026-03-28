package com.urlshortener.app;

import com.urlshortener.app.routes.AppProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(AppProperties.class)
public class UrlShortenerBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(UrlShortenerBackendApplication.class, args);
	}
}
