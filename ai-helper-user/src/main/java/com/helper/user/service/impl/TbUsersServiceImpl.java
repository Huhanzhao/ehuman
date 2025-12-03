package com.helper.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.helper.commonauth.utils.JwtUtils;
import com.helper.commonservicescommon.entity.BusinessException;
import com.helper.commonservicescommon.entity.Result;
import com.helper.commonservicescommon.utils.RedisUtil;
import com.helper.user.entity.TbUsers;
import com.helper.user.entity.dto.LoginDto;
import com.helper.user.entity.dto.RegisterDto;
import com.helper.user.enums.UserRoleEnum;
import com.helper.user.mapper.TbUsersMapper;
import com.helper.user.service.TbUsersService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.UUID;

/**
* @author issuser
* @description 针对表【TB_USERS】的数据库操作Service实现
* @createDate 2025-11-17 17:26:04
*/
@Service
public class TbUsersServiceImpl extends ServiceImpl<TbUsersMapper, TbUsers>
    implements TbUsersService{

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RedisUtil redisUtil;

    @Override
    public Result<Map<String, String>> login(LoginDto loginDto) {
        String password = loginDto.getPassword();
        String userId = loginDto.getUserId();
        TbUsers tbUsers = this.getOne(new QueryWrapper<TbUsers>().eq("user_id", userId));
        if (tbUsers == null) {
            throw new BusinessException("用户不存在");
        }
        String secretPassword = tbUsers.getPassword();
        boolean matches = passwordEncoder.matches(password, secretPassword);
        if (matches) {
            String version = UUID.randomUUID().toString();
            redisUtil.hSet("user:" + userId, "version",version);
            redisUtil.expire("user:" + userId, 60 * 60 * 24 * 7);
            String token = JwtUtils.generateToken(userId, version);
            String refreshToken = JwtUtils.generateRefreshToken(userId);
            return Result.success(Map.of("token", token, "refreshToken", refreshToken));
        }
        // todo 补充邮箱登录
        return Result.fail("账号或密码错误");
    }

    @Override
    public boolean register(RegisterDto registerDto) {
        // 判断是否存在
        TbUsers tbUsers = this.getOne(new QueryWrapper<TbUsers>().eq("user_id", registerDto.getUserId()));
        if (tbUsers != null) {
            throw new BusinessException("用户已存在");
        }

        String password = registerDto.getPassword();
        String encodePassword = passwordEncoder.encode(password);
        return this.save(new TbUsers(registerDto.getUserId(), encodePassword,
                registerDto.getEmail(), UserRoleEnum.fromCode(registerDto.getRole())));
    }
}




