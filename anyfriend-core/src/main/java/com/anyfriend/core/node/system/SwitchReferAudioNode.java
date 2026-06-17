package com.anyfriend.core.node.system;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.RunnableConfig;
import com.alibaba.cloud.ai.graph.action.InterruptionMetadata;
import com.anyfriend.core.utils.SendMessageUtil;
import com.anyfriend.dao.entity.TbUserModelConfigDo;
import com.anyfriend.dao.entity.TbUserReferAudioDo;
import com.anyfriend.dao.enums.ChatModelCategoryEnum;
import com.anyfriend.dao.enums.NodeEnum;
import com.anyfriend.dao.service.TbUserModelConfigDoService;
import com.anyfriend.dao.service.TbUserReferAudioDoService;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

import static com.anyfriend.dao.enums.ChatModelCategoryEnum.IMAGE;
import static com.anyfriend.dao.enums.ChatModelCategoryEnum.TTS;

/**
 * @Author: huhanzhao
 * @CreateTime: 2026-06-16
 * @Description: 更新节点的apiconfig
 */
@Data
@Slf4j
@Component
public class SwitchReferAudioNode extends SystemInterruptionNode{

    @Autowired
    private SendMessageUtil sendMessageUtil;

    private String nodeName = "switchReferAudioNode";

    @Autowired
    private TbUserReferAudioDoService tbUserReferAudioDoService;

    @Override
    public Optional<InterruptionMetadata> interrupt(String nodeId, OverAllState state, RunnableConfig config) {
        log.info("进入了{}节点中断方法",nodeName);
        // 检查是否需要中断
        String userInput = state.value("userInput",String.class).orElse("").trim();
        Long userId = Long.valueOf(state.value("userId", String.class).orElse(""));
        String type = state.value("type", String.class).orElse("");
        InterruptionMetadata interruption = null;
        List<TbUserReferAudioDo> referAudioDos = tbUserReferAudioDoService.getDisableReferAudioByUserIdAndType(userId, type);
        if (referAudioDos.isEmpty()){
            sendMessageUtil.sendSystemMsg(userId,"没有可切换的参考音频，已退出引导");
            return Optional.empty();
        }
        if (userInput.isEmpty()) {
            // 中断等待用户输入
            StringBuilder userMessageBuilder = new StringBuilder("当前可切换的api配置\n");
            for (int i = 0; i < referAudioDos.size(); i++) {
                TbUserReferAudioDo tbUserReferAudioDo = referAudioDos.get(i);
                userMessageBuilder.append(i + 1).append(" . 参考音频文件名:")
                        .append(tbUserReferAudioDo.getReferAudioName()).append("\n")
                        .append("\n\n");
            }
            sendMessageUtil.sendSystemMsg(userId, userMessageBuilder.toString().trim());
            // 返回 InterruptionMetadata 来中断执行
            interruption = InterruptionMetadata.builder(nodeId, state)
                    .addMetadata("message", nodeName + "节点中断")
                    .addMetadata("node", nodeId)
                    .build();
            log.info("{}节点中断",nodeName);
            return Optional.of(interruption);
        }

        // 判断用户输入是否合法
        int index = 0;
        try {
            index = Long.valueOf(userInput).intValue();
        }catch(Exception exception){
            log.error("非法输入");
            index = -1;
        }

        if (index <= 0 || index > referAudioDos.size()){
            sendMessageUtil.sendSystemMsg(userId, "请按照要求输入");
            // 返回 InterruptionMetadata 来中断执行
            interruption = InterruptionMetadata.builder(nodeId, state)
                    .addMetadata("message", nodeName + "节点中断")
                    .addMetadata("node", nodeId)
                    .build();
            return Optional.of(interruption);
        }

        // 保存
        TbUserReferAudioDo tbUserReferAudioDo = referAudioDos.get(index - 1);
        tbUserReferAudioDo.setEnable(true);
        tbUserReferAudioDoService.changeReferAudio(userId,tbUserReferAudioDo);
        // 3. 切换完毕
        sendMessageUtil.sendSystemMsg(userId,"更新成功");
        return Optional.empty();
    }
}
