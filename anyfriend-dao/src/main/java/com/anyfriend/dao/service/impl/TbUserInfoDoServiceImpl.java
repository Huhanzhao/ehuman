package com.anyfriend.dao.service.impl;

import com.anyfriend.dao.mapper.TbUserInfoDoMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anyfriend.dao.entity.TbUserInfoDo;
import com.anyfriend.dao.service.TbUserInfoDoService;
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




