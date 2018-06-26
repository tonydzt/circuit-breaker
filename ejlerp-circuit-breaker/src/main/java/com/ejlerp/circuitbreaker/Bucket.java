package com.ejlerp.circuitbreaker;

import java.util.concurrent.atomic.AtomicLong;

/**
 * @author dzt
 * @date 18/6/21
 * Hope you know what you have done
 */
public class Bucket {

    private AtomicLong startTime= new AtomicLong(0);
    private AtomicLong endTime=new AtomicLong(0);
    private AtomicLong longAdderTotle =new AtomicLong();
    private AtomicLong longAdderFail =new AtomicLong();
    public void addLongAdderTotle(){
        longAdderTotle.incrementAndGet();
    }
    public void addLongAdderFail(){
        longAdderFail.incrementAndGet();
    }

    /**
     * 求和。。。。。
     * @return
     */
    public long sumLongAdderTotle(){
        return longAdderTotle.longValue();
    }
    public long sumLongAdderFail(){
        return longAdderFail.longValue();
    }

    public void setStartTime(){
        startTime.set(System.currentTimeMillis());
    }
    public void setStartTime(long sTime){
        startTime.set(sTime);
    }
    public long getStartTime() {
        return startTime.get();
    }
    public void setEndTime(long edTime){
        endTime.set(edTime);
    }
    public long getEndTime(){
        return endTime.get();
    }
    public Bucket(){
        this.setStartTime();
        this.setEndTime(startTime.get()+1000);
    }
    public void reset(long eTime){
        this.setStartTime(eTime-1000);
        this.setEndTime(eTime);
        longAdderTotle.set(0);
        longAdderFail.set(0);
    }
    @Override
    public String toString() {
        return "Bucket{" +
                "startTime=" + startTime.get() +
                ", endTime=" + endTime.get() +
                ", longAdderTotle=" + longAdderTotle.toString() +
                ", longAdderFail=" + longAdderFail.toString() +
                '}';
    }
}
