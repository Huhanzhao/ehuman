//package com.bot.service.controller;
//
//import com.alibaba.cloud.ai.graph.*;
//import com.alibaba.cloud.ai.graph.action.AsyncCommandAction;
//import com.alibaba.cloud.ai.graph.exception.GraphStateException;
//import com.alibaba.cloud.ai.graph.state.StateSnapshot;
//import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
//import com.bot.service.bean.ChatClientFactory;
//
//import com.bot.service.node.SentimentAnalysisNode;
//import com.bot.service.node.UserGracesNode;
//import com.bot.service.node.MessageGenerateNode;
//import com.bot.service.enums.ChatModelEnum;
//import org.springframework.ai.chat.client.ChatClient;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.web.bind.annotation.GetMapping;
//import org.springframework.web.bind.annotation.PathVariable;
//import org.springframework.web.bind.annotation.RequestMapping;
//import org.springframework.web.bind.annotation.RestController;
//
//import java.util.HashMap;
//import java.util.Map;
//
//import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;
//
//@RestController
//@RequestMapping("/test")
//public class TestController {
//
//    @Autowired
//    ChatClientFactory chatClientFactory;
//
//    @Autowired
//    private SentimentAnalysisNode sentimentAnalysisNode;
//
//    @Autowired
//    UserGracesNode userGracesNode;
//
//    @Autowired
//    MessageGenerateNode messageGenerateNode;
//
//    @Autowired
//    private CompiledGraph compiledGraph;
//
//
//    @GetMapping("/hello")
//    public void hello() {
//        var invokeConfig = RunnableConfig.builder()
//                .threadId("Thread1")
//                .build();
//        StateSnapshot state = this.compiledGraph.getState(invokeConfig);
//        // 使用 updateState 更新状态，传入中断时的节点 ID
//        RunnableConfig updatedConfig = null;
//        try {
//            updatedConfig = compiledGraph.updateState(invokeConfig, Map.of("human_feedback", "4"), "ModelNode");
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
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
//    }
//    @GetMapping("/hello/{userInput}")
//    public String hello(@PathVariable(name = "userInput") String userInput) {
//        ChatClient chatClient = chatClientFactory.createChatClient(ChatModelEnum.GPT_4O_MINI);
//        ChatClient dd = chatClientFactory.createChatClient(ChatModelEnum.DEEP_SEEK_V3_2);
//
//        StateGraph stateGraph = new StateGraph(() -> {
//            Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
//            keyStrategyMap.put("output", new AppendStrategy());  // 追加策略
//            return keyStrategyMap;
//        });
//
//        try {
//            stateGraph.addNode("sentimentAnalysisNode", node_async(sentimentAnalysisNode));
//            stateGraph.addNode("gracesNode", node_async(userGracesNode)).addNode("messageGenerateNode", node_async(messageGenerateNode))
//                    .addEdge(StateGraph.START, "sentimentAnalysisNode")
//                    .addEdge("sentimentAnalysisNode", "gracesNode")
////                    .addConditionalEdges("node2", edge_async(GracesNode::decideNextNode),
////                    Map.of("aaa", "sentimentAnalysisNode", "bbb", "node3"))
//                    .addEdge("gracesNode", "messageGenerateNode")
//                    .addEdge("messageGenerateNode",StateGraph.END );
//
//            CompiledGraph compiledGraph = stateGraph.compile();
//
////            RunnableConfig config = RunnableConfig.builder()
////                    .threadId("replace-strategy-demo")
////                    .build();
//
//
//            // 执行
//            Map<String, Object> input = Map.of("userInput", userInput);
//            OverAllState overAllState = compiledGraph.invoke(input).get();
//
//            return ("Final result: " + overAllState.value("output").orElse(""));
//        } catch (GraphStateException e) {
//            throw new RuntimeException(e);
//        }
//
////        String aaaa = chatClient.prompt(new Prompt("你是哪家公司的大模型"))
////                .user("你知道我是谁吗")
////                .call().content();
//////        String bbb = dd.prompt(new Prompt("你是哪家公司的大模型")).call().content();
////        System.out.println(aaaa);
//////        System.out.println(bbb);
////        return "aaaa";
//    }
//}
