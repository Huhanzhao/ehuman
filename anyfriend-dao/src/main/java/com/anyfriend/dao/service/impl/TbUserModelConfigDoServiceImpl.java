package com.anyfriend.dao.service.impl;

import com.anyfriend.dao.enums.ChatModelCategoryEnum;
import com.anyfriend.dao.enums.LlmTypeEnum;
import com.anyfriend.dao.mapper.TbUserModelConfigDoMapper;
import com.baomidou.mybatisplus.extension.conditions.query.LambdaQueryChainWrapper;
import com.baomidou.mybatisplus.extension.conditions.update.LambdaUpdateChainWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.anyfriend.dao.entity.TbUserModelConfigDo;
import com.anyfriend.dao.service.TbUserModelConfigDoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.anyfriend.dao.enums.LlmTypeEnum.*;

/**
* @author Lenovo
* @description 针对表【tb_user_model_config】的数据库操作Service实现
* @createDate 2026-02-11 21:33:53
*/
@Service
public class TbUserModelConfigDoServiceImpl extends ServiceImpl<TbUserModelConfigDoMapper, TbUserModelConfigDo>
    implements TbUserModelConfigDoService{

    @Override
    public TbUserModelConfigDo getByUserIdAndNodeNum(Long userId, LlmTypeEnum llmTypeEnum) {
        return lambdaQuery()
                .eq(TbUserModelConfigDo::getUserId, userId)
                .eq(TbUserModelConfigDo::getLlmTypeEnum, llmTypeEnum)
                .one();
    }

    @Override
    public List<TbUserModelConfigDo> getByUserId(Long userId) {
        return lambdaQuery()
                .isNotNull(TbUserModelConfigDo::getLlmTypeEnum)
                .eq(TbUserModelConfigDo::getUserId, userId)
                .list();
    }

    @Override
    public List<TbUserModelConfigDo> getByUserIdNodeNumNotZero(Long userId) {
        return lambdaQuery()
                .eq(TbUserModelConfigDo::getUserId, userId)
                .ne(TbUserModelConfigDo::getLlmTypeEnum,DISABLE)
                .orderByAsc(TbUserModelConfigDo::getLlmTypeEnum)
                .list();
    }

    @Override
    public void clearNodeNumByUserIdAndNodeNum(Long userId,  LlmTypeEnum llmTypeEnum) {
        lambdaUpdate()
                .set(TbUserModelConfigDo::getLlmTypeEnum, null)
                .eq(TbUserModelConfigDo::getUserId, userId)
                .eq(TbUserModelConfigDo::getLlmTypeEnum, llmTypeEnum)
                .update();
    }

    @Override
    public Page<TbUserModelConfigDo> getByUserIdPage(Long userId, int pageNum, int pageSize) {
        Page<TbUserModelConfigDo> page = new Page<>(pageNum, pageSize);
        return lambdaQuery()
                .eq(TbUserModelConfigDo::getLlmTypeEnum,DISABLE)
                .eq(TbUserModelConfigDo::getUserId, userId)
                .page(page);
    }

    @Override
    public List<TbUserModelConfigDo> getDisableApiConfigByCategory(Long userId, ChatModelCategoryEnum categoryEnum) {
        LambdaQueryChainWrapper<TbUserModelConfigDo> eq = lambdaQuery()
                .eq(TbUserModelConfigDo::getUserId,userId)
                .eq(TbUserModelConfigDo::getLlmTypeEnum,DISABLE);
        if (categoryEnum != null){
            eq.eq(TbUserModelConfigDo::getCategory, categoryEnum.getCategory());
        }else{
            eq.ne(TbUserModelConfigDo::getLlmTypeEnum,IMAGE_ANALYSIS).ne(TbUserModelConfigDo::getLlmTypeEnum,TTS);
        }

        return eq.list();
    }

    @Override
    @Transactional
    public void changeApiModelById(Long userId, TbUserModelConfigDo tbUserModelConfigDo,ChatModelCategoryEnum chatModelCategoryEnum) {
        LambdaUpdateChainWrapper<TbUserModelConfigDo> eq =
                lambdaUpdate().set(TbUserModelConfigDo::getLlmTypeEnum, DISABLE)
                        .eq(TbUserModelConfigDo::getLlmTypeEnum, tbUserModelConfigDo.getLlmTypeEnum())
                .eq(TbUserModelConfigDo::getUserId, userId);
        // 设置所有 nodeNum为0
        if (chatModelCategoryEnum == null){
            eq.ne(TbUserModelConfigDo::getLlmTypeEnum,IMAGE_ANALYSIS).ne(TbUserModelConfigDo::getLlmTypeEnum,TTS);
        }else{
            eq.eq(TbUserModelConfigDo::getCategory, chatModelCategoryEnum.getCategory());
        }
        eq.update();

        // 更新do
        lambdaUpdate().eq(TbUserModelConfigDo::getId, tbUserModelConfigDo.getId()).update(tbUserModelConfigDo);
    }
}




