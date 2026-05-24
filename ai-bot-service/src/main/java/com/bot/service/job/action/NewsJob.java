package com.bot.service.job.action;

import com.bot.service.bean.ChatClientFactory;
import com.bot.service.enums.QQbotIdEnum;
import com.bot.service.tools.CommandTool;
import com.mikuac.shiro.core.Bot;
import com.mikuac.shiro.core.BotContainer;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class NewsJob implements Job {

    @Autowired
    private CommandTool commandTool;

    @Autowired
    private BotContainer botContainer;

    @Autowired
    private ChatClientFactory chatClientFactory;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        System.out.println("Quartz任务执行: " + LocalDateTime.now());
        String news = commandTool.execCmd("tencent-news-cli hot");
        Bot bot = botContainer.robots.get(QQbotIdEnum.DEFAULT.getId());
        CommandTool commandTool = new CommandTool();
        String content = commandTool.execCmd("tencent-news-cli hot ––limit 10");
        String[] contentSplit = content.split("\n");
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < contentSplit.length; i++) {
            if (!(contentSplit[i].contains("摘要:")
                    || contentSplit[i].contains("来源:")
                    || contentSplit[i].contains("发布时间:")
//                    || contentSplit[i].contains("链接:")
                    || contentSplit[i].contains("条热点新闻")
            )){
                sb.append(contentSplit[i].replace("标题：","")).append("\n");
            }
        }

        bot.sendPrivateMsg(2752662938L, sb.toString().trim(),false);
    }
}