package com.ehuman.core.job.action;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.ehuman.core.bean.SystemConfigBean;
import com.ehuman.core.node.LlmNode;
import com.ehuman.core.service.CommonAuxiliaryService;
import com.ehuman.core.utils.SendMessageUtil;
import com.ehuman.dao.entity.TbAiChatMemoryDo;
import com.ehuman.dao.entity.TbUpdateGraceHistoryDo;
import com.ehuman.dao.entity.TbUserBotStatusDo;
import com.ehuman.dao.entity.TbUserInfoDo;
import com.ehuman.dao.enums.LlmChatEnum;
import com.ehuman.dao.enums.LlmTypeEnum;
import com.ehuman.dao.service.TbAiChatMemoryService;
import com.ehuman.dao.service.TbUpdateGraceHistoryDoService;
import com.ehuman.dao.service.TbUserBotStatusDoService;
import com.ehuman.dao.service.TbUserInfoDoService;
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
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class UserInfoAndGraceUpdateJob implements Job {

    @Autowired
    private TbUserBotStatusDoService tbUserBotStatusDoService;

    @Autowired
    private TbUpdateGraceHistoryDoService tbUpdateGraceHistoryDoService;

    @Autowired
    private TbUserInfoDoService tbUserInfoDoService;

    @Autowired
    private TbAiChatMemoryService tbAiChatMemoryService;

    @Autowired
    private SystemConfigBean systemConfigBean;

    @Autowired
    private SendMessageUtil sendMessageUtil;

    @Autowired
    private CommonAuxiliaryService commonAuxiliaryService;



    @Override
    public void execute(JobExecutionContext jobExecutionContext) throws JobExecutionException {
        // 查询所有有好感度记录的用
        List<TbUserBotStatusDo> graceList = tbUserBotStatusDoService.list();
        if (graceList == null || graceList.isEmpty()) {
            return;
        }

        LocalDate yesterday  = LocalDate.now().minusDays(1);
        Date yesterdayStart = Date.from(yesterday.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date yesterdayEnd = Date.from(yesterday.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

        for (TbUserBotStatusDo graceDo : graceList) {
            Long userId = graceDo.getUserId();
            Integer currentGrace = graceDo.getGrace();

            // 查询该用户昨天的所有消息
            List<TbAiChatMemoryDo> todayMessages = tbAiChatMemoryService.list(
                    Wrappers.<TbAiChatMemoryDo>lambdaQuery()
                            .eq(TbAiChatMemoryDo::getMemoryId, userId)
                            .ge(TbAiChatMemoryDo::getCreateTime, yesterdayStart)
                            .lt(TbAiChatMemoryDo::getCreateTime, yesterdayEnd)
                            .orderByAsc(TbAiChatMemoryDo::getCreateTime)
            );

            int delta;
            String reason;

            if (todayMessages == null || todayMessages.isEmpty()) {
                // 没有消息，好感度逐渐趋于0
                if (currentGrace > 0) {
                    delta = -1;
                    reason = "昨日无互动，好感度-1";
                } else if (currentGrace < 0) {
                    delta = 1;
                    reason = "昨日无互动，好感度+1";
                } else {
                    continue;
                }
            } else {
                // 有消息，用AI评分、提取用户画像
                List<TbUserInfoDo> existingInfoList = tbUserInfoDoService.getUserInfoByUserId(userId);
                String existingInfo = (existingInfoList != null && !existingInfoList.isEmpty())
                        ? existingInfoList.getFirst().getUserInfo() : null;

                AiEvaluation evaluation = evaluateByAi(userId, todayMessages, existingInfo);
                delta = evaluation.delta();
                reason = evaluation.reason();

                // 最后如果不足二十条，直接取最后二十条做向量化
                commonAuxiliaryService.summaryMessages(userId,1);

                // 保存或更新用户画像
                if (evaluation.userInfo() != null && !evaluation.userInfo().isBlank()) {
                    saveOrUpdateUserInfo(userId, evaluation.userInfo());
                }
            }

            // 更新好感度，限制在-100~100范围内
            int newGrace = Math.max(-100, Math.min(100, currentGrace + delta));
            tbUserBotStatusDoService.updateGraceByUserId(userId, newGrace);

            // 记录好感度修改历史
            TbUpdateGraceHistoryDo history = new TbUpdateGraceHistoryDo();
            history.setUserId(userId);
            if (delta >= 0) {
                history.setIncrease(delta);
                history.setDecrease(0);
            } else {
                history.setIncrease(0);
                history.setDecrease(Math.abs(delta));
            }
            history.setReason(reason);
            tbUpdateGraceHistoryDoService.save(history);

            log.info("用户{}好感度更新: {} -> {} (delta={})", userId, currentGrace, newGrace, delta);
        }
        try{
            sendMessageUtil.sendMegToSelf("好感度定时任务执行完毕");
        }catch(Exception e){
            log.error("发送消息失败", e);
        }
    }

    private AiEvaluation evaluateByAi(Long userId, List<TbAiChatMemoryDo> messages, String existingUserInfo) {
        // 拼接聊天记录
        StringBuilder chatLog = new StringBuilder();
        for (TbAiChatMemoryDo msg : messages) {
            chatLog.append(msg.getRole()).append(": ").append(msg.getMessage()).append("\n");
        }
        String userInfoSection = (existingUserInfo != null && !existingUserInfo.isBlank())
                ? "\n用户已有的画像信息：\n" + existingUserInfo
                : "\n该用户暂无画像信息。";
        LlmNode llmNode = new LlmNode(LlmChatEnum.USER_WANT_TO, LlmTypeEnum.AUXILIARY, systemConfigBean);
        String content = llmNode.invoke(userId, Map.of("userInfoSection",userInfoSection,"chatLog",chatLog.toString()));
        log.info("AI评估结果: {}", content);
        return parseEvaluation(content);
    }

    private AiEvaluation parseEvaluation(String content) {
        if (content == null || content.isBlank()) {
            return new AiEvaluation(0, "AI返回为空", null);
        }

        // 提取评分
        int delta = 0;
        Pattern scorePattern = Pattern.compile("评分[:：]\\s*(-?\\d+)");
        Matcher scoreMatcher = scorePattern.matcher(content);
        if (scoreMatcher.find()) {
            delta = Math.max(-5, Math.min(5, Integer.parseInt(scoreMatcher.group(1))));
        }

        // 提取原因
        String reason = "AI评估昨日聊天";
        Pattern reasonPattern = Pattern.compile("原因[:：]\\s*(.+)");
        Matcher reasonMatcher = reasonPattern.matcher(content);
        if (reasonMatcher.find()) {
            reason = reasonMatcher.group(1).trim();
        }

        // 提取画像
        String userInfo = null;
        Pattern userInfoPattern = Pattern.compile("画像[:：]\\s*(.+)");
        Matcher userInfoMatcher = userInfoPattern.matcher(content);
        if (userInfoMatcher.find()) {
            userInfo = userInfoMatcher.group(1).trim();
        }

        return new AiEvaluation(delta, reason, userInfo);
    }

    private void saveOrUpdateUserInfo(Long userId, String newInfo) {
        List<TbUserInfoDo> existingList = tbUserInfoDoService.getUserInfoByUserId(userId);
        if (existingList != null && !existingList.isEmpty()) {
            // 已有记录，更新
            TbUserInfoDo existing = existingList.getFirst();
            existing.setUserInfo(newInfo);
            tbUserInfoDoService.updateById(existing);
        } else {
            // 新建记录
            TbUserInfoDo userInfoDo = new TbUserInfoDo();
            userInfoDo.setUserId(userId);
            userInfoDo.setUserInfo(newInfo);
            tbUserInfoDoService.save(userInfoDo);
        }
    }

    private record AiEvaluation(int delta, String reason, String userInfo) {}
}
