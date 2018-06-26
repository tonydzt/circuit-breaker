package com.ejlerp.circuitbreaker;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author dzt
 * @date 18/6/21
 * Hope you know what you have done
 */
public class BucketTest {

    public static void main(String[] args) throws InterruptedException {
        final Bucket bucket = new Bucket();
        bucket.setStartTime();
        ExecutorService cachedThreadPool = Executors.newCachedThreadPool();
        for (int i = 0; i < 1000; i++) {
            cachedThreadPool.execute(() -> {
                for(int i1 = 0; i1 <100; i1++){
                    try{
                        bucket.addLongAdderTotle();
                    }catch (Exception e){
                        e.printStackTrace();
                    }
                }
            });
        }
        Thread.sleep(100);
        System.out.println( bucket.sumLongAdderTotle());
        /* bucket.addLongAdderFail();*/
        /*   Thread.sleep(1000);*/
        /*   System.out.println(bucket.getErrorThresholdPercentage());*/
    }
}
