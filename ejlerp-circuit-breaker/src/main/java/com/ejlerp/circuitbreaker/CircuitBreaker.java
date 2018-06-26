package com.ejlerp.circuitbreaker;

import com.alibaba.dubbo.common.logger.Logger;
import com.alibaba.dubbo.common.logger.LoggerFactory;
import com.alibaba.dubbo.common.utils.ConfigUtils;
import com.alibaba.dubbo.common.utils.LogHelper;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author dzt
 * @date 18/6/21
 * Hope you know what you have done
 */
public class CircuitBreaker {

    private static final Logger logger = LoggerFactory.getLogger(CircuitBreaker.class);

    private String name;
    /**
     * 重新初始化buckets
     */
    private Lock incrLock = new ReentrantLock();
    /**
     * 计算bucket位置锁
     */
    private Lock tradLock = new ReentrantLock();

    private CircuitBreakerConfig config;

    private AtomicInteger state = new AtomicInteger(CircuitBreakerState.CLOSED.ordinal());

    /**
     * 最近进入open状态的时间
     */
    private volatile long lastOpenedTime;

    /**
     * half-open状态的连续成功次数,失败立即清零
     */
    private AtomicInteger consecutiveSuccCount = new AtomicInteger(0);

    private Bucket[] buckets = null;
    /**
     * 指向当前 bucket
     */
    private AtomicInteger point = new AtomicInteger(0);
    /**
     * 缓冲时间 熔断起作用 应大于 buckets.length*1000*
     */
    private AtomicLong buffTime = new AtomicLong(0);
    private AtomicBoolean buffStatus = new AtomicBoolean(false);

    public final static int BUCKETMIN = 2;

    /**
     * 单位毫秒
     */
    public final static int MILLIS = 1000;

    public CircuitBreaker(String name, CircuitBreakerConfig config) {

        String bucket = ConfigUtils.getProperty(Constants.CIRCUIT_BREAKER_ROLLING_STATISTICAL_WINDOWBUCKETS,
                ThresholdConfig.CIRCUIT_BREAKER_ROLLING_WINDOWBUCKETS);
        //该值设置太小没意义
        int b = Integer.valueOf(bucket);
        if (b < BUCKETMIN) {
            b = Integer.valueOf(ThresholdConfig.CIRCUIT_BREAKER_ROLLING_WINDOWBUCKETS);
        }
        buckets = new Bucket[b];
        for (int i = 0; i < buckets.length; i++) {
            buckets[i] = new Bucket();
        }
        this.config = config;
        this.name = name;
    }

    /**
     * 状态判断
     *
     * @return
     */
    public boolean isOpen() {
        return CircuitBreakerState.OPEN.ordinal() == state.get();
    }

    public boolean isHalfOpen() {
        return CircuitBreakerState.HALF_OPEN.ordinal() == state.get();
    }

    public boolean isClosed() {
        return CircuitBreakerState.CLOSED.ordinal() == state.get();
    }

    //状态操作

    /**
     * closed->open | halfopen -> open
     */
    public void open() {
        lastOpenedTime = System.currentTimeMillis();
        state.getAndSet(CircuitBreakerState.OPEN.ordinal());
    }

    /**
     * open -> halfopen
     */
    public void openHalf() {
        consecutiveSuccCount.set(0);
        state.getAndSet(CircuitBreakerState.HALF_OPEN.ordinal());
    }

    /**
     * halfopen -> close
     */
    public void close() {
        state.getAndSet(CircuitBreakerState.CLOSED.ordinal());
    }

    //阈值判断

    /**
     * 是否应该转到half open
     * 前提是 open state
     *
     * @return
     */
    public boolean inSleepWindowNew() {
        long tmp = System.currentTimeMillis() - lastOpenedTime;
        return tmp < config.getCircuitBreakerSleepWindowInMilliseconds();
    }

    /**
     * 窗口内时间
     *
     * @return
     */
    public long getTimeOfWindow() {
        if (state.get() == CircuitBreakerState.OPEN.ordinal()) {
            return System.currentTimeMillis() - lastOpenedTime;
        } else {
            return 0L;
        }
    }

    /**
     * 是否应该从close转到open
     *
     * @return
     */
    public boolean closeFailThresholdReached() {
        if (getTotleRequest() > config.getCircuitBreakerRequestVolumeThreshold()
                && getErrorThresholdPercentage() > config.getCircuitBreakerErrorThresholdPercentage() && isClosed()) {
            LogHelper.error(logger, "short-circuited To reach the threshold totleRequest=" + getTotleRequest() + ",ErrorThresholdPercentage=" + getErrorThresholdPercentage());
            return true;
        } else {
            LogHelper.warn(logger, "totleRequest:" + getTotleRequest() + ",ErrorThresholdPercentage:" + getErrorThresholdPercentage());
            return false;
        }
    }

    /**
     * half-open状态下是否达到close的阈值
     *
     * @return
     */
    public boolean isConsecutiveSuccessThresholdReached() {
        return consecutiveSuccCount.get() >= config.getConsecutiveSuccThreshold();
    }

    public boolean getBuffStatus() {
        return buffStatus.get();
    }

    /**
     * 每个bucket对应时间段内的一秒，15个bucket就代表以15秒为一个整体统计错误率，下一个15秒重新统计
     */
    public void incrTotleCount() {
        try {
            if (!buffStatus.get()) {
                logger.warn("short-circuited is buff time");
                if (buffTime.get() == 0L) {
                    buffTime.set(System.currentTimeMillis());
                } else {
                    if ((System.currentTimeMillis() - buffTime.get()) > buckets.length * MILLIS) {
                        buffStatus.set(true);
                        logger.warn("short-circuited start work");
                    }
                }
            }
            long currentTime = System.currentTimeMillis();
            long bucketStartTime = buckets[point.get()].getStartTime();
            long bucketEndTime = buckets[point.get()].getEndTime();
            if (bucketStartTime <= currentTime && currentTime <= bucketEndTime) {
                //该请求落在当前bucket
                buckets[point.get()].addLongAdderTotle();
            } else {
                /*超过固定时间 重新计数*/
                if ((currentTime - bucketEndTime) > buckets.length * MILLIS) {
                    if (incrLock.tryLock()) {
                        try {
                            buckets[point.get()].reset(currentTime);
                            buckets[point.get()].addLongAdderTotle();
                            for (int i = 0; i < buckets.length; i++) {
                                if (i != point.get()) {
                                    buckets[i].reset(currentTime);
                                }
                            }
                        } finally {
                            incrLock.unlock();
                        }
                    }
                } else {
                    int j = 1;
                    /*少于固定时间 buckets.length*1000 计算指针位置*/
                    long endtime = bucketEndTime + j * MILLIS;
                    if (tradLock.tryLock()) {
                        try {
                            while (endtime - currentTime < MILLIS) {
                                if (point.get() == buckets.length - 1) {
                                    point.set(0);
                                } else {
                                    point.incrementAndGet();
                                }
                                buckets[point.get()].reset(endtime);
                                j++;
                                endtime = bucketEndTime + j * MILLIS;
                            }
                        } finally {
                            tradLock.unlock();
                        }
                    }
                    buckets[point.get()].addLongAdderTotle();
                }
            }
        } catch (Exception e) {
            logger.error("incrTotleCount error", e);
        }
    }

    public void incrFailCount() {
        try {
            buckets[point.get()].addLongAdderFail();
        } catch (Exception e) {
            logger.error("incrFailCount error", e);
        }
    }

    public long getTotleRequest() {
        try {
            long totle = 0;
            for (int i = 0; i < buckets.length; i++) {
                totle = totle + buckets[i].sumLongAdderTotle();
            }
            return totle;
        } catch (Exception e) {
            logger.error("getTotleRequest error", e);
        }
        return 0;
    }

    public long getTotleErrorRequest() {
        try {
            long totle = 0;
            for (int i = 0; i < buckets.length; i++) {
                totle = totle + buckets[i].sumLongAdderFail();
            }
            return totle;
        } catch (Exception e) {
            logger.error("getTotleRequest error", e);
        }
        return 0;
    }

    /**
     * 平均错误率
     *
     * @return
     */
    public double getErrorThresholdPercentage() {
        try {
            long totle = 0;
            long error = 0;
            for (int i = 0; i < buckets.length; i++) {
                totle = totle + buckets[i].sumLongAdderTotle();
                error = error + buckets[i].sumLongAdderFail();
            }
            if (error > totle) {
                return 100.00;
            }
            if (totle == 0 || error == 0) {
                return 0.00;
            }
            return error * 1.0 / totle * 100;
        } catch (Exception e) {
            logger.error("getErrorThresholdPercentage error", e);
        }
        return 0.00;
    }

    public AtomicInteger getConsecutiveSuccCount() {
        return consecutiveSuccCount;
    }

    public CircuitBreakerState getState() {
        return CircuitBreakerState.values()[state.get()];
    }

    public CircuitBreakerConfig getConfig() {
        return config;
    }

    @Override
    public String toString() {

        return "errorThresholdPercentage=" + getErrorThresholdPercentage() +
                "&totleRequest=" + getTotleRequest() +
                "&totleErrorRequest=" + getTotleErrorRequest() +
                "&state=" + state +
                "&consecutiveSuccCount=" + consecutiveSuccCount +
                "&timeOfWindow=" + getTimeOfWindow() +
                "&circuitBreakerRequestVolumeThreshold=" + config.getCircuitBreakerRequestVolumeThreshold() +
                "&circuitBreakerSleepWindowInMilliseconds=" + config.getCircuitBreakerSleepWindowInMilliseconds() +
                "&circuitBreakerErrorThresholdPercentage=" + config.getCircuitBreakerErrorThresholdPercentage();
    }
}
