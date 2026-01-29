package com.helper.services.plugin;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.KeyStrategy;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.helper.services.config.ChatClientFactory;
import com.helper.services.entity.TbAiChatMemoryDo;
import com.helper.services.node.MessageCheckNode;
import com.helper.services.node.MessageGenerateNode;
import com.helper.services.node.SentimentAnalysisNode;
import com.helper.services.node.UserGracesNode;
import com.helper.services.service.TbAiChatMemoryService;
import com.helper.services.utils.MemoryCastUtils;
import com.mikuac.shiro.annotation.MessageHandlerFilter;
import com.mikuac.shiro.annotation.PrivateMessageHandler;
import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.MsgUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.BotPlugin;
import com.mikuac.shiro.dto.event.message.PrivateMessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.Message;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;

@Component
@Shiro
@RequiredArgsConstructor
@Slf4j
public class QQBotPlugin extends BotPlugin {

    private final SentimentAnalysisNode sentimentAnalysisNode;

    private final UserGracesNode userGracesNode;

    private final MessageGenerateNode messageGenerateNode;

    private final MessageCheckNode messageCheckNode;

    private final TbAiChatMemoryService tbAiChatMemoryService;

    @PrivateMessageHandler
    @MessageHandlerFilter()
    public void privateHandler(Bot bot , PrivateMessageEvent event, Matcher matcher){

        String userInput = event.getMessage();
        // 保存最新消息
        tbAiChatMemoryService.save(new TbAiChatMemoryDo().setMemoryId(event.getUserId()).setRole("user").setMessage(userInput));

        StateGraph stateGraph = new StateGraph(
//                () -> {
//            Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
//            keyStrategyMap.put("output", new AppendStrategy());  // 追加策略 状态机里面会变成arrayList类型
//            return keyStrategyMap;
//        }
        );

        try {
            stateGraph.addNode("sentimentAnalysisNode", sentimentAnalysisNode)
                      .addNode("gracesNode", userGracesNode)
                    .addNode("messageGenerateNode", messageGenerateNode)
                    .addNode("messageCheckNode", messageCheckNode)
                      .addEdge(StateGraph.START, "sentimentAnalysisNode")
                      .addEdge("sentimentAnalysisNode", "gracesNode")
//                    .addConditionalEdges("node2", edge_async(GracesNode::decideNextNode),
//                    Map.of("aaa", "sentimentAnalysisNode", "bbb", "node3"))
                      .addEdge("gracesNode", "messageGenerateNode")
                      .addEdge("messageGenerateNode", "messageCheckNode")
                      .addEdge("messageCheckNode",StateGraph.END );

            CompiledGraph compiledGraph = stateGraph.compile();

//            RunnableConfig config = RunnableConfig.builder()
//                    .threadId("replace-strategy-demo")
//                    .build();

            // 执行
            Map<String, Object> input = Map.of(
                    "userInput", userInput,
                    "userId", event.getUserId()
            );
            OverAllState overAllState = compiledGraph.invoke(input).get();

            String aiMessage = (String) overAllState.value("finalOutput", String.class).orElse("");

            if (aiMessage.isEmpty()) {
                log.error("no message");
                return;
            }

            log.info("aiMessage: {}", aiMessage);
            String[] splitMessages = aiMessage.split("&&");
            for (String splitMessage : splitMessages) {
                Thread.sleep(1000 + (int) (Math.random() * 1000));
                String msg = MsgUtils.builder().text(splitMessage).build();
                bot.sendPrivateMsg(event.getUserId(),msg,false);
                tbAiChatMemoryService.save(new TbAiChatMemoryDo().setMemoryId(event.getUserId()).setRole("assistant").setMessage(splitMessage));
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
