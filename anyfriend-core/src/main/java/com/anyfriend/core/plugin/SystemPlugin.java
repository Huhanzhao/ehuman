package com.anyfriend.core.plugin;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.anyfriend.core.bean.SystemGraph;
import com.anyfriend.core.utils.SendMessageUtil;
import com.anyfriend.dao.entity.TbChatConfigDo;
import com.anyfriend.dao.entity.TbUserBotStatusDo;
import com.anyfriend.dao.entity.TbUserModelConfigDo;
import com.anyfriend.dao.entity.TbUserReferAudioDo;
import com.anyfriend.dao.enums.NodeEnum;
import com.anyfriend.dao.service.*;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.mikuac.shiro.annotation.MessageHandlerFilter;
import com.mikuac.shiro.annotation.PrivateMessageHandler;
import com.mikuac.shiro.common.utils.MsgUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.BotPlugin;
import com.mikuac.shiro.dto.event.message.PrivateMessageEvent;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class SystemPlugin extends BotPlugin {

    @Autowired
    private TbAiChatMemoryService tbAiChatMemoryService;

    @Autowired
    private TbChatConfigDoService tbChatConfigDoService;

    @Autowired
    private TbUserBotStatusDoService tbUserBotStatusDoService;

    @Autowired
    private TbUserModelConfigDoService tbUserModelConfigDoService;

    @Autowired
    private TbUserReferAudioDoService tbUserReferAudioDoService;

    @Autowired
    private SystemGraph systemGraph;

    private CompiledGraph apiConfigGraph;

    // todo 声音voice graph
//    private CompiledGraph voiceConfigGraph;

    @Autowired
    private SendMessageUtil sendMessageUtil;

    private final ConcurrentHashMap<Long, Disposable> runningDisposables = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Long, String> userCurrentNodeMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, String> userThreadIdMap = new ConcurrentHashMap<>();


    @PostConstruct
    public void init() {
        this.apiConfigGraph = systemGraph.getApiConfigGraph();
//        this.voiceConfigGraph = systemGraph.getVoiceGraph();
    }

    @PrivateMessageHandler
    @MessageHandlerFilter
    @Override
    public int onPrivateMessage(Bot bot , PrivateMessageEvent event){
        String userInput = event.getMessage().trim();
        Long userId = event.getUserId();

        // 如果当前有中断的图 继续执行api配置引导图
        if (runningDisposables.containsKey(event.getUserId())){
            return apiConfigurationResume(userId, userInput);
        }

        // 主菜单
        if (userInput.trim().equals("#")){
            return userInstruction(userId);
        }

        // 查询或修改声音model
        if (userInput.startsWith("#声音")){
            return getOrUpdateVoiceModel(userId,userInput);
        }

        // bot状态查询和修改
        if (userInput.startsWith("#好感度")) {
            return getOrUpdateBotStatus(userId, userInput);
        }

        // 查询api配置
        if (userInput.startsWith("#api查询")){
            return getApiConfig(userId, userInput);
        }

        // 查询节点启用的模型
        if (userInput.startsWith("#模型查询")){
            return getNodeEnableModel(userId);
        }

        // api引导
        if (userInput.startsWith("#开始配置api")){
            return apiConfigurationGuide(userId);
        }


        // todo 节点大模型切换引导

        // todo 声音参考音频查询或切换

        // todo 声音音频导入引导

        // todo 人设导入引导

        // todo 人设查询或切换

        // todo 导入表情包引导

        // todo 我的画像查询

        // todo 支持作者

        // 非法命令提示
        if (userInput.startsWith("#")){
            sendMessageUtil.sendSystemMsg(userId,"无效命令，输入\"#\" 查询当前可用命令");
            return MESSAGE_BLOCK;
        }
        return MESSAGE_IGNORE;
    }

    private int apiConfigurationResume(Long userId, String userInput) {
        if (userCurrentNodeMap.get(userId).equals(StateGraph.END)){
            log.info("已经执行到了end节点，移除会话标记");
            userCurrentNodeMap.remove(userId);
            runningDisposables.remove(userId);
            userCurrentNodeMap.remove(userId);
            userThreadIdMap.remove(userId);
            return MESSAGE_BLOCK;
        }

        if ("#exit".equals(userInput)){
            runningDisposables.get(userId).dispose();
            runningDisposables.remove(userId);
            userCurrentNodeMap.remove(userId);
            userThreadIdMap.remove(userId);
            sendMessageUtil.sendSystemMsg(userId,"已退出当前配置");
            return MESSAGE_BLOCK;
        }

        // 配置线程 ID
        var invokeConfig = RunnableConfig.builder()
                .threadId(userThreadIdMap.get(userId))
                .build();
        // 使用 updateState 更新状态，传入中断时的节点 ID
        RunnableConfig updatedConfig = null;
        try {
            Map<String,Object> map = new HashMap<>();
            map.put("userInput",userInput);

            // 如果第一个节点就中断 放入model实例
            if (userCurrentNodeMap.get(userId).equals("modelCategoryNode")){
                map.put("modelConfig",new TbUserModelConfigDo());
            }
            updatedConfig = apiConfigGraph.updateState(invokeConfig, map, userCurrentNodeMap.get(userId));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // 创建恢复配置，添加 HUMAN_FEEDBACK_METADATA_KEY
        RunnableConfig resumeConfig = RunnableConfig.builder(updatedConfig)
                .addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, "placeholder")
                .build();
        // 继续执行 Graph（input 为 null，使用之前的状态）
        apiConfigGraph.stream(null,resumeConfig)
                .doOnNext(event1 -> {
                    log.info("当前迭代节点: " + event1);
                    userCurrentNodeMap.put(userId, event1.node());
                    if (event1.node().equals(StateGraph.END)){
                        runningDisposables.remove(userId);
                        userCurrentNodeMap.remove(userId);
                        userThreadIdMap.remove(userId);
                    }
                })
                .doOnError(error -> System.err.println("流错误: " + error.getMessage()))
                .doOnComplete(() -> System.out.println("流完成"))
                .blockLast();
        return MESSAGE_BLOCK;
    }

    private int apiConfigurationGuide(Long userId) {
        String threadId = userThreadIdMap.get(userId);
        if (!userThreadIdMap.containsKey(userId)){
            threadId = UUID.randomUUID().toString();
            userThreadIdMap.put(userId, threadId);
        }

        if (runningDisposables.containsKey(userId)){
            sendMessageUtil.sendSystemMsg(userId,"有未推出的api配置，输入#exit退出");
            return MESSAGE_BLOCK;
        }
        // 执行
        Map<String, Object> input = Map.of(
                "userInput", "",
                "userId", userId.toString()
        );
        // 配置线程 ID
        var invokeConfig = RunnableConfig.builder()
                .threadId(threadId)
                .build();
        // 执行图并获取流
        Flux<NodeOutput> stream = apiConfigGraph.stream(input, invokeConfig)
                .subscribeOn(Schedulers.boundedElastic()); // 异步 执行

        // 异步处理每个输出
        var disposable = stream.subscribe(
                output -> {
                    log.info("当前迭代节点: " + output);
                    userCurrentNodeMap.put(userId, output.node());
                    if (output.node().equals(StateGraph.END)){
                        runningDisposables.remove(userId);
                        userCurrentNodeMap.remove(userId);
                        userThreadIdMap.remove(userId);
                    }
                },
                error -> log.error("流错误: " + error.getMessage()),
                () -> {
                    log.info("流完成");
                }

        );

        log.info("标记会话");
        runningDisposables.put(userId, disposable);
        return MESSAGE_BLOCK;
    }

    private int getNodeEnableModel(Long userId) {
        List<TbUserModelConfigDo> list = tbUserModelConfigDoService.getByUserIdNodeNumNotZero(userId);
        StringBuilder msg = new StringBuilder();
        for (TbUserModelConfigDo tbUserModelConfigDo : list) {
            msg.append(NodeEnum.getByNameNodeNum(tbUserModelConfigDo.getNodeEnum().getNodeNum()))
                    .append(":")
                    .append(tbUserModelConfigDo.getModel())
                    .append("\n");
        }

        sendMessageUtil.sendSystemMsg(userId,msg.toString());
        return MESSAGE_BLOCK;
    }

    private int getApiConfig(Long userId , String userInput) {
        String param = userInput.replace("#api查询", "").trim();
        int pageNum = 1;
        if (!param.isEmpty()) {
            try {
                pageNum = Integer.parseInt(param);
            } catch (NumberFormatException e) {
                // 页码格式错误，默认第1页
            }
        }
        int pageSize = 10;
        Page<TbUserModelConfigDo> page = tbUserModelConfigDoService.getByUserIdPage(userId, pageNum, pageSize);
        List<TbUserModelConfigDo> list = page.getRecords();
        if (list.isEmpty()) {
            sendMessageUtil.sendSystemMsg(userId, "暂无API配置");
            return MESSAGE_BLOCK;
        }
        StringBuilder msg = new StringBuilder();
        int start = (pageNum - 1) * pageSize + 1;
        for (int i = 0; i < list.size(); i++) {
            TbUserModelConfigDo config = list.get(i);
            msg.append("【").append(start + i).append("】\n");
            msg.append("模型: ").append(config.getModel()).append("\n");
            msg.append("分类: ").append(config.getCategory()).append("\n");
            msg.append("baseUrl: ").append(config.getBaseUrl()).append("\n");
            msg.append("apiKey: ").append(config.getApiKey()).append("\n");
            msg.append("代理: ").append(Boolean.TRUE.equals(config.getNeedProxy()) ? "是" : "否").append("\n\n");
        }
        msg.append("第 ").append(pageNum).append("/").append(page.getPages()).append(" 页，共 ").append(page.getTotal()).append(" 条");
        msg.append("\n翻页：#api查询 页码");
        sendMessageUtil.sendSystemMsg(userId, msg.toString().trim());
        return MESSAGE_BLOCK;
    }

    private int getOrUpdateBotStatus(Long userId, String userInput) {
        String param = userInput.replace("#好感度", "").trim();
        // 修改好感度  todo 默认关闭
        if (!param.isEmpty()){
            Integer grace = Integer.valueOf(param);
            tbUserBotStatusDoService.updateGraceByUserId(userId, grace);
        }
        // 查询当前好感度
        TbUserBotStatusDo tbUserBotStatusDo = tbUserBotStatusDoService.getByUserId(userId);
        String msg = MsgUtils.builder().text("当前好感度: " + tbUserBotStatusDo.getGrace() + "\n\n修改格式：#好感度 50").build();
        sendMessageUtil.sendSystemMsg(userId,msg);
        return MESSAGE_BLOCK;
    }

    private int getOrUpdateVoiceModel(Long userId, String userInput) {
        String param = userInput.replace("#声音", "").trim();
        // 切换声音
        if (!param.isEmpty()) {
            boolean success = tbUserReferAudioDoService.switchVoiceModel(userId, param);
            if (success) {
                sendMessageUtil.sendSystemMsg(userId,"已切换到声音模型：" + param);
            } else {
                sendMessageUtil.sendSystemMsg(userId,"没有找到该声音模型：" + param);
            }
            return MESSAGE_BLOCK;
        }
        // 查询声音列表
        List<TbUserReferAudioDo> voiceModels = tbUserReferAudioDoService.getByUserId(userId);
        if (voiceModels.isEmpty()) {
            sendMessageUtil.sendSystemMsg(userId,"暂无声音模型");
            return MESSAGE_BLOCK;
        }
        StringBuilder msg = new StringBuilder();
        for (TbUserReferAudioDo model : voiceModels) {
            msg.append(model.getReferAudioName());
            if (Boolean.TRUE.equals(model.getEnable())) {
                msg.append(" [当前启用]");
            }
            msg.append("\n");
        }
        msg.append("\n切换声音：#声音 模型名");
        sendMessageUtil.sendSystemMsg(userId,msg.toString().trim());
        return MESSAGE_BLOCK;
    }

    private int userInstruction(Long userId) {
        String msg = MsgUtils.builder()
                .text("#声音 [模型名] - 查看/切换声音模型\n" +
                        "#温度 [温度 topP] - 查看/修改模型参数\n" +
                        "#好感度 [数值] - 查看/修改好感度\n" +
                        "#模型查询 - 查看已配置模型\n" +
                        "#api查询 - 查看API配置\n" +
                        "#开始配置api - 配置API引导")
                .build();
        sendMessageUtil.sendSystemMsg(userId,msg);
        return MESSAGE_BLOCK;
    }
}
