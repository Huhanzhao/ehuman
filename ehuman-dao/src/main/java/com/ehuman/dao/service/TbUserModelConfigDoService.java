package com.ehuman.dao.service;

import com.ehuman.dao.entity.TbUserModelConfigDo;
import com.ehuman.dao.enums.ChatModelCategoryEnum;
import com.ehuman.dao.enums.LlmTypeEnum;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author Lenovo
* @description 针对表【tb_user_model_config】的数据库操作Service
* @createDate 2026-02-11 21:33:53
*/
public interface TbUserModelConfigDoService extends IService<TbUserModelConfigDo> {

    TbUserModelConfigDo getByUserIdAndNodeNum(Long userId, LlmTypeEnum llmTypeEnum);

    List<TbUserModelConfigDo> getByUserId(Long userId);

    List<TbUserModelConfigDo> getByUserIdNodeNumNotZero(Long userId);

    void clearNodeNumByUserIdAndNodeNum(Long userId,  List<LlmTypeEnum> llmTypeEnum);

    Page<TbUserModelConfigDo> getByUserIdPage(Long userId, int pageNum, int pageSize);

    List<TbUserModelConfigDo> getDisableApiConfigByCategory(Long userId, ChatModelCategoryEnum categoryEnum);

    void changeApiModelById(Long userId,TbUserModelConfigDo tbUserModelConfigDo,ChatModelCategoryEnum chatModelCategoryEnum);
}
