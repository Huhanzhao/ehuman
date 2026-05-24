package com.helper.services.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.helper.services.entity.TbAiChatMemoryDo;
import com.helper.services.enums.QQbotIdEnum;
import com.helper.services.service.TbAiChatMemoryService;
import com.helper.services.utils.VoiceSendUtil;
import com.mikuac.shiro.common.utils.MsgUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.BotContainer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class SendMessageNode implements NodeAction {

    private final BotContainer botContainer;

    private final TbAiChatMemoryService tbAiChatMemoryService;

    private final VoiceSendUtil voiceSendUtil;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String aiMessage = (String) state.value("finalOutput", String.class).orElse("");
        Long userId = (Long)state.value("userId").orElse(0L);

        if (aiMessage.isEmpty()) {
            log.error("no message");
        }

        log.info("aiMessage: {}", aiMessage);
        String[] splitMessages = aiMessage.split("\n");
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
        return Map.of();
    }
}
