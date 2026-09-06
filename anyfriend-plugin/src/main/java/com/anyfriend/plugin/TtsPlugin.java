package com.anyfriend.plugin;

import java.util.Map;

/**
 * 插件接口 - 所有插件都需要实现这个接口
 */
public interface TtsPlugin {
    
    /**
     * 获取插件名称
     */
    String getName();
    
    /**
     * 文字获取语音
     */
    Map<String, String> textToSpeech(String text, String referVoicePath,String apiKey,String localPath,String proxyPath) throws Exception;
}