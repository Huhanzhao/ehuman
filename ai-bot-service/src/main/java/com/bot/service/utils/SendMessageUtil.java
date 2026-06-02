package com.bot.service.utils;

import com.bot.plugin.PluginManager;
import com.bot.service.common.entity.BusinessException;
import com.bot.service.entity.TbAiChatMemoryDo;
import com.bot.service.entity.TbUserModelConfigDo;
import com.bot.service.entity.TbUserReferAudioDo;
import com.bot.service.enums.CustomActionPath;
import com.bot.service.enums.NodeEnum;
import com.bot.service.service.TbAiChatMemoryService;
import com.bot.service.service.TbUserModelConfigDoService;
import com.bot.service.service.TbUserReferAudioDoService;
import com.mikuac.shiro.common.utils.MsgUtils;
import com.mikuac.shiro.common.utils.OneBotMedia;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.BotContainer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class SendMessageUtil {

    private final BotContainer botContainer;

    private final PluginManager pluginManager;

    private final TbAiChatMemoryService tbAiChatMemoryService;

    private final TbUserModelConfigDoService tbUserModelConfigDoService;

    private final TbUserReferAudioDoService tbUserReferAudioDoService;

    private final String referAudioPath = "../../../qq-bot-plugin/plugin/TTS/audio/";

    private final String outputPath = "/app/audio/output/";

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
            if (splitMessage.startsWith("<voice") && splitMessage.endsWith("</voice>")){
                sendVoiceMsg(userId,splitMessage);
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
            }else if (splitMessage.startsWith("<text>") && splitMessage.endsWith("</text>")){
                bot.sendPrivateMsg(userId,msg.replaceAll("<text>","").replaceAll("</text>",""),false);
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

        String splitMessage = message;
        // 匹配 <voice tone="xxx">content</voice>
        Pattern pattern = Pattern.compile("<voice\\s+tone=\"([^\"]+)\">(.*?)</voice>");
        Matcher matcher = pattern.matcher(message);
        if (matcher.find()) {
            splitMessage = matcher.group(2);
            if (ttsModel.getModel().equals("MimoTTS")) {
                splitMessage = "(" + matcher.group(1) + ")" +matcher.group(2);
            }
        }

        Map<String,String> resultMap = null;
        try {
            resultMap = pluginManager.textToSpeech(ttsModel.getModel(),splitMessage,enableAudioList.getFirst().getReferAudioUrl(),ttsModel.getApiKey(),referAudioPath + enableAudioList.getFirst().getReferAudioName());
        } catch (Exception e) {
            throw new BusinessException("语音合成失败" + e.getMessage());
        }

        String base64 = resultMap.get("base64");
        String url = resultMap.get("url");
        String build = null;
        if (url != null && !url.isEmpty()){
            build = MsgUtils.builder().voice(url).build();
        }else if(base64 != null && !base64.isEmpty()){
            // 加载本地路径
            build = MsgUtils.builder()
                .voice(OneBotMedia.builder()
                .file("base64://" + base64)).build();
        }else{
            throw new BusinessException("插件输出错误");
        }

        // 发送消息 保存历史记录
        botContainer.robots.get(Long.valueOf(System.getProperty("BOT_ID"))).sendPrivateMsg(userId,build,false);
        tbAiChatMemoryService.save(new TbAiChatMemoryDo().setMemoryId(userId).setRole("assistant").setMessage(message));
    }

    public void forwardMsg(Long userId, String messageId){
        // 通过机器人账号取出 Bot 对象
        Long botId = Long.valueOf(System.getProperty("BOT_ID"));
        Bot bot = botContainer.robots.get(botId);
        bot.customRequest(CustomActionPath.FORWARD_MSG, Map.of("message_id",messageId,"user_id",userId));
    }
}
