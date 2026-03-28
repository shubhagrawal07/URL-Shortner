package com.urlshortener.app.utils;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

@Component
public class ShortCodeGenerator {

	public static final int SHORT_CODE_LENGTH = 7;

	private final SecureRandom secureRandom = new SecureRandom();

	public String next() {
		String chars = Base62Alphabet.CHARS;
		int base = chars.length();
		StringBuilder sb = new StringBuilder(SHORT_CODE_LENGTH);
		for (int i = 0; i < SHORT_CODE_LENGTH; i++) {
			sb.append(chars.charAt(secureRandom.nextInt(base)));
		}
		return sb.toString();
	}
}
