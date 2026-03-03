package com.helper.services.plugin;

import cn.hutool.core.net.url.UrlBuilder;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.helper.services.bean.ChatClientFactory;
import com.helper.services.entity.TbAiChatMemoryDo;
import com.helper.services.entity.TbChatConfigDo;
import com.helper.services.entity.TbUserGraceDo;
import com.helper.services.entity.TbUserModelConfigDo;
import com.helper.services.enums.NodeEnum;
import com.mikuac.shiro.annotation.MessageHandlerFilter;
import com.mikuac.shiro.annotation.PrivateMessageHandler;
import com.mikuac.shiro.common.utils.MsgUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.BotPlugin;
import com.mikuac.shiro.dto.event.message.PrivateMessageEvent;
import com.mikuac.shiro.model.ArrayMsg;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


@Component
@Slf4j
public class TestPlugin extends BotPlugin{

    @Autowired
    private ChatClientFactory chatClientFactory;

    @PrivateMessageHandler
    @MessageHandlerFilter
    @Override
    public int onPrivateMessage(Bot bot , PrivateMessageEvent event){
        String userInput = event.getMessage();
        String build = MsgUtils.builder().face(123).build();
//        Pattern pattern = Pattern.compile("url=([^,]+)");
//        Matcher matcher = pattern.matcher(userInput.replaceAll("&amp;","&"));
//        matcher.find();
//
//        ChatClient chatClient = chatClientFactory.createChatClient(event.getUserId(), NodeEnum.IMAGE_ANALYSIS);
//
//        String path = matcher.group(1);
//        Message userMessage = null;
//        try {
//            userMessage = UserMessage.builder()
//                    .text("请描述图片内容") // content
//                    .media(new Media(MimeTypeUtils.IMAGE_PNG, new URI( path))) // media
//                    .build();
//        } catch (URISyntaxException e) {
//            throw new RuntimeException(e);
//        }
//        String content = chatClient.prompt().messages(userMessage).call().content();
//
//        bot.sendPrivateMsg(event.getUserId(), content,false);
//
//            bot.sendPrivateMsg(event.getUserId(), event.getMessage(),false);

        return MESSAGE_IGNORE;
    }
}
