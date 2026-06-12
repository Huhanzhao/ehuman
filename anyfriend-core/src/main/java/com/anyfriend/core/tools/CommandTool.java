package com.anyfriend.core.tools;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Slf4j
@Component
public class CommandTool {


    @Tool(description = "在 Windows 命令行执行安全命令，返回执行结果")
    public String execCmd(String command) {
        log.info("执行了cmd命令：{}",command);
        StringBuilder result = new StringBuilder();

        try {
            ProcessBuilder builder = new ProcessBuilder("cmd", "/c", "\"" +  "chcp 65001 >nul && " + command + "\"");
            builder.redirectErrorStream(true);

            Process process = builder.start();

            InputStreamReader inputStreamReader;
            if (command.contains("tencent")){
                inputStreamReader = new InputStreamReader(process.getInputStream());
            }else{
                inputStreamReader = new InputStreamReader(process.getInputStream(), "GBK");
            }

            BufferedReader reader = new BufferedReader(
                    inputStreamReader
            );

            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line).append("\n");
            }
            log.info("执行cmd命令结果：================================={}=========================================",result);

            process.waitFor();

        } catch (Exception e) {
            e.printStackTrace();
            return "执行异常：" + e.getMessage();
        }

        return result.toString();
    }
}
