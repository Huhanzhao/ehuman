package com.helper.services.plugin;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.helper.services.entity.TbAiChatMemoryDo;
import com.helper.services.entity.TbChatConfigDo;
import com.helper.services.entity.TbUserGraceDo;
import com.helper.services.service.TbAiChatMemoryService;
import com.helper.services.service.TbChatConfigDoService;
import com.helper.services.service.TbUserGraceDoService;
import com.mikuac.shiro.annotation.MessageHandlerFilter;
import com.mikuac.shiro.annotation.PrivateMessageHandler;
import com.mikuac.shiro.annotation.common.Order;
import com.mikuac.shiro.annotation.common.Shiro;
import com.mikuac.shiro.common.utils.MsgUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.BotPlugin;
import com.mikuac.shiro.dto.event.message.PrivateMessageEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;

@Component
@Shiro
@RequiredArgsConstructor
@Slf4j
public class SystemPlugin extends BotPlugin {

    private final TbAiChatMemoryService tbAiChatMemoryService;

    private final TbChatConfigDoService tbChatConfigDoService;

    private final TbUserGraceDoService tbUserGraceDoService;

    @PrivateMessageHandler
    @MessageHandlerFilter(cmd = "#.*")
    public void onPrivateMessages(Bot bot , PrivateMessageEvent event){
        String userInput = event.getMessage();
        if (userInput.startsWith("#删除")){
            userInput = userInput.replace("#删除", "");
            tbAiChatMemoryService.remove(Wrappers.<TbAiChatMemoryDo>lambdaQuery()
                    .eq(TbAiChatMemoryDo::getRole,"assistant")
                    .eq(TbAiChatMemoryDo::getMemoryId,event.getUserId())
                    .like(TbAiChatMemoryDo::getMessage,userInput)
            );
        }else if (userInput.startsWith("#对话配置")){
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
        }else if (userInput.startsWith("#指令")){
            String msg = MsgUtils.builder().text("#删除+模糊词\n#对话配置\n#好感度").build();
            bot.sendPrivateMsg(event.getUserId(),msg,false);
        } else if (userInput.startsWith("#好感度")) {
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
        }
    }
}
