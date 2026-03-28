package com.urlshortener.app.utils;

/**
 * Shared character set for Base62 encoding and short-code generation. Must stay in sync across callers.
 */
final class Base62Alphabet {

	static final String CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
	static final int BASE = CHARS.length();

	private Base62Alphabet() {}
}
