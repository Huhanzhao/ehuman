package com.anyfriend.core.job.action;

import com.anyfriend.core.bean.ChatClientFactory;
import com.anyfriend.core.job.JobManager;
import com.anyfriend.core.utils.SendMessageUtil;
import com.anyfriend.dao.entity.TbBotLifeDetailDo;
import com.anyfriend.dao.entity.TbUserMessageSummaryDo;
import com.anyfriend.dao.enums.NodeEnum;
import com.anyfriend.dao.service.TbBotLifeDetailDoService;
import com.anyfriend.dao.service.TbBotPromptDoService;
import com.anyfriend.dao.service.TbUserBotStatusDoService;
import com.anyfriend.dao.service.TbUserMessageSummaryDoService;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class BotLifeDetailCreateJob implements Job {

    @Autowired
    private TbBotLifeDetailDoService tbBotLifeDetailDoService;

    @Autowired
    private TbUserMessageSummaryDoService tbUserMessageSummaryDoService;

    @Autowired
    private ChatClientFactory chatClientFactory;

    @Autowired
    private JobManager jobManager;

    @Autowired
    private SendMessageUtil sendMessageUtil;

    @Autowired
    private TbBotPromptDoService tbBotPromptDoService;

    @Autowired
    private TbUserBotStatusDoService tbUserBotStatusDoService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        Long botId = Long.valueOf(System.getProperty("BOT_ID"));
        // 查询近三天的聊天记录总结
        LocalDate today = LocalDate.now();
        Date threeDaysAgo = Date.from(today.minusDays(3).atStartOfDay(ZoneId.systemDefault()).toInstant());
        List<TbUserMessageSummaryDo> summaries = tbUserMessageSummaryDoService.list(
                Wrappers.<TbUserMessageSummaryDo>lambdaQuery()
                        .ge(TbUserMessageSummaryDo::getCreateTime, threeDaysAgo)
                        .orderByDesc(TbUserMessageSummaryDo::getCreateTime)
        );

        StringBuilder summaryText = new StringBuilder();
        if (summaries != null && !summaries.isEmpty()) {
            SimpleDateFormat sdf = new SimpleDateFormat("MM月dd日");
            for (TbUserMessageSummaryDo s : summaries) {
                String date = s.getCreateTime() != null ? sdf.format(s.getCreateTime()) : "未知日期";
                summaryText.append(date).append(": ").append(s.getSummary()).append("\n");
            }
        } else {
            summaryText.append("暂无近期聊天记录。");
        }

        // 生成今天的日期字符串
        String todayStr = today.getYear() + "年" + today.getMonthValue() + "月" + today.getDayOfMonth() + "日";
        String weekDay = getWeekDay(today.getDayOfWeek().getValue() - 1);

        // 获取bot角色设定
        String botRole = tbBotPromptDoService.getRolePrompt();

        // 调用AI生成今天的生活细节
        String prompt = "你是一个虚拟生活规划师。请为以下角色（情感陪伴机器人）生成今天（" + todayStr + " " + weekDay + "）的生活安排。\n\n" +
                "## 角色信息\n" +
                botRole +
                "\n\n" +
                "## 近三天的聊天记录总结\n" + summaryText +
                "\n## 要求\n" +
                "1. 生成一整天24小时的生活细节，从00:00到24:00\n" +
                "2. 用户与机器人是异地，只生成机器人自己的生活细节，不要涉及到与用户的互动，也不要生成类似发照片，语音，电话，消息或者等待用户消息类似的情节\n" +
                "3. 每个时间段要具体、真实、贴近角色的日常生活 （比如吃饭 睡觉 玩手机 和朋友聊天等）\n" +
                "4. 根据近期聊天内容适当融入相关情节（比如聊到过的话题、用户提到的事情等）\n" +
                "5. 时间段要细致划分，一天至少15-20条记录，覆盖全天\n" +
                "6. 每条描述只说做了什么，10字以内\n\n" +
                "## 输出格式\n" +
                "严格按以下格式输出，每行一条，不要输出其他内容：\n" +
                "HH:mm-HH:mm: 生活细节描述\n" +
                "HH:mm-HH:mm: 生活细节描述\n" +
                "...\n\n" +
                "示例：\n" +
                "00:00-07:30: 在宿舍睡觉，玩了会手机\n" +
                "07:30-08:00: 起床洗漱\n" +
                "08:00-08:30: 和室友去食堂吃早餐";

        try {
            // 使用userId来获取chatClient
            Long userId = tbUserBotStatusDoService.list().getFirst().getUserId();
            ChatClient chatClient = chatClientFactory.createChatClient(userId, NodeEnum.AUXILIARY);
            String content = chatClient.prompt(prompt).call().content();
            log.info("AI生成的生活细节:\n{}", content);

            // 解析并保存
            List<TbBotLifeDetailDo> details = parseLifeDetails(botId, today, content);
            if (!details.isEmpty()) {
                tbBotLifeDetailDoService.saveBatch(details);
                log.info("已保存{}条生活细节", details.size());
            }
        } catch (Exception e) {
            log.error("生成生活细节失败", e);
        }
        try{
            sendMessageUtil.sendMegToSelf("生活细节定时任务执行完毕");
        }catch(Exception e){
            log.error("发送消息失败", e);
        }
    }

    private List<TbBotLifeDetailDo> parseLifeDetails(Long botId, LocalDate date, String content) {
        List<TbBotLifeDetailDo> list = new ArrayList<>();
        if (content == null || content.isBlank()) {
            return list;
        }

        // 匹配 HH:mm-HH:mm: 描述 格式
        Pattern pattern = Pattern.compile("(\\d{1,2}:\\d{2})\\s*[-~—]\\s*(\\d{1,2}:\\d{2})\\s*[:：]\\s*(.+)");
        String[] lines = content.split("\n");

        for (String line : lines) {
            Matcher matcher = pattern.matcher(line.trim());
            if (matcher.find()) {
                String startStr = matcher.group(1);
                String endStr = matcher.group(2);
                String detail = matcher.group(3).trim();

                Date startTime = parseTime(date, startStr);
                Date endTime = parseTime(date, endStr);

                if (startTime != null && endTime != null) {
                    TbBotLifeDetailDo detailDo = new TbBotLifeDetailDo();
                    detailDo.setBotId(botId);
                    detailDo.setStartTime(startTime);
                    detailDo.setEndTime(endTime);
                    detailDo.setDetail(detail);
                    list.add(detailDo);
                }
            }
        }
        return list;
    }

    private Date parseTime(LocalDate date, String timeStr) {
        try {
            String[] parts = timeStr.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            return Date.from(date.atTime(hour, minute).atZone(ZoneId.systemDefault()).toInstant());
        } catch (Exception e) {
            log.warn("解析时间失败: {}", timeStr);
            return null;
        }
    }

    private String getWeekDay(int dayOfWeek) {
        return switch (dayOfWeek) {
            case 1 -> "星期一";
            case 2 -> "星期二";
            case 3 -> "星期三";
            case 4 -> "星期四";
            case 5 -> "星期五";
            case 6 -> "星期六";
            case 7 -> "星期日";
            default -> "";
        };
    }
}
