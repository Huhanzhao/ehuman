package com.helper.services.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.helper.services.entity.TbUserModelConfigDo;
import com.helper.services.service.TbUserModelConfigDoService;
import com.helper.services.mapper.TbUserModelConfigDoMapper;
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
                .eq(modelConfigDo.getNodeNum() != null,TbUserModelConfigDo::getNodeNum,modelConfigDo.getNodeNum())
                .eq(modelConfigDo.getId() != null,TbUserModelConfigDo::getId,modelConfigDo.getId())
                .eq(modelConfigDo.getApiKey() != null,TbUserModelConfigDo::getApiKey,modelConfigDo.getApiKey())
                .eq(modelConfigDo.getBaseUrl() != null,TbUserModelConfigDo::getBaseUrl,modelConfigDo.getBaseUrl())
                .list();
    }

    @Override
    public TbUserModelConfigDo getByUserIdAndNodeNum(Long userId, Integer nodeNum) {
        return lambdaQuery()
                .eq(TbUserModelConfigDo::getUserId, userId)
                .eq(TbUserModelConfigDo::getNodeNum, nodeNum)
                .one();
    }

    @Override
    public List<TbUserModelConfigDo> getByUserId(Long userId) {
        return lambdaQuery()
                .eq(TbUserModelConfigDo::getUserId, userId)
                .list();
    }

    @Override
    public List<TbUserModelConfigDo> getByUserIdNodeNumNotZero(Long userId) {
        return lambdaQuery()
                .eq(TbUserModelConfigDo::getUserId, userId)
                .ne(TbUserModelConfigDo::getNodeNum, 0)
                .orderByAsc(TbUserModelConfigDo::getNodeNum)
                .list();
    }

    @Override
    public void clearNodeNumByUserIdAndNodeNum(Long userId, Integer nodeNum) {
        lambdaUpdate()
                .set(TbUserModelConfigDo::getNodeNum, 0)
                .eq(TbUserModelConfigDo::getUserId, userId)
                .eq(TbUserModelConfigDo::getNodeNum, nodeNum)
                .update();
    }
}




