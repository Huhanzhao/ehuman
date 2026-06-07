package com.bot.service.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.bot.service.bean.ChatClientFactory;
import com.bot.service.common.config.LocalCache;
import com.bot.service.entity.TbAiChatMemoryDo;
import com.bot.service.entity.TbUserStickerMsgDo;
import com.bot.service.enums.CustomActionPath;
import com.bot.service.enums.NodeEnum;
import com.bot.service.enums.PromptTypeEnum;
import com.bot.service.service.TbAiChatMemoryService;
import com.bot.service.service.TbBotPromptDoService;
import com.bot.service.service.TbUserStickerMsgDoService;
import com.mikuac.shiro.core.BotContainer;
import com.mikuac.shiro.dto.action.common.ActionData;
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
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class ImageAnalysisNode implements NodeAction {

    @Autowired
    private ChatClientFactory chatClientFactory;

    @Autowired
    private TbAiChatMemoryService tbAiChatMemoryService;

    @Autowired
    private TbUserStickerMsgDoService tbUserStickerMsgDoService;

    @Autowired
    private BotContainer botContainer;

    @Autowired
    private TbBotPromptDoService tbBotPromptDoService;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String userInput = (String) state.value("userInput").orElse("");
        Long userId = (Long) state.value("userId").orElse(0L);

        if (userInput.startsWith("[CQ:image")){
            try{
                Pattern pattern = Pattern.compile("url=([^,]+)");
                Matcher matcher = pattern.matcher(userInput.replaceAll("&amp;","&"));
                matcher.find();

                ChatClient chatClient = chatClientFactory.createChatClient(userId, NodeEnum.IMAGE_ANALYSIS);

                String path = matcher.group(1);
                Message message = null;
                try {
                    message = UserMessage.builder()
                            .text("请根据要求输出") // content
                            .media(new Media(MimeTypeUtils.IMAGE_PNG, new URI(path))) // media
                            .build();
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                }
                String systemPrompt = """
                    # 你是一个图片分析专家，请分析图片是否为表情包并描述图片 并且按照要求格式输出
                    本次消息的String格式：""" + userInput + """
                    
                    ## 要求：
                       1. 简短描述图片内容，内容用<image></image> 或者<emoji></emoji>标签包裹
                       2. 判断图片是否为表情包，如果是表情包就给<emoji>标签加上属性 emo:xxx, 是普通图片就用image标签
                       
                       例如：
                         <emoji emo="吃瓜">一个熊猫穿着黄色衣服拿着西瓜</emoji>
                         <image>在高铁站的照片，有很多人</image>
                    """;
                String content = chatClient.prompt(systemPrompt).messages(message).call().content();
                log.info("图片描述：" + content);

                Pattern resultPattern = Pattern.compile(
                        "<emoji\\s+emo=\"([^\"]+)\">(.*?)</emoji>"
                );

                Matcher resultMatcher = resultPattern.matcher(content);

                // 如果是表情包 就查询消息id存到数据库
                if (resultMatcher.find()){
                    // 获取最近一条图片消息msgId
                    String msgId = getImageMsgId(userId);
                    // 保存
                    tbUserStickerMsgDoService.save(new TbUserStickerMsgDo()
                            .setStickerMsgId(msgId)
                            .setEmo(resultMatcher.group(1))
                            .setDesc(resultMatcher.group(2))
                            .setUserId(userId)
                    );
                }

                // 保存图片描述作为用户输入
                tbAiChatMemoryService.save(new TbAiChatMemoryDo().setMemoryId(userId).setRole("user").setMessage(content));
            }finally {
                LocalCache.getLocalCache().remove(userId + "_image");
            }
        }
        String target = tbBotPromptDoService.getContext(PromptTypeEnum.TARGET);
        return Map.of("userInput",userInput,"target",target);
    }

    private String getImageMsgId(Long userId) {
        ActionData actionData = botContainer.robots.get(Long.valueOf(System.getProperty("BOT_ID")))
                .customRequest(CustomActionPath.GET_FRIEND_MSG_HISTORY,
                        Map.of("user_id", userId.toString(),
                                "message_seq", 0,
                                "count", 5,
                                "reverse_order", true)
                );
        List<Object> imageList = JSON.parseObject(actionData.getData().toString())
//                .getJSONObject("data")
                .getJSONArray("messages").stream().filter(item -> {
                    JSONArray messageArray = JSON.parseObject(item.toString()).getJSONArray("message");
                    String type = messageArray.getJSONObject(0).getString("type");
                    return "image".equals(type);
                }).toList();
        if (imageList.isEmpty()){
            return null;
        }
        return JSON.parseObject(imageList.getFirst().toString()).getLong("message_id").toString();
    }
}
