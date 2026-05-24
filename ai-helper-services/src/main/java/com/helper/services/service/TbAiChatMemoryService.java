package com.helper.services.service;

import com.helper.services.entity.TbAiChatMemoryDo;
import com.baomidou.mybatisplus.extension.service.IService;
import org.springframework.ai.chat.messages.Message;

import java.util.List;

/**
* @author Lenovo
* @description 针对表【tb_ai_chat_memory(ai聊天记忆表)】的数据库操作Service
* @createDate 2025-11-03 22:19:57
*/
public interface TbAiChatMemoryService extends IService<TbAiChatMemoryDo> {

    List<TbAiChatMemoryDo> getMessageList(Long userId,Integer pageSize);

    void removeByMemoryIdAndLikeMessage(Long memoryId, String message);
}
