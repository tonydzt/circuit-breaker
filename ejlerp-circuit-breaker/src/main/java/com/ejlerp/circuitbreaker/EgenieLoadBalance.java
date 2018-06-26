package com.ejlerp.circuitbreaker;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.cluster.loadbalance.RandomLoadBalance;

import java.util.List;
import java.util.Random;

/**
 * @author dzt
 * @date 18/6/20
 * Hope you know what you have done
 */
public class EgenieLoadBalance extends RandomLoadBalance{

    public static final String NAME = "egenie";
    private final Random random = new Random();

    @Override
    protected <T> Invoker<T> doSelect(List<Invoker<T>> list, URL url, Invocation invocation) {

        CircuitBreakerManager circuitBreakerManager = CircuitBreakerManagerDefault.getBreakerManager();
        list = circuitBreakerManager.filterCricuitBreakInvoker(list, invocation);
        if(haveOutSleepWindowInvoker(list, invocation)){
            return doCircuitBreakerSelect(list, url, invocation);
        }
        return super.doSelect(list, url, invocation);
    }

    private <T> boolean haveOutSleepWindowInvoker(List<Invoker<T>> invokers,Invocation invocation){
        int i=0;
        while(i<invokers.size()){
            Invoker invoker = invokers.get(i++);
            CircuitBreakerManager circuitBreakerManager = CircuitBreakerManagerDefault.getBreakerManager();
            if(circuitBreakerManager.isSwitchLoadBalance(invoker,invocation)){
                return true;
            }
        }
        return false;
    }

    private <T> Invoker<T> doCircuitBreakerSelect(List<Invoker<T>> invokers, URL url, Invocation invocation) {
        int length = invokers.size();
        int totalWeight = 0;
        boolean sameWeight = true;
        for (int i = 0; i < length; i++) {
            int weight = getCircuitBreakerWeight(invokers.get(i), invocation);
            totalWeight += weight;
            if (sameWeight && i > 0
                    && weight != getCircuitBreakerWeight(invokers.get(i - 1), invocation)) {
                sameWeight = false;
            }
        }
        if (totalWeight > 0 && ! sameWeight) {
            // 如果权重不相同且权重大于0则按总权重数随机
            int offset = random.nextInt(totalWeight);
            // 并确定随机值落在哪个片断上
            for (Invoker<T> invoker : invokers) {
                offset -= getCircuitBreakerWeight(invoker, invocation);
                if (offset < 0) {
                    return invoker;
                }
            }
        }
        // 如果权重相同或权重为0则均等随机
        return  invokers.get(random.nextInt(length));
    }

    private int getCircuitBreakerWeight(Invoker<?> invoker, Invocation invocation) {
        URL url=invoker.getUrl();
        int weight;
        CircuitBreakerManager circuitBreakerManager = CircuitBreakerManagerDefault.getBreakerManager();
        if(circuitBreakerManager.isCircuitBreakerInterface(invoker,invocation)){
            weight = url.getParameter(Constants.CRICUIT_BREAKER_OPEN_WEIGHT_KEY, Integer.parseInt(ThresholdConfig.CIRCUIT_BREAKER_OPEN_WEIGHT));
        }else {
            weight = url.getParameter(Constants.CRICUIT_BREAKER_CLOSE_WEIGHT_KEY, Constants.CB_CLOSE_DEFAULT_WEIGHT);
        }
        if (weight > 0) {
            long timestamp = invoker.getUrl().getParameter(Constants.REMOTE_TIMESTAMP_KEY, 0L);
            if (timestamp > 0L) {
                int uptime = (int) (System.currentTimeMillis() - timestamp);
                int warmup = invoker.getUrl().getParameter(com.alibaba.dubbo.common.Constants.WARMUP_KEY, com.alibaba.dubbo.common.Constants.DEFAULT_WARMUP);
                if (uptime > 0 && uptime < warmup) {
                    weight = calculateWarmupWeight(uptime, warmup, weight);
                }
            }
        }
        return weight;
    }

    private static int calculateWarmupWeight(int uptime, int warmup, int weight) {
        int ww = (int)((float)uptime / ((float)warmup / (float)weight));
        return ww < 1 ? 1 : (ww > weight ? weight : ww);
    }
}
