package com.helper.services;

import com.helper.services.service.TbAiChatMemoryService;
import com.helper.services.service.VoiceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.time.LocalDateTime;

@SpringBootTest
public class sdfsd {

    @Autowired
    private VoiceService voiceService;

    public static void main(String[] args) {
        System.out.println(LocalDateTime.now().getDayOfWeek());
    }

    @Test
    public void test() throws IOException {
        File file = new File("C:\\Users\\Lenovo\\OneDrive\\桌面\\4444443746.m4a");

        FileInputStream input = new FileInputStream(file);

        MultipartFile multipartFile = new MockMultipartFile(
                file.getName(), // name
                file.getName(), // original filename
                "application/octet-stream", // content type
                input
        );
        voiceService.createVoiceModel(null, 1L, "tts");
    }
}
