package com.helper.user.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.helper.commonservicescommon.entity.Result;
import com.helper.user.entity.TbUsers;
import com.helper.user.entity.dto.LoginDto;
import com.helper.user.entity.dto.RegisterDto;

import java.util.Map;

/**
* @author issuser
* @description 针对表【TB_USERS】的数据库操作Service
* @createDate 2025-11-17 17:26:04
*/
public interface TbUsersService extends IService<TbUsers> {

    Result<Map<String, String>> login(LoginDto loginDto);

    boolean register(RegisterDto registerDto);
}
