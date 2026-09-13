package com.anyfriend.core.job.action;

import com.anyfriend.core.bean.ChatClientFactory;
import com.anyfriend.core.node.LlmNode;
import com.anyfriend.core.utils.SendMessageUtil;
import com.anyfriend.dao.entity.TbBotLifeDetailDo;
import com.anyfriend.dao.enums.LlmChatEnum;
import com.anyfriend.dao.enums.LlmTypeEnum;
import com.anyfriend.dao.service.TbAiChatMemoryService;
import com.anyfriend.dao.service.TbBotLifeDetailDoService;
import com.anyfriend.dao.service.TbBotPromptDoService;
import com.anyfriend.dao.service.TbUserBotStatusDoService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class BotLifeDetailCreateJob implements Job {

    @Autowired
    private TbBotLifeDetailDoService tbBotLifeDetailDoService;

    @Autowired
    private ChatClientFactory chatClientFactory;

    @Autowired
    private SendMessageUtil sendMessageUtil;

    @Autowired
    private TbBotPromptDoService tbBotPromptDoService;

    @Autowired
    private TbUserBotStatusDoService tbUserBotStatusDoService;

    @Autowired
    private TbAiChatMemoryService tbAiChatMemoryService;

    @Override
    public void execute(JobExecutionContext jobExecutionContext) {
        Long botId = Long.valueOf(System.getProperty("BOT_ID"));
        LocalDate today = LocalDate.now();
        // 使用userId来获取chatClient
        Long userId = tbUserBotStatusDoService.list().getFirst().getUserId();

        // 判断昨天是否有用户消息 有就生成今天的 没有就不生成了
        boolean existsUserYesterdayMessage = tbAiChatMemoryService.existsUserYesterdayMessage(userId);
        if(!existsUserYesterdayMessage){
            // 昨天没有消息 不用生成生活细节，直接拷贝最近一天的生活细节
            boolean copySuccess = tbBotLifeDetailDoService.copyLastDayDetail(botId);
            if (copySuccess){
                sendMessageUtil.sendMegToSelf("生活细节拷贝成功");
                log.info("生活细节拷贝成功");
                return;
            }
        }

        // 生成今天的日期字符串
        String todayStr = today.getYear() + "年" + today.getMonthValue() + "月" + today.getDayOfMonth() + "日";
        String weekDay = getWeekDay(today.getDayOfWeek().getValue() - 1);

        // 调用AI生成今天的生活细节
        LlmNode lifeDetailGenerateNode = new LlmNode(LlmChatEnum.LIFE_DETAIL_GENERATE, LlmTypeEnum.AUXILIARY, chatClientFactory);
        // 获取bot角色设定
        String botRole = tbBotPromptDoService.getRolePrompt();
        if(botRole == null){
            throw new RuntimeException("人设为空");
        }
        try {
            String content = lifeDetailGenerateNode.invoke(userId,Map.of("todayStr",todayStr,
                    "weekDay",weekDay,
                    "botRole",botRole));
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
