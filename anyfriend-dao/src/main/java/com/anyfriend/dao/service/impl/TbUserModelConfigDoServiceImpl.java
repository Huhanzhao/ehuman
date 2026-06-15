package com.anyfriend.dao.service.impl;

import com.anyfriend.dao.enums.NodeEnum;
import com.anyfriend.dao.mapper.TbUserModelConfigDoMapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anyfriend.dao.entity.TbUserModelConfigDo;
import com.anyfriend.dao.service.TbUserModelConfigDoService;
import org.springframework.stereotype.Service;

import java.util.List;

/**
* @author Lenovo
* @description 针对表【tb_user_model_config】的数据库操作Service实现
* @createDate 2026-02-11 21:33:53
*/
@Service
public class TbUserModelConfigDoServiceImpl extends ServiceImpl<TbUserModelConfigDoMapper, TbUserModelConfigDo>
    implements TbUserModelConfigDoService{

    @Override
    public List<TbUserModelConfigDo> getModelList(TbUserModelConfigDo modelConfigDo) {
        return lambdaQuery()
                .eq(modelConfigDo.getUserId() != null,TbUserModelConfigDo::getUserId,modelConfigDo.getUserId())
                .eq(modelConfigDo.getModel() != null,TbUserModelConfigDo::getModel,modelConfigDo.getModel())
                .eq(modelConfigDo.getCategory() != null,TbUserModelConfigDo::getCategory,modelConfigDo.getCategory())
                .eq(modelConfigDo.getNeedProxy() != null,TbUserModelConfigDo::getNeedProxy,modelConfigDo.getNeedProxy())
                .eq(modelConfigDo.getNodeEnum() != null,TbUserModelConfigDo::getNodeEnum,modelConfigDo.getNodeEnum())
                .eq(modelConfigDo.getId() != null,TbUserModelConfigDo::getId,modelConfigDo.getId())
                .eq(modelConfigDo.getApiKey() != null,TbUserModelConfigDo::getApiKey,modelConfigDo.getApiKey())
                .eq(modelConfigDo.getBaseUrl() != null,TbUserModelConfigDo::getBaseUrl,modelConfigDo.getBaseUrl())
                .list();
    }

    @Override
    public TbUserModelConfigDo getByUserIdAndNodeNum(Long userId, NodeEnum nodeEnum) {
        return lambdaQuery()
                .eq(TbUserModelConfigDo::getUserId, userId)
                .eq(TbUserModelConfigDo::getNodeEnum, nodeEnum)
                .one();
    }

    @Override
    public List<TbUserModelConfigDo> getByUserId(Long userId) {
        return lambdaQuery()
                .isNotNull(TbUserModelConfigDo::getNodeEnum)
                .eq(TbUserModelConfigDo::getUserId, userId)
                .list();
    }

    @Override
    public List<TbUserModelConfigDo> getByUserIdNodeNumNotZero(Long userId) {
        return lambdaQuery()
                .eq(TbUserModelConfigDo::getUserId, userId)
                .isNotNull(TbUserModelConfigDo::getNodeEnum)
                .orderByAsc(TbUserModelConfigDo::getNodeEnum)
                .list();
    }

    @Override
    public void clearNodeNumByUserIdAndNodeNum(Long userId,  NodeEnum nodeEnum) {
        lambdaUpdate()
                .set(TbUserModelConfigDo::getNodeEnum, null)
                .eq(TbUserModelConfigDo::getUserId, userId)
                .eq(TbUserModelConfigDo::getNodeEnum,  nodeEnum)
                .update();
    }

    @Override
    public Page<TbUserModelConfigDo> getByUserIdPage(Long userId, int pageNum, int pageSize) {
        Page<TbUserModelConfigDo> page = new Page<>(pageNum, pageSize);
        return lambdaQuery()
                .eq(TbUserModelConfigDo::getUserId, userId)
                .page(page);
    }
}




