package com.helper.user.controller;

import com.helper.commonservicescommon.entity.Result;
import com.helper.user.entity.dto.LoginDto;
import com.helper.user.entity.dto.RegisterDto;
import com.helper.user.service.TbUsersService;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserLoginController {

    @Resource
    private TbUsersService tbUsersService;

    // todo 注册
    @PostMapping("/register")
    public Result<?> register(@RequestBody RegisterDto registerDto){
        return tbUsersService.register(registerDto) ? Result.success() : Result.fail("注册失败");
    }


    // todo 登录
    @PostMapping("/login")
    public Result<Map<String,String>> login(@RequestBody LoginDto loginDto){
        return tbUsersService.login(loginDto);
    }
    // todo 登出
}
