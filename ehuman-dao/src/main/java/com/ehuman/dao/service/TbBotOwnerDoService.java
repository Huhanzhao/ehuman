package com.ehuman.dao.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ehuman.dao.entity.TbBotOwnerDo;

public interface TbBotOwnerDoService extends IService<TbBotOwnerDo> {

    /**
     * 获取主人 userId；不存在时取最早绑定 apiKey 的账号并持久化。
     * 没有任何可用配置时返回 null。
     */
    Long getOrClaimOwner();
}
