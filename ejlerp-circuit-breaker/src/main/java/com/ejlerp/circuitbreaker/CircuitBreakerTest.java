package com.ejlerp.circuitbreaker;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author dzt
 * @date 18/6/21
 * Hope you know what you have done
 */
public class CircuitBreakerTest {

    public static void main(String[] args) {
        CircuitBreakerConfig circuitBreakerConfig = CircuitBreakerConfig.newDefault();
        final CircuitBreaker circuitBreaker = new CircuitBreaker("ttttt", circuitBreakerConfig);
        final CircuitBreaker ss = new CircuitBreaker("ssss", circuitBreakerConfig);
        ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
        final AtomicInteger count = new AtomicInteger(0);
        for (int i = 0; i < 50; i++) {
            cachedThreadPool.execute(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < 100; i++) {
                        try {
                            ss.incrTotleCount();
                            circuitBreaker.incrTotleCount();
                            /*  Thread.sleep(10);*/
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
        for (int i = 0; i < 50; i++) {
            cachedThreadPool.execute(() -> {
                for (int i1 = 0; i1 < 1000; i1++) {
                    try {
                        circuitBreaker.incrFailCount();
                        /*  Thread.sleep(10);*/
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            });
        }

        while (true) {
            try {
                Thread.sleep(1000L);
                System.out.println("circuitBreaker:" + circuitBreaker.toString());
                System.out.println("error:" + circuitBreaker.closeFailThresholdReached());
                System.out.println("ss===========:" + ss.toString());
                System.out.println("ss===============:" + ss.closeFailThresholdReached());

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

    }
}

