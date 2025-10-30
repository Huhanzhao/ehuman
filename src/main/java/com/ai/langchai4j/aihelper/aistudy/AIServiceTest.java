package com.ai.langchai4j.aihelper.aistudy;

import dev.langchain4j.service.TokenStream;
import reactor.core.publisher.Flux;

//@AiService  自由度低
public interface AIServiceTest {

//    @SystemMessage("我叫{{name}}")
////    @SystemMessage(fromResource = "systemPrompt.txt")  // 从txt中直接加载
//    String chatWithString(@V("name") String name, @UserMessage String message, @MemoryId String memoryId);

    // 流式输出测试
    // TokenStream
    TokenStream chatStream(String message);

    Flux<String> chatStream2(String message);

    String chatWithString(String message);

}
