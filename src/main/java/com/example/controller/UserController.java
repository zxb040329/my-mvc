package com.example.controller;

import com.example.annotation.MyController;
import com.example.annotation.MyRequestMapping;
import com.example.annotation.MyResponseBody;
import com.example.vo.User;

import java.util.Arrays;

@MyController
@MyRequestMapping("/user")
public class UserController {

    // http://localhost:8080/mvc/user/all?name=zhangsan&age=10
    // 返回json
    @MyRequestMapping("/all")
    @MyResponseBody
    public Object getUser(Integer age, String name) {
        System.out.println("age: " +  age + "## name = " + name);
        User u1 = new User("张三", 1);
        User u2 = new User("李四", 2);
        return Arrays.asList(u1, u2);
    }

    // 返回json
    @MyRequestMapping("/getAll")
    @MyResponseBody
    public Object getUser(String username) {
        System.out.println("## username = " + username);
        User u1 = new User("张三", 1);
        User u2 = new User("李四", 2);
        return Arrays.asList(u1, u2);
    }

    // http://localhost:8080/mvc/user/toList?name=zhangsan&age=10
    // 到达页面
    @MyRequestMapping("/toList")
    public Object toList() {
        System.out.println("到达List页面");
        return "list";
    }

    public void output() {

    }
}
