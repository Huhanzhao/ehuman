package com.bot.service.service;

import com.bot.service.entity.TbBotLifeDetailDo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author Lenovo
* @description 针对表【tb_bot_life_detail】的数据库操作Service
* @createDate 2026-05-11 21:56:55
*/
public interface TbBotLifeDetailDoService extends IService<TbBotLifeDetailDo> {
    String getCurrentActivity(Long botId);

}
