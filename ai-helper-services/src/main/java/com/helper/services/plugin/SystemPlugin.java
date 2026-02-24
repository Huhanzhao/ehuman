package com.helper.services.plugin;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.state.StateSnapshot;
import com.alibaba.fastjson2.JSON;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.helper.services.entity.TbAiChatMemoryDo;
import com.helper.services.entity.TbChatConfigDo;
import com.helper.services.entity.TbUserGraceDo;
import com.helper.services.entity.TbUserModelConfigDo;
import com.helper.services.service.TbAiChatMemoryService;
import com.helper.services.service.TbChatConfigDoService;
import com.helper.services.service.TbUserGraceDoService;
import com.helper.services.service.TbUserModelConfigDoService;
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

    private final CompiledGraph qqSystemGraph;

    private final ConcurrentHashMap<Long, Disposable> runningDisposables = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<Long, String> userCurrentNodeMap = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, String> userThreadIdMap = new ConcurrentHashMap<>();

    public SystemPlugin(@Qualifier("qqSystemGraph")CompiledGraph qqSystemGraph){
        this.qqSystemGraph = qqSystemGraph;
    }

    @PrivateMessageHandler
    @MessageHandlerFilter
    @Override
    public int onPrivateMessage(Bot bot , PrivateMessageEvent event){
        String userInput = event.getMessage();

        if (userInput.startsWith("#指令")){
            String msg = MsgUtils.builder().text("#删除+模糊词\n#对话配置\n#好感度\n#api\n#模型查询\n#api查询").build();
            bot.sendPrivateMsg(event.getUserId(),msg,false);
            return MESSAGE_BLOCK;
        }

        if (userInput.startsWith("#删除")){
            userInput = userInput.replace("#删除", "");
            tbAiChatMemoryService.remove(Wrappers.<TbAiChatMemoryDo>lambdaQuery()
                    .eq(TbAiChatMemoryDo::getRole,"assistant")
                    .eq(TbAiChatMemoryDo::getMemoryId,event.getUserId())
                    .like(TbAiChatMemoryDo::getMessage,userInput)
            );
            return MESSAGE_BLOCK;
        }

        if (userInput.startsWith("#对话配置")){
            userInput = userInput.replace("#对话配置", "");
            if (!userInput.isEmpty()){
                String[] split = userInput.trim().split(" ");
                tbChatConfigDoService.update(Wrappers.<TbChatConfigDo>lambdaUpdate()
                        .set(TbChatConfigDo::getTemperature,Double.valueOf(split[0]))
                        .set(TbChatConfigDo::getTopP,Double.valueOf(split[1]))
                        .eq(TbChatConfigDo::getUserId,event.getUserId()));
            }
            TbChatConfigDo tbChatConfigDo = tbChatConfigDoService.getOne(Wrappers.<TbChatConfigDo>lambdaQuery()
                    .eq(TbChatConfigDo::getUserId,event.getUserId()));
            String msg = MsgUtils.builder().text("temp:" + tbChatConfigDo.getTemperature() + "\ntopP:" + tbChatConfigDo.getTopP()).build();
            bot.sendPrivateMsg(event.getUserId(),msg,false);
            return MESSAGE_BLOCK;
        }

        if (userInput.startsWith("#好感度")) {
            userInput = userInput.replace("#好感度", "");
            if (!userInput.isEmpty()){
                Integer grace = Integer.valueOf(userInput);
                tbUserGraceDoService.update(Wrappers.<TbUserGraceDo>lambdaUpdate()
                        .set(TbUserGraceDo::getGrace,grace)
                        .eq(TbUserGraceDo::getUserId,event.getUserId()));
            }
            TbUserGraceDo tbUserGraceDo = tbUserGraceDoService.getOne(Wrappers.<TbUserGraceDo>lambdaQuery()
                    .eq(TbUserGraceDo::getUserId,event.getUserId()));
            String msg = MsgUtils.builder().text(tbUserGraceDo.getGrace().toString()).build();
            bot.sendPrivateMsg(event.getUserId(),msg,false);
            return MESSAGE_BLOCK;
        }

        if (userInput.startsWith("#api查询")){
            List<TbUserModelConfigDo> list = tbUserModelConfigDoService.list(Wrappers.<TbUserModelConfigDo>lambdaQuery()
                    .eq(TbUserModelConfigDo::getUserId, event.getUserId()));
            bot.sendPrivateMsg(event.getUserId(), JSON.toJSONString(list),false);
            return MESSAGE_BLOCK;
        }

        if (userInput.startsWith("#模型查询")){
            List<TbUserModelConfigDo> list = tbUserModelConfigDoService.list(Wrappers.<TbUserModelConfigDo>lambdaQuery()
                    .eq(TbUserModelConfigDo::getUserId, event.getUserId()).ne(TbUserModelConfigDo::getNodeNum,0)
                    .orderByAsc(TbUserModelConfigDo::getNodeNum));
            StringBuilder msg = new StringBuilder();
            for (TbUserModelConfigDo tbUserModelConfigDo : list) {
                msg.append("nodeNum:").append(tbUserModelConfigDo.getNodeNum()).append(",")
                        .append("model:").append(tbUserModelConfigDo.getModel())
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
            Flux<NodeOutput> stream = qqSystemGraph.stream(input, invokeConfig)
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
                updatedConfig = qqSystemGraph.updateState(invokeConfig, map, userCurrentNodeMap.get(event.getUserId()));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            // 创建恢复配置，添加 HUMAN_FEEDBACK_METADATA_KEY
            RunnableConfig resumeConfig = RunnableConfig.builder(updatedConfig)
                    .addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, "placeholder")
                    .build();
            // 继续执行 Graph（input 为 null，使用之前的状态）
            qqSystemGraph.stream(null,resumeConfig)
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
