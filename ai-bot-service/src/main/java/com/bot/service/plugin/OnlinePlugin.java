package com.bot.service.plugin;

import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.CoreEvent;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

@Primary
@Component
public class OnlinePlugin extends CoreEvent {

    @Override
    public void online(Bot bot) {
        // 客户端上线事件
        // 如需获取上线的机器人账号可以调用
        bot.sendPrivateMsg(bot.getSelfId(), "我上线啦～", false);
        System.setProperty("BOT_ID", bot.getSelfId() + "");
    }

    @Override
    public void offline(long account) {
        // 客户端离线事件
        System.clearProperty("BOT_ID");
    }

    @Override
    public boolean session(WebSocketSession session) {
        // 可以通过 session.getHandshakeHeaders().getFirst("x-self-id") 获取上线的机器人账号
        // 例如当服务端为开放服务时，并且只有白名单内的账号才允许连接，此时可以检查账号是否存在于白名内
        // 不存在的话返回 false 即可禁止连接
        return true;
    }

}