package com.ehuman.core.utils;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.concurrent.TimeUnit;

@Slf4j
public class ProcessUtils {

    /**
     * 1. 判断指定名称的进程是否正在运行 (适用于 Windows)
     * @param processName 进程名 (例如 "xxx.exe")
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
     * @param processName 进程名 (例如 "xxx.exe")
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
     * 杀掉指定 SnowLuma 目录下启动的进程（node.exe 及其 launcher cmd），
     * 通过可执行文件路径 / 命令行中的目录名区分，不会误杀其他 node 程序
     */
    public static void killSnowLumaProcesses(String snowLumaDir) {
        String keyword = new File(snowLumaDir).getName();
        String script = "$kw='" + keyword + "'; " +
                "Get-CimInstance Win32_Process -Filter \"Name='node.exe' OR Name='cmd.exe'\" | " +
                "Where-Object { ($_.Name -eq 'node.exe' -and $_.ExecutablePath -like \"*$kw*\") -or " +
                "($_.Name -eq 'cmd.exe' -and $_.CommandLine -like \"*$kw*\") } | " +
                "ForEach-Object { Stop-Process -Id $_.ProcessId -Force -ErrorAction SilentlyContinue }";

        try {
            // 必须用 -EncodedCommand：Java 传参时会把脚本里的双引号吞掉，导致 -like "*xxx*" 语法错误
            String encoded = Base64.getEncoder().encodeToString(script.getBytes(StandardCharsets.UTF_16LE));
            Process process = new ProcessBuilder("powershell", "-NoProfile", "-WindowStyle", "Hidden", "-EncodedCommand", encoded)
                    .redirectErrorStream(true)
                    .start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "GBK"))) {
                while (reader.readLine() != null) {
                    // 消费输出，防止阻塞
                }
            }
            process.waitFor(15, TimeUnit.SECONDS);
            log.info("已尝试清理已启动的 SnowLuma 进程: {}", keyword);
        } catch (Exception e) {
            log.warn("清理 SnowLuma 进程失败: {}", e.getMessage());
        }
    }

}