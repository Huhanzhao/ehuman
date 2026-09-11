package com.anyfriend.dao.service.impl;

import com.anyfriend.dao.entity.TbAiChatMemoryDo;
import com.anyfriend.dao.enums.MessageTypeEnum;
import com.anyfriend.dao.mapper.TbAiChatMemoryMapper;
import com.anyfriend.dao.service.TbAiChatMemoryService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
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

    @Override
    public List<TbAiChatMemoryDo> getAllNoVectorMessageList(Long userId) {
        return lambdaQuery().eq(TbAiChatMemoryDo::getMemoryId,userId)
                .eq(TbAiChatMemoryDo::getVectored,false)
                .orderByAsc(TbAiChatMemoryDo::getCreateTime)
                .list();
    }

    @Override
    public void setVectorFlagByIds(List<Long> idList) {
        lambdaUpdate().set(TbAiChatMemoryDo::getVectored,true).in(TbAiChatMemoryDo::getId, idList).update();
    }

    @Override
    public boolean existsUserYesterdayMessage(Long userId) {

        // 直接用 JVM 默认时区（前提是 JVM 和数据库时区一致）
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime yesterdayStart = now.toLocalDate().minusDays(1).atStartOfDay();
        LocalDateTime yesterdayEnd = now.toLocalDate().atStartOfDay().minusNanos(1);

        return lambdaQuery()
                        .eq(TbAiChatMemoryDo::getMemoryId,userId)
                        .eq(TbAiChatMemoryDo::getRole, MessageTypeEnum.USER_MESSAGE)
                        .between(TbAiChatMemoryDo::getCreateTime,yesterdayStart,yesterdayEnd).exists();
    }
}




