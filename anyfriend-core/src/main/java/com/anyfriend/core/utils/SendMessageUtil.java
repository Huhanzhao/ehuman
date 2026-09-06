package com.anyfriend.core.utils;

import com.anyfriend.dao.entity.TbAiChatMemoryDo;
import com.anyfriend.dao.entity.TbUserModelConfigDo;
import com.anyfriend.dao.entity.TbUserReferAudioDo;
import com.anyfriend.dao.enums.LlmTypeEnum;
import com.anyfriend.dao.enums.MessageTypeEnum;
import com.anyfriend.dao.service.TbAiChatMemoryService;
import com.anyfriend.dao.service.TbUserModelConfigDoService;
import com.anyfriend.dao.service.TbUserReferAudioDoService;
import com.anyfriend.plugin.SystemPath;
import com.anyfriend.plugin.VoicePluginManager;
import com.mikuac.shiro.common.utils.MsgUtils;
import com.mikuac.shiro.common.utils.OneBotMedia;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.BotContainer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class SendMessageUtil {

    private final BotContainer botContainer;

    private final VoicePluginManager voicePluginManager;

    private final TbAiChatMemoryService tbAiChatMemoryService;

    private final TbUserModelConfigDoService tbUserModelConfigDoService;

    private final TbUserReferAudioDoService tbUserReferAudioDoService;

    private final SystemPath systemPath;

    @Value("${proxy.host:127.0.0.1}")
    private String proxyHost;

    // 发送文字消息
    public void sendChatMsg(Long userId, String message) {
        Long botId = Long.valueOf(System.getProperty("BOT_ID"));
        if (message.isEmpty()) {
            log.error("no message");
        }

        log.info("aiMessage: {}", message);
        String[] splitMessages = message.split("\n");
        StringBuilder resultMessage = new StringBuilder();
        for (String splitMessage : splitMessages) {
            if (splitMessage.isEmpty()) continue;
            splitMessage  = splitMessage.replaceAll("。", "").trim();
            if (splitMessage.startsWith("<voice") && splitMessage.endsWith("</voice>")){
                sendVoiceMsg(userId,splitMessage);
                resultMessage.append(splitMessage).append("\n");
                continue;
            }
            if (splitMessage.startsWith("<emoji")){
                String stickerMsg = sendStickerMsg(userId,splitMessage);
                if (stickerMsg != null) {
                    resultMessage.append(stickerMsg).append("\n");
                }else{
                    resultMessage.append(splitMessage).append("\n");
                }
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
            resultMessage.append(splitMessage).append("\n");
        }
        tbAiChatMemoryService.save(new TbAiChatMemoryDo().setMemoryId(userId).setRole(MessageTypeEnum.AI_MESSAGE)
                .setMessage(resultMessage.toString().trim()));
    }


    // 发送表情包消息
    public String sendStickerMsg(Long userId, String message) {

        Pattern pattern = Pattern.compile("emo=\"([^\"]+)\"");
        Matcher matcher = pattern.matcher(message);
        String emojiPath = null;
        String stickerType = null;
        if (matcher.find()) {
            stickerType = matcher.group(1);
            // 拼接表情包路径
            emojiPath = systemPath.getEmojiBasePath() + stickerType + "/";
        }

        // 判断emojiPath是否存在
        if (emojiPath != null && Files.exists(Paths.get(emojiPath))){
            // 存在随机获取一个表情文件
            try {
                List<Path> emojiList = Files.list(Paths.get(emojiPath)).toList();
                if (emojiList.isEmpty()) return null;

                // 创建 Random 对象
                Random random = new Random();
                // 生成随机索引
                int randomIndex = random.nextInt(emojiList.size());
                Path path = emojiList.get(randomIndex);
                Path fileName = path.getFileName();
                byte[] bytes = Files.readAllBytes(path);

                // 发送消息 保存历史记录
                String msg = MsgUtils.builder().img(bytes).build();
                botContainer.robots.get(Long.valueOf(System.getProperty("BOT_ID")))
                        .sendPrivateMsg(userId, msg,false);


                String saveMessage = "<emoji emo=\"" +
                        stickerType +
                        "\">" +
                        fileName +
                        "</emoji>";
                return saveMessage;
            } catch (IOException e) {
                log.info("获取文件异常，跳过表情消息");
            }
        }else{
            log.info("没找表情包，跳过消息");
        }
        return null;
    }

    // 给自己发送给消息
    public void sendMegToSelf(String message) {
        Long botId = Long.valueOf(System.getProperty("BOT_ID"));
        if (message.isEmpty()) {
            log.error("no message");
            return;
        }

        // 通过机器人账号取出 Bot 对象
        Bot bot = botContainer.robots.get(botId);
        bot.sendPrivateMsg(botId,message,false);
    }

    // 发送语音消息
    public void sendVoiceMsg(Long userId, String message){
        // 查询当前启用的插件名字和参考声音url
        TbUserModelConfigDo ttsModel = tbUserModelConfigDoService.getByUserIdAndNodeNum(userId, LlmTypeEnum.TTS);
        List<TbUserReferAudioDo> enableAudioList = tbUserReferAudioDoService.getReferAudioByUserId(userId,true);
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
            resultMap = voicePluginManager.textToSpeech(ttsModel.getModel(),
                    splitMessage,
                    enableAudioList.getFirst().getReferAudioUrl(),
                    ttsModel.getApiKey(),
                    systemPath.getClassesUseAudioPath() + enableAudioList.getFirst().getReferAudioName(),proxyHost);
        } catch (Exception e) {
            log.error("语音合成失败{}",e.getMessage());
            sendMegToSelf("语音合成失败" + e.getMessage());
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
            log.error("插件输出错误");
            sendMegToSelf("插件输出错误");
        }

        // 发送消息
        botContainer.robots.get(Long.valueOf(System.getProperty("BOT_ID"))).sendPrivateMsg(userId,build,false);
    }

    // 发送系统消息
    public void sendSystemMsg(Long userId,String message) {
        Long botId = Long.valueOf(System.getProperty("BOT_ID"));
        botContainer.robots.get(botId).sendPrivateMsg(userId,message,false);
    }
}
