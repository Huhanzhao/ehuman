package com.anyfriend.core.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.anyfriend.core.bean.ChatClientFactory;
import com.anyfriend.core.utils.MemoryCastUtils;
import com.anyfriend.dao.entity.TbAiChatMemoryDo;
import com.anyfriend.dao.enums.NodeEnum;
import com.anyfriend.dao.service.TbAiChatMemoryService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.PromptTemplate;
import org.springframework.stereotype.Component;

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
        Long userId = (Long) state.value("userId").orElse(0L);
        // todo 会话结束后不发送消息
        // 查询历史消息
        List<TbAiChatMemoryDo> messageList = tbAiChatMemoryService.getMessageList(userId, 15);
        String memoryString = MemoryCastUtils.memoryCastToString(messageList);
        List<TbAiChatMemoryDo> userMessageList = messageList.stream().filter(memoryDo -> "user".equals(memoryDo.getRole())).toList();
        if (userMessageList.isEmpty()){
            memoryString += "\n[旁白： 用户一直没有回复你的消息 " + tbAiChatMemoryService.getLastUserMessage(userId) + "]";
        }
        ChatClient chatClient = chatClientFactory.createChatClient(userId, NodeEnum.AUXILIARY);
        PromptTemplate promptTemplate = PromptTemplate.builder().template(
                """
                       ## 你是聊天结束意图判断模块
                                   
                       ## 任务
                       判断用户当前消息是否需要回复
                       
                       ## 输入
                       - 历史消息：{memoryString}
                       - 用户当前消息：（由调用方传入）
                       
                       ## 输出规则
                       只输出以下两个单词之一，不输出任何其他内容：
                       
                       - `exit`：不需要回复（对话已结束）
                       - `next`：需要回复（其他所有情况）
                       
                       ## 判定标准
                       **exit（对话结束）** 必须同时满足：
                       - 用户明确表达结束（晚安/拜拜/再见等）
                       - AI在前序消息中已回复过同样的结束语
                       
                       **next（需要回复）** 包括：
                       - 用户未说结束语
                       - 用户说了结束语但AI尚未回复
                       - 暧昧、试探等任何其他内容
                        """
        ).build();
        String prompt = promptTemplate.render(Map.of("memoryString", memoryString));
        String content = chatClient.prompt(prompt)
                .user(" 以上是用户与ai对话历史，请分析会话是否结束，是否需要ai回复").call().content();
        log.info("用户意图结果：{}", content);
        return Map.of("userWantTo", content,"memoryString", memoryString);
    }

    public static String nextNode(OverAllState state) {
        // 路由下一个节点
        return (String) state.value("userWantTo").orElse("");
    }
}
