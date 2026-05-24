package com.helper.services.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.helper.services.entity.TbUserGraceDo;
import com.helper.services.service.TbUserGraceDoService;
import com.helper.services.mapper.TbUserGraceDoMapper;
import org.springframework.stereotype.Service;

/**
* @author Lenovo
* @description 针对表【tb_user_grace】的数据库操作Service实现
* @createDate 2026-01-25 15:37:28
*/
@Service
public class TbUserGraceDoServiceImpl extends ServiceImpl<TbUserGraceDoMapper, TbUserGraceDo>
    implements TbUserGraceDoService{

    @Override
    public TbUserGraceDo getByUserId(Long userId) {
        return lambdaQuery()
                .eq(TbUserGraceDo::getUserId, userId)
                .one();
    }

    @Override
    public void updateGraceByUserId(Long userId, Integer grace) {
        lambdaUpdate()
                .set(TbUserGraceDo::getGrace, grace)
                .eq(TbUserGraceDo::getUserId, userId)
                .update();
    }
}




