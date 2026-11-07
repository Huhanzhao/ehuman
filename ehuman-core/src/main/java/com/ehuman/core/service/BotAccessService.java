package com.ehuman.core.service;

import com.ehuman.dao.service.TbBotOwnerDoService;
import com.ehuman.dao.service.TbBotWhitelistDoService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class BotAccessService {

    private final TbBotOwnerDoService tbBotOwnerDoService;

    private final TbBotWhitelistDoService tbBotWhitelistDoService;

    public Long getOwnerId() {
        return tbBotOwnerDoService.getOrClaimOwner();
    }

    public boolean isOwner(Long userId) {
        Long ownerId = getOwnerId();
        log.info("ownerId :{}", ownerId);
        return ownerId != null && ownerId.equals(userId);
    }

    public boolean isWhitelisted(Long userId) {
        return userId != null && tbBotWhitelistDoService.existsByUserId(userId);
    }

    /**
     * 白名单用户（且不是主人）复用主人的 API 配置
     */
    public boolean isSharedKeyUser(Long userId) {
        return userId != null && !isOwner(userId) && isWhitelisted(userId);
    }

    public String contactOwnerMessage() {
        Long ownerId = getOwnerId();
        return "请联系主人 qq" + (ownerId == null ? "" : ownerId) + "添加白名单";
    }

    public String noPermissionMessage() {
        Long ownerId = getOwnerId();
        return "无权限，请联系主人 qq" + (ownerId == null ? "" : ownerId) + "修改配置";
    }
}
