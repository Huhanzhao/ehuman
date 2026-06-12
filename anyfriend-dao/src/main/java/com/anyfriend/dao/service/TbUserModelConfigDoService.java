package com.anyfriend.dao.service;

import com.anyfriend.dao.entity.TbUserModelConfigDo;
import com.anyfriend.dao.enums.NodeEnum;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

/**
* @author Lenovo
* @description 针对表【tb_user_model_config】的数据库操作Service
* @createDate 2026-02-11 21:33:53
*/
public interface TbUserModelConfigDoService extends IService<TbUserModelConfigDo> {

    List<TbUserModelConfigDo> getModelList(TbUserModelConfigDo modelConfigDo);

    TbUserModelConfigDo getByUserIdAndNodeNum(Long userId, NodeEnum nodeEnum);

    List<TbUserModelConfigDo> getByUserId(Long userId);

    List<TbUserModelConfigDo> getByUserIdNodeNumNotZero(Long userId);

    void clearNodeNumByUserIdAndNodeNum(Long userId,  NodeEnum nodeEnum);

    Page<TbUserModelConfigDo> getByUserIdPage(Long userId, int pageNum, int pageSize);
}
