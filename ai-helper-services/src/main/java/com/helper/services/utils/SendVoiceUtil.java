package com.helper.services.utils;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson2.JSON;
import com.helper.services.entity.TbAiChatMemoryDo;
import com.helper.services.entity.TbUserVoiceModelDo;
import com.helper.services.enums.QQbotIdEnum;
import com.helper.services.service.TbAiChatMemoryService;
import com.helper.services.service.TbUserVoiceModelDoService;
import com.mikuac.shiro.common.utils.MsgUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.BotContainer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class SendVoiceUtil{

    private final BotContainer botContainer;

    private final TbAiChatMemoryService tbAiChatMemoryService;

    private final TbUserVoiceModelDoService tbUserVoiceModelDoService;

    private final String API_KEY = "Bearer sk-GXqCEbErJlwrUYDwhMzrKhv4AxnIvpvSvEIkb1XBdkx3Ct2m";

    public boolean sendVoice(String message,Long userId) {
        TbUserVoiceModelDo tbUserVoiceModelDo = tbUserVoiceModelDoService.getVoiceModelByUserId(userId).getFirst();

        // 通过机器人账号取出 Bot 对象
        Bot bot = botContainer.robots.get(QQbotIdEnum.DEFAULT.getId());

        List<String> taskIdList = new ArrayList<>();
        // 语音合成
        Map<String,Object> paramMap = new HashMap();
        paramMap.put("text", message.replaceAll("<voice>", "").replaceAll("</voice>", ""));
        paramMap.put("speaker_audio_url",tbUserVoiceModelDo.getVoiceModelUrl() );
        paramMap.put("emotion_audio_url", tbUserVoiceModelDo.getVoiceModelUrl());
//            paramMap.put("speaker_aud io_url", "https://file.302.ai/gpt/imgs/20260308/555729ebec7de9b020ae42c9c5e59d85.m4a");
//            paramMap.put("emotion_audio_url", "https://file.302.ai/gpt/imgs/20260308/555729ebec7de9b020ae42c9c5e59d85.m4a");
        paramMap.put("emotion_alpha", 1);

        HttpResponse taskResponse = HttpRequest.post("https://api.302.ai/302/index_tts2/task")
                .header("Authorization", API_KEY)
                .body(JSON.toJSONString(paramMap)).execute();
        int status = taskResponse.getStatus();
        log.info("语音合成相应 responseBody:{}", taskResponse.body());
        if (status != 200) {
            log.error("status: {}", status);
            log.info("taskResponse: {}", taskResponse.body());
            String build = MsgUtils.builder().text(message).build();
            bot.sendPrivateMsg(userId,build,false);
        }else{
            log.info("taskResponse: {}", taskResponse.body());
            Map<String,String> taskMap = (Map<String, String>) JSON.parse(taskResponse.body());
            String taskId = taskMap.get("task_id");
            log.info("消息：{} 语音合成任务发送成功 taskId: {}",message, taskId);
            taskIdList.add(taskId);
        }
        for (int j = 0; j < taskIdList.size(); j++) {
            try {
                Thread.sleep(1000 + (int) (Math.random() * 1000));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }

            boolean voiceSend = false;

            Map<String,String> resultMap = null;
            for (int i = 0; i < 30; i++){
                HttpResponse response = HttpRequest
                        .get("https://api.302.ai/302/index_tts2/task")
                        .header("Authorization", API_KEY)
                        .form("task_id", taskIdList.get(j))
                        .execute();
                log.info("taskId:{} 查询执行完毕 responseBody:{}", taskIdList.get(j),response.body());

                resultMap = (Map<String, String>) JSON.parse(response.body());
                if (!resultMap.get("state").equals("SUCCESS")){
                    if (resultMap.get("state").equals("FAILURE")){
                        log.info("语音合成失败：{}", resultMap.get("message"));
                        break;
                    }
                    log.info("消息：{} 语音合成中 状态：{}", message, resultMap.get("state"));
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }else {
                    log.info("消息：{} 语音合成成功", message);
                    voiceSend = true;
                    break;
                }
            }

            // 成功发送语音，失败发送文字
            if (voiceSend){
                String build = MsgUtils.builder().voice(resultMap.get("audio_url")).build();
                bot.sendPrivateMsg(userId,build,false);
                log.info("消息：{} 语音发送成功", message);
            }else {
                log.error("消息：{} 语音合成失败", message);
                String build = MsgUtils.builder().text(message).build();
                bot.sendPrivateMsg(userId,build,false);
            }


            tbAiChatMemoryService.save(new TbAiChatMemoryDo().setMemoryId(userId).setRole("assistant").setMessage(message));
        }
        return true;
    }
}
