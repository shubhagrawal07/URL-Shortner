package com.urlshortener.app.utils;

public final class Base62 {

	private Base62() {}

	public static String encode(long value) {
		if (value <= 0) {
			throw new IllegalArgumentException("value must be positive");
		}
		StringBuilder sb = new StringBuilder();
		long n = value;
		while (n > 0) {
			sb.append(Base62Alphabet.CHARS.charAt((int) (n % Base62Alphabet.BASE)));
			n /= Base62Alphabet.BASE;
		}
		return sb.reverse().toString();
	}
}
