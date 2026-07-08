package com.anyfriend.dao.service;

import com.anyfriend.dao.entity.TbAiChatMemoryDo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author Lenovo
* @description 针对表【tb_ai_chat_memory(ai聊天记忆表)】的数据库操作Service
* @createDate 2025-11-03 22:19:57
*/
public interface TbAiChatMemoryService extends IService<TbAiChatMemoryDo> {
    List<TbAiChatMemoryDo> getMessageList(Long userId,Integer pageSize);

    void removeByMemoryIdAndLikeMessage(Long memoryId, String message);

    List<TbAiChatMemoryDo> getAllNoVectorMessageList(Long userId);

    void setVectorFlagByIds(List<Long> idList);
}
