package com.urlshortener.app.routes;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public class AppProperties {

	private String publicShortLinkBase = "http://localhost:8080";
	private Duration redisCacheTtl = Duration.ofHours(24);
	private String corsAllowedOrigins = "http://localhost:3000";

	public String getPublicShortLinkBase() {
		return publicShortLinkBase;
	}

	public void setPublicShortLinkBase(String publicShortLinkBase) {
		this.publicShortLinkBase = publicShortLinkBase;
	}

	public Duration getRedisCacheTtl() {
		return redisCacheTtl;
	}

	public void setRedisCacheTtl(Duration redisCacheTtl) {
		this.redisCacheTtl = redisCacheTtl;
	}

	public String getCorsAllowedOrigins() {
		return corsAllowedOrigins;
	}

	public void setCorsAllowedOrigins(String corsAllowedOrigins) {
		this.corsAllowedOrigins = corsAllowedOrigins;
	}
}
