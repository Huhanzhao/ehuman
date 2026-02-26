package com.helper.services.node;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.alibaba.fastjson2.JSON;
import com.helper.services.entity.TbAiChatMemoryDo;
import com.helper.services.enums.QQbotIdEnum;
import com.helper.services.service.TbAiChatMemoryService;
import com.mikuac.shiro.common.utils.MsgUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.BotContainer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.Serializable;
import java.util.*;

@Component
@Slf4j
@RequiredArgsConstructor
public class SendVoiceNode implements NodeAction {

    private final BotContainer botContainer;

    private final TbAiChatMemoryService tbAiChatMemoryService;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String aiMessage = (String) state.value("finalOutput", String.class).orElse("");
        Long userId = (Long)state.value("userId").orElse(0L);

        if (aiMessage.isEmpty()) {
            log.error("no message");
        }

        log.info("aiMessage: {}", aiMessage);
        String[] splitMessages = aiMessage.replaceAll("\n\n","\n").split("\n");

        List<String> taskIdList = new ArrayList<>();
        for (String splitMessage : splitMessages) {
            if (splitMessage.isEmpty() || splitMessage.isBlank()) continue;
            // 语音合成
            Map<String,Object> paramMap = new HashMap();
            paramMap.put("text", splitMessage);
            paramMap.put("speaker_audio_url", "https://file.302.ai/gpt/imgs/20260223/9324e78b1f5d1cc60b5a713a43dbb5fb.m4a");
            paramMap.put("emotion_audio_url", "https://file.302.ai/gpt/imgs/20260223/9324e78b1f5d1cc60b5a713a43dbb5fb.m4a");
            paramMap.put("emotion_alpha", 1);

            HttpResponse taskResponse = HttpRequest.post("https://api.302.ai/302/index_tts2/task")
                    .header("Authorization", "Bearer sk-Vqex6dWmqqFMVS1wsuwMjywSvFvfq7RFzTX3v0N8T05fg0q7")
                    .body(JSON.toJSONString(paramMap)).execute();
            int status = taskResponse.getStatus();
            if (status != 200) {
                log.error("status: {}", status);
                log.error("taskResponse: {}", taskResponse.body());
            }
            Map<String,String> taskMap = (Map<String, String>) JSON.parse(taskResponse.body());
            String taskId = taskMap.get("task_id");
            log.info("消息：{} 发送成功 taskId: {}",splitMessage, taskId);
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
                        .header("Authorization", "Bearer sk-Vqex6dWmqqFMVS1wsuwMjywSvFvfq7RFzTX3v0N8T05fg0q7")
                        .form("task_id", taskIdList.get(j))
                        .execute();
                log.info("taskId:{} 查询执行完毕", taskIdList.get(j));
                resultMap = (Map<String, String>) JSON.parse(response.body());
                if (!resultMap.get("state").equals("SUCCESS")){
                    if (resultMap.get("state").equals("FAILURE")){
                        log.info("语音合成失败：{}", resultMap.get("message"));
                        break;
                    }
                    log.info("消息：{} 语音合成中 状态：{}", splitMessages[j], resultMap.get("state"));
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
                }else {
                    log.info("消息：{} 语音合成成功", splitMessages[j]);
                    voiceSend = true;
                    break;
                }
            }

            // 通过机器人账号取出 Bot 对象
            Bot bot = botContainer.robots.get(QQbotIdEnum.DEFAULT.getId());

            // 成功发送语音，失败发送文字
            if (voiceSend){
                String build = MsgUtils.builder().voice(resultMap.get("audio_url")).build();
                bot.sendPrivateMsg(userId,build,false);
                log.info("消息：{} 语音发送成功", splitMessages[j]);
            }else {
                log.error("消息：{} 语音合成失败", splitMessages[j]);
                String build = MsgUtils.builder().text(splitMessages[j]).build();
                bot.sendPrivateMsg(userId,build,false);
            }


            tbAiChatMemoryService.save(new TbAiChatMemoryDo().setMemoryId(userId).setRole("assistant").setMessage(splitMessages[j]));
        }
        return Map.of();
    }
}
