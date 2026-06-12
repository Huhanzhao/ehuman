package com.anyfriend.dao.service.impl;

import com.alibaba.fastjson2.util.DateUtils;
import com.anyfriend.dao.mapper.TbAiChatMemoryMapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anyfriend.dao.entity.TbAiChatMemoryDo;
import com.anyfriend.dao.service.TbAiChatMemoryService;
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
    public String getLastUserMessage(Long userId) {
        // 查询最后一条用户消息的时间
        TbAiChatMemoryDo lastUserMessage = this.getOne(
                Wrappers.<TbAiChatMemoryDo>lambdaQuery()
                        .eq(TbAiChatMemoryDo::getMemoryId, userId)
                        .eq(TbAiChatMemoryDo::getRole, "user")
                        .orderByDesc(TbAiChatMemoryDo::getCreateTime)
                        .last("LIMIT 1 OFFSET 1")
        );
        String lastMessageTime = "";
        if (lastUserMessage != null && lastUserMessage.getCreateTime() != null) {
            lastMessageTime = "用户上一条消息是" + DateUtils.format(lastUserMessage.getCreateTime(), "MM月dd日 HH:mm");
        }
        return lastMessageTime;
    }

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




