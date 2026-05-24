package com.bot.service.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.bot.service.bean.ChatClientFactory;
import com.bot.service.enums.NodeEnum;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class ImageAnalysisNode implements NodeAction {

    @Autowired
    private ChatClientFactory chatClientFactory;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String userInput = (String) state.value("userInput").orElse("");
        Long userId = (Long) state.value("userId").orElse(0L);

        if (userInput.startsWith("[CQ:image")){
            Pattern pattern = Pattern.compile("url=([^,]+)");
            Matcher matcher = pattern.matcher(userInput.replaceAll("&amp;","&"));
            matcher.find();

            ChatClient chatClient = chatClientFactory.createChatClient(userId, NodeEnum.IMAGE_ANALYSIS);

            String path = matcher.group(1);
            Message message = null;
            try {
                message = UserMessage.builder()
                        .text("请描述图片内容") // content
                        .media(new Media(MimeTypeUtils.IMAGE_PNG, new URI(path))) // media
                        .build();
            } catch (URISyntaxException e) {
                throw new RuntimeException(e);
            }
            String content = chatClient.prompt().messages(message).call().content();
            userInput = "<image: 这是一张图片，图片描述：" + content + ">";
            log.info("图片描述：" + content);
        }
        return Map.of("userInput",userInput);
    }
}
