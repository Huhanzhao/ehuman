package com.bot.service.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.bot.service.entity.TbUserModelConfigDo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author Lenovo
* @description 针对表【tb_user_model_config】的数据库操作Service
* @createDate 2026-02-11 21:33:53
*/
public interface TbUserModelConfigDoService extends IService<TbUserModelConfigDo> {

    List<TbUserModelConfigDo> getModelList(TbUserModelConfigDo modelConfigDo);

    TbUserModelConfigDo getByUserIdAndNodeNum(Long userId, Integer nodeNum);

    List<TbUserModelConfigDo> getByUserId(Long userId);

    List<TbUserModelConfigDo> getByUserIdNodeNumNotZero(Long userId);

    void clearNodeNumByUserIdAndNodeNum(Long userId, Integer nodeNum);

    Page<TbUserModelConfigDo> getByUserIdPage(Long userId, int pageNum, int pageSize);
}
