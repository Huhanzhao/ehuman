package com.anyfriend.core.plugin;

import cn.hutool.http.HttpResponse;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.anyfriend.core.node.test.test1;
import com.anyfriend.core.node.test.test2;
import com.anyfriend.core.service.CommonAuxiliaryService;
import com.anyfriend.core.utils.SendMessageUtil;
import com.mikuac.shiro.annotation.MessageHandlerFilter;
import com.mikuac.shiro.annotation.PrivateMessageHandler;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.BotContainer;
import com.mikuac.shiro.core.BotPlugin;
import com.mikuac.shiro.dto.event.message.PrivateMessageEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


@Component
@Slf4j
public class TestPlugin extends BotPlugin {

    @Autowired
    private CompiledGraph sendMessageCompiledGraph;

    @Autowired
    private SendMessageUtil sendMessageUtil;

    @Autowired
    private BotContainer container;
    @Autowired
    private test1 test1;
    @Autowired
    private test2 test2;

    @Autowired
    private CommonAuxiliaryService commonAuxiliaryService;


    @PrivateMessageHandler
    @MessageHandlerFilter
    @Override
    public int onPrivateMessage(Bot bot, PrivateMessageEvent event)  {
//        String target = """
//            ## 目标：请基于你当前在做的事情和状态还有历史消息，生成一条主动发给用户的自然消息,一行或者两行。
//            不要太突兀，要与上下文相和谐 ，可以分享正在做的事、表达想念、撒娇、问好等。
//            """;
//        sendMessageCompiledGraph.invoke(Map.of("userId", event.getUserId(), "target", target, "initiative", true));
//        sendMessageUtil.sendVoiceMsg(event.getUserId(),event.getMessage());

//        byte[] bytes = new byte[0];
//        try {
//            HttpResponse response = HttpRequest.get("https://multimedia.nt.qq.com.cn/download?appid=1406&fileid=EhTmzzmDzQn3FVkgxf2p1PWWo7sDKRjsViD-Cii50LS08veUAzIEcHJvZFCAuy9aEP06sXaHgBqsnxdh-H7J1vd6AoGAggECbmo&rkey=CAISMEjqu1yUtfmpNgZJJp9Zxpjo3NJKfxV0jnGH6hxndS_z3VDlvxLgddHKoOySls9MaQ").execute();
//             bytes = response.bodyBytes();
//            // 2. 确定文件后缀（从 Content-Type 或 URL 中获取）
//            String suffix = getFileSuffix(response);
//
//            // 3. 创建目录
//            String emojiDir = "./emoji/开心/";
//            File dir = new File(emojiDir);
//            if (!dir.exists()) {
//                dir.mkdirs();  // 创建多级目录
//            }
//
//            // 4. 生成文件名（避免重名）
//            String fileName = System.currentTimeMillis() + suffix;
//            File outputFile = new File(dir, fileName);
//
//            // 5. 写入文件
//            try (FileOutputStream fos = new FileOutputStream(outputFile)) {
//                fos.write(bytes);
//            }
//
//
//            // 文件夹直接分类放图片
//            bytes = Files.readAllBytes(Paths.get("./emoji/开心/一只卡通兔子表情，下面写着开心.gif"));
//        } catch (Exception e) {
//            throw new RuntimeException(e);
//        }
//        String build = MsgUtils.builder().img(bytes).build();
//
//        bot.sendPrivateMsg(event.getUserId(), build,false);

//        sendMessageUtil.sendStickerMsg(event.getUserId(), "<emoji emo=\"偷笑\"/>");

//        String target = """
//            ## 目标：请基于你当前在做的事情和状态还有历史消息，生成一条主动发给用户的自然消息,一行或者两行。
//            不要太突兀，要与上下文相和谐
//            1. 可以分享正在做的事、表达想念、撒娇、问好等。
//            """;
//        String chatId = UUID.randomUUID().toString();
//        // 配置线程 ID
//        var invokeConfig = RunnableConfig.builder()
//                .threadId(chatId)
//                .build();
//        sendMessageCompiledGraph.invoke(
//                Map.of("userId", event.getUserId(), "target", target, "initiative", true),
//                invokeConfig
//        );
        return MESSAGE_IGNORE;
    }

    /**
     * 获取文件后缀
     */
    private String getFileSuffix(HttpResponse response) {
        // 方法1：从 Content-Type 获取
        String contentType = response.header("Content-Type");
        if (contentType != null) {
            switch (contentType) {
                case "image/png": return ".png";
                case "image/jpeg": return ".jpg";
                case "image/jpg": return ".jpg";
                case "image/gif": return ".gif";
                case "image/webp": return ".webp";
                case "image/bmp": return ".bmp";
            }
        }

        // 方法2：从 URL 中提取
        // 如果有文件扩展名，可以从 URL 获取
        // 默认返回 .png
        return ".png";
    }
}
