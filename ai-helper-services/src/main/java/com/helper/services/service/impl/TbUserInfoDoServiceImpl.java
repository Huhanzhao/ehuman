package com.helper.services.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.helper.services.entity.TbUserInfoDo;
import com.helper.services.service.TbUserInfoDoService;
import com.helper.services.mapper.TbUserInfoDoMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author Lenovo
* @description 针对表【tb_user_info】的数据库操作Service实现
* @createDate 2026-03-08 20:04:47
*/
@Service
public class TbUserInfoDoServiceImpl extends ServiceImpl<TbUserInfoDoMapper, TbUserInfoDo>
    implements TbUserInfoDoService{

    @Override
    public List<TbUserInfoDo> getUserInfoByUserId(Long userId) {
        return lambdaQuery().eq(TbUserInfoDo::getUserId, userId).list();
    }
}




