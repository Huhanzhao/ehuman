package com.anyfriend.dao.service;

import com.anyfriend.dao.entity.TbChatConfigDo;
import com.baomidou.mybatisplus.extension.service.IService;

/**
* @author Lenovo
* @description 针对表【tb_chat_config】的数据库操作Service
* @createDate 2026-02-09 00:47:23
*/
public interface TbChatConfigDoService extends IService<TbChatConfigDo> {

    TbChatConfigDo getByUserId(Long userId);

    void updateByUserId(Long userId, Double temperature, Double topP);
}
