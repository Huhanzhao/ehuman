package com.bot.service.plugin;

import com.alibaba.cloud.ai.graph.*;
import com.bot.service.common.config.LocalCache;
import com.bot.service.entity.TbAiChatMemoryDo;
import com.bot.service.service.TbAiChatMemoryService;
import com.mikuac.shiro.annotation.MessageHandlerFilter;
import com.mikuac.shiro.annotation.PrivateMessageHandler;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.BotPlugin;
import com.mikuac.shiro.dto.event.message.PrivateMessageEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class QQBotPlugin extends BotPlugin {

    private final CompiledGraph qqBotMainGraph;

    @Autowired
    private TbAiChatMemoryService tbAiChatMemoryService;
    private final LocalCache localCache = LocalCache.getLocalCache();

    public QQBotPlugin(@Qualifier("qqBotMainGraph")CompiledGraph qqBotMainGraph){
        this.qqBotMainGraph = qqBotMainGraph;
    }

    @PrivateMessageHandler
    @MessageHandlerFilter // 不是井号开头
    @Override
    public int onPrivateMessage(Bot bot , PrivateMessageEvent event){
        String chatId = UUID.randomUUID().toString();
        String userInput = event.getMessage();
        Long userId = event.getUserId();

        try {
            for (int i = 0; i < 20; i++){
                if (localCache.containsKey(userId + "_image")){
                    // 正在解析图片但是收到了新消息 等待图片解析一分钟
                    log.info("正在解析图片等待3秒");
                    Thread.sleep(3000);
                }
            }
        }catch (InterruptedException e) {
            throw new RuntimeException(e);
        }finally {
            // 一分钟还没有解析完毕取消
            LocalCache.getLocalCache().remove(userId + "_image");
        }

        // 保存用户消息
        if (!userInput.startsWith("[CQ:image")){
            // 不是图片直接保存 是图片在图片节点解析之后保存
            tbAiChatMemoryService.save(new TbAiChatMemoryDo().setMemoryId(userId).setRole("user").setMessage(event.getMessage()));
        }else {
            LocalCache.getLocalCache().put(userId + "_image", true); // 标记正在解析图片 防止没有解析完毕被下次消息打断
        }

        if (localCache.containsKey(userId + "_Graph")){
            Disposable disposable = (Disposable) localCache.get(userId + "_Graph");
            disposable.dispose();
            log.info("已取消上次图执行");
            localCache.remove(userId + "_Graph");
        }

        // 执行
        Map<String, Object> input = Map.of(
                "userInput", userInput,
                "userId", event.getUserId()
        );
        // 配置线程 ID
        var invokeConfig = RunnableConfig.builder()
                .threadId(chatId)
                .build();
        // 执行图并获取流
        Flux<NodeOutput> stream = qqBotMainGraph.stream(input, invokeConfig)
                .subscribeOn(Schedulers.boundedElastic()); // 异步 执行

        // 异步处理每个输出
        var disposable = stream.subscribe(
                output -> {
                    log.info("当前迭代节点: " + output);
                },
                error -> log.error("流错误: " + error.getMessage()),
                () -> {
                    localCache.remove(userId + "_Graph");
                    log.info("流完成");
                }
        );

        log.info("标记会话");
        localCache.put(userId + "_Graph", disposable);

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
//        var updatedConfig = compiledGraph.updateState(invokeConfig, Map.of("human_feedback", "4"), "ModelNode");
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
////        compiledGraph.updateState(invokeConfig, Map.of("message", "4"), "ModelNode");
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
