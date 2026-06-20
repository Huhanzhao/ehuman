package com.anyfriend.dao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anyfriend.dao.entity.TbUserPluginDo;
import com.anyfriend.dao.service.TbUserPluginDoService;
import com.anyfriend.dao.mapper.TbUserPluginDoMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
* @author Lenovo
* @description 针对表【tb_user_plugin】的数据库操作Service实现
* @createDate 2026-06-19 14:52:29
*/
@Service
public class TbUserPluginDoServiceImpl extends ServiceImpl<TbUserPluginDoMapper, TbUserPluginDo>
    implements TbUserPluginDoService{

    @Override
    @Transactional
    public void turnOnOrOffTTSPlugin(Long userId, boolean enable) {
        boolean exists = lambdaQuery().eq(TbUserPluginDo::getUserId, userId).exists();
        if (!exists) {
            TbUserPluginDo tbUserPluginDo = new TbUserPluginDo();
            tbUserPluginDo.setUserId(userId);
            tbUserPluginDo.setPluginType(1);
            tbUserPluginDo.setEnable(enable);
            save(tbUserPluginDo);
        }else{
            lambdaUpdate().set(TbUserPluginDo::getEnable,enable).eq(TbUserPluginDo::getUserId, userId).update();
        }
    }

    @Override
    public boolean isEnableByUserId(Long userId) {
        return lambdaQuery().eq(TbUserPluginDo::getUserId, userId)
                .eq(TbUserPluginDo::getPluginType, 1)
                .eq(TbUserPluginDo::getEnable, true).exists();
    }
}




