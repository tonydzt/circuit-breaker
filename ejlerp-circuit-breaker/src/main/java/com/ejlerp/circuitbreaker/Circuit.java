package com.ejlerp.circuitbreaker;

/**
 * @author dzt
 * @date 18/6/21
 * Hope you know what you have done
 */
enum Circuit {

    /**
     * 熔断器切换
     */
    DFIRE("dfire"),
    /**
     * //默认熔断器
     */
    DEFAULT("default");
    String value;

    Circuit(String circuitBreaker) {
        this.value = circuitBreaker;
    };

    public static Circuit getByValue(String value) {
        for (Circuit typeEnum : Circuit.values()) {
            if (typeEnum.value.equals(value)) {
                return typeEnum;
            }
        }
        return DEFAULT;
    }
}
