package com.ejlerp.circuitbreaker;

import com.alibaba.dubbo.rpc.*;

/**
 * @author dzt
 * @date 18/6/22
 * Hope you know what you have done
 */
public class CircuitBreakerFilter implements Filter {

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        CircuitBreakerManager circuitBreakerManager = CircuitBreakerManagerDefault.getBreakerManager();
        return circuitBreakerManager.invoke(invoker,invocation);
    }
}
