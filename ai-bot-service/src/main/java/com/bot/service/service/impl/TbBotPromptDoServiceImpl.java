package com.bot.service.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bot.service.entity.TbBotPromptDo;
import com.bot.service.enums.PromptTypeEnum;
import com.bot.service.service.TbBotPromptDoService;
import com.bot.service.mapper.TbBotPromptDoMapper;
import org.springframework.stereotype.Service;

/**
* @author Lenovo
* @description 针对表【tb_bot_prompt】的数据库操作Service实现
* @createDate 2026-05-30 11:48:54
*/
@Service
public class TbBotPromptDoServiceImpl extends ServiceImpl<TbBotPromptDoMapper, TbBotPromptDo>
    implements TbBotPromptDoService{

    @Override
    public String getContext(PromptTypeEnum promptTypeEnum) {
        return lambdaQuery()
                .eq(TbBotPromptDo::getPromptTypeEnum, promptTypeEnum)
                .oneOpt()
                .map(TbBotPromptDo::getContent)
                .orElse(null);
    }
}




