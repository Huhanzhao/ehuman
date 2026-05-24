package com.helper.services.plugin;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.helper.services.bean.SystemGraph;
import com.helper.services.entity.TbChatConfigDo;
import com.helper.services.entity.TbUserGraceDo;
import com.helper.services.entity.TbUserModelConfigDo;
import com.helper.services.entity.TbUserVoiceModelDo;
import com.helper.services.enums.NodeEnum;
import com.helper.services.service.TbAiChatMemoryService;
import com.helper.services.service.TbChatConfigDoService;
import com.helper.services.service.TbUserGraceDoService;
import com.helper.services.service.TbUserModelConfigDoService;
import com.helper.services.service.TbUserVoiceModelDoService;
import com.mikuac.shiro.annotation.MessageHandlerFilter;
import com.mikuac.shiro.annotation.PrivateMessageHandler;
import com.mikuac.shiro.common.utils.MsgUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.BotContainer;
import com.mikuac.shiro.core.BotPlugin;
import com.mikuac.shiro.dto.action.common.ActionData;
import com.mikuac.shiro.dto.action.response.BooleanResp;
import com.mikuac.shiro.dto.event.message.PrivateMessageEvent;
import groovyjarjarantlr4.v4.misc.Graph;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.net.URI;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
@Slf4j
public class SystemPlugin extends BotPlugin {

    @Autowired
    private  TbAiChatMemoryService tbAiChatMemoryService;

    @Autowired
    private  TbChatConfigDoService tbChatConfigDoService;

    @Autowired
    private  TbUserGraceDoService tbUserGraceDoService;

    @Autowired
    private  TbUserModelConfigDoService tbUserModelConfigDoService;

    @Autowired
    private TbUserVoiceModelDoService tbUserVoiceModelDoService;

    @Autowired
    private SystemGraph systemGraph;

    private CompiledGraph apiConfigGraph;

    // todo 声音voice graph
    private CompiledGraph voiceConfigGraph;

    private final ConcurrentHashMap<Long, Disposable> runningDisposables = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Long, String> userCurrentNodeMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, String> userThreadIdMap = new ConcurrentHashMap<>();


    @PostConstruct
    public void init() {
        this.apiConfigGraph = systemGraph.getApiConfigGraph();
        this.voiceConfigGraph = systemGraph.getVoiceGraph();
    }

    @PrivateMessageHandler
    @MessageHandlerFilter
    @Override
    public int onPrivateMessage(Bot bot , PrivateMessageEvent event){
        String userInput = event.getMessage();

        if (userInput.trim().equals("#")){
            String msg = MsgUtils.builder()
                    .text("#声音 [模型名] - 查看/切换声音模型\n" +
                          "#温度 [温度 topP] - 查看/修改模型参数\n" +
                          "#好感度 [数值] - 查看/修改好感度\n" +
                          "#模型查询 - 查看已配置模型\n" +
                          "#api查询 - 查看API配置\n" +
                          "#api - 配置API引导")
                    .build();
            bot.sendPrivateMsg(event.getUserId(),msg,false);
            return MESSAGE_BLOCK;
        }

        if (userInput.startsWith("#声音")){
            String param = userInput.replace("#声音", "").trim();
            // 切换声音
            if (!param.isEmpty()) {
                boolean success = tbUserVoiceModelDoService.switchVoiceModel(event.getUserId(), param);
                if (success) {
                    bot.sendPrivateMsg(event.getUserId(), "已切换到声音模型：" + param, false);
                } else {
                    bot.sendPrivateMsg(event.getUserId(), "没有找到该声音模型：" + param, false);
                }
                return MESSAGE_BLOCK;
            }
            // 查询声音列表
            List<TbUserVoiceModelDo> voiceModels = tbUserVoiceModelDoService.getByUserId(event.getUserId());
            if (voiceModels.isEmpty()) {
                bot.sendPrivateMsg(event.getUserId(), "暂无声音模型", false);
                return MESSAGE_BLOCK;
            }
            StringBuilder msg = new StringBuilder();
            for (TbUserVoiceModelDo model : voiceModels) {
                msg.append(model.getVoiceModelName());
                if (Boolean.TRUE.equals(model.getEnable())) {
                    msg.append(" [当前启用]");
                }
                msg.append("\n");
            }
            msg.append("\n切换声音：#声音 模型名");
            bot.sendPrivateMsg(event.getUserId(), msg.toString().trim(), false);
            return MESSAGE_BLOCK;
        }

        if (userInput.startsWith("#删除")){
            userInput = userInput.replace("#删除", "");
            tbAiChatMemoryService.removeByMemoryIdAndLikeMessage(event.getUserId(), userInput);
            return MESSAGE_BLOCK;
        }

        if (userInput.startsWith("#温度")){
            String param = userInput.replace("#温度", "").trim();
            // 修改参数
            if (!param.isEmpty()){
                String[] split = param.split(" ");
                tbChatConfigDoService.updateByUserId(event.getUserId(), Double.valueOf(split[0]), Double.valueOf(split[1]));
            }
            // 查询当前配置
            TbChatConfigDo tbChatConfigDo = tbChatConfigDoService.getByUserId(event.getUserId());
            String msg = MsgUtils.builder().text("温度: " + tbChatConfigDo.getTemperature() + "\ntopP: " + tbChatConfigDo.getTopP() + "\n\n修改格式：#温度 0.7 1.0").build();
            bot.sendPrivateMsg(event.getUserId(),msg,false);
            return MESSAGE_BLOCK;
        }

        if (userInput.startsWith("#好感度")) {
            String param = userInput.replace("#好感度", "").trim();
            // 修改好感度
            if (!param.isEmpty()){
                Integer grace = Integer.valueOf(param);
                tbUserGraceDoService.updateGraceByUserId(event.getUserId(), grace);
            }
            // 查询当前好感度
            TbUserGraceDo tbUserGraceDo = tbUserGraceDoService.getByUserId(event.getUserId());
            String msg = MsgUtils.builder().text("当前好感度: " + tbUserGraceDo.getGrace() + "\n\n修改格式：#好感度 50").build();
            bot.sendPrivateMsg(event.getUserId(),msg,false);
            return MESSAGE_BLOCK;
        }

        if (userInput.startsWith("#api查询")){
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
            Page<TbUserModelConfigDo> page = tbUserModelConfigDoService.getByUserIdPage(event.getUserId(), pageNum, pageSize);
            List<TbUserModelConfigDo> list = page.getRecords();
            if (list.isEmpty()) {
                bot.sendPrivateMsg(event.getUserId(), "暂无API配置", false);
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
            bot.sendPrivateMsg(event.getUserId(), msg.toString().trim(), false);
            return MESSAGE_BLOCK;
        }

        if (userInput.startsWith("#模型查询")){
            List<TbUserModelConfigDo> list = tbUserModelConfigDoService.getByUserIdNodeNumNotZero(event.getUserId());
            StringBuilder msg = new StringBuilder();
            for (TbUserModelConfigDo tbUserModelConfigDo : list) {
                msg.append(NodeEnum.getByNameNodeNum(tbUserModelConfigDo.getNodeNum()))
                        .append(":")
                        .append(tbUserModelConfigDo.getModel())
                        .append("\n");
            }

            bot.sendPrivateMsg(event.getUserId(), msg.toString(),false);
            return MESSAGE_BLOCK;
        }

        if (userInput.startsWith("#api")){
            bot.sendPrivateMsg(event.getUserId(), "输入 #开始配置api 进入api配置引导状态",false);
            return MESSAGE_BLOCK;
        }

        if (userInput.startsWith("#开始配置api")){

            String threadId = userThreadIdMap.get(event.getUserId());
            if (!userThreadIdMap.containsKey(event.getUserId())){
                threadId = UUID.randomUUID().toString();
                userThreadIdMap.put(event.getUserId(), threadId);
            }

            if (runningDisposables.containsKey(event.getUserId())){
                bot.sendPrivateMsg(event.getUserId(), "请先退出当前api配置",false);
                return MESSAGE_BLOCK;
            }
            // 执行
            Map<String, Object> input = Map.of(
                    "userInput", "",
                    "userId", event.getUserId().toString()
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
                        userCurrentNodeMap.put(event.getUserId(), output.node());
                        if (output.node().equals(StateGraph.END)){
                            runningDisposables.remove(event.getUserId());
                            userCurrentNodeMap.remove(event.getUserId());
                            userThreadIdMap.remove(event.getUserId());
                        }
                    },
                    error -> log.error("流错误: " + error.getMessage()),
                    () -> {
                        log.info("流完成");
                    }

            );

            log.info("标记会话");
            runningDisposables.put(event.getUserId(), disposable);
            return MESSAGE_BLOCK;
        }

        // 如果当前有阻塞的图 继续执行图
        if (runningDisposables.containsKey(event.getUserId())){
            if (userCurrentNodeMap.get(event.getUserId()).equals(StateGraph.END)){
                log.info("已经执行到了end节点，移除会话标记");
                userCurrentNodeMap.remove(event.getUserId());
                runningDisposables.remove(event.getUserId());
                userCurrentNodeMap.remove(event.getUserId());
                userThreadIdMap.remove(event.getUserId());
                return MESSAGE_BLOCK;
            }

            if ("exit".equals(userInput)){
                runningDisposables.get(event.getUserId()).dispose();
                runningDisposables.remove(event.getUserId());
                userCurrentNodeMap.remove(event.getUserId());
                userThreadIdMap.remove(event.getUserId());
                bot.sendPrivateMsg(event.getUserId(), "已退出当前配置",false);
                return MESSAGE_BLOCK;
            }

            // 配置线程 ID
            var invokeConfig = RunnableConfig.builder()
                    .threadId(userThreadIdMap.get(event.getUserId()))
                    .build();
            // 使用 updateState 更新状态，传入中断时的节点 ID
            RunnableConfig updatedConfig = null;
            try {
                Map<String,Object> map = new HashMap<>();
                map.put("userInput",userInput);

                // 如果第一个节点就中断 放入model实例
                if (userCurrentNodeMap.get(event.getUserId()).equals("modelCategoryNode")){
                    map.put("modelConfig",new TbUserModelConfigDo());
                }
                updatedConfig = apiConfigGraph.updateState(invokeConfig, map, userCurrentNodeMap.get(event.getUserId()));
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
                        userCurrentNodeMap.put(event.getUserId(), event1.node());
                        if (event1.node().equals(StateGraph.END)){
                            runningDisposables.remove(event.getUserId());
                            userCurrentNodeMap.remove(event.getUserId());
                            userThreadIdMap.remove(event.getUserId());
                        }
                    })
                    .doOnError(error -> System.err.println("流错误: " + error.getMessage()))
                    .doOnComplete(() -> System.out.println("流完成"))
                    .blockLast();
            return MESSAGE_BLOCK;
        }

        return MESSAGE_IGNORE;
    }
}
