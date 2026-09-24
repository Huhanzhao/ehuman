package com.ehuman.dao.service.impl;

import com.ehuman.dao.entity.TbBotPromptDo;
import com.ehuman.dao.mapper.TbBotPromptDoMapper;
import com.ehuman.dao.service.TbBotPromptDoService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
* @author Lenovo
* @description 针对表【tb_bot_prompt】的数据库操作Service实现
* @createDate 2026-05-30 11:48:54
*/
@Service
public class TbBotPromptDoServiceImpl extends ServiceImpl<TbBotPromptDoMapper, TbBotPromptDo>
    implements TbBotPromptDoService{

    @Override
    public String getRolePrompt() {
        return lambdaQuery()
                .eq(TbBotPromptDo::getEnable, true)
                .oneOpt()
                .map(TbBotPromptDo::getContent)
                .orElse(null);
    }

    @Override
    @Transactional
    public void switchEnableRolePrompt(String roleName) {
        lambdaUpdate().set(TbBotPromptDo::getEnable,false).eq(TbBotPromptDo::getEnable,true).update();
        lambdaUpdate().set(TbBotPromptDo::getEnable,true).eq(TbBotPromptDo::getName,roleName).update();
    }
}




