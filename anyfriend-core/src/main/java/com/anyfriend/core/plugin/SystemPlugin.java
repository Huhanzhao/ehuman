package com.anyfriend.core.plugin;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.NodeOutput;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.anyfriend.core.bean.SystemGraph;
import com.anyfriend.core.utils.SendMessageUtil;
import com.anyfriend.dao.entity.TbBotPromptDo;
import com.anyfriend.dao.entity.TbUserBotStatusDo;
import com.anyfriend.dao.entity.TbUserModelConfigDo;
import com.anyfriend.dao.entity.TbUserReferAudioDo;
import com.anyfriend.dao.enums.NodeEnum;
import com.anyfriend.dao.service.TbBotPromptDoService;
import com.anyfriend.dao.service.TbUserBotStatusDoService;
import com.anyfriend.dao.service.TbUserModelConfigDoService;
import com.anyfriend.dao.service.TbUserReferAudioDoService;
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
    private TbUserBotStatusDoService tbUserBotStatusDoService;

    @Autowired
    private TbUserModelConfigDoService tbUserModelConfigDoService;

    @Autowired
    private TbUserReferAudioDoService tbUserReferAudioDoService;

    @Autowired
    private SystemGraph systemGraph;

    @Autowired
    private SendMessageUtil sendMessageUtil;

    @Autowired
    private TbBotPromptDoService tbBotPromptDoService;

    // 保存是否有运行中的图
    private final ConcurrentHashMap<Long, Disposable> runningDisposables = new ConcurrentHashMap<>();

    // 保存图当前执行到的节点
    private final ConcurrentHashMap<Long, String> userCurrentNodeMap = new ConcurrentHashMap<>();

    // 保存当前执行图的线程id
    private final ConcurrentHashMap<Long, String> userThreadIdMap = new ConcurrentHashMap<>();

    // 保存当前执行中的图
    private final ConcurrentHashMap<Long, CompiledGraph> userGraphMap = new ConcurrentHashMap<>();

    private CompiledGraph apiConfigGraph;

    private CompiledGraph changeModeGraph;

    private CompiledGraph referAudioConfig;

    private CompiledGraph switchReferAudioGraph;

    private CompiledGraph createRoleGraph;

    @PostConstruct
    public void init() {
        this.apiConfigGraph = systemGraph.getApiConfigGraph();
        this.changeModeGraph = systemGraph.getChangeModelCompiledGraph();
        this.referAudioConfig = systemGraph.getReferAudioConfigGraph();
        this.switchReferAudioGraph = systemGraph.getSwitchReferAudioGraph();
        this.createRoleGraph = systemGraph.getCreateRoleGraph();
    }


    @PrivateMessageHandler
    @MessageHandlerFilter
    @Override
    public int onPrivateMessage(Bot bot , PrivateMessageEvent event){
        String userInput = event.getMessage().trim();
        Long userId = event.getUserId();

        if (userInput.equals("#exit")){
            runningDisposables.get(userId).dispose();
            clearFlag(userId);
            sendMessageUtil.sendSystemMsg(userId,"已退出当前配置");
            return MESSAGE_BLOCK;
        }

        // 如果当前有中断的图 继续执行api配置引导图
        if (runningDisposables.containsKey(event.getUserId())){
            return resumeGraph(userId, userInput,userGraphMap.get(userId));
        }

        // 主菜单
        if (userInput.trim().equals("#")){
            return userInstruction(userId);
        }

        // bot状态查询和修改
        if (userInput.startsWith("#好感度")) {
            return getOrUpdateBotStatus(userId, userInput);
        }

        // 查询闲置api配置
        if (userInput.equals("#查询未启用api")){
            return getApiConfig(userId, userInput);
        }

        // 已启用api查询
        if (userInput.equals("#查询已启用api")){
            return getNodeEnableModel(userId);
        }

        // api引导
        if (userInput.equals("#开始配置api")){
            return startNewGraph(userId,apiConfigGraph);
        }

        // 节点大模型切换引导
        if (userInput.equals("#切换api")){
            return startNewGraph(userId, changeModeGraph);
        }

        // 声音音频导入引导
        if (userInput.equals("#配置参考音频")){
            return startNewGraph(userId,referAudioConfig);
        }

        // 查询未启动参考音频
        if(userInput.equals("#查询未启用参考音频")){
            return getReferAudioByUserId(userId,false);
        }

        // 查询已启动参考音频
        if(userInput.equals("#查询已启用参考音频")){
            return getReferAudioByUserId(userId,true);
        }

        // 切换参考音频
        if (userInput.startsWith("#切换参考音频")){
            return startNewGraph(userId,switchReferAudioGraph);
        }


        //  人设查询或切换 todo 调试
        if(userInput.startsWith("#人设")){
            return getOrSwitchRole(userId,userInput);
        }

        //  添加人设
        if(userInput.equals("#添加人设")){
            return startNewGraph(userId, createRoleGraph);
        }

        // 导入表情包引导
        if (userInput.equals("#导入表情包")){
            sendMessageUtil.sendSystemMsg(userId,"图片解析节点配置完毕之后随着聊天收集，也可以在emoji文件夹下面自行管理");
            return MESSAGE_BLOCK;
        }

        // todo 加载配置文件  通过启动来加载
        // todo 语音没有配置的时候生成文字 改提示词

        // 以后再做
        // todo 我的画像查询
        // todo 角色创建引导
        // todo 支持作者

        // 非法命令提示
        if (userInput.startsWith("#")){
            sendMessageUtil.sendSystemMsg(userId,"无效命令，输入\"#\" 查询当前可用命令");
            return MESSAGE_BLOCK;
        }
        return MESSAGE_IGNORE;
    }


    private int getOrSwitchRole(Long userId, String userInput) {
        String roleName = userInput.replace("#人设", "").trim();
        List<TbBotPromptDo> promptDos = tbBotPromptDoService.list();

        if (roleName.isEmpty()) {
            // 查询所有人设
            StringBuilder msg = new StringBuilder();
            TbBotPromptDo currentRole = null;
            StringBuilder optionalRoles = new StringBuilder();

            for (TbBotPromptDo promptDo : promptDos) {
                if (promptDo.getPromptTypeEnum().getType() == 0) {
                    currentRole = promptDo;
                } else {
                    optionalRoles.append(promptDo.getName()).append("\n");
                }
            }

            if (currentRole != null) {
                msg.append("当前人设：").append(currentRole.getName());
            } else {
                msg.append("当前人设：未设置");
            }

            if (optionalRoles.length() > 0) {
                msg.append("\n\n可选人设：\n").append(optionalRoles);
                msg.append("\n切换格式：#人设 人设名称");
            }

            sendMessageUtil.sendSystemMsg(userId, msg.toString().trim());
            return MESSAGE_BLOCK;
        }

        // 切换人设 - 根据名称查找
        TbBotPromptDo targetRole = null;
        for (TbBotPromptDo promptDo : promptDos) {
            if (roleName.equals(promptDo.getName())) {
                targetRole = promptDo;
                break;
            }
        }

        if (targetRole == null) {
            sendMessageUtil.sendSystemMsg(userId, "未找到人设：" + roleName);
            return MESSAGE_BLOCK;
        }

        // 找到当前人设并更新
        TbBotPromptDo currentRole = null;
        for (TbBotPromptDo promptDo : promptDos) {
            if (promptDo.getPromptTypeEnum().getType() == 0) {
                currentRole = promptDo;
                break;
            }
        }

        if (currentRole != null) {
            // 将目标人设的内容更新到当前人设
            currentRole.setContent(targetRole.getContent());
            currentRole.setName(targetRole.getName());
            tbBotPromptDoService.updateById(currentRole);
            sendMessageUtil.sendSystemMsg(userId, "已切换人设为：" + targetRole.getName());
        } else {
            sendMessageUtil.sendSystemMsg(userId, "未找到当前人设配置，无法切换");
        }

        return MESSAGE_BLOCK;
    }

    private int getReferAudioByUserId(Long userId, boolean enbale) {
        List<TbUserReferAudioDo> audioDos = tbUserReferAudioDoService.getReferAudioByUserId(userId, enbale);
        StringBuilder msg = new StringBuilder();
        if (audioDos.isEmpty()){
            msg.append("没有查询到符合条件的参考音频");
        }
        audioDos.forEach(param -> {
            msg.append("音频名:")
                    .append(param.getReferAudioName()).append("\n音频url:")
                    .append(param.getReferAudioUrl()).append("\n音频类型:")
                    .append(param.getType()).append("\n\n");
        });
        sendMessageUtil.sendSystemMsg(userId,msg.toString().trim());
        return MESSAGE_BLOCK;
    }

    // 继续执行图
    private int resumeGraph(Long userId, String userInput, CompiledGraph compiledGraph) {
        if (userInput.startsWith("#")){
            sendMessageUtil.sendSystemMsg(userId, "当前有未完成的引导，输入 #exit 退出引导后重试");
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
            updatedConfig = compiledGraph.updateState(invokeConfig, map, userCurrentNodeMap.get(userId));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // 创建恢复配置，添加 HUMAN_FEEDBACK_METADATA_KEY
        RunnableConfig resumeConfig = RunnableConfig.builder(updatedConfig)
                .addMetadata(RunnableConfig.HUMAN_FEEDBACK_METADATA_KEY, "placeholder")
                .build();
        doCompiledGraph(userId,compiledGraph,null,resumeConfig);
        return MESSAGE_BLOCK;
    }

    // 开始执行一个新图
    private int startNewGraph(Long userId, CompiledGraph compiledGraph) {
        String threadId = userThreadIdMap.get(userId);
        if (!userThreadIdMap.containsKey(userId)){
            threadId = UUID.randomUUID().toString();
            userThreadIdMap.put(userId, threadId);
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

        doCompiledGraph(userId,compiledGraph,input,invokeConfig);
        return MESSAGE_BLOCK;
    }

    private void doCompiledGraph(Long userId,CompiledGraph compiledGraph, Map<String,Object> input,RunnableConfig runnableConfig){
        // 执行图并获取流
        Flux<NodeOutput> stream = compiledGraph.stream(input, runnableConfig)
                .subscribeOn(Schedulers.boundedElastic()); // 异步 执行

        // 标记当前执行的图
        userGraphMap.put(userId,compiledGraph);
        runningDisposables.put(userId,() -> {});  // 先放一个空

        // 异步处理每个输出
        var disposable = stream.subscribe(
                output -> {
                    log.info("当前迭代节点: " + output);
                    userCurrentNodeMap.put(userId, output.node());
                    if (output.isEND()){
                        clearFlag(userId);
                    }
                },
                error -> {
                    log.error("流错误: " + error);
                    sendMessageUtil.sendSystemMsg(userId,"系统错误已退出当前引导");
                    clearFlag(userId);
                },
                () -> log.info("流完成")
        );

        if (runningDisposables.containsKey(userId)) {
            runningDisposables.put(userId, disposable);
            log.info("标记会话");
        }
    }

    private void clearFlag(Long userId) {
        // 去除当前图标记
        runningDisposables.remove(userId);
        userCurrentNodeMap.remove(userId);
        userThreadIdMap.remove(userId);
        userGraphMap.remove(userId);
    }

    private int getNodeEnableModel(Long userId) {
        List<TbUserModelConfigDo> list = tbUserModelConfigDoService.getByUserIdNodeNumNotZero(userId);
        StringBuilder msg = new StringBuilder();
        if (list.isEmpty()){
            msg.append("无");
        }
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

    private int userInstruction(Long userId) {
        String msg = MsgUtils.builder()
                .text("#声音 [模型名] - 查看/切换声音模型\n" +
                        "#好感度 [数值] - 查看/修改好感度\n" +
                        "#模型查询 - 查看已配置模型\n" +
                        "#api查询 - 查看API配置\n" +
                        "#开始配置api - 配置API引导")
                .build();
        sendMessageUtil.sendSystemMsg(userId,msg);
        return MESSAGE_BLOCK;
    }
}
