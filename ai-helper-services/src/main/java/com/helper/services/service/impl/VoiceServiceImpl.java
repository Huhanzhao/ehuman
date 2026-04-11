package com.helper.services.service.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.helper.services.service.VoiceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class VoiceServiceImpl implements VoiceService {

    @Override
    public String getVoice(String text) {
        //
        Map<String,Object> paramMap = new HashMap<>();
        paramMap.put("text", text);
        paramMap.put("chunk_length", 200);
        paramMap.put("normalize", true);
        paramMap.put("format", "mp3");
        paramMap.put("mp3_bitrate", 64);
        paramMap.put("opus_bitrate", 32);
        paramMap.put("latency", "normal");
        paramMap.put("reference_id", "160f658ffcde40a69bb82a059489927d");  // todo 获取声音模型id
        HttpRequest request = HttpRequest.post("https://api.302.ai/fish-audio/v1/tts?response_format=url")
                .header("Authorization", "Bearer sk-Vqex6dWmqqFMVS1wsuwMjywSvFvfq7RFzTX3v0N8T05fg0q7")
                .header("model", "s1")
//                .form("response_format", "data")
                .body(JSONObject.toJSONString(paramMap));
        HttpResponse taskResponse = request.execute();

        if (taskResponse.getStatus() != 200){
            log.error("tts失败 status: {}", taskResponse.getStatus());
            return null;
        }
        JSONObject resultJsonObject = JSONObject.parse(taskResponse.body());
        return resultJsonObject.getString("url");
    }

    @Override
    public String createVoiceModel(MultipartFile file, Long userId,String title) throws IOException {

        Map<String,Object> paramMap = new HashMap<>();
        paramMap.put("type", "tts");
        paramMap.put("visibility", "private");
        paramMap.put("title", "tts");
        paramMap.put("train_mode", "fast");

        // todo 管理用户token
        HttpResponse taskResponse = HttpRequest.post("https://api.302.ai/fish-audio/model")
                .header("Authorization", "Bearer sk-Vqex6dWmqqFMVS1wsuwMjywSvFvfq7RFzTX3v0N8T05fg0q7")
                .form(paramMap)
                .form("voices", file.getInputStream(), file.getOriginalFilename())
                .execute();

        if (taskResponse.getStatus() != 200){
            log.error("创建声音模型失败 status: {}", taskResponse.getStatus());
            return null;
        }
        Map<String,String> taskMap = (Map<String, String>) JSON.parse(taskResponse.body());
        return taskMap.get("_id");
    }
}
