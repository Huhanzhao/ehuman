package com.ehuman.dao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ehuman.dao.entity.TbBotOwnerDo;
import com.ehuman.dao.entity.TbUserModelConfigDo;
import com.ehuman.dao.mapper.TbBotOwnerDoMapper;
import com.ehuman.dao.service.TbBotOwnerDoService;
import com.ehuman.dao.service.TbUserModelConfigDoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TbBotOwnerDoServiceImpl extends ServiceImpl<TbBotOwnerDoMapper, TbBotOwnerDo>
        implements TbBotOwnerDoService {

    private static final Long OWNER_ROW_ID = 1L;

    @Autowired
    private TbUserModelConfigDoService tbUserModelConfigDoService;

    @Override
    public synchronized Long getOrClaimOwner() {
        TbBotOwnerDo owner = getById(OWNER_ROW_ID);
        if (owner != null) {
            return owner.getUserId();
        }

        // 第一个绑定 apiKey 的账号即主人
        TbUserModelConfigDo first = tbUserModelConfigDoService.lambdaQuery()
                .isNotNull(TbUserModelConfigDo::getApiKey)
                .ne(TbUserModelConfigDo::getApiKey, "")
                .orderByAsc(TbUserModelConfigDo::getCreateTime)
                .orderByAsc(TbUserModelConfigDo::getId)
                .last("limit 1")
                .one();
        if (first == null || first.getUserId() == null) {
            return null;
        }

        owner = new TbBotOwnerDo().setId(OWNER_ROW_ID).setUserId(first.getUserId());
        save(owner);
        return owner.getUserId();
    }
}
