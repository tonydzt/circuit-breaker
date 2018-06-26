package com.ejlerp.circuitbreaker;

/**
 * @author dzt
 * @date 18/6/21
 * Hope you know what you have done
 */
public class CircuitBreakerConfig {

    /**
     * half-open状态下成功次数阈值
     */
    private int consecutiveSuccThreshold = 5;

    private volatile int circuitBreakerRequestVolumeThreshold = Integer.parseInt(ThresholdConfig.CIRCUIT_BREAKER_REQUEST_THRESHOLD);
    private volatile int circuitBreakerSleepWindowInMilliseconds = Integer.parseInt(ThresholdConfig.CIRCUIT_BREAKER_SLEEP_MILLISECONDS);
    private volatile int circuitBreakerErrorThresholdPercentage = Integer.parseInt(ThresholdConfig.CIRCUIT_BREAKER_ERROR_PERCENTAGE);

    private CircuitBreakerConfig() {

    }

    public static CircuitBreakerConfig newDefault() {
        return new CircuitBreakerConfig();
    }

    public int getCircuitBreakerRequestVolumeThreshold() {
        return circuitBreakerRequestVolumeThreshold;
    }

    public void setCircuitBreakerRequestVolumeThreshold(int circuitBreakerRequestVolumeThreshold) {
        this.circuitBreakerRequestVolumeThreshold = circuitBreakerRequestVolumeThreshold;
    }

    public int getCircuitBreakerSleepWindowInMilliseconds() {
        return circuitBreakerSleepWindowInMilliseconds;
    }

    public void setCircuitBreakerSleepWindowInMilliseconds(int circuitBreakerSleepWindowInMilliseconds) {
        this.circuitBreakerSleepWindowInMilliseconds = circuitBreakerSleepWindowInMilliseconds;
    }

    public int getCircuitBreakerErrorThresholdPercentage() {
        return circuitBreakerErrorThresholdPercentage;
    }

    public void setCircuitBreakerErrorThresholdPercentage(int circuitBreakerErrorThresholdPercentage) {
        this.circuitBreakerErrorThresholdPercentage = circuitBreakerErrorThresholdPercentage;
    }


    public int getConsecutiveSuccThreshold() {
        return consecutiveSuccThreshold;
    }

    public void setConsecutiveSuccThreshold(int consecutiveSuccThreshold) {
        this.consecutiveSuccThreshold = consecutiveSuccThreshold;
    }

    public void parmEqual(int a1, int a2, int a3) {
        if (a1 != circuitBreakerRequestVolumeThreshold) {
            circuitBreakerRequestVolumeThreshold = a1;
        }
        if (a2 != circuitBreakerSleepWindowInMilliseconds) {
            circuitBreakerSleepWindowInMilliseconds = a2;
        }
        if (a3 != circuitBreakerErrorThresholdPercentage) {
            circuitBreakerErrorThresholdPercentage = a3;
        }
    }

    @Override
    public String toString() {
        return "CircuitBreakerConfig{" +
                "consecutiveSuccThreshold=" + consecutiveSuccThreshold +
                ", circuitBreakerRequestVolumeThreshold=" + circuitBreakerRequestVolumeThreshold +
                ", circuitBreakerSleepWindowInMilliseconds=" + circuitBreakerSleepWindowInMilliseconds +
                ", circuitBreakerErrorThresholdPercentage=" + circuitBreakerErrorThresholdPercentage +
                '}';
    }
}
