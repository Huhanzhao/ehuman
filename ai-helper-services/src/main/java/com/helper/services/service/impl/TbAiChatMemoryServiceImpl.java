package com.helper.services.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.helper.services.entity.TbAiChatMemoryDo;
import com.helper.services.service.TbAiChatMemoryService;
import com.helper.services.mapper.TbAiChatMemoryMapper;
import com.helper.services.utils.MemoryCastUtils;
import org.springframework.ai.chat.messages.Message;
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
    public List<Message> getMessageList(Long userId, Integer pageSize) {
        Page<TbAiChatMemoryDo> page = new Page<>(1,pageSize);
        List<TbAiChatMemoryDo> memoryList = this.page(page,Wrappers.<TbAiChatMemoryDo>lambdaQuery()
                .eq(TbAiChatMemoryDo::getMemoryId, userId)
                .orderByDesc(TbAiChatMemoryDo::getCreateTime)
        ).getRecords().reversed();
        return MemoryCastUtils.memoryCastToMessages(memoryList);
    }
}




