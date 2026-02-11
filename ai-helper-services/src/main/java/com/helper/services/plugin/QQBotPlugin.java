package com.helper.services.plugin;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.helper.services.entity.TbAiChatMemoryDo;
import com.helper.services.entity.TbChatConfigDo;
import com.helper.services.node.*;
import com.helper.services.service.TbAiChatMemoryService;
import com.helper.services.service.TbChatConfigDoService;
import com.mikuac.shiro.annotation.MessageHandlerFilter;
import com.mikuac.shiro.annotation.PrivateMessageHandler;
import com.mikuac.shiro.annotation.common.Order;
import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.MsgUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.BotPlugin;
import com.mikuac.shiro.dto.event.message.PrivateMessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;

@Component
@Shiro
@RequiredArgsConstructor
@Slf4j
public class QQBotPlugin extends BotPlugin {

    private final CompiledGraph compiledGraph;

    private final TbAiChatMemoryService tbAiChatMemoryService;

    private final TbChatConfigDoService tbChatConfigDoService;

    @PrivateMessageHandler
    @MessageHandlerFilter(cmd = "(?s)^(?!#).*") // 不是井号开头
    public int onPrivateMessage(Bot bot , PrivateMessageEvent event){
        String chatId = UUID.randomUUID().toString();
        String userInput = event.getMessage();


        // 执行
        Map<String, Object> input = Map.of(
                "userInput", userInput,
                "userId", event.getUserId()
        );
        // 配置线程 ID
        var invokeConfig = RunnableConfig.builder()
                .threadId(chatId)
                .build();
        OverAllState overAllState = compiledGraph.invoke(input,invokeConfig).get();



            String aiMessage = (String) overAllState.value("finalOutput", String.class).orElse("");

            if (aiMessage.isEmpty()) {
                log.error("no message");
                return MESSAGE_IGNORE;
            }

            log.info("aiMessage: {}", aiMessage);
            String[] splitMessages = aiMessage.split("\n");
        for (String splitMessage : splitMessages) {
                if (splitMessage.isEmpty()) continue;
                splitMessage  = splitMessage.replaceAll("。", "").trim();
            try {
                Thread.sleep(1000 + (int) (Math.random() * 1000));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            String msg = MsgUtils.builder().text(splitMessage).build();
            bot.sendPrivateMsg(event.getUserId(),msg,false);
            tbAiChatMemoryService.save(new TbAiChatMemoryDo().setMemoryId(event.getUserId()).setRole("assistant").setMessage(splitMessage));
        }
        return MESSAGE_IGNORE;
    }

//    @MessageHandlerFilter(cmd = "2")
//    @PrivateMessageHandler
//    public void privateHandler2(Bot bot , PrivateMessageEvent event, Matcher matcher) throws Exception {
//        var invokeConfig = RunnableConfig.builder()
//                .threadId("Thread1")
//                .build();
//        StateSnapshot state = this.compiledGraph.getState(invokeConfig);
//        System.out.println(aaa);
//        // 使用 updateState 更新状态，传入中断时的节点 ID
//        var updatedConfig = compiledGraph.updateState(invokeConfig, Map.of("human_feedback", "4"), "IntrerrptableNodeTest");
//        System.out.printf("--State after update--\n%s\n", compiledGraph.getState(updatedConfig));
//        // 创建恢复配置，添加 HUMAN_FEEDBACK_METADATA_KEY
//        RunnableConfig resumeConfig = RunnableConfig.builder(updatedConfig)
//                .addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, "placeholder")
//                .build();
//        // 继续执行 Graph（input 为 null，使用之前的状态）
//        this.compiledGraph.stream(null,resumeConfig)
////        this.compiledGraph.stream(Map.of("human_feedback","4"), resumeConfig)
//                .doOnNext(event1 -> System.out.println("节点输出: " + event1))
//                .doOnError(error -> System.err.println("流错误: " + error.getMessage()))
//                .doOnComplete(() -> System.out.println("流完成"))
//                .blockLast();
////        compiledGraph.updateState(invokeConfig, Map.of("message", "4"), "IntrerrptableNodeTest");
////        StateSnapshot stateSnapshot=this.compiledGraph.getState(invokeConfig);
////   OverAllState state    = stateSnapshot.state();
////    state.withResume();
////
////    Map<String, Object> objectMap = new HashMap<>();
////   // 注入用户反馈
////    objectMap.put("feedback", feedBack);
////    state.
////    state.withHumanFeedback(new OverAllState.HumanFeedback(objectMap,""));
////
////    Sinks.Many<ServerSentEvent<String>> sink     =Sinks
////       .many().unicast().onBackpressureBuffer();
////   GraphProcess graphProcess=new GraphProcess(this.compiledGraph);
////   // 从当前状态恢复执行
////    Flux<NodeOutput> resultFuture = compiledGraph
////                  .fluxStreamFromInitialNode(state, runnableConfig);
//
//    }
}
