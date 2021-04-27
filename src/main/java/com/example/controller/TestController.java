package com.example.controller;

import com.example.annotation.MyController;
import com.example.annotation.MyRequestMapping;
import com.example.annotation.MyResponseBody;

import java.util.Arrays;
import java.util.List;

@MyController
public class TestController {

    @MyResponseBody
    @MyRequestMapping("/test")
    public Object test() {
        List<String> list = Arrays.asList("abc", "xml");
        return list;
    }

    @MyResponseBody
    @MyRequestMapping("/test2")
    public Object test2() {
        List<String> list = Arrays.asList("abc", "xml");
        return list;
    }
}
