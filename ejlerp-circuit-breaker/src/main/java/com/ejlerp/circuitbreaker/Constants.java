package com.ejlerp.circuitbreaker;

/**
 * @author dzt
 * @date 18/6/21
 * Hope you know what you have done
 */
public class Constants {

    public static final String REMOTE_TIMESTAMP_KEY = "remote.timestamp";
    public static final String CIRCUIT_BREAKER_REQUEST_VOLUME_THRESHOLD = "circuitBreakerRequestVolumeThreshold";
    public static final String CIRCUIT_BREAKER_SLEEP_WINDOWIN_MILLISECONDS = "circuitBreakerSleepWindowInMilliseconds";
    public static final String CIRCUIT_BREAKER_ERROR_THRESHOLD_PERCENTAGE = "circuitBreakerErrorThresholdPercentage";
    /*bucket数量*/
    public static final String CIRCUIT_BREAKER_ROLLING_STATISTICAL_WINDOWBUCKETS = "circuitBreakerRollingStatisticalWindowBuckets";
    public static final String CRICUIT_BREAKER_OPEN_WEIGHT_KEY = "circuitBreakerOpenWeight";
    public static final String CRICUIT_BREAKER_CLOSE_WEIGHT_KEY = "circuitBreakerCloseWeight";
    public static final int CB_CLOSE_DEFAULT_WEIGHT = 100;
}
