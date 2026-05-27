package com.bot.service.utils;

import com.bot.service.entity.TbAiChatMemoryDo;
import com.bot.service.enums.QQbotIdEnum;
import com.bot.service.service.TbAiChatMemoryService;
import com.mikuac.shiro.common.utils.MsgUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.BotContainer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SendMessageUtil {

    private final BotContainer botContainer;

    private final VoiceSendUtil voiceSendUtil;

    private final TbAiChatMemoryService tbAiChatMemoryService;

    // 发送文字消息
    public void sendMsg(Long userId, String message) {
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
            Bot bot = botContainer.robots.get(QQbotIdEnum.DEFAULT.getId());

            // 保持沉默的时候给自己发送
            if (splitMessage.startsWith("<silence>") && splitMessage.endsWith("</silence>")){
                bot.sendPrivateMsg(QQbotIdEnum.DEFAULT.getId(),msg,false);
            }else{
                bot.sendPrivateMsg(userId,msg,false);
            }
            tbAiChatMemoryService.save(new TbAiChatMemoryDo().setMemoryId(userId).setRole("assistant").setMessage(splitMessage));
        }
    }
}
