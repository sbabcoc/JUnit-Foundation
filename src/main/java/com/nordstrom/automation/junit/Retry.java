package com.nordstrom.automation.junit;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/** Used to annotate JUnit tests as being retryable */
@Retention(RetentionPolicy.RUNTIME)
public @interface Retry {
    
    /** Retries for at most this many milliseconds */ 
    int timeoutMsec() default -1;
    
    /** Wait this many milliseconds between retries */
    int intervalMsec() default -1;
}   