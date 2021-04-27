package com.example.commons;

public class HttpInfo {

    // 参数错误
    public static final int PARAMETER_ERROR = 401;

    // 没有权限
    public static final int FORBIDEN = 403;

    // 无访问资源
    public static final int NO_RESOURCE = 404;

    // 协议错误
    public static final int INVALID_PROTOCOL = 405;

    //  无访问资源提示信息
    public static final String NO_RESOURCE_TIPS = "咦，资源没找到. 404";


    //  无访问资源提示信息
    public static final String PARAMETER_ERROR_TIPS = "参数类型错误. 401";
}
