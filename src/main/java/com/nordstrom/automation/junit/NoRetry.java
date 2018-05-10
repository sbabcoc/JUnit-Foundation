package com.nordstrom.automation.junit;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * Use this annotation to mark test methods and classes for which automatic retry is failures not desired:
 * 
 * <blockquote><pre>
 * &#64;Test
 * &#64;NoRetry
 * public void testLongRunning() {
 *     // test implementation goes here
 * }</pre></blockquote>
 */
@Retention(RUNTIME)
@Target({TYPE, METHOD})
public @interface NoRetry { }
