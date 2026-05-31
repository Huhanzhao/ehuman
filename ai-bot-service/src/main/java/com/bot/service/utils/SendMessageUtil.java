package com.bot.service.utils;

import ch.qos.logback.core.util.StringUtil;
import com.bot.service.common.entity.BusinessException;
import com.bot.service.entity.TbAiChatMemoryDo;
import com.bot.service.entity.TbUserModelConfigDo;
import com.bot.service.entity.TbUserReferAudioDo;
import com.bot.service.enums.NodeEnum;
import com.bot.service.service.TbAiChatMemoryService;
import com.bot.service.service.TbUserModelConfigDoService;
import com.bot.service.service.TbUserReferAudioDoService;
import com.bot.service.tts.PluginManager;
import com.mikuac.shiro.common.utils.MsgUtils;
import com.mikuac.shiro.common.utils.OneBotMedia;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.BotContainer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SendMessageUtil {

    private final BotContainer botContainer;

    private final PluginManager pluginManager;

    private final TbAiChatMemoryService tbAiChatMemoryService;

    private final TbUserModelConfigDoService tbUserModelConfigDoService;

    private final TbUserReferAudioDoService tbUserReferAudioDoService;

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
        // 查询当前启用的插件名字和参考声音url
        TbUserModelConfigDo ttsModel = tbUserModelConfigDoService.getByUserIdAndNodeNum(userId, NodeEnum.TTS);
        List<TbUserReferAudioDo> enableAudioList = tbUserReferAudioDoService.getEnableReferAudioByUserId(userId);
        if (enableAudioList.isEmpty()) {
            log.error("no enable audio");
            return;
        }

        String splitMessage = "";
        splitMessage = message.replaceAll("<voice>","").replaceAll("</voice>","");
        Map<String,String> resultMap = null;
        try {
            String path = StringUtil.isNullOrEmpty(enableAudioList.getFirst().getReferAudioUrl())?
                    "../../../plugin/TTS/voice/" + enableAudioList.getFirst().getReferAudioName() :
                    enableAudioList.getFirst().getReferAudioUrl();
            resultMap = pluginManager.textToSpeech(ttsModel.getModel(),splitMessage,path,ttsModel.getApiKey());
        } catch (Exception e) {
            throw new BusinessException("语音合成失败");
        }

        String path = resultMap.get("path");
        String url = resultMap.get("url");
        String build = null;
        if (url != null && !url.isEmpty()){
            build = MsgUtils.builder().voice(url).build();
        }else if(path != null && !path.isEmpty()){
            // 加载本地路径
            build = MsgUtils.builder()
                .voice(OneBotMedia.builder()
                .file("file:///app/voice/output/" + path)).build();
        }else{
            throw new BusinessException("插件输出错误");
        }

        // 发送消息 保存历史记录
        botContainer.robots.get(Long.valueOf(System.getProperty("BOT_ID"))).sendPrivateMsg(userId,build,false);
        tbAiChatMemoryService.save(new TbAiChatMemoryDo().setMemoryId(userId).setRole("assistant").setMessage(message));
    }
}
