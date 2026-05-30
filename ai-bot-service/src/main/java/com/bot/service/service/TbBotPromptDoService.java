package com.bot.service.service;

import com.bot.service.entity.TbBotPromptDo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author Lenovo
* @description 针对表【tb_bot_prompt】的数据库操作Service
* @createDate 2026-05-30 11:48:54
*/
public interface TbBotPromptDoService extends IService<TbBotPromptDo> {

    String getBotRoleContext();
}
