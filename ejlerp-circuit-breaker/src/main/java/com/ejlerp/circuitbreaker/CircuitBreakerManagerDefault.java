package com.ejlerp.circuitbreaker;

import com.alibaba.dubbo.common.URL;
import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.LogHelper;
import com.alibaba.dubbo.common.utils.StringUtils;
import com.alibaba.dubbo.rpc.Invocation;
import com.alibaba.dubbo.rpc.Invoker;
import com.alibaba.dubbo.rpc.Result;
import com.alibaba.dubbo.rpc.RpcException;
import com.ejlerp.dal.framework.conf.HttpConf;
import com.ejlerp.sms.send.params.SystemMonitorParams;
import com.ejlerp.sms.send.sender.IndustrySmsSender;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author dzt
 * @date 18/6/21
 * Hope you know what you have done
 */
public class CircuitBreakerManagerDefault implements CircuitBreakerManager {

    private static final CircuitBreakerManagerDefault BREAKER_MANAGER = new CircuitBreakerManagerDefault();

    private static final ConcurrentHashMap<String, CircuitBreaker> BREAKERS = new ConcurrentHashMap<>();

    private static Logger logger = LoggerFactory.getLogger(CircuitBreakerManagerDefault.class);

    private static final String DEFAULT_HOUST = "localhost";

    @Override
    public Result invoke(Invoker<?> invoker, Invocation invocation) throws RpcException {
        URL url = invoker.getUrl();
        boolean cbSwitch = isOpenCircuitBreaker(url);
        //熔断打开
        if (cbSwitch) {
            String key = getHystrixCommandKey(invoker, invocation);
            CircuitBreaker breaker = getCircuitBreaker(url, key);
            if (breaker == null) {
                return invoker.invoke(invocation);
            }
            breaker.incrTotleCount();
            Result returnValue = null;
            /* 进入了熔断状态*/
            if (breaker.isOpen()) {
                if (!isForbidInvoker(key)) {
                    throw new RpcException("short-circuited is half-opened, try again!" + key);
                } else {
                    throw new RpcException("short-circuited is opened!" + key);
                }
            } else if (breaker.isClosed()) {
                try {
                    returnValue = invoker.invoke(invocation);
                } catch (RpcException e) {
                    /* 增加计数*/
                    breaker.incrFailCount();
                    /*达到熔断开启条件*/
                    if (breaker.getBuffStatus()&&breaker.isClosed() && breaker.closeFailThresholdReached()) {
                        if (breaker.isClosed()) {
                            breaker.open();  //触发阈值，打开
                            LogHelper.error(logger, "short-circuited threshold opened " + key);
                            if (isSmsReady()) {
                                sendSmsMsg("熔断: " + key);
                            } else {
                                LogHelper.error(logger, "发送短信地址未配置，无法发送短信！");
                            }
                        }
                        throw e;
                    } else {
                        throw e;
                    }
                }
            } else if (breaker.isHalfOpen()) {
                returnValue = processHalfOpen(breaker, invoker, invocation, key);
            }
            return returnValue;
        } else {
            return invoker.invoke(invocation);
        }
    }

    private Result processHalfOpen(CircuitBreaker breaker, Invoker<?> invoker, Invocation invocation, String key) throws RpcException {
        try {
            Result returnValue = invoker.invoke(invocation);
            int count = breaker.getConsecutiveSuccCount().incrementAndGet();
            //达到成功次数 关闭熔断
            if (breaker.isConsecutiveSuccessThresholdReached()) {
                if (!breaker.isClosed()) {
                    breaker.close();//调用成功则进入close状态
                    LogHelper.error(logger, "short-circuited ####### try success! " + count + " close success! key:" + key);
                }
            }
            return returnValue;
        } catch (RpcException e) {
            if (!breaker.isOpen()) {
                LogHelper.error(logger, "short-circuited reopen :" + key);
                breaker.open();
            }
            LogHelper.error(logger, "short-circuited try close fail! state:" + breaker.getState().name() + " key:" + key);
            throw e;
        }
    }

    /**
     * 是否为熔断接口 --- concurrentHashMap中存在且已经发生了熔断包括 open 和 halfopen
     *
     * @param invoker
     * @param invocation
     * @return
     */
    @Override
    public boolean isCircuitBreakerInterface(Invoker invoker, Invocation invocation) {
        try {
            if (BREAKERS.size() == 0) {
                return false;
            }
            String commandKey = getHystrixCommandKey(invoker, invocation);
            if (StringUtils.isBlank(commandKey)) {
                return false;
            }
            if (BREAKERS.containsKey(commandKey) && !BREAKERS.get(commandKey).isClosed()) {
                return true;
            }
        } catch (Exception e) {
            LogHelper.error(logger, "isCircuitBreakerInterface", e);
        }

        return false;
    }

    @Override
    public <T> List<Invoker<T>> filterCricuitBreakInvoker(List<Invoker<T>> invokers, Invocation invocation) {
        if (invokers.size() == 0) {
            return invokers;
        }
        List<Invoker<T>> list = new ArrayList<Invoker<T>>();
        try {
            for (Invoker invoker : invokers) {
                boolean isOpen = isOpenCircuitBreaker(invoker.getUrl());
                if (isOpen) {
                    String commandKey = getHystrixCommandKey(invoker, invocation);
                    if (!isForbidInvoker(commandKey)) {
                        list.add(invoker);
                    }
                } else {
                    list.add(invoker);
                    removeCircuitBreaker(invoker, invocation);
                }
            }
        } catch (Exception e) {
            LogHelper.error(logger, "short-circuited filterCricuitBreakInvoker", e);
        }
        return list;
    }


    private String getHystrixCommandKey(Invoker<?> invoker, Invocation invocation) {
        return invoker.getUrl().getHost() + "_" + invoker.getUrl().getServiceInterface() + "_" + invoker.getUrl().getParameter("group") + "_" + invocation.getMethodName() + "_" +
                invoker.getUrl().getParameter("version") + "_" +
                (invocation.getArguments() == null ? 0 : invocation.getArguments().length);
    }

    /**
     * 是否开启了熔断机制
     *
     * @param url
     * @return
     */
    private boolean isOpenCircuitBreaker(URL url) {
        return true;
//        if (url == null) {
//            return false;
//        }
//        return url.getParameter(Constants.CIRCUIT_BREAKER_SWITCH, false);
    }

    /**
     * 关闭熔断机制 后 移除熔断器
     *
     * @param invoker
     * @param invocation
     */
    private void removeCircuitBreaker(Invoker invoker, Invocation invocation) {
        try {
            String commandKey = getHystrixCommandKey(invoker, invocation);
            if (BREAKERS.containsKey(commandKey)) {
                BREAKERS.remove(commandKey);
                LogHelper.error(logger, "short-circuited circuit.breaker.switch close remove key:" + commandKey);
            }
        } catch (Exception e) {
            LogHelper.error(logger, "short-circuited  removeParmsAndCircuitBreaker", e);
        }
    }

    private boolean isForbidInvoker(String commandKey) {
        try {
            CircuitBreaker breaker = BREAKERS.get(commandKey);
            if (breaker == null || breaker.isClosed()) {
                return false;
            }
            if (breaker.isOpen()) {
                if (breaker.inSleepWindowNew()) {
                    return true;
                } else {
                    breaker.openHalf();
                    return false;
                }
            }
        } catch (Exception e) {
            LogHelper.error(logger, "short-circuited allowInvoker ", e);
        }
        return false;
    }

    /**
     * 如果存在 HalfOpen 可重试接口
     *
     * @param invoker
     * @param invocation
     * @return
     */
    @Override
    public boolean isSwitchLoadBalance(Invoker invoker, Invocation invocation) {
        try {
            String commandKey = getHystrixCommandKey(invoker, invocation);
            CircuitBreaker breaker = BREAKERS.get(commandKey);
            if (breaker != null && breaker.isHalfOpen()) {
                LogHelper.error(logger, "short-circuited isSwitchLoadBalance isHalfOpen:" + commandKey);
                return true;
            } else {
                return false;
            }

        } catch (Exception e) {
            LogHelper.error(logger, "short-circuited isSwitchLoadBalance ", e);
            return false;
        }
    }

    /**
     * 获取熔断器
     *
     * @param url
     * @param key
     * @return
     */
    private CircuitBreaker getCircuitBreaker(URL url, String key) {
        try {
            int circuitBreakerRequestVolumeThreshold = url.getParameter(Constants.CIRCUIT_BREAKER_REQUEST_VOLUME_THRESHOLD, Integer.parseInt(ThresholdConfig.CIRCUIT_BREAKER_REQUEST_THRESHOLD));
            int circuitBreakerSleepWindowInMilliseconds = url.getParameter(Constants.CIRCUIT_BREAKER_SLEEP_WINDOWIN_MILLISECONDS, Integer.parseInt(ThresholdConfig.CIRCUIT_BREAKER_SLEEP_MILLISECONDS));
            int circuitBreakerErrorThresholdPercentage = url.getParameter(Constants.CIRCUIT_BREAKER_ERROR_THRESHOLD_PERCENTAGE, Integer.parseInt(ThresholdConfig.CIRCUIT_BREAKER_ERROR_PERCENTAGE));
            CircuitBreaker breaker = BREAKERS.get(key);
            if (breaker == null) {
                CircuitBreakerConfig cfg = CircuitBreakerConfig.newDefault();
                cfg.setCircuitBreakerRequestVolumeThreshold(circuitBreakerRequestVolumeThreshold);
                cfg.setCircuitBreakerSleepWindowInMilliseconds(circuitBreakerSleepWindowInMilliseconds);
                cfg.setCircuitBreakerErrorThresholdPercentage(circuitBreakerErrorThresholdPercentage);
                breaker = new CircuitBreaker(key, cfg);
                BREAKERS.putIfAbsent(key, breaker);
            } else {
                CircuitBreakerConfig cfg = breaker.getConfig();
                cfg.parmEqual(circuitBreakerRequestVolumeThreshold, circuitBreakerSleepWindowInMilliseconds, circuitBreakerErrorThresholdPercentage);
            }
            return breaker;
        } catch (Exception e) {
            LogHelper.error(logger, "short-circuited getCircuitBreaker() ", e);
        }
        return null;
    }

    public static CircuitBreakerManagerDefault getBreakerManager() {
        return BREAKER_MANAGER;
    }

    private void sendSmsMsg(String msg) {
        SystemMonitorParams systemMonitorParams = new SystemMonitorParams("sms", new Date(),
                msg, null, null, null);
        new IndustrySmsSender(systemMonitorParams).sendMessageAsyn();
    }

    private boolean isSmsReady() {
        return !DEFAULT_HOUST.equals(HttpConf.GIM_REST_HOST) && !DEFAULT_HOUST.equals(HttpConf.TENANT_REST_HOST) && !DEFAULT_HOUST.equals(HttpConf.ERPSMS_REST_HOST);
    }
}
