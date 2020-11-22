package com.nordstrom.automation.junit;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest(PowerMockCases.StaticClass.class)
public class PowerMockCases {
	
	@Test
	public void testHappyPath() {
        mockStatic(StaticClass.class);
        when(StaticClass.staticMethod()).thenReturn("mocked");
        assertThat(StaticClass.staticMethod(), equalTo("mocked"));
	}
	
	@Test
	public void testFailure() {
        mockStatic(StaticClass.class);
        when(StaticClass.staticMethod()).thenReturn("mocked");
        assertThat(StaticClass.staticMethod(), nullValue());
	}

	static class StaticClass {
		public static String staticMethod() {
			return null;
		}
	}
}
