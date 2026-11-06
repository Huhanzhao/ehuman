package com.ehuman.dao.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.ehuman.dao.entity.TbBotWhitelistDo;

import java.util.List;

public interface TbBotWhitelistDoService extends IService<TbBotWhitelistDo> {

    boolean existsByUserId(Long userId);

    TbBotWhitelistDo getByUserId(Long userId);

    List<TbBotWhitelistDo> listAll();

    void removeByUserId(Long userId);

    TbBotWhitelistDo saveIfAbsent(Long userId, String nickname);
}
