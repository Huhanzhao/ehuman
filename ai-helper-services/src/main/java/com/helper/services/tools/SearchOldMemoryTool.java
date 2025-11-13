package com.helper.services.tools;

import dev.langchain4j.agent.tool.Tool;
import dev.langchain4j.service.V;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class SearchOldMemoryTool {

    @Tool("矢量库中搜索问题相关历史聊天记录")
    public String searchChatMessage(@V("用户提问的消息") String userMessage, @V("memoryId 记忆id")String memoryId){
        log.info("搜索历史聊天记录方法被调用了,用户提问：{},用户记忆id：{}", userMessage,memoryId);
        // todo 完善检索记忆
        return "搜索历史聊天记录方法被调用了,开始搜索：" + userMessage;
    }
}
