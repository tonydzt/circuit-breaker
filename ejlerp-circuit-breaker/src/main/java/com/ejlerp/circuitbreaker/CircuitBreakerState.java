package com.ejlerp.circuitbreaker;

/**
 * @author dzt
 * @date 18/6/21
 * Hope you know what you have done
 */
enum  CircuitBreakerState {
    // working normally, calls are transparently passing through
    CLOSED,
    // method calls are being intercepted and CircuitBreakerExceptions are being thrown instead
    OPEN,
    // method calls are passing through; if another blacklisted exception is thrown, reverts back to OPEN
    HALF_OPEN;
}
