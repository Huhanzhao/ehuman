package com.anyfriend.core.node;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.anyfriend.core.bean.ChatClientFactory;
import com.anyfriend.core.utils.LocalCacheUtil;
import com.anyfriend.dao.entity.TbAiChatMemoryDo;
import com.anyfriend.dao.enums.FeatureTypeEnum;
import com.anyfriend.dao.enums.NodeEnum;
import com.anyfriend.dao.service.TbAiChatMemoryService;
import com.anyfriend.dao.service.TbBotPromptDoService;
import com.anyfriend.dao.service.TbUserFeatureButtonDoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class ImageAnalysisNode implements NodeAction {

    @Autowired
    private ChatClientFactory chatClientFactory;

    @Autowired
    private TbAiChatMemoryService tbAiChatMemoryService;

    @Autowired
    private TbBotPromptDoService tbBotPromptDoService;

    @Autowired
    private TbUserFeatureButtonDoService tbUserFeatureButtonDoService;

    @Override
    public Map<String, Object> apply(OverAllState state) throws Exception {
        String userInput = (String) state.value("userInput").orElse("");
        Long userId = (Long) state.value("userId").orElse(0L);

        if (userInput.startsWith("[CQ:image")){
            try{
                boolean enableByUserId = tbUserFeatureButtonDoService.isEnableByUserId(userId, FeatureTypeEnum.IMAGE_ANALYSIS);
                if (!enableByUserId){
                    throw new RuntimeException("没有启用图片解析");
                }
                Pattern pattern = Pattern.compile("url=([^,]+)");
                Matcher matcher = pattern.matcher(userInput.replaceAll("&amp;","&"));
                matcher.find();

                ChatClient chatClient = chatClientFactory.createChatClient(userId, NodeEnum.IMAGE_ANALYSIS);

                String path = matcher.group(1);
                Message message = null;
                try {
                    message = UserMessage.builder()
                            .text("请根据要求输出") // content
                            .media(new Media(MimeTypeUtils.IMAGE_PNG, new URI(path))) // media
                            .build();
                } catch (URISyntaxException e) {
                    throw new RuntimeException(e);
                } // todo 提示词抽取md文件放到文件夹里面
                String systemPrompt = """
                    # 你是一个图片分析专家，请分析图片是否为表情包并描述图片 并且按照要求格式输出
                    本次消息的String格式：""" + userInput + """
                    
                    ## 要求：
                       1. 简短描述图片内容，内容用<image></image> 或者<emoji></emoji>标签包裹
                       2. 判断图片是否为表情包，如果是表情包就给<emoji>标签加上属性 emo:xxx, 是普通图片就用image标签
                       
                       例如：
                         <emoji emo="吃瓜">一个熊猫穿着黄色衣服拿着西瓜</emoji>
                         <image>在高铁站的照片，有很多人</image>
                    """;
                String content = chatClient.prompt(systemPrompt).messages(message).call().content();
                log.info("图片描述：" + content);
                if (content.isEmpty()){
                    throw new RuntimeException("图片描述为空");
                }

                Pattern resultPattern = Pattern.compile(
                        "<emoji\\s+emo=\"([^\"]+)\">(.*?)</emoji>"
                );

                Matcher resultMatcher = resultPattern.matcher(content);

                // 如果是表情包 保存到emoji目录
                boolean enableEmojiFetch = tbUserFeatureButtonDoService.isEnableByUserId(userId, FeatureTypeEnum.EMOJI_FETCH);
                if (resultMatcher.find() && enableEmojiFetch){
                    String emo = resultMatcher.group(1);
                    String fileName = resultMatcher.group(2);

                    downloadAndSaveImage(path,emo,fileName);
                }

                // 保存图片描述作为用户输入
                tbAiChatMemoryService.save(new TbAiChatMemoryDo().setMemoryId(userId).setRole("user").setMessage(content));
            }finally {
                LocalCacheUtil.getLocalCacheUtil().remove(userId + "_image");
            }
        }
        return Map.of("userInput",userInput);
    }

    public void downloadAndSaveImage(String imageUrl,String emo, String customName) throws IOException {
        // 1. 下载图片
        HttpResponse response = HttpRequest.get(imageUrl)
                .execute();
        byte[] imageBytes = response.bodyBytes();

        // 2. 获取 Content-Type 来判断图片格式
        String contentType = response.header("Content-Type");

        // 3. 根据 Content-Type 获取文件扩展名
        String extension = getExtensionFromContentType(contentType);

        // 4. 构建保存路径
        String fileName = customName + "." + extension;
        Path savePath = Paths.get("./emoji/" + emo + "/", fileName);

        // 5. 确保目录存在
        Files.createDirectories(savePath.getParent());

        // 6. 保存文件
        Files.write(savePath, imageBytes);

        log.info("表情包已保存: " + savePath);
    }

    private String getExtensionFromContentType(String contentType) {
        if (contentType == null) return "png"; // 默认格式

        switch (contentType.toLowerCase()) {
            case "image/jpeg":
            case "image/jpg":
                return "jpg";
            case "image/png":
                return "png";
            case "image/gif":
                return "gif";
            case "image/webp":
                return "webp";
            case "image/bmp":
                return "bmp";
            case "image/svg+xml":
                return "svg";
            default:
                return "png"; // 默认
        }
    }

}
