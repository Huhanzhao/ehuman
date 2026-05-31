package com.bot.service.enums;

import com.mikuac.shiro.enums.ActionPath;
import lombok.Getter;

@Getter
public enum CustomActionPath implements ActionPath {

    // 定义 Endpoint
    FORWARD_MSG("forward_friend_single_msg"),
    GET_FRIEND_MSG_HISTORY("get_friend_msg_history");

    private final String path;

    CustomActionPath(String path) {
        this.path = path;
    }

    @Override
    public String getPath() {
        return this.path;
    }
}
