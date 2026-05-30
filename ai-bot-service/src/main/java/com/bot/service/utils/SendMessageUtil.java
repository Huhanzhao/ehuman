package com.bot.service.utils;

import com.bot.service.entity.TbAiChatMemoryDo;
import com.bot.service.service.TbAiChatMemoryService;
import com.mikuac.shiro.common.utils.MsgUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.BotContainer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SendMessageUtil {

    private final BotContainer botContainer;

    private final VoiceSendUtil voiceSendUtil;

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
                voiceSendUtil.sendVoice(splitMessage,userId);
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
}
