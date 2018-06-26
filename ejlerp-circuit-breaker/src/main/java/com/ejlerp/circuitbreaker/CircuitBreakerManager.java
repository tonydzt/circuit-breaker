package com.ejlerp.circuitbreaker;

import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;

import java.util.List;

/**
 * @author dzt
 * @date 18/6/21
 * Hope you know what you have done
 */

public interface CircuitBreakerManager {

    /**
     *
     * @param invoker
     * @param invocation
     * @return
     */
    Result invoke(Invoker<?> invoker, Invocation invocation);

    /**
     * 是否为熔断接口 --- concurrentHashMap中存在且已经发生了熔断包括 open 和 halfopen
     * @param invoker
     * @param invocation
     * @return
     */
    boolean isCircuitBreakerInterface(Invoker invoker, Invocation invocation);

    /**
     * 过滤 服务接口
     * @param invokers
     * @param invocation
     * @param <T>
     * @return
     */
    <T> List<Invoker<T>> filterCricuitBreakInvoker(List<Invoker<T>> invokers, Invocation invocation);

    /**
     * 如果存在 HalfOpen 可重试接口
     * @param invoker
     * @param invocation
     * @return
     */
    boolean isSwitchLoadBalance(Invoker invoker, Invocation invocation);
}
