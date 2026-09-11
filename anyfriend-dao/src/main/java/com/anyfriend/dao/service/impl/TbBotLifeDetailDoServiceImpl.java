package com.anyfriend.dao.service.impl;

import com.anyfriend.dao.entity.TbBotLifeDetailDo;
import com.anyfriend.dao.mapper.TbBotLifeDetailDoMapper;
import com.anyfriend.dao.service.TbBotLifeDetailDoService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

/**
* @author Lenovo
* @description 针对表【tb_bot_life_detail】的数据库操作Service实现
* @createDate 2026-05-11 21:56:55
*/
@Service
public class TbBotLifeDetailDoServiceImpl extends ServiceImpl<TbBotLifeDetailDoMapper, TbBotLifeDetailDo>
    implements TbBotLifeDetailDoService{

    @Override
    public String getCurrentActivity(Long botId) {
        // 查询bot当前正在做的事
        Date now = new Date();
        TbBotLifeDetailDo currentLife = this.getOne(
                Wrappers.<TbBotLifeDetailDo>lambdaQuery()
                        .le(TbBotLifeDetailDo::getStartTime, now)
                        .eq(TbBotLifeDetailDo::getBotId,botId)
                        .ge(TbBotLifeDetailDo::getEndTime, now)
                        .last("limit 1")
        );
        return currentLife != null ? currentLife.getDetail() : "";
    }

    @Override
    public boolean copyLastDayDetail(Long botId) {
        // 获取最后一条细节 时间倒序
        TbBotLifeDetailDo lastOne = lambdaQuery()
                .eq(TbBotLifeDetailDo::getBotId, botId)
                .orderByDesc(TbBotLifeDetailDo::getCreateTime).last("limit 1")
                .one();

        if (lastOne == null){
            return false;
        }

        Date createTime = lastOne.getCreateTime();

        // 将 Date 转换为 LocalDate（只取年月日）
        LocalDate date = createTime.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDate();

        LocalDate today = LocalDate.now();
        // 相差天数（date 比 today 多几天）
        long days = ChronoUnit.DAYS.between(date, today);

        // 当天开始：00:00:00
        LocalDateTime startOfDay = date.atStartOfDay();
        // 当天结束：23:59:59.999999999
        LocalDateTime endOfDay = date.atTime(LocalTime.MAX);

        List<TbBotLifeDetailDo> list  = lambdaQuery()
                .eq(TbBotLifeDetailDo::getBotId, botId)
                .between(TbBotLifeDetailDo::getCreateTime, startOfDay, endOfDay)
                .list();

        // 清空 ID 和时间字段，让数据库自动生成或让填充器生效
        list.forEach(item -> {
            item.setId(null);           // 清空 ID，让数据库自增
            item.setCreateTime(null);   // 清空创建时间，触发 insertFill
            item.setUpdateTime(null);   // 清空修改时间，触发 insertFill
            item.setStartTime(Date.from(item.getStartTime().toInstant().plus(days,ChronoUnit.DAYS)));
            item.setEndTime(Date.from(item.getEndTime().toInstant().plus(days,ChronoUnit.DAYS)));
        });

        // 批量复制
        this.saveBatch(list);
        return true;
    }
}




