package com.ehuman.core.init;

import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.ehuman.core.EHumanCoreApplication;
import com.ehuman.core.utils.ProcessUtils;
import com.ehuman.core.utils.StartupDialogUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
@Slf4j
public class StartupAction implements CommandLineRunner {

    private static final String SNOWLUMA_DIR_NAME = "SnowLuma-v2.4.0-win-x64";

    @Value("${spring.profiles.active}")
    private String env;

    @Override
    public void run(String... args){
        if(env.equals("exe")){
            // 1. 复用程序启动时弹出的“正在启动”窗口
            StartupDialogUtil startupDialog = EHumanCoreApplication.startupDialogUtil;
            if (startupDialog == null) {
                startupDialog = new StartupDialogUtil();
                startupDialog.showLoading();
                EHumanCoreApplication.startupDialogUtil = startupDialog;
            }
            log.info("exe 开始检测环境");
            startupDialog.updateStatus(true, "正在检测环境...",false);
            String baseUrl = System.getProperty("user.dir") + "/app";
            // 初始化运行环境
            startupDialog.updateStatus(true, "正在初始化运行环境...",false);
            boolean initSuccess = initEnvironment(baseUrl);
            startupDialog.updateStatus(initSuccess, initSuccess ? "初始化运行环境成功..." : "初始化运行环境失败...",!initSuccess);

            // 启动 SnowLuma
            startupDialog.updateStatus(true, "正在启动 SnowLuma...",false);
            boolean startSuccess = startSnowLuma(baseUrl);
            startupDialog.updateStatus(startSuccess, startSuccess ? "SnowLuma 启动完成..." : "SnowLuma 启动失败...", !startSuccess);

            // 异步注入 SnowLuma 默认 wsClient 配置。
            // 用户可能启动几分钟后才登录，onebot_<uin>.json 才会生成，所以持续监控 10 分钟
            new Thread(() -> {
                monitorAndInjectSnowLumaConfig(baseUrl, 60 * 10);
            }).start();

            startupDialog.updateStatus(true, "正在获取 SnowLuma 控制台地址...",false);

            // 读取 SnowLuma 启动日志，寻找 WebUI 地址和首次启动的初始密码
            String[] snowLumaInfo = monitorSnowLumaLog(Paths.get(baseUrl, "snowluma_run.log").toString(), 120, startupDialog);
            String webUrl = snowLumaInfo[0];
            String initialPassword = snowLumaInfo[1];

            if(System.getProperty("BOT_ID") != null && !System.getProperty("BOT_ID").isEmpty()){
                startupDialog.updateStatus(true, "快速登录成功",false);
                startupDialog.updateStatus(true, "qq[" + System.getProperty("BOT_ID")
                        + "]qq连接成功,SnowLuma控制台：" + webUrl + ",点击下方确定按钮开始对话吧",true);
            }else{
                String waitMessage = "等待qq连接,(首次使用请在SnowLuma控制台启用default连接)SnowLuma控制台：\n" + webUrl;
                if (initialPassword != null && !initialPassword.isEmpty()) {
                    // 首次启动：初始密码标红提示
                    startupDialog.updateStatusWithPassword(true, waitMessage, initialPassword, false);
                } else {
                    startupDialog.updateStatus(true, waitMessage, false);
                }
                // 循环检查 QQ 连接状态，连接成功后才显示确定按钮
                for(int i = 0; i < 100; i++) {
                    try {
                        Thread.sleep(2000);
                        if(System.getProperty("BOT_ID") != null && !System.getProperty("BOT_ID").isEmpty()){
                            startupDialog.updateStatus(true, "qq[" + System.getProperty("BOT_ID")
                                    + "]qq连接成功,SnowLuma控制台：" + webUrl + ",点击下方确定按钮开始对话吧",true);
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
                // 退出前把拉起来的 SnowLuma 杀掉
                ProcessUtils.killSnowLumaProcesses(Paths.get(baseUrl, SNOWLUMA_DIR_NAME).toString());
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
     * 启动 SnowLuma 的 launcher.bat，并把控制台输出重定向到日志文件，方便后续解析 WebUI 地址
     * @param baseDir 当前 app 的绝对路径
     */
    private boolean startSnowLuma(String baseDir) {
        File snowLumaDir = new File(baseDir, SNOWLUMA_DIR_NAME);
        File launcher = new File(snowLumaDir, "launcher.bat");
        if (!launcher.exists()) {
            log.error("未找到 SnowLuma launcher: {}", launcher.getAbsolutePath());
            return false;
        }

        // 先杀掉上一次残留的 SnowLuma 进程，否则端口和日志文件都会被占用
        ProcessUtils.killSnowLumaProcesses(snowLumaDir.getAbsolutePath());

        Path logFile = Paths.get(baseDir, "snowluma_run.log");
        for (int i = 0; i < 10; i++) {
            try {
                Files.deleteIfExists(logFile);
                break;
            } catch (Exception e) {
                log.warn("删除旧的 snowluma_run.log 失败，第 {} 次重试", i + 1);
                try {
                    Thread.sleep(300);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }

        try {
            // 用完整路径启动，方便按命令行识别并清理这个 launcher 进程
            ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", launcher.getAbsolutePath());
            // 工作目录必须是 launcher.bat 所在目录
            pb.directory(snowLumaDir);
            pb.redirectErrorStream(true);
            pb.redirectOutput(ProcessBuilder.Redirect.to(logFile.toFile()));

            // 优先使用 SnowLuma 自带的 node.exe
            Map<String, String> environment = pb.environment();
            environment.put("PATH", snowLumaDir.getAbsolutePath() + File.pathSeparator
                    + environment.getOrDefault("PATH", ""));

            pb.start();
            log.info("SnowLuma launcher 已启动，日志文件: {}", logFile.toAbsolutePath());
            return true;
        } catch (Exception e) {
            log.error("启动 SnowLuma 失败", e);
            return false;
        }
    }

    /**
     * 读取 SnowLuma 启动日志，寻找 WebUI 地址并打开浏览器。
     * 兼容首次启动（带初始账号密码）和后续启动两种日志格式：
     * [WebUI] initial credentials: user=admin password=xxx
     * [WebUI] listening http://127.0.0.1:5099
     *
     * @return [0] WebUI 地址，[1] 初始密码（非首次启动时为 null）
     */
    public String[] monitorSnowLumaLog(String logFilePath, int timeoutSeconds, StartupDialogUtil startupDialog) {
        long startTime = System.currentTimeMillis();
        Pattern urlPattern = Pattern.compile("listening\\s+(https?://127\\.0\\.0\\.1:\\d+)");
        Pattern passwordPattern = Pattern.compile("initial credentials:\\s*user=\\S+\\s+password=(\\S+)");
        String initialPassword = null;

        while (System.currentTimeMillis() - startTime < timeoutSeconds * 1000L) {
            try {
                Path path = Paths.get(logFilePath);
                if (Files.exists(path)) {
                    // 日志可能正写到一半，用 new String 兜底替换掉不完整的多字节字符
                    String content = new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
                    for (String line : content.split("\\r?\\n")) {
                        if (initialPassword == null) {
                            Matcher passwordMatcher = passwordPattern.matcher(line);
                            if (passwordMatcher.find()) {
                                initialPassword = passwordMatcher.group(1);
                            }
                        }

                        Matcher matcher = urlPattern.matcher(line);
                        if (matcher.find()) {
                            String webUiUrl = matcher.group(1);
                            startupDialog.updateStatus(true, "尝试打开浏览器，如果没有自动打开，请手动复制浏览器打开登录\n" + webUiUrl, false);
                            openBrowserUrl(webUiUrl);
                            return new String[]{webUiUrl, initialPassword};
                        }
                    }
                }
                Thread.sleep(1000);
            } catch (Exception ignored) {
                // 忽略文件正在被写入时的短暂报错
            }
        }
        return new String[]{null, initialPassword};
    }

    /**
     * 监控 SnowLuma 的 onebot_<uin>.json 配置，为每个账号注入一条默认 wsClient（默认不启用）。
     * 用户登录后才会生成该文件，所以需要持续监控一段时间（由调用方传入 10 分钟）。
     * @param baseUrl app 目录
     * @param timeoutSeconds 最长监控时间（秒）
     */
    public void monitorAndInjectSnowLumaConfig(String baseUrl, int timeoutSeconds) {
        File configDir = Paths.get(baseUrl, SNOWLUMA_DIR_NAME, "config").toFile();
        log.info("开始监控 SnowLuma 配置目录: {}", configDir.getAbsolutePath());

        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < timeoutSeconds * 1000L) {
            File[] jsonFiles = configDir.listFiles((dir, name) ->
                    name.startsWith("onebot_") && name.endsWith(".json"));

            if (jsonFiles != null) {
                for (File file : jsonFiles) {
                    injectDefaultWsClient(file);
                }
            }

            try {
                TimeUnit.SECONDS.sleep(3);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("SnowLuma 配置监控被中断");
                return;
            }
        }

        log.info("SnowLuma 配置目录监控结束");
    }

    private void injectDefaultWsClient(File file) {
        try {
            String content = new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8);
            if (content.trim().isEmpty()) return;

            JSONObject root = JSONUtil.parseObj(content);
            JSONObject networks = root.getJSONObject("networks");
            if (networks == null) return;

            JSONArray clients = networks.getJSONArray("wsClients");
            if (clients == null) {
                clients = new JSONArray();
                networks.set("wsClients", clients);
            }

            boolean changed = false;
            boolean hasDefault = false;
            for (int i = 0; i < clients.size(); i++) {
                JSONObject client = clients.getJSONObject(i);
                if ("default".equals(client.getStr("name"))) {
                    hasDefault = true;
                    // 兼容旧端口配置：8081 -> 48081
                    String url = client.getStr("url");
                    if (url != null && url.contains(":8081")) {
                        client.set("url", url.replace(":8081", ":48081"));
                        client.set("enabled", false);
                        changed = true;
                        log.info("已更新 {} 中 default 连接端口为 48081 并设为禁用", file.getName());
                    }
                    break;
                }
            }

            if (!hasDefault) {
                JSONObject defaultClient = new JSONObject();
                defaultClient.set("name", "default");
                // SnowLuma 中 enabled=false 表示不启用
                defaultClient.set("enabled", false);
                defaultClient.set("accessToken", "ehuman");
                defaultClient.set("messageFormat", "array");
                defaultClient.set("reportSelfMessage", false);
                defaultClient.set("url", "ws://127.0.0.1:48081/ws");
                defaultClient.set("role", "Universal");
                defaultClient.set("reconnectIntervalMs", 5000);
                clients.add(defaultClient);
                changed = true;
                log.info("已为 {} 注入默认 wsClient 配置 (enabled=false)", file.getName());
            }

            if (changed) {
                Files.write(file.toPath(), root.toStringPretty().getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception e) {
            // 文件可能正在被 SnowLuma 写入，等待下一轮重试
            log.warn("处理 SnowLuma 配置文件 {} 失败，稍后重试", file.getName());
        }
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
        if(ProcessUtils.isProcessRunning("EHumanInstaller.exe")){
            ProcessUtils.killProcess("EHumanInstaller.exe");
        }
        log.info("初始化环境结束");
        return true;
    }
}