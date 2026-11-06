package com.ehuman.dao.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.ehuman.dao.entity.TbBotWhitelistDo;
import com.ehuman.dao.mapper.TbBotWhitelistDoMapper;
import com.ehuman.dao.service.TbBotWhitelistDoService;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TbBotWhitelistDoServiceImpl extends ServiceImpl<TbBotWhitelistDoMapper, TbBotWhitelistDo>
        implements TbBotWhitelistDoService {

    @Override
    public boolean existsByUserId(Long userId) {
        return lambdaQuery().eq(TbBotWhitelistDo::getUserId, userId).exists();
    }

    @Override
    public TbBotWhitelistDo getByUserId(Long userId) {
        return lambdaQuery().eq(TbBotWhitelistDo::getUserId, userId).one();
    }

    @Override
    public List<TbBotWhitelistDo> listAll() {
        return lambdaQuery().orderByAsc(TbBotWhitelistDo::getCreateTime).list();
    }

    @Override
    public void removeByUserId(Long userId) {
        lambdaUpdate().eq(TbBotWhitelistDo::getUserId, userId).remove();
    }

    @Override
    public synchronized TbBotWhitelistDo saveIfAbsent(Long userId, String nickname) {
        TbBotWhitelistDo existing = getByUserId(userId);
        if (existing != null) {
            return existing;
        }
        TbBotWhitelistDo whitelist = new TbBotWhitelistDo().setUserId(userId).setNickname(nickname);
        save(whitelist);
        return whitelist;
    }
}
