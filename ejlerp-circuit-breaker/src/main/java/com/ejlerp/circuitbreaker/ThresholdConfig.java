package com.ejlerp.circuitbreaker;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * @author dzt
 * @date 18/6/25
 * Hope you know what you have done
 */
@Component
@Configuration
public class ThresholdConfig {


    public static String CIRCUIT_BREAKER_REQUEST_THRESHOLD = "10";
    public static String CIRCUIT_BREAKER_SLEEP_MILLISECONDS = "60000";
    public static String CIRCUIT_BREAKER_ERROR_PERCENTAGE = "30";
    public static String CIRCUIT_BREAKER_ROLLING_WINDOWBUCKETS = "60";
    public static String CIRCUIT_BREAKER_OPEN_WEIGHT = "1";

    /**
     * 熔断门槛量（总请求数量超过该值，才启动熔断测试）
     */
    @Value("${circuit.breaker.requestThreshold:10}")
    public void requestThreshold(String requestThreshold) {
        CIRCUIT_BREAKER_REQUEST_THRESHOLD = requestThreshold;
    }

    /**
     * 熔断重试间隔时间（ms）
     */
    @Value("${circuit.breaker.sleepMilliseconds:60000}")
    public void sleepMilliseconds(String sleepMilliseconds) {
        CIRCUIT_BREAKER_SLEEP_MILLISECONDS = sleepMilliseconds;
    }

    /**
     * 熔断门槛错误率（%）
     */
    @Value("${circuit.breaker.errorPercentage:30}")
    public void errorPercentage(String errorPercentage) {
        CIRCUIT_BREAKER_ERROR_PERCENTAGE = errorPercentage;
    }

    /**
     * 熔断测试单元（s, 每个测试单元重置总数量和错误数量）
     */
    @Value("${circuit.breaker.rollingWindowbuckets:60}")
    public void rollingWindowbuckets(String rollingWindowbuckets) {
        CIRCUIT_BREAKER_ROLLING_WINDOWBUCKETS = rollingWindowbuckets;
    }

    /**
     * 半开关状态下服务的权重
     */
    @Value("${circuit.breaker.openWeight:1}")
    public void openWeight(String openWeight) {
        CIRCUIT_BREAKER_OPEN_WEIGHT = openWeight;
    }
}
