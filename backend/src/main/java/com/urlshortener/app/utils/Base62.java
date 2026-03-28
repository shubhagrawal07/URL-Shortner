package com.urlshortener.app.utils;

public final class Base62 {

	private static final String ALPHABET = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	private static final int BASE = ALPHABET.length();

	private Base62() {}

	public static String encode(long value) {
		if (value <= 0) {
			throw new IllegalArgumentException("value must be positive");
		}
		StringBuilder sb = new StringBuilder();
		long n = value;
		while (n > 0) {
			sb.append(ALPHABET.charAt((int) (n % BASE)));
			n /= BASE;
		}
		return sb.reverse().toString();
	}
}
