package com.bot.service.plugin;

import com.bot.service.bean.ChatClientFactory;
import com.bot.service.service.VoiceService;
import com.mikuac.shiro.annotation.MessageHandlerFilter;
import com.mikuac.shiro.annotation.PrivateMessageHandler;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.BotPlugin;
import com.mikuac.shiro.dto.event.message.PrivateMessageEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


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
