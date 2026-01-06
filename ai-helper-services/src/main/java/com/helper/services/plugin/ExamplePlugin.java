package com.helper.services.plugin;

import com.mikuac.shiro.common.utils.MsgUtils;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.BotPlugin;
import com.mikuac.shiro.dto.event.message.GroupMessageEvent;
import com.mikuac.shiro.dto.event.message.PrivateMessageEvent;
import org.springframework.stereotype.Component;

@Component
public class ExamplePlugin extends BotPlugin {

//    @Override
//    public int onPrivateMessage(Bot bot, PrivateMessageEvent event) {
//        if ("你好".equals(event.getMessage())) {
//            // 构建消息
//            String sendMsg = MsgUtils.builder()
//                    .face(66)
//                    .text("hello, this is shiro example plugin.")
//                    .build();
//            // 发送私聊消息
//            bot.sendPrivateMsg(event.getUserId(), sendMsg, false);
//        }
//        // 返回 MESSAGE_IGNORE 执行 plugin-list 下一个插件，返回 MESSAGE_BLOCK 则不执行下一个插件
//        return MESSAGE_IGNORE;
//    }
//
//    @Override
//    public int onGroupMessage(Bot bot, GroupMessageEvent event) {
//        if ("hi".equals(event.getMessage())) {
//            // 构建消息
//            String sendMsg = MsgUtils.builder()
//                    .at(event.getUserId())
//                    .face(66)
//                    .text("hello, this is shiro example plugin.")
//                    .build();
//            // 发送群消息
//            bot.sendGroupMsg(event.getGroupId(), sendMsg, false);
//        }
//        // 返回 MESSAGE_IGNORE 执行 plugin-list 下一个插件，返回 MESSAGE_BLOCK 则不执行下一个插件
//        return MESSAGE_IGNORE;
//    }

}
