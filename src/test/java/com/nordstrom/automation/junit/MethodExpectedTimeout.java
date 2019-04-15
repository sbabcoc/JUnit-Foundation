package com.nordstrom.automation.junit;

import static org.junit.Assert.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class MethodExpectedTimeout {
    
    @Test
    public void testTimeout() throws InterruptedException {
        System.out.println("testTimeout");
        TimeUnit.MILLISECONDS.sleep(1000);
        assertTrue(true);
    }
    
    @Test(timeout = 400)
    public void testTimeoutWithShorterInterval() throws InterruptedException {
        System.out.println("testTimeoutWithShorterInterval");
        TimeUnit.MILLISECONDS.sleep(1000);
        assertTrue(true);
    }

    @Test(timeout = 600)
    public void testTimeoutWithLongerInterval() throws InterruptedException {
        System.out.println("testTimeoutWithLongerInterval");
        TimeUnit.MILLISECONDS.sleep(1000);
        assertTrue(true);
    }

}
