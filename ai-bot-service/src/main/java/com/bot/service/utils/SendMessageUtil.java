package com.bot.service.utils;

import com.bot.service.entity.TbAiChatMemoryDo;
import com.bot.service.service.TbAiChatMemoryService;
import com.bot.service.tts.PluginManager;
import com.mikuac.shiro.common.utils.MsgUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.BotContainer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SendMessageUtil {

    private final BotContainer botContainer;

    private final PluginManager pluginManager;

    private final TbAiChatMemoryService tbAiChatMemoryService;

    // 发送文字消息
    public void sendChatMsg(Long userId, String message) {
        Long botId = Long.valueOf(System.getProperty("BOT_ID"));
        if (message.isEmpty()) {
            log.error("no message");
        }

        log.info("aiMessage: {}", message);
        String[] splitMessages = message.split("\n");
        for (String splitMessage : splitMessages) {
            if (splitMessage.isEmpty()) continue;
            splitMessage  = splitMessage.replaceAll("。", "").trim();
            if (splitMessage.startsWith("<voice>") && splitMessage.endsWith("</voice>")){
                sendVoiceMsg(userId,splitMessage.replaceAll("<voice>","").replaceAll("</voice>",""));
                continue;
            }
            try {
                Thread.sleep(1000 + (int) (Math.random() * 1000));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            String msg = MsgUtils.builder().text(splitMessage).build();
            // 通过机器人账号取出 Bot 对象
            Bot bot = botContainer.robots.get(botId);

            // 保持沉默的时候给自己发送
            if (splitMessage.startsWith("<silence>") && splitMessage.endsWith("</silence>")){
                sendMegToSelf(msg);
            }else{
                bot.sendPrivateMsg(userId,msg,false);
            }
            tbAiChatMemoryService.save(new TbAiChatMemoryDo().setMemoryId(userId).setRole("assistant").setMessage(splitMessage));
        }
    }

    public void sendMegToSelf(String message) {
        Long botId = Long.valueOf(System.getProperty("BOT_ID"));
        if (message.isEmpty()) {
            log.error("no message");
        }

        // 通过机器人账号取出 Bot 对象
        Bot bot = botContainer.robots.get(botId);
        bot.sendPrivateMsg(botId,message,false);
    }

    public void sendVoiceMsg(Long userId, String message){
        // todo 查询当前启用的插件和声音url 如果插件返回的是本地目录，直接发送本地文件
        String splitMessage = "";
        splitMessage = message.replaceAll("<voice>","").replaceAll("</voice>","");
        Map<String,String> resultMap = null;
        try {
//            resultMap = pluginManager.textToSpeech("IndexTTS2",message,"https://qq-bot-hhz.oss-cn-beijing.aliyuncs.com/%E5%A4%B9%E5%AD%90%E9%9F%B3%E9%9F%B3%E9%87%8F%E5%A2%9E%E5%BC%BA%E7%89%88.m4a","sk-GXqCEbErJlwrUYDwhMzrKhv4AxnIvpvSvEIkb1XBdkx3Ct2m");
            resultMap = pluginManager.textToSpeech("VoxCpm2",message,"../../../plugin/TTS/voice/茉莉导航.m4a","sk-GXqCEbErJlwrUYDwhMzrKhv4AxnIvpvSvEIkb1XBdkx3Ct2m");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        String type = resultMap.get("type");
        String result = resultMap.get("url");
        String build = null;
        if (type.equals("remote")){
            build = MsgUtils.builder().voice(result).build();
            // todo 加载本地路径

        }else{
            //        String  build = MsgUtils.builder()
//                .voice(OneBotMedia.builder()
//                        .file("file:///app/aaa.png")).build();
        }
        botContainer.robots.get(Long.valueOf(System.getProperty("BOT_ID"))).sendPrivateMsg(userId,build,false);
        tbAiChatMemoryService.save(new TbAiChatMemoryDo().setMemoryId(userId).setRole("assistant").setMessage(message));
    }
}
