package com.bot.service.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.bot.service.entity.TbAiChatMemoryDo;
import com.bot.service.service.TbAiChatMemoryService;
import com.bot.service.mapper.TbAiChatMemoryMapper;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author Lenovo
* @description 针对表【tb_ai_chat_memory(ai聊天记忆表)】的数据库操作Service实现
* @createDate 2025-11-03 22:19:57
*/
@Service
public class TbAiChatMemoryServiceImpl extends ServiceImpl<TbAiChatMemoryMapper, TbAiChatMemoryDo>
    implements TbAiChatMemoryService {

    @Override
    public List<TbAiChatMemoryDo> getMessageList(Long userId, Integer pageSize) {
        Page<TbAiChatMemoryDo> page = new Page<>(1,pageSize);
        return this.page(page,Wrappers.<TbAiChatMemoryDo>lambdaQuery()
                .eq(TbAiChatMemoryDo::getMemoryId, userId)
                .orderByDesc(TbAiChatMemoryDo::getCreateTime)
        ).getRecords().reversed();
    }

    @Override
    public void removeByMemoryIdAndLikeMessage(Long memoryId, String message) {
        remove(lambdaQuery()
                .eq(TbAiChatMemoryDo::getRole, "assistant")
                .eq(TbAiChatMemoryDo::getMemoryId, memoryId)
                .like(TbAiChatMemoryDo::getMessage, message));
    }
}




