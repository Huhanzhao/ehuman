package com.ehuman.core.init;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ehuman.core.utils.ProcessUtils;
import com.ehuman.core.utils.StartupDialogUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

@Component
@Slf4j
public class StartupAction implements CommandLineRunner {

    @Value("${spring.profiles.active}")
    private String env;

    @Override
    public void run(String... args){
        if(env.equals("exe")){
            // 1. 弹出“正在启动”的窗口
            StartupDialogUtil startupDialog = new StartupDialogUtil();
            startupDialog.showLoading();
            log.info("exe 开始检测环境");
            startupDialog.updateStatus(true, "正在检测环境...",false);
            String baseUrl = System.getProperty("user.dir") + "/app";
            // 初始化运行环境
            startupDialog.updateStatus(true, "正在初始化运行环境...",false);
            boolean initSuccess = initEnvironment(baseUrl);
            startupDialog.updateStatus(initSuccess, initSuccess ? "初始化运行环境成功..." : "初始化运行环境失败...",!initSuccess);

            // 1. 找napcat.xxx.shell 文件夹 安装napcat
            Path napcatBaseDir = Paths.get(baseUrl, "NapCat.Shell.Windows.OneKey");
            if (Files.exists(napcatBaseDir)) {
                File baseFolder = napcatBaseDir.toFile();
                File[] subFolders = baseFolder.listFiles((dir, name) -> name.startsWith("NapCat.") && name.endsWith(".Shell"));
                if (subFolders != null && subFolders.length > 0) {
                    // 找到了具体的 Shell 目录，比如 D:\soft\ehuman\app\NapCat.Shell.Windows.OneKey\NapCat.52230.Shell
                    String napcatShellDir = subFolders[0].getAbsolutePath();
                    log.info("找到 NapCat 核心目录: {} 跳过安装步骤", napcatShellDir);
                } else {
                    log.info("没有找到 NapCat.*.Shell 文件夹");
                    // 执行安装逻辑
                    startupDialog.updateStatus(true, "正在安装 NapCat, 请等待安装完成",false);
                    installNapCat(baseUrl,startupDialog);
                    startupDialog.updateStatus(true, "NapCat 安装完成...",false);
                }
                // 启动napcat
                startupDialog.updateStatus(true, "正在启动 NapCat...",false);
                boolean startNapCat = startNapCat(baseUrl);
                startupDialog.updateStatus(startNapCat, startNapCat ? "NapCat 启动完成..." : "NapCat 启动失败...", !startNapCat);
            }else{
                log.error("目录不存在: {}", napcatBaseDir);
                startupDialog.updateStatus(false, "目录不存在: " + napcatBaseDir,true);
            }

            startupDialog.updateStatus(true, "正在获取NapCat Token...",false);

            // 读取napcat启动日志，寻找token
            String webUrl = monitorNapCatLog(Paths.get(baseUrl, "napcat_run.log").toString(), 120, startupDialog);

            // 异步初始化NapCat默认配置
            new Thread(() -> {
                monitorAndInjectConfig(baseUrl, 60 * 10);
            }).start();

            if(System.getProperty("BOT_ID") != null && !System.getProperty("BOT_ID").isEmpty()){
                startupDialog.updateStatus(true, "快速登录成功",false);
                startupDialog.updateStatus(true, "qq[" + System.getProperty("BOT_ID")
                        + "]qq连接成功,NapCat控制台：" + webUrl + ",点击下方确定按钮开始对话吧",true);
            }else{
                startupDialog.updateStatus(true, "等待qq连接,(首次登录需要手动开启网络配置中的default配置)NapCat地址：\n" + webUrl, false);
                for(int i = 0; i < 100; i++) {
                    try {
                        Thread.sleep(2000);
                        if(System.getProperty("BOT_ID") != null && !System.getProperty("BOT_ID").isEmpty()){
                            startupDialog.updateStatus(true, "qq[" + System.getProperty("BOT_ID")
                                    + "]qq连接成功,NapCat控制台：" + webUrl + ",点击下方确定按钮开始对话吧",true);
                            break;
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            addSystemTray(baseUrl);

        }
    }

    /**
     * 添加系统托盘图标，方便用户随时退出
     */
    private void addSystemTray(String baseUrl) {
        if (!SystemTray.isSupported()) {
            log.warn("当前系统不支持系统托盘");
            return;
        }
        try {
            SystemTray tray = SystemTray.getSystemTray();

            // 1. 打印绝对路径，方便你去资源管理器里看一眼到底对不对
            File iconFile = new File(baseUrl, "ehuman.png");
            log.info("【托盘排查】尝试读取图标路径: {}", iconFile.getAbsolutePath());
            log.info("【托盘排查】图标文件是否存在: {}", iconFile.exists());

            Image image = null;

            if (iconFile.exists()) {
                try {
                    // 尝试用 ImageIO 读取
                    image = javax.imageio.ImageIO.read(iconFile);
                } catch (Exception e) {
                    log.warn("ImageIO 读取失败，原因可能是 Java 不支持 ICO 格式", e);
                }
            } else {
                log.error("【托盘排查】图标文件不存在，请检查 baseUrl 是否正确");
            }

            // 2. 如果没找到 ico，或者 ImageIO 读取失败返回了 null，我们强行造一个图标兜底
            if (image == null) {
                log.warn("【托盘排查】图片读取结果为 null（Java 原生不支持 ico），使用默认绘制图标兜底");
                // 画一个 16x16 的绿色圆点
                java.awt.image.BufferedImage bufferedImage = new java.awt.image.BufferedImage(16, 16, java.awt.image.BufferedImage.TYPE_INT_ARGB);
                Graphics2D g2d = bufferedImage.createGraphics();
                g2d.setColor(new Color(0, 153, 51));
                g2d.fillOval(0, 0, 16, 16);
                g2d.dispose();
                image = bufferedImage;
            }

            // 3. 创建右键菜单
            PopupMenu popup = new PopupMenu();
            MenuItem exitItem = new MenuItem("exit ehuman");
            exitItem.addActionListener(e -> {
                log.info("用户通过系统托盘退出了程序");
                // 退出前顺手把拉起来的 NapCat 和 QQ 杀了
                ProcessUtils.killNapCatQQ();
                ProcessUtils.killProcess("NapCatWinBootMain.exe");
                ProcessUtils.killProcess("NapCatInstaller.exe");
                ProcessUtils.killProcessByCommandLine("napcat.bat");
                ProcessUtils.killProcessByCommandLine("napcat.quick.bat");
                System.exit(0);
            });
            popup.add(exitItem);

            MenuItem aboutItem = new MenuItem("About ehuman");
            aboutItem.addActionListener(e -> {
                // 用 Swing 弹出一个支持中文和 HTML 的提示框
                SwingUtilities.invokeLater(() -> {
                    String msg = "<html><body style='width: 250px;'>"
                            + "<h3>ehuman 机器人</h3>"
                            + "<p>欢迎加入飞书交流群：</p>"
                            + "<a href='https://applink.feishu.cn/client/chat/chatter/add_by_link?link_token=6fcv2e01-bfae-4ed4-af10-f49d3e04cf56'>点击加入飞书群</a>"
                            + "<p>开源地址：</p>"
                            + "<a href='https://gitee.com/huhanzhao/ehuman'>Gitee 仓库</a>"
                            + "</body></html>";

                    // 【唯一修改点】把 JLabel 换成 JEditorPane
                    JEditorPane ep = new JEditorPane();
                    ep.setContentType("text/html");
                    ep.setEditable(false);
                    ep.setOpaque(false);
                    ep.setText(msg);

                    // 自动把鼠标变成小手
                    ep.setCursor(new Cursor(Cursor.HAND_CURSOR));

                    // 监听用户具体点击了哪个链接
                    ep.addHyperlinkListener(evt -> {
                        if (evt.getEventType() == javax.swing.event.HyperlinkEvent.EventType.ACTIVATED) {
                            try {
                                // evt.getURL() 会自动获取用户点击的那个具体链接
                                java.awt.Desktop.getDesktop().browse(evt.getURL().toURI());
                            } catch (Exception ex) {
                                ex.printStackTrace();
                            }
                        }
                    });

                    // 弹出对话框（这里原来的 JOptionPane 代码保持不变）
                    JOptionPane.showMessageDialog(null, ep, "关于 ehuman", JOptionPane.INFORMATION_MESSAGE);
                });
            });
            popup.add(aboutItem);

            // 4. 创建托盘
            TrayIcon trayIcon = new TrayIcon(image, "ehuman 正在后台运行", popup);
            trayIcon.setImageAutoSize(true);
            tray.add(trayIcon);
            log.info("【托盘排查】系统托盘创建成功！");

        } catch (Exception e) {
            log.error("创建系统托盘发生异常", e);
        }
    }


    /**
     * 监控并注入 NapCat 的默认 WebSocket 配置
     * @param baseUrl baseUrl
     * @param timeoutSeconds 最长等待时间（秒）
     */
    public void monitorAndInjectConfig(String baseUrl, int timeoutSeconds) {
        long startTime = System.currentTimeMillis();
        String configDirPath = Paths.get(baseUrl, "NapCat.Shell.Windows.OneKey", "NapCat.52230.Shell",
                "versions", "9.9.33-52230", "resources", "app", "napcat", "config").toString();
        Path dirPath = Paths.get(configDirPath);

        log.info("开始监控 NapCat 配置目录: {}", configDirPath);

        while (System.currentTimeMillis() - startTime < timeoutSeconds * 1000L) {
            File folder = dirPath.toFile();
            if (folder.exists() && folder.isDirectory()) {
                // 查找所有符合 onebot11_*.json 的文件
                File[] jsonFiles = folder.listFiles((dir, name) ->
                        name.startsWith("onebot11_") && name.endsWith(".json"));

                if (jsonFiles != null && jsonFiles.length > 0) {
                    boolean allProcessed = true;

                    for (File file : jsonFiles) {
                        try {
                            // 读取文件内容
                            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
                            if (content.trim().isEmpty()) continue;

                            JSONObject root = JSONUtil.parseObj(content);
                            JSONObject network = root.getJSONObject("network");
                            if (network == null) continue;

                            JSONArray clients = network.getJSONArray("websocketClients");
                            if (clients == null) {
                                clients = new JSONArray();
                                network.set("websocketClients", clients);
                            }

                            // 检查是否已经存在 name 为 "default" 的配置
                            boolean hasDefault = false;
                            for (int i = 0; i < clients.size(); i++) {
                                JSONObject client = clients.getJSONObject(i);
                                if ("default".equals(client.getStr("name"))) {
                                    hasDefault = true;
                                    break;
                                }
                            }

                            // 如果没有，则注入默认配置
                            if (!hasDefault) {
                                JSONObject defaultClient = new JSONObject();
                                defaultClient.set("enable", false); // 默认关闭，需用户手动开启
                                defaultClient.set("name", "default");
                                defaultClient.set("url", "ws://localhost:8081/ws");
                                defaultClient.set("reportSelfMessage", false);
                                defaultClient.set("messagePostFormat", "array");
                                defaultClient.set("token", "ehuman");
                                defaultClient.set("debug", false);
                                defaultClient.set("heartInterval", 30000);
                                defaultClient.set("reconnectInterval", 3000);
                                defaultClient.set("verifyCertificate", true);

                                clients.add(defaultClient);

                                // 写回文件，保持美观的缩进
                                Files.write(file.toPath(), root.toStringPretty().getBytes(StandardCharsets.UTF_8));
                                log.info("已为 {} 注入默认 WebSocket 配置 (enable=false)", file.getName());
                            } else {
                                log.info("{} 已存在 default 配置，跳过", file.getName());
                            }

                        } catch (Exception e) {
                            log.error("处理配置文件 {} 失败", file.getName(), e);
                            allProcessed = false; // 如果出错，下一轮重试
                        }
                    }

                    // 如果所有文件都处理完了，并且确实找到了文件，直接结束监控
                    if (allProcessed) {
                        log.info("NapCat 配置文件注入完成！");
                        return;
                    }
                }
            }

            // 没找到或处理失败，等 3 秒再试
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("配置监控被中断");
                return;
            }
        }

        log.warn("监控 NapCat 配置目录超时 ({}秒)，未能完成注入", timeoutSeconds);
    }
    public String monitorNapCatLog(String logFilePath, int timeoutSeconds, StartupDialogUtil startupDialog) {
        long startTime = System.currentTimeMillis();
        // 核心修改：\\d+ 匹配任意端口号
        Pattern urlPattern = Pattern.compile("(http://127\\.0\\.0\\.1:\\d+/webui\\?token=[a-zA-Z0-9]+)");

        boolean isQrCode = false, isQuickLogin = false, isError = false;
        String webUiUrl = null;

        while (System.currentTimeMillis() - startTime < timeoutSeconds * 1000L) {
            try {
                Path path = Paths.get(logFilePath);
                if (Files.exists(path)) {
                    // 直接全量读取，因为启动日志非常小，不用担心性能
                    List<String> lines = Files.readAllLines(path, StandardCharsets.UTF_8);
                    for (String line : lines) {
                        if (line.contains("将使用二维码登录方式")) isQrCode = true;
                        if (line.contains("正在快速登录")) isQuickLogin = true;
                        if (line.toLowerCase().contains("error")) isError = true;

                        if (webUiUrl == null && line.contains("WebUi User Panel Url: http")) {
                            Matcher matcher = urlPattern.matcher(line);
                            if (matcher.find()) {
                                webUiUrl = matcher.group(1);
                            }
                        }
                    }

                    if (webUiUrl != null) {
                        if (isQrCode) {
                            startupDialog.updateStatus(true, "尝试打开浏览器，如果没有自动打开，请手动复制浏览器打开登录\n" + webUiUrl, false);
                            openBrowserUrl(webUiUrl);
                            return webUiUrl;
                        }
                        if (isQuickLogin && isError){
                            startupDialog.updateStatus(true, "尝试打开浏览器，如果没有自动打开，请手动复制浏览器打开登录\n" + webUiUrl, false);
                            openBrowserUrl(webUiUrl);
                            return webUiUrl;
                        }
                        // 快速登录之前肯定登录过，必定有一个default配置 登录之后就可以连接上服务
                        String botId = System.getProperty("BOT_ID");
                        if (isQuickLogin && botId != null && !botId.isEmpty()){
                            return webUiUrl;
                        }
                    }
                }
                Thread.sleep(1000);
            } catch (Exception ignored) {
                // 忽略文件正在被写入时的短暂报错
            }
        }
        return null;
    }

    public void openBrowserUrl(String webUiUrl){
        try {
            // 注意：这里的 "" 是必须的，代表 cmd start 的窗口标题
            Runtime.getRuntime().exec(new String[]{"cmd", "/c", "start", "", webUiUrl});
        } catch (Exception e) {
            log.error("调用 cmd start 打开浏览器失败", e);
        }
    }

    private boolean initEnvironment(String baseUrl) {
        log.info("初始化环境");
        if(ProcessUtils.isProcessRunning("NapCatWinBootMain.exe")){
            ProcessUtils.killProcess("NapCatWinBootMain.exe");
        }
        if(ProcessUtils.isProcessRunning("NapCatInstaller.exe")){
            ProcessUtils.killProcess("NapCatInstaller.exe");
        }
        if(ProcessUtils.isProcessRunning("EHumanInstaller.exe")){
            ProcessUtils.killProcess("EHumanInstaller.exe");
        }
        ProcessUtils.killProcessByCommandLine("napcat.bat");
        ProcessUtils.killProcessByCommandLine("napcat.quick.bat");

        log.info("清理上次napcat_run.log");
        // 拼接出文件的绝对路径 (baseUrl 是 app 目录)
        Path logFile = Paths.get(baseUrl, "napcat_run.log");
        try {
            // 如果文件存在则删除
            boolean deleted = Files.deleteIfExists(logFile);
            if (deleted) {
                log.info("已成功删除旧的 napcat_run.log 文件");
            } else {
                log.info("没有找到旧的 napcat_run.log 文件，无需删除");
            }
        } catch (Exception e) {
            log.error("删除 napcat_run.log 失败，可能文件正被其他进程占用", e);
            return false;
        }
        log.info("初始化环境结束");
        return true;
    }


    /**
     * 执行安装 NapCat 的逻辑
     */
    private void installNapCat(String baseDir, StartupDialogUtil startupDialog) {
        // 假设 NapCatInstaller.exe 就在 NapCat.Shell.Windows.OneKey 目录下
        Path installerPath = Paths.get(baseDir, "NapCat.Shell.Windows.OneKey", "NapCatInstaller.exe");

        if (Files.exists(installerPath)) {
            try {
                log.info("正在启动安装器: {}", installerPath.toString());

                // 使用 ProcessBuilder 启动安装器
                ProcessBuilder pb = new ProcessBuilder(installerPath.toString());
                // 关键：一定要设置工作目录为它所在的目录，否则它下载的东西会乱跑
                pb.directory(installerPath.getParent().toFile());
                // 合并错误流，确保所有输出都能被读取
                pb.redirectErrorStream(true);
                Process process = pb.start();

                log.info("NapCat 安装器已启动，请根据弹出的窗口完成安装...");

                // ==========================================
                // 【新增】启动异步线程读取日志，并更新到弹窗
                // ==========================================
                new Thread(() -> {
                    try (java.io.InputStream is = process.getInputStream();
                         java.io.InputStreamReader isr = new java.io.InputStreamReader(is, "GBK")) {
                        int c;
                        StringBuilder currentLine = new StringBuilder();
                        while ((c = isr.read()) != -1) {
                            char ch = (char) c;
                            // 遇到回车或换行，代表当前行结束（处理动态进度条的关键）
                            if (ch == '\n' || ch == '\r') {
                                String line = currentLine.toString().trim();
                                if (!line.isEmpty()) {
                                    log.info("[安装器] {}", line);
                                    // 把最新进度刷新到弹窗
                                    if(line.contains("下载进度")){
                                        startupDialog.updateProgress("正在下载组件，" + line.replaceAll("=","")
                                                .replaceAll("\\[","")
                                                .replaceAll("]","")
                                                .replaceAll(">",""));
                                        if(line.contains("100%")){
                                            startupDialog.updateProgress("下载完成，正在解压");
                                        }
                                    }

                                    // 如果检测到结束标志，强制关掉进程（相当于帮用户按键）
                                    if (line.contains("欢迎使用哦~~ 双击启动即可")) {
                                        log.info("检测到安装结束标志，自动关闭安装器窗口...");
                                        process.destroyForcibly();
                                        break;
                                    }
                                }
                                currentLine.setLength(0); // 清空当前行
                            } else {
                                currentLine.append(ch);
                            }
                        }
                    } catch (Exception e) {
                        // 进程被强制结束时可能会抛出流异常，这里忽略即可
                    }
                }).start();

                // 原有逻辑：这里设置最长等待 10 分钟（600秒）
                // 如果上面的线程检测到了标志并杀掉了进程，waitFor 会立刻返回
                boolean finished = process.waitFor(10, java.util.concurrent.TimeUnit.MINUTES);

                if (finished) {
                    int exitCode = process.exitValue();
                    log.info("NapCat 安装器已关闭，退出码: {}", exitCode);
                    if (exitCode == 0) {
                        log.info("NapCat 安装流程正常结束，准备下一步...");
                    } else {
                        log.warn("NapCat 安装器非正常退出（退出码非0），可能是用户取消或安装出错。");
                    }
                } else {
                    log.warn("NapCat 安装器运行超过 10 分钟，为了防止程序卡死，主程序将不再等待。");
                    // 超时，强制杀掉安装器
                    process.destroy();
                }
            } catch (Exception e) {
                log.error("启动或等待 NapCat 安装器失败", e);
            }
        } else {
            log.error("严重错误：在 {} 下找不到 NapCatInstaller.exe！请检查打包文件是否完整。", installerPath);
        }
    }


    /**
     * 启动已有的 NapCat 核心
     * @param baseDir 当前 app 的绝对路径 (D:\soft\ehuman\app)
     */
    private boolean startNapCat(String baseDir) {
        // 这里的目录根据你的实际情况，如果有多个版本取第一个
        File baseFolder = new File(baseDir, "NapCat.Shell.Windows.OneKey");
        File[] subFolders = baseFolder.listFiles((dir, name) -> name.startsWith("NapCat.") && name.endsWith(".Shell"));

        if (subFolders == null || subFolders.length == 0) {
            log.error("未找到 NapCat.*.Shell 目录！");
            return false;
        }

        String napcatShellDir = subFolders[0].getAbsolutePath();
        log.info("找到 NapCat 核心目录: {}", napcatShellDir);

        // 1. 尝试寻找已登录过的 QQ 号
        String quickLoginQQ = findLatestQQ(napcatShellDir);

        try {
            ProcessBuilder pb = new ProcessBuilder();
            File napcatOneKeyDir = new File(baseDir, "NapCat.Shell.Windows.OneKey");

            if (quickLoginQQ != null && !quickLoginQQ.isEmpty()) {
                log.info("检测到历史登录账号 [{}]，准备快速登录 (隐藏窗口)...", quickLoginQQ);

                // 【核心动作】：直接重写 napcat.quick.bat
                Path quickBatPath = Paths.get(napcatShellDir, "napcat.quick.bat");
                String batContent = "@echo off\nNapCatWinBootMain.exe " + quickLoginQQ;
                Files.writeString(quickBatPath, batContent, StandardCharsets.UTF_8);

                // 执行隐藏启动的 VBS
                File vbsFile = new File(napcatOneKeyDir, "run_quick_hidden.vbs");
                pb.command("wscript.exe", vbsFile.getAbsolutePath());

            } else {
                log.info("未检测到历史账号，将以扫码方式启动 (隐藏窗口)...");

                // 执行普通启动的 VBS
                File vbsFile = new File(napcatOneKeyDir, "run_napcat.vbs");
                pb.command("wscript.exe", vbsFile.getAbsolutePath());
            }

            // 启动进程
            pb.start();
            log.info("NapCat VBS 启动指令已发送，请稍候去 napcat_run.log 获取 WebUI 地址。");

        } catch (Exception e) {
            log.error("启动 NapCat 失败", e);
            return false;
        }
        return true;
    }

    /**
     * 在 config 目录下寻找最后修改的 onebot11_xxx.json 文件，提取 QQ 号
     */
    private String findLatestQQ(String napcatShellDir) {
        // 拼接 config 目录的绝对路径
        Path configPath = Paths.get(napcatShellDir, "versions", "9.9.33-52230", "resources", "app", "napcat", "config");

        if (!Files.exists(configPath)) {
            return null;
        }

        try (Stream<Path> stream = Files.list(configPath)) {
            // 过滤出 onebot11_*.json，并按修改时间倒序排列
            Optional<Path> latestFile = stream
                    .filter(p -> p.getFileName().toString().startsWith("onebot11_") && p.getFileName().toString().endsWith(".json")).max(Comparator.comparingLong(p -> {
                        try {
                            return Files.getLastModifiedTime((Path) p).toMillis();
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    }));

            if (latestFile.isPresent()) {
                String fileName = latestFile.get().getFileName().toString();
                // 从 onebot11_317.json 中提取 317
                return fileName.replace("onebot11_", "").replace(".json", "");
            }
        } catch (Exception e) {
            log.error("读取 NapCat 配置文件失败", e);
        }
        return null;
    }
}