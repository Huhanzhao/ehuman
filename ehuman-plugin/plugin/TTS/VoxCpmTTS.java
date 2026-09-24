package com.ehuman.plugin;

import cn.hutool.core.io.FileUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Component
public class VoxCpmTTS implements TtsPlugin {
    @Override
    public String getName() {
        return "VoxCpmTTS";
    }

    @Override
    public Map<String, String> textToSpeech(String message, String referVoicePath, String apiKey, String localPath, String proxyHost) throws Exception {
        if(!localPath.endsWith(".wav")){
            log.error("当前语音插件VoxCpmTTS只支持wav格式的参考音频，当前参考音频{}",localPath);
            throw new RuntimeException("当前语音插件VoxCpmTTS只支持wav格式的参考音频");
        }
        byte[] audioBytes = Files.readAllBytes(FileUtil.file(localPath).toPath());
        String voiceBase64 = Base64.getEncoder().encodeToString(audioBytes);

        // 构造请求体
        Map<String, Object> body = new HashMap<>();
        body.put("model", "VoxCPM2");
        body.put("input", message);
        body.put("ref_audio", "data:audio/wav;base64," + voiceBase64);

        HttpResponse response = HttpRequest.post("https://api.modelbest.cn/v1/audio/speech")
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .header("Accept", "text/event-stream")  // 关键：声明要 SSE
                .body(JSONUtil.toJsonStr(body))
                .execute();

        // 关键：不要用 response.body()，而是用 response.getInputStream()
        List<String> audioPieces = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(response.bodyStream(), StandardCharsets.UTF_8))) {

            String line;
            while ((line = reader.readLine()) != null) {
                // 处理每一行
                if (line.startsWith("data:")) {
                    String jsonStr = line.substring(5).trim();
                    if (!jsonStr.isEmpty() && !jsonStr.equals("[DONE]")) {
                        // 解析 JSON 获取 audio 字段
                        JSONObject json = JSONUtil.parseObj(jsonStr);
                        String audioBase64 = json.getStr("audio");
                        if (audioBase64 != null && !audioBase64.isEmpty()) audioPieces.add(audioBase64);
                    }
                }
            }
        }

        // 解码并合并
        byte[] fullAudio = mergeAudioFragments(audioPieces);
        String audioData = Base64.getEncoder().encodeToString(fullAudio);

        // base64传音频数据 url传空
        return Map.of("base64", audioData, "url", "");
    }


    public static byte[] mergeAudioFragments(List<String> audioBase64List) {
        // 1. 先解码每个片段
        List<byte[]> decodedPieces = audioBase64List.stream()
                .map(base64 -> Base64.getDecoder().decode(base64))
                .collect(Collectors.toList());

        // 2. 计算总长度
        int totalLen = decodedPieces.stream()
                .mapToInt(piece -> piece.length)
                .sum();

        // 3. 合并所有二进制数据
        byte[] merged = new byte[totalLen];
        int offset = 0;
        for (byte[] piece : decodedPieces) {
            System.arraycopy(piece, 0, merged, offset, piece.length);
            offset += piece.length;
        }

        return merged;
    }
}
