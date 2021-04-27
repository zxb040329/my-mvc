package com.example.definition;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

public class MethodDefinition {
    // @MyController类中对应的方法
    private Method method;

    // 方法对应的对象
    private Object obj;

    //是否返回json
    private Boolean isJson = false;

    // LinkedHashMap 是有序， 因为在反射调用的时候，必须是有序的， 一定不能用hashmap来做。
    private Map<String, Class<?>> parametersInfo = new LinkedHashMap<>();

    public Method getMethod() {
        return method;
    }

    public void setMethod(Method method) {
        this.method = method;
    }

    public Object getObj() {
        return obj;
    }

    public void setObj(Object obj) {
        this.obj = obj;
    }

    public Boolean getJson() {
        return isJson;
    }

    public void setJson(Boolean json) {
        isJson = json;
    }

    public Map<String, Class<?>> getParametersInfo() {
        return parametersInfo;
    }

    public void setParametersInfo(Map<String, Class<?>> parametersInfo) {
        this.parametersInfo = parametersInfo;
    }
}
