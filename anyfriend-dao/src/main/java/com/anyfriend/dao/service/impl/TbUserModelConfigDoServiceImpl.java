package com.anyfriend.dao.service.impl;

import com.anyfriend.dao.enums.ChatModelCategoryEnum;
import com.anyfriend.dao.enums.NodeEnum;
import com.anyfriend.dao.mapper.TbUserModelConfigDoMapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anyfriend.dao.entity.TbUserModelConfigDo;
import com.anyfriend.dao.service.TbUserModelConfigDoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.anyfriend.dao.enums.NodeEnum.*;

/**
* @author Lenovo
* @description 针对表【tb_user_model_config】的数据库操作Service实现
* @createDate 2026-02-11 21:33:53
*/
@Service
public class TbUserModelConfigDoServiceImpl extends ServiceImpl<TbUserModelConfigDoMapper, TbUserModelConfigDo>
    implements TbUserModelConfigDoService{

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
                .ne(TbUserModelConfigDo::getNodeEnum,DISABLE)
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
                .eq(TbUserModelConfigDo::getNodeEnum,DISABLE)
                .eq(TbUserModelConfigDo::getUserId, userId)
                .page(page);
    }

    @Override
    public List<TbUserModelConfigDo> getDisableApiConfigByCategory(Long userId, ChatModelCategoryEnum categoryEnum) {
        LambdaQueryChainWrapper<TbUserModelConfigDo> eq = lambdaQuery()
                .eq(TbUserModelConfigDo::getUserId,userId)
                .eq(TbUserModelConfigDo::getNodeEnum,DISABLE);
        if (categoryEnum != null){
            eq.eq(TbUserModelConfigDo::getCategory, categoryEnum.getCategory());
        }else{
            eq.ne(TbUserModelConfigDo::getNodeEnum,IMAGE_ANALYSIS).ne(TbUserModelConfigDo::getNodeEnum,TTS);
        }

        return eq.list();
    }

    @Override
    @Transactional
    public void changeApiModelById(Long userId, TbUserModelConfigDo tbUserModelConfigDo,ChatModelCategoryEnum chatModelCategoryEnum) {
        // 设置所有 nodeNum为0
        lambdaUpdate().set(TbUserModelConfigDo::getNodeEnum,DISABLE)
                .eq(TbUserModelConfigDo::getUserId,userId)
                .eq(TbUserModelConfigDo::getCategory,chatModelCategoryEnum.getCategory()).update();

        // 更新do
        lambdaUpdate().update(tbUserModelConfigDo);
    }
}




