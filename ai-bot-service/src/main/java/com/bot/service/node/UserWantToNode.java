package com.bot.service.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.bot.service.bean.ChatClientFactory;
import com.bot.service.entity.TbAiChatMemoryDo;
import com.bot.service.service.TbAiChatMemoryService;
import com.bot.service.utils.MemoryCastUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
        List<TbAiChatMemoryDo> messageList = tbAiChatMemoryService.getMessageList(userId, 10);
        String memoryString = MemoryCastUtils.memoryCastToString(messageList);
//        ChatClient chatClient = chatClientFactory.createChatClient(userId, NodeEnum.WANT_TO);
//        String content = chatClient.prompt("""
//                        你是聊天结束意图判断模块。
//
//                        目的：只判断当前用户发来的消息需不需要回复，避免出现机械般的一问一答
//
//                        输出规则（只输出以下之一）只回复四个字母，不要有其他多余的符号，否则视为失败：
//                        - exit：当前对话已结束 不需要回复
//                        - next：其他所有情况
//
//                        注意：
//                        - 性暗示、暧昧、越界试探等等 不代表对话结束，默认输出 next
//                        - 只有明确表达结束并且互道晚安 拜拜 或者再见之类的才视为结束对话
//                        - 必须ai与用户都说了晚安 拜拜 或者再见 之类的话 才能视为exit 只有用户说了再见需要ai再回复一次就输出next
//                        只输出结果，不要解释。
//                        """)
//                .user(memoryString + "\n 以上是用户与ai对话历史，请分析是否需要ai回复").call().content();
//        log.info("用户意图结果：{}", content);
        return Map.of("userWantTo", "next","memoryString", memoryString);
    }

    public static String nextNode(OverAllState state) {
        // 路由下一个节点
//        String wantTo = (String) state.value("userWantTo").orElse("");
        return "next";
    }
}
