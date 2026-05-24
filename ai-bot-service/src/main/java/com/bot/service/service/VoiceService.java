package com.bot.service.service;


import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * tts服务
 */
public interface VoiceService {

    String getVoice(String text);

    String createVoiceModel(MultipartFile file, Long userId, String title) throws IOException;
}
