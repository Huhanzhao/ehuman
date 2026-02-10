package com.helper.services.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.helper.commonservicescommon.entity.BusinessException;
import com.helper.services.bean.ChatClientFactory;
import com.helper.services.entity.TbAiChatMemoryDo;
import com.helper.services.enums.ChatModelEnum;
import com.helper.services.service.TbAiChatMemoryService;
import com.helper.services.utils.MemoryCastUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequiredArgsConstructor
@Slf4j
@Component
public class UserWantToNode implements NodeAction {
    private final ChatClientFactory chatClientFactory;

    private final TbAiChatMemoryService tbAiChatMemoryService;

    @Override
    public Map<String, Object> apply (OverAllState state){
        String userMessage = (String) state.value("userInput").orElse("");
        Long userId = (Long) state.value("userId").orElse(0L);
        List<TbAiChatMemoryDo> messageList = tbAiChatMemoryService.getMessageList(userId, 10);
        if (messageList.isEmpty()) {
            throw new BusinessException("用户输入读取失败");
        }
        String memoryString = MemoryCastUtils.memoryCastToString(messageList);
        log.info("用户输入:{}", userMessage);
        ChatClient chatClient = chatClientFactory.createChatClient(ChatModelEnum.GPT_5);
        String content = chatClient.prompt("""
                        你是聊天结束意图判断模块。
                                                
                        目的：只判断当前用户发来的消息需不需要回复，避免出现机械般的一问一答
                                                
                        输出规则（只输出以下之一）：
                        - exit：当前对话已结束 不需要回复
                        - next：其他所有情况
                        
                        注意：
                        - 性暗示、暧昧、越界试探等等 不代表对话结束，默认输出 next
                        - 只有明确表达结束并且互道晚安 拜拜 或者再见之类的才视为结束对话
                        只输出结果，不要解释。
                        """)
                .user(memoryString + "\n 以上是用户与ai对话历史，请分析用户意图").call().content();
        log.info("用户意图结果：{}", content);
        return Map.of("userWantTo", content,"memoryString", memoryString);
    }

    public static String nextNode(OverAllState state) {
        // 路由下一个节点
        String wantTo = (String) state.value("userWantTo").orElse("");
        return wantTo.isEmpty()? "next" : wantTo;
    }
}
