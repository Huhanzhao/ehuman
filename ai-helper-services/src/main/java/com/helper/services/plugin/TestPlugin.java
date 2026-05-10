package com.helper.services.plugin;

import cn.hutool.core.net.url.UrlBuilder;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.helper.services.bean.ChatClientFactory;
import com.helper.services.entity.*;
import com.helper.services.enums.NodeEnum;
import com.helper.services.service.VoiceService;
import com.helper.services.tools.CommandTool;
import com.mikuac.shiro.annotation.MessageHandlerFilter;
import com.mikuac.shiro.annotation.PrivateMessageHandler;
import com.mikuac.shiro.common.utils.MsgUtils;
import com.mikuac.shiro.common.utils.OneBotMedia;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.BotPlugin;
import com.mikuac.shiro.dto.event.message.PrivateMessageEvent;
import com.mikuac.shiro.model.ArrayMsg;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.content.Media;
import org.springframework.ai.image.ImageModel;
import org.springframework.ai.image.ImagePrompt;
import org.springframework.ai.image.ImageResponse;
import org.springframework.ai.openai.OpenAiImageModel;
import org.springframework.ai.openai.OpenAiImageOptions;
import org.springframework.ai.openai.api.OpenAiImageApi;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.net.InetSocketAddress;
import java.net.Proxy;


@Component
@Slf4j
public class TestPlugin extends BotPlugin{

    @Autowired
    private ChatClientFactory chatClientFactory;

    @Autowired
    private VoiceService voiceService;
    @PrivateMessageHandler
    @MessageHandlerFilter
    @Override
    public int onPrivateMessage(Bot bot , PrivateMessageEvent event){
//        String userInput = event.getMessage();
//        String build = MsgUtils.builder().face(123).build();
//        String result = voiceService.getVoice(userInput);
//        String voice = MsgUtils.builder().voice(result).build();
//        bot.sendPrivateMsg(event.getUserId(),voice,false);
//        List<Message> messages = new ArrayList<>();
//        messages.add(new UserMessage("你好"));
//        messages.add(new AssistantMessage("你好呀"));
//        messages.add(new UserMessage("你叫什么名字"));
//        messages.add(new AssistantMessage("我叫小助手"));
//        messages.add(new UserMessage("我刚刚问的你什么问题"));
//
//        ChatClient chatClient = chatClientFactory.createChatClient(event.getUserId(), NodeEnum.GENERATE);
//        if (chatClient instanceof GrokChatClient){
//            GrokChatClient grokChatClient = (GrokChatClient) chatClient;
//            grokChatClient.setMessages( messages);
//            grokChatClient.setPrompt(new Prompt("你是一个聊天小助手，请简短回答用户的问题不超过20字符"));
//            String result = grokChatClient.call();
//            bot.sendPrivateMsg(event.getUserId(),result,false);
//        }
//        SimpleClientHttpRequestFactory factory =  new SimpleClientHttpRequestFactory();
//
//        Proxy proxy = new Proxy(
//                Proxy.Type.HTTP,
//                new InetSocketAddress("127.0.0.1", 7890)
//        );
//
//        factory.setProxy(proxy);
//
//
//        OpenAiImageOptions build = OpenAiImageOptions.builder().model("grok-imagine-image-pro").build();
//        RestClient.Builder builder = RestClient.builder()
//                .requestFactory(factory);
//        ImageModel imageModel = new OpenAiImageModel(OpenAiImageApi.builder().restClientBuilder(builder)
//                .apiKey("xai-GMCqe")
//                .baseUrl("https://api.x.ai").build());
//
//
//        ImageResponse resp = imageModel.call(new ImagePrompt("生成一张清纯的女生照片，充满生活气息",build));
        // 图片生成太贵不要了
        return MESSAGE_IGNORE;
    }
}
