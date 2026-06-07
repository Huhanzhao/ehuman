package com.bot.service.plugin;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.bot.service.node.test.test1;
import com.bot.service.node.test.test2;
import com.bot.service.utils.SendMessageUtil;
import com.mikuac.shiro.annotation.MessageHandlerFilter;
import com.mikuac.shiro.annotation.PrivateMessageHandler;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.BotContainer;
import com.mikuac.shiro.core.BotPlugin;
import com.mikuac.shiro.dto.event.message.PrivateMessageEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class TestPlugin extends BotPlugin {

    @Autowired
    private CompiledGraph sendMessageCompiledGraph;

    @Autowired
    private SendMessageUtil sendMessageUtil;

    @Autowired
    private BotContainer container;
    @Autowired
    private test1 test1;
    @Autowired
    private test2 test2;


    @PrivateMessageHandler
    @MessageHandlerFilter
    @Override
    public int onPrivateMessage(Bot bot, PrivateMessageEvent event)  {
//        String target = """
//            ## 目标：请基于你当前在做的事情和状态还有历史消息，生成一条主动发给用户的自然消息,一行或者两行。
//            不要太突兀，要与上下文相和谐 ，可以分享正在做的事、表达想念、撒娇、问好等。
//            """;
//        sendMessageCompiledGraph.invoke(Map.of("userId", event.getUserId(), "target", target, "initiative", true));
//        sendMessageUtil.sendVoiceMsg(event.getUserId(),event.getMessage());

        // 配置 KeyStrategyFactory
//        KeyStrategyFactory keyStrategyFactory = () -> {
//            HashMap<String, KeyStrategy> keyStrategyHashMap = new HashMap<>();
//            keyStrategyHashMap.put("test", new AppendStrategy());
//            keyStrategyHashMap.put("failReason", new AppendStrategy());
//            keyStrategyHashMap.put("human_feedback", new ReplaceStrategy());
//            return keyStrategyHashMap;
//        };
//
//        StateGraph stateGraph = new StateGraph(keyStrategyFactory);
//
//        try {
//            stateGraph
//                    .addNode("test1", node_async(test1))
//                    .addNode("test2", node_async(test2))
//
//                    .addEdge(StateGraph.START, "test1")
//                    .addEdge("test1", "test2")
//                    .addEdge("test2", "test1")
//                    .addEdge("test1", StateGraph.END);
//
//            // 配置内存保存器（用于状态持久化）
//            var saver = new MemorySaver();
//            var compileConfig = CompileConfig.builder()
//                    .saverConfig(SaverConfig.builder()
//                            .register(saver)
//                            .build())
//                    .build();
//
//            CompiledGraph compile = stateGraph.compile(compileConfig);
//            compile.invoke(Map.of());
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }

        return MESSAGE_IGNORE;
    }
}
