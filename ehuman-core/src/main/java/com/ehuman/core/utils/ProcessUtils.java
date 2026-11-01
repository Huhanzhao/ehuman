package com.ehuman.core.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.InputStreamReader;

@Slf4j
public class ProcessUtils {

    /**
     * 1. 判断指定名称的进程是否正在运行 (适用于 Windows)
     * @param processName 进程名 (例如 "NapCatInstaller.exe")
     * @return true 如果存在
     */
    public static boolean isProcessRunning(String processName) {
        try {
            Process process = Runtime.getRuntime().exec(
                    "tasklist /FI \"IMAGENAME eq " + processName + "\"");

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "GBK"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (line.contains(processName)) {
                        return true;
                    }
                }
            }
            process.waitFor();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * 2. 结束指定名称的进程（如果进程不存在，不会报错）
     * @param processName 进程名 (例如 "NapCatInstaller.exe")
     * @return true 代表执行了杀进程命令，false 代表执行异常
     */
    public static boolean killProcess(String processName) {
        // 安全防御：防止传入空字符串把系统进程都杀了
        if (processName == null || processName.trim().isEmpty()) {
            return false;
        }

        try {
            // 使用 /F 强制结束，/IM 按名字匹配，/T 结束该进程及其子进程树
            Process process = Runtime.getRuntime().exec("taskkill /F /IM " + processName + " /T");

            // 必须消费掉输出流，否则可能导致进程阻塞
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "GBK"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // 如果需要打印出具体的结束日志，可以在这里 System.out.println(line);
                }
            }

            int exitCode = process.waitFor();
            // exitCode 为 0 代表成功杀掉了，为 128 代表进程没找到（说明已经不在运行了）
            if (exitCode == 0) {
                log.info("成功结束进程: {}", processName);
                return true;
            }
            return false;
        } catch (Exception e) {
            log.error("结束进程 {} 时发生异常: {}", processName, e.getMessage());
            return false;
        }
    }

    /**
     * 通过命令行关键字，精准强杀隐藏的进程（专门用来杀 VBS 拉起的隐藏 cmd）
     * @param keyword 命令行关键字（比如 "napcat.bat"）
     */
    public static void killProcessByCommandLine(String keyword) {
        try {
            // 使用 WMIC 命令查找命令行包含指定关键字的进程
            // 注意：WMIC 的语法中，% 需要转义为 %% (如果是在 bat 中)，在 Java 中直接传 %
            String command = "wmic process where \"commandline like '%" + keyword + "%'\" call terminate";

            Process process = Runtime.getRuntime().exec(command);

            // 必须读取输出流，防止进程卡死
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "GBK"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    // 如果你想看具体杀了谁，可以在这里打印 line
                }
            }
            process.waitFor();
            log.info("已尝试清理包含关键字 [{}] 的后台进程", keyword);

        } catch (Exception e) {
            log.info("精准清理进程失败: {}", e.getMessage());
        }
    }

    /**
     * 精准杀掉由 NapCat 启动的 QQ.exe 进程（不会误杀用户自己的 QQ）
     */
    public static void killNapCatQQ() {
        try {
            String psCommand = "powershell -NoProfile -WindowStyle Hidden -Command \"" +
                    "Get-CimInstance Win32_Process -Filter \\\"Name = 'QQ.exe'\\\" | " +
                    "Where-Object { (Get-Process -Id $_.ParentProcessId -ErrorAction SilentlyContinue).Name -eq 'NapCatWinBootMain' } | " +
                    "ForEach-Object { Stop-Process -Id $_.ProcessId -Force }\"";

            Process process = Runtime.getRuntime().exec(psCommand);

            // 消费掉输出流，防止阻塞
            try (java.io.BufferedReader reader = new java.io.BufferedReader(
                    new java.io.InputStreamReader(process.getInputStream(), "GBK"))) {
                while (reader.readLine() != null) {}
            }
            process.waitFor();
            log.info("已精准清理 NapCat 启动的 QQ 进程");

        } catch (Exception e) {
            log.info("精准清理 NapCat QQ 进程失败: {}", e.getMessage());
        }
    }

    public static void main(String[] args) {
        ProcessUtils.killNapCatQQ();

        if(ProcessUtils.isProcessRunning("NapCatWinBootMain.exe")){
            ProcessUtils.killProcess("NapCatWinBootMain.exe");
        }
        if(ProcessUtils.isProcessRunning("NapCatInstaller.exe")){
            ProcessUtils.killProcess("NapCatInstaller.exe");
        }
        if(ProcessUtils.isProcessRunning("ehuman-1.0.0.exe")){
            ProcessUtils.killProcess("ehuman-1.0.0.exe");
        }

        ProcessUtils.killProcessByCommandLine("napcat.bat");
        ProcessUtils.killProcessByCommandLine("napcat.quick.bat");

    }
}