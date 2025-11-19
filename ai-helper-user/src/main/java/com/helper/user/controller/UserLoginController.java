package com.helper.user.controller;

import com.helper.common.entity.Result;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
public class UserLoginController {

    // todo 注册
    // todo 登录
    @PostMapping("/login")
    public Result<?> login(){
        // todo 做公共返回类
        // todo 看聚合文档要求的登录返回体格式
        return null;
    }
    // todo 登出
}
