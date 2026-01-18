package com.helper.services.controller;

import com.alibaba.cloud.ai.graph.*;
import com.alibaba.cloud.ai.graph.exception.GraphStateException;
import com.alibaba.cloud.ai.graph.state.strategy.AppendStrategy;
import com.helper.services.config.ChatClientFactory;
import com.helper.services.entity.ChatNode;
import com.helper.services.entity.ChatNode2;
import com.helper.services.entity.ChatNode3;
import com.helper.services.enums.ChatModelEnum;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static com.alibaba.cloud.ai.graph.action.AsyncEdgeAction.edge_async;

@RestController
@RequestMapping("/test")
public class TestController {

    @Autowired
    ChatClientFactory chatClientFactory;

    @Autowired
    ChatNode chatNode;

    @Autowired
    ChatNode2 chatNode2;

    @Autowired
    ChatNode3 chatNode3;

    @GetMapping("/hello")
    public String hello() {
        ChatClient chatClient = chatClientFactory.createChatClient(ChatModelEnum.GPT_4O_MINI);
        ChatClient dd = chatClientFactory.createChatClient(ChatModelEnum.DEEP_SEEK_V3_2);

        StateGraph stateGraph = new StateGraph(() -> {
            Map<String, KeyStrategy> keyStrategyMap = new HashMap<>();
            keyStrategyMap.put("output", new AppendStrategy());
            return keyStrategyMap;
        });

        try {
            stateGraph.addNode("node1",chatNode);
            stateGraph.addNode("node2",chatNode2).addNode("node3",chatNode3)
                    .addEdge(StateGraph.START, "node1")
                    .addEdge("node1", "node2")
                    .addConditionalEdges("node2", edge_async(ChatNode2::decideNextNode),
                    Map.of("node1", "node1", "node3", "node3"))
//                    .addEdge("node1", "node3")
//                    .addConditionalEdges("node2", "",)
                    .addEdge("node3",StateGraph.END );

            CompiledGraph compiledGraph = stateGraph.compile();

            RunnableConfig config = RunnableConfig.builder()
                    .threadId("replace-strategy-demo")
                    .build();


            // 执行
            Map<String, Object> input = Map.of("input", 0);
            OverAllState overAllState = compiledGraph.invoke(input).get();

            System.out.println("Final result: " + overAllState.value("output").orElse(""));
        } catch (GraphStateException e) {
            throw new RuntimeException(e);
        }

//        String aaaa = chatClient.prompt(new Prompt("你是哪家公司的大模型"))
//                .user("你知道我是谁吗")
//                .call().content();
////        String bbb = dd.prompt(new Prompt("你是哪家公司的大模型")).call().content();
//        System.out.println(aaaa);
////        System.out.println(bbb);
        return "aaaa";
    }
}
