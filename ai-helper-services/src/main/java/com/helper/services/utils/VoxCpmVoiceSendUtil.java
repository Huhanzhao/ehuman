package com.helper.services.utils;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.helper.common.entity.BusinessException;
import com.helper.services.entity.TbAiChatMemoryDo;
import com.helper.services.entity.TbUserVoiceModelDo;
import com.helper.services.enums.QQbotIdEnum;
import com.helper.services.service.TbAiChatMemoryService;
import com.mikuac.shiro.common.utils.MsgUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.BotContainer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
@Slf4j
public class VoxCpmVoiceSendUtil {

    private final BotContainer botContainer;

    private final TbAiChatMemoryService tbAiChatMemoryService;

    public void sendVoice(String message,Long userId) {
        try{
            // 上传参考录音文件 返回音频路径
            String tempFilePath = uploadVoiceFile("ai-helper-services/voice/茉莉导航.m4a");

            // 生成唯一hashCode
            String hashCode = java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 11);

            // 开始音频生成任务
            createTTS(hashCode,message.replaceAll("<voice>","").replaceAll("</voice>",""),tempFilePath);

            // 用hashCode获取录音文件路径 流式
            String resultPath = getResultVoicePath(hashCode);

            // 发送音频消息
            String build = MsgUtils.builder().voice(resultPath).build();
            botContainer.robots.get(QQbotIdEnum.DEFAULT.getId()).sendPrivateMsg(userId,build,false);
        }catch(Exception e){
            e.printStackTrace();
            // 失败了发送汉字
            log.info("失败发送文字消息");
            botContainer.robots.get(QQbotIdEnum.DEFAULT.getId()).sendPrivateMsg(userId,
                    message.replaceAll("<voice>","").replaceAll("</voice>",""),
                    false);
        }

        // 保存发送的ai消息
        tbAiChatMemoryService.save(new TbAiChatMemoryDo().setMemoryId(userId).setRole("assistant").setMessage(message));
    }

    private String getResultVoicePath(String hashCode) {

        HttpResponse response = HttpRequest
                .get("https://voxcpm.modelbest.cn/gradio_api/queue/data")
                .form("session_hash",hashCode)
                .execute();

        String body = response.body();
        log.info("获取录音路径响应体：{}",body);
        Pattern pattern = Pattern.compile("\"path\"\\s*:\\s*\"(.*?)\"");
        Matcher matcher = pattern.matcher(body);

        if (matcher.find()) {
            return "https://voxcpm.modelbest.cn/gradio_api/file=" + matcher.group(1);
        }
        throw new BusinessException("没有找到响应体的path");
    }

    private void createTTS(String hashCode,String message,String tempPath) {
        Map<String ,Object> dataMap = new HashMap<>();
        dataMap.put("path",tempPath);
        dataMap.put("url","https://voxcpm.modelbest.cn/gradio_api/file=" + tempPath);
        dataMap.put("orig_name","茉莉导航.m4a");
        dataMap.put("size",163381);
        dataMap.put("mime_type","audio/x-m4a");
        dataMap.put("meta",Map.of("_type","gradio.FileData"));

        List<Object> dataList = new ArrayList<>();
        dataList.add(message);
        dataList.add("声音很软很浪，耳边低语，语速缓慢，频繁出现湿润喘息和娇笑");
        dataList.add(dataMap);
        dataList.add(true);  // 极致克隆模式（基于文本引导的极致克隆）自动识别参考音频文本，完整还原音色、节奏、情感等全部声音细节。开启后 Control Instruction 将暂时禁用
        dataList.add("准备出发，哥哥，今天依然是全世界最棒最厉害的男人了，又是要赚超多钱的一天哦。前方直行，宝贝，就是这个角度往前，一直往前，就快要到了抵达目的地了，你这天车子我好喜欢下一次我们再长一点，好吗？你个小坏蛋。");
        dataList.add(3);  // 引导强度）数值越高 → 越贴合提示/参考音色；数值越低 → 生成风格更自由 0-3
        dataList.add(false);  // 参考音频降噪增强
        dataList.add(false);  // 文本规范化
        dataList.add(25);  // 流匹配迭代步数 LocDiT 流匹配生成迭代步数 — 步数越多 → 可能生成更好的音频质量，但速度变慢 0-50
        dataList.add("fp-5daogj60jtlr");


        Map<String,Object> bodyMap = new HashMap<>();
        bodyMap.put("fn_index",2);
        bodyMap.put("trigger_id",19);
        bodyMap.put("session_hash",hashCode);
        bodyMap.put("data",dataList);

        HttpResponse response = HttpRequest
                .post("https://voxcpm.modelbest.cn/gradio_api/queue/join")
                .body(JSON.toJSONString(bodyMap))
                .execute();

        log.info("创建任务响应体：{}",response.body());
        if (response.getStatus() != 200){
            throw new BusinessException("voxcpm创建任务失败");
        }
    }

    private String uploadVoiceFile(String filePath) {
        HttpResponse response = HttpRequest
                .post("https://voxcpm.modelbest.cn/gradio_api/upload")
                .form("files", new File(filePath))
                .execute();
        if (response.getStatus() != 200){
            throw new BusinessException("voxcpm上传录音失败");
        }

        log.info("上传录音响应体:{}",response.body());
        List<String> resultList = (List<String>) JSON.parse(response.body());

        if (resultList.isEmpty()){
            throw new BusinessException("voxcpm上传录音失败 响应体是空");
        }

        return resultList.getFirst();
    }
}
