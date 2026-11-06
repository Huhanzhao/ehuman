package com.ehuman.core.service;

import com.ehuman.dao.entity.TbChatConfigDo;
import com.ehuman.dao.entity.TbUserModelConfigDo;
import com.ehuman.dao.entity.TbUserReferAudioDo;
import com.ehuman.dao.enums.LlmTypeEnum;
import com.ehuman.dao.service.TbChatConfigDoService;
import com.ehuman.dao.service.TbUserModelConfigDoService;
import com.ehuman.dao.service.TbUserReferAudioDoService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

/**
 * 运行时配置解析：本人配置优先，白名单用户回退到主人配置。
 * 仅用于调用大模型等运行路径，配置管理命令仍使用用户本人的配置。
 */
@Service
@RequiredArgsConstructor
public class EffectiveConfigResolver {

    private final TbUserModelConfigDoService tbUserModelConfigDoService;

    private final TbChatConfigDoService tbChatConfigDoService;

    private final TbUserReferAudioDoService tbUserReferAudioDoService;

    private final BotAccessService botAccessService;

    public TbUserModelConfigDo getModelConfig(Long userId, LlmTypeEnum llmTypeEnum) {
        TbUserModelConfigDo own = tbUserModelConfigDoService.getByUserIdAndNodeNum(userId, llmTypeEnum);
        if (own != null) {
            return own;
        }
        Long ownerId = sharedOwnerId(userId);
        return ownerId == null ? null : tbUserModelConfigDoService.getByUserIdAndNodeNum(ownerId, llmTypeEnum);
    }

    public TbUserModelConfigDo getFallbackChatModelConfig(Long userId) {
        TbUserModelConfigDo own = pickChatModel(tbUserModelConfigDoService.getByUserId(userId));
        if (own != null) {
            return own;
        }
        Long ownerId = sharedOwnerId(userId);
        return ownerId == null ? null : pickChatModel(tbUserModelConfigDoService.getByUserId(ownerId));
    }

    public List<TbUserModelConfigDo> getUsableModelConfigs(Long userId) {
        List<TbUserModelConfigDo> own = tbUserModelConfigDoService.getByUserId(userId);
        if (!own.isEmpty()) {
            return own;
        }
        Long ownerId = sharedOwnerId(userId);
        return ownerId == null ? own : tbUserModelConfigDoService.getByUserId(ownerId);
    }

    public TbChatConfigDo getChatConfig(Long userId) {
        TbChatConfigDo own = tbChatConfigDoService.getByUserId(userId);
        if (own != null) {
            return own;
        }
        Long ownerId = sharedOwnerId(userId);
        return ownerId == null ? null : tbChatConfigDoService.getByUserId(ownerId);
    }

    public List<TbUserReferAudioDo> getEnabledReferAudio(Long userId) {
        List<TbUserReferAudioDo> own = tbUserReferAudioDoService.getReferAudioByUserId(userId, true);
        if (own != null && !own.isEmpty()) {
            return own;
        }
        Long ownerId = sharedOwnerId(userId);
        if (ownerId == null) {
            return own == null ? Collections.emptyList() : own;
        }
        List<TbUserReferAudioDo> ownerList = tbUserReferAudioDoService.getReferAudioByUserId(ownerId, true);
        return ownerList == null ? Collections.emptyList() : ownerList;
    }

    private Long sharedOwnerId(Long userId) {
        if (!botAccessService.isSharedKeyUser(userId)) {
            return null;
        }
        return botAccessService.getOwnerId();
    }

    private TbUserModelConfigDo pickChatModel(List<TbUserModelConfigDo> configs) {
        if (configs == null) {
            return null;
        }
        return configs.stream()
                .filter(e -> e.getLlmTypeEnum().equals(LlmTypeEnum.MASTER)
                        || e.getLlmTypeEnum().equals(LlmTypeEnum.AUXILIARY))
                .findFirst()
                .orElse(null);
    }
}
