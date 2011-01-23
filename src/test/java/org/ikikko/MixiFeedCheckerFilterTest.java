package org.ikikko;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

public class MixiFeedCheckerFilterTest {

	private MixiFeedCheckerFilter filter;

	@Before
	public void setUp() {
		filter = new MixiFeedCheckerFilter();
	}

	@Test
	public void noPropetyTokenDefined() throws Exception {
		assertThat(filter.isAuthentiated(null, "token"), is(true));
		assertThat(filter.isAuthentiated("", "token"), is(true));
	}

	@Test
	public void equalsPropetyAndParameterToken() throws Exception {
		assertThat(filter.isAuthentiated("token", "token"), is(true));
	}

	@Test
	public void notEqualsPropetyAndParameterToken() throws Exception {
		assertThat(filter.isAuthentiated("token", "badToken"), is(false));
	}

}
