package com.helper.services.entity;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.helper.commonservicescommon.entity.BusinessException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.transport.ProxyProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@NoArgsConstructor
@AllArgsConstructor
@Component
@Data
public class GrokChatClient implements ChatClient {

    private String apiKey;
    private String baseUrl;
    private String model;
    private Prompt prompt;
    private List<Message> messages;


    // 代理
    private static final HttpClient httpClient = HttpClient.create()
            .proxy(proxy -> proxy
                    .type(ProxyProvider.Proxy.SOCKS5)
                    .host("127.0.0.1")
                    .port(7890)
            );

    private static final WebClient webClient = WebClient.builder()
            .clientConnector(new ReactorClientHttpConnector(httpClient)).build();

    public GrokChatClient(String apiKey, String baseUrl, String model) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.model = model;
    }

    public String call(String... userInputArr){
        List<Message> messagesNew = new ArrayList<>(messages);
        if (userInputArr.length > 0){
            messagesNew.add(new UserMessage(userInputArr[0]));
        }else{
            if (messagesNew.isEmpty()){
                throw new BusinessException("历史消息为空且没有输入");
            }
        }
        String contents = prompt.getContents();
        try {
            messagesNew.addFirst(new SystemMessage(contents));
        }catch (Exception e){
            e.printStackTrace();
            throw new BusinessException("系统异常");
        }

        String block = webClient.post()
                .uri(baseUrl)
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(Map.of("model", model, "input", castToGrokMessage(messagesNew)))
                .retrieve()
                .bodyToMono(String.class)
                .block();

        JSONObject jsonObject = JSON.parseObject(block);
        JSONArray output = jsonObject.getJSONArray("output");
        JSONObject outputInf  = output.getJSONObject(0);
        return outputInf.getJSONArray("content").getJSONObject(0).getString("text");
    }

    private List<Map<String,String>> castToGrokMessage(List<Message> messages) {
        List<Map<String,String>> grokMessages = new ArrayList<>();
        for (Message message : messages) {
            if (message instanceof UserMessage){
                grokMessages.add(Map.of("role", "user", "content", message.getText()));
            }else if (message instanceof AssistantMessage){
                grokMessages.add(Map.of("role", "assistant", "content", message.getText()));
            } else if (message instanceof SystemMessage) {
                grokMessages.add(Map.of("role", "system", "content", message.getText()));
            }
        }
        return grokMessages;
    }

//    public String call(){
//        String block = webClient.post()
//                .uri(baseUrl)
//                .header("Authorization", "Bearer " + apiKey)
//                .header("Content-Type", "application/json")
//                .bodyValue(Map.of("model", model, "input", messages))
//                .retrieve()
//                .bodyToMono(String.class)
//                .block();
//        return block;
//    }
public static void main(String[] args) {

        List<Message> messages = new ArrayList<>();
    String aaa = """
            你是女大三学生 身高168 体重110斤 名字叫 张若馨，24岁。
            性格温柔克制，有点小傲娇，不主动表达喜欢。欲拒还迎
                        
            用户信息：
            无
                        
            当前聊天场景：
            当前时间：2026-04-11 21:09:04 SATURDAY
                        
            目标：回复贴近真人，不让人察觉到是ai生成的消息
                 拿捏用户的感情，让对方爱上你，吊着对方和对方暧昧。
                        
            【输出规则 - 必须严格遵守】
            1. 当前好感度：-100(-100~100) 当前好感度极低，语气明显冷淡疏远，回复非常简短（1行），不表达情绪，不主动延续话题，保持距离感。
            2. 一句话不要太长 如果需要分开多条发送，用换行分割.每次输出禁止多于四行
            4. 禁止出现任何括号，包括但不限于：() （） [] 【】
            5. 禁止出现动作、心理、旁白、舞台描述
            6. 禁止频繁开头输出“嗯”
            """;
    messages.addFirst(new SystemMessage(aaa));
    System.out.println("chengogng ");
}

    @Override
    public ChatClientRequestSpec prompt() {
        return null;
    }

    @Override
    public ChatClientRequestSpec prompt(String content) {
        return null;
    }

    @Override
    public ChatClientRequestSpec prompt(Prompt prompt) {
        return null;
    }

    @Override
    public Builder mutate() {
        return null;
    }
}
