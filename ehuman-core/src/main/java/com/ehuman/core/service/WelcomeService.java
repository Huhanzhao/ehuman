package com.ehuman.core.service;

import com.ehuman.core.utils.LocalCacheUtil;
import com.ehuman.core.utils.SendMessageUtil;
import com.ehuman.dao.service.TbAiChatMemoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 白名单用户首次聊天后的欢迎语
 */
@Service
@RequiredArgsConstructor
public class WelcomeService {

    private static final String WELCOME_TEXT =
            "（欢迎使用ehuman，本项目完全开源，gitee地址https://gitee.com/huhanzhao/ehuman QQ群：832149010）";

    private static final String CACHE_KEY_SUFFIX = "_firstChatWelcome";

    private final BotAccessService botAccessService;

    private final TbAiChatMemoryService tbAiChatMemoryService;

    private final SendMessageUtil sendMessageUtil;

    /**
     * 在保存用户消息之前调用：历史为空说明是首次聊天
     */
    public void markFirstChatIfNeeded(Long userId) {
        if (!botAccessService.isSharedKeyUser(userId)) {
            return;
        }
        if (tbAiChatMemoryService.countByMemoryId(userId) > 0) {
            return;
        }
        LocalCacheUtil.getLocalCacheUtil().put(userId + CACHE_KEY_SUFFIX, true);
    }

    /**
     * 本轮回复完成之后调用
     */
    public void sendWelcomeIfPending(Long userId) {
        String key = userId + CACHE_KEY_SUFFIX;
        LocalCacheUtil localCacheUtil = LocalCacheUtil.getLocalCacheUtil();
        if (!localCacheUtil.containsKey(key)) {
            return;
        }
        // 先消费标记，保证只发一次
        localCacheUtil.remove(key);
        if (!botAccessService.isSharedKeyUser(userId)) {
            return;
        }
        sendMessageUtil.sendSystemMsg(userId, WELCOME_TEXT);
    }
}
