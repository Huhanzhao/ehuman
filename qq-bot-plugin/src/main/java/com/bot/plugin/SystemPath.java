package com.bot.plugin;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "bot")
public class SystemPath {
    private String classesUseAudioPath;
    private String audioPath;
    private String emojiBasePath;
    private String voicePluginSourcePath;
    private String voicePluginClassesPath;

}
