package com.helper.services.controller;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.helper.services.config.ChatClientFactory;
import com.helper.services.node.SentimentAnalysisNode;
import com.helper.services.node.UserGracesNode;
import com.helper.services.node.MessageGenerateNode;
import com.helper.services.enums.ChatModelEnum;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    ChatClientFactory chatClientFactory;

    @Autowired
    SentimentAnalysisNode sentimentAnalysisNode;

    @Autowired
    UserGracesNode userGracesNode;

    @Autowired
    MessageGenerateNode messageGenerateNode;



    @GetMapping("/hello/{userInput}")
    public String hello(@PathVariable(name = "userInput") String userInput) {
        ChatClient chatClient = chatClientFactory.createChatClient(ChatModelEnum.GPT_4O_MINI);
        ChatClient dd = chatClientFactory.createChatClient(ChatModelEnum.DEEP_SEEK_V3_2);

        StateGraph stateGraph = new StateGraph(() -> {
            Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
            keyStrategyMap.put("output", new AppendStrategy());  // 追加策略
            return keyStrategyMap;
        });

        try {
            stateGraph.addNode("sentimentAnalysisNode", sentimentAnalysisNode);
            stateGraph.addNode("gracesNode", userGracesNode).addNode("messageGenerateNode", messageGenerateNode)
                    .addEdge(StateGraph.START, "sentimentAnalysisNode")
                    .addEdge("sentimentAnalysisNode", "gracesNode")
//                    .addConditionalEdges("node2", edge_async(GracesNode::decideNextNode),
//                    Map.of("aaa", "sentimentAnalysisNode", "bbb", "node3"))
                    .addEdge("gracesNode", "messageGenerateNode")
                    .addEdge("messageGenerateNode",StateGraph.END );

            CompiledGraph compiledGraph = stateGraph.compile();

//            RunnableConfig config = RunnableConfig.builder()
//                    .threadId("replace-strategy-demo")
//                    .build();


            // 执行
            Map<String, Object> input = Map.of("userInput", userInput);
            OverAllState overAllState = compiledGraph.invoke(input).get();

            return ("Final result: " + overAllState.value("output").orElse(""));
        } catch (GraphStateException e) {
            throw new RuntimeException(e);
        }

//        String aaaa = chatClient.prompt(new Prompt("你是哪家公司的大模型"))
//                .user("你知道我是谁吗")
//                .call().content();
////        String bbb = dd.prompt(new Prompt("你是哪家公司的大模型")).call().content();
//        System.out.println(aaaa);
////        System.out.println(bbb);
//        return "aaaa";
    }
}
