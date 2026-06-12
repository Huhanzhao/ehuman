package com.anyfriend.core.job.action;

import com.anyfriend.core.tools.CommandTool;
import com.anyfriend.core.utils.SendMessageUtil;
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
    private SendMessageUtil sendMessageUtil;

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        System.out.println("Quartz任务执行: " + LocalDateTime.now());
        String news = commandTool.execCmd("tencent-news-cli hot");
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

//        sendMessageUtil.sendChatMsg(2752662938L, sb.toString().trim());
    }
}