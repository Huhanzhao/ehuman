package com.ehuman.plugin;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson2.JSON;
import lombok.extern.slf4j.Slf4j;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class IndexTTS2 implements TtsPlugin {

    @Override
    public String getName() {
        return "IndexTTS2";
    }

    @Override
    public Map<String, String> textToSpeech(String text, String referVoicePath,String apiKey,String localPath, String proxyHost) throws Exception {
        // 提交任务
        String taskId = submitTask(text, referVoicePath,apiKey);

        // 获取结果
        String resultPath = getResult(taskId,apiKey);
        HttpResponse response;
        try {
            response = HttpRequest.get(resultPath).timeout(10000).execute();
        }catch (Exception e){
            Proxy proxy = new Proxy(
                    Proxy.Type.HTTP,
                    // 失败了使用代理访问 端口可自行配置
                    new InetSocketAddress(proxyHost, 7890)
            );
            response = HttpRequest.get(resultPath).setProxy(proxy).execute();
        }

        if (response.getStatus() != 200){
            log.error("voxcpm tts2 语音合成失败,resultPath:{}",resultPath);
        }

        // 直接获取响应体的字节数组
        byte[] audioBytes = response.bodyBytes();

        // 转 Base64
        String base64 = Base64.getEncoder().encodeToString(audioBytes);

        // 公网url url传链接 base64传空字符串
        return Map.of("base64",base64,"url","");
    }

    public String submitTask(String message, String voice,String apiKey) throws Exception {
        Map<String,Object> paramMap = new HashMap();
        paramMap.put("text", message);
        paramMap.put("speaker_audio_url",voice);
        paramMap.put("emotion_audio_url", voice);
        paramMap.put("emotion_alpha", 1);

        HttpResponse taskResponse = HttpRequest.post("https://api.302ai.com/302/index_tts2/task")
                .header("Authorization", "Bearer " + apiKey)
                .body(JSON.toJSONString(paramMap)).execute();
        log.info("taskResponse: {}", taskResponse.body());
        Map<String,String> taskMap = (Map<String, String>) JSON.parse(taskResponse.body());
        String taskId = taskMap.get("task_id");
        log.info("消息：{} 语音合成任务发送成功 taskId: {}",message, taskId);
        return taskId;
    }

    public String getResult(String taskId,String apiKey) throws Exception {
        Map<String,String> resultMap = null;
        for (int i = 0; i < 30; i++){
            HttpResponse response = HttpRequest
                    .get("https://api.302ai.com/302/index_tts2/task")
                    .header("Authorization", "Bearer " + apiKey)
                    .form("task_id", taskId)
                    .execute();
            log.info("taskId:{} 查询执行完毕 responseBody:{}", taskId,response.body());

            resultMap = (Map<String, String>) JSON.parse(response.body());
            if (!resultMap.get("state").equals("SUCCESS")){
                if (resultMap.get("state").equals("FAILURE")){
                    log.info("语音合成失败：{}", resultMap.get("message"));
                    break;
                }
                log.info("taskId：{} 语音合成中 状态：{}", taskId, resultMap.get("state"));
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }else {
                log.info("taskId：{} 语音合成成功", taskId);
                break;
            }
        }
        return (String)resultMap.get("audio_url");
    }
}
