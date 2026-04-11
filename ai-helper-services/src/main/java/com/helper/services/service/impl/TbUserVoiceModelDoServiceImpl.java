package com.helper.services.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.helper.services.entity.TbUserVoiceModelDo;
import com.helper.services.service.TbUserVoiceModelDoService;
import com.helper.services.mapper.TbUserVoiceModelDoMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author Lenovo
* @description 针对表【tb_user_voice_model】的数据库操作Service实现
* @createDate 2026-03-15 13:14:18
*/
@Service
public class TbUserVoiceModelDoServiceImpl extends ServiceImpl<TbUserVoiceModelDoMapper, TbUserVoiceModelDo>
    implements TbUserVoiceModelDoService{

    @Override
    public List<TbUserVoiceModelDo> getVoiceModelByUserId(Long userId) {
        return this.list(Wrappers.<TbUserVoiceModelDo>lambdaQuery().eq(TbUserVoiceModelDo::getUserId, userId)
                .eq(TbUserVoiceModelDo::getEnable, true)
        );
    }
}




