package com.anyfriend.dao.service.impl;

import com.anyfriend.dao.mapper.TbChatConfigDoMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anyfriend.dao.entity.TbChatConfigDo;
import com.anyfriend.dao.service.TbChatConfigDoService;
import org.springframework.stereotype.Service;

/**
* @author Lenovo
* @description 针对表【tb_chat_config】的数据库操作Service实现
* @createDate 2026-02-09 00:47:23
*/
@Service
public class TbChatConfigDoServiceImpl extends ServiceImpl<TbChatConfigDoMapper, TbChatConfigDo>
    implements TbChatConfigDoService{

    @Override
    public TbChatConfigDo getByUserId(Long userId) {
        return lambdaQuery()
                .eq(TbChatConfigDo::getUserId, userId)
                .one();
    }

    @Override
    public void updateByUserId(Long userId, Double temperature, Double topP) {
        lambdaUpdate()
                .set(TbChatConfigDo::getTemperature, temperature)
                .set(TbChatConfigDo::getTopP, topP)
                .eq(TbChatConfigDo::getUserId, userId)
                .update();
    }
}




