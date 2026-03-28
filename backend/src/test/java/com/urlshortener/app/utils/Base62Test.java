package com.urlshortener.app.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

class Base62Test {

	@Test
	void encodesOne() {
		assertThat(Base62.encode(1)).isEqualTo("1");
	}

	@Test
	void encodesSixtyTwo() {
		assertThat(Base62.encode(62)).isEqualTo("10");
	}

	@Test
	void rejectsNonPositive() {
		assertThatThrownBy(() -> Base62.encode(0)).isInstanceOf(IllegalArgumentException.class);
		assertThatThrownBy(() -> Base62.encode(-1)).isInstanceOf(IllegalArgumentException.class);
	}
}
