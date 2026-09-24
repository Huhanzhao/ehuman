package com.ehuman.plugin;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;

import java.nio.file.Files;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class MimoTTS implements TtsPlugin {
    @Override
    public String getName() {
        return "MimoTTS";
    }

    @Override
    public Map<String, String> textToSpeech(String message, String referVoicePath, String apiKey, String localPath, String proxyHost) throws Exception {
        byte[] audioBytes = Files.readAllBytes(FileUtil.file(localPath).toPath());
        String voiceBase64 = Base64.getEncoder().encodeToString(audioBytes);

        // 构造请求体
        Map<String, Object> body = new HashMap<>();
        body.put("model", "mimo-v2.5-tts-voiceclone");
        body.put("messages", List.of(
                Map.of("role", "user", "content", ""),
                Map.of("role", "assistant", "content", message)
        ));
        body.put("audio", Map.of(
                "format", "mp3",
                "voice", "data:audio/mp3;base64," + voiceBase64
        ));

        HttpResponse response = HttpRequest.post("https://token-plan-cn.xiaomimimo.com/v1/chat/completions")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .body(JSONUtil.toJsonStr(body))
                .execute();

        log.info("MimoTTS 响应状态: {}", response.getStatus());
        log.info("MimoTTS 响应体: {}", response.body());

        JSONObject root = JSONUtil.parseObj(response.body());
        Object audioDataObj = root.getByPath("choices[0].message.audio.data");
        if (audioDataObj == null) {
            throw new RuntimeException("TTS 响应中无音频数据: " + response.body());
        }
        String audioData = audioDataObj.toString();

        // base64传音频数据 url传空
        return Map.of("base64", audioData, "url", "");
    }
}
