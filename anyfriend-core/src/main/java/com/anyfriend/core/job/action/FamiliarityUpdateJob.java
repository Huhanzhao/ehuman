package com.anyfriend.core.job.action;

import com.anyfriend.core.utils.SendMessageUtil;
import com.anyfriend.dao.entity.TbAiChatMemoryDo;
import com.anyfriend.dao.entity.TbUserBotStatusDo;
import com.anyfriend.dao.service.TbAiChatMemoryService;
import com.anyfriend.dao.service.TbUserBotStatusDoService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class FamiliarityUpdateJob implements Job {

    @Autowired
    private TbUserBotStatusDoService tbUserBotStatusDoService;

    @Autowired
    private TbAiChatMemoryService tbAiChatMemoryService;

    @Autowired
    private SendMessageUtil sendMessageUtil;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        List<TbUserBotStatusDo> allStatus = tbUserBotStatusDoService.list();
        if (allStatus == null || allStatus.isEmpty()) {
            return;
        }

        LocalDate yesterday = LocalDate.now().minusDays(1);
        Date yesterdayStart = Date.from(yesterday.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date yesterdayEnd = Date.from(yesterday.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

        for (TbUserBotStatusDo status : allStatus) {
            Long userId = status.getUserId();
            Integer currentFamiliarity = status.getFamiliarity();
            if (currentFamiliarity == null) {
                currentFamiliarity = 0;
            }

            // 查询该用户昨天的所有消息
            List<TbAiChatMemoryDo> messages = tbAiChatMemoryService.list(
                    Wrappers.<TbAiChatMemoryDo>lambdaQuery()
                            .eq(TbAiChatMemoryDo::getMemoryId, userId)
                            .ge(TbAiChatMemoryDo::getCreateTime, yesterdayStart)
                            .lt(TbAiChatMemoryDo::getCreateTime, yesterdayEnd)
            );

            int newFamiliarity;
            if (messages == null || messages.isEmpty()) {
                // 昨天没有消息，亲密度-1
                newFamiliarity = currentFamiliarity - 1;
                log.info("用户{}亲密度更新: {} -> {} (昨日无互动，-1)", userId, currentFamiliarity, newFamiliarity);
            } else {
                // 检查是否同时有用户消息和AI消息
                Set<String> roles = messages.stream()
                        .map(message -> message.getRole().getRole())
                        .collect(Collectors.toSet());
                boolean hasUserMsg = roles.contains("user");
                boolean hasAiMsg = roles.contains("assistant");

                if (hasUserMsg && hasAiMsg) {
                    // 有正常聊天，亲密度+2
                    newFamiliarity = currentFamiliarity + 2;
                    log.info("用户{}亲密度更新: {} -> {} (昨日有互动，+2)", userId, currentFamiliarity, newFamiliarity);
                } else {
                    // 有消息但不构成正常聊天（只有单方面），不调整
                    log.info("用户{}亲密度不变: {} (昨日消息不完整，role={})", userId, currentFamiliarity, roles);
                    continue;
                }
            }

            newFamiliarity = Math.max(0, Math.min(200, newFamiliarity));
            tbUserBotStatusDoService.updateFamiliarityByUserId(userId, newFamiliarity);
        }

        log.info("亲密度定时任务执行完毕");
        try {
            sendMessageUtil.sendMegToSelf("亲密度定时任务执行完毕");
        } catch (Exception e) {
            log.error("发送消息失败", e);
        }
    }
}
