package com.ehuman.core;

import com.ehuman.core.utils.StartupDialogUtil;
import lombok.extern.slf4j.Slf4j;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import javax.swing.*;
import java.io.File;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;

@SpringBootApplication(scanBasePackages = "com.ehuman")
@MapperScan(basePackages = "com.ehuman.dao.mapper")
@Slf4j
public class EHumanCoreApplication {

    private static FileLock globalLock;
    private static FileChannel globalChannel;
    private static RandomAccessFile globalRaf;

    public static void main(String[] args) {
        // 强制关闭无头模式，允许弹窗
        System.setProperty("java.awt.headless", "false");
        // 1. 获取当前 app 目录
        String baseDir = System.getProperty("user.dir");
        File appFolder = new File(baseDir, "app");
        String lockFilePath = (appFolder.exists() ? appFolder.getAbsolutePath() : baseDir) + File.separator + "ehuman.lock";
        try {
            File lockFile = new File(lockFilePath);
            globalRaf = new RandomAccessFile(lockFile, "rw");
            globalChannel = globalRaf.getChannel();

            // 尝试获取锁
            globalLock = globalChannel.tryLock();

            if (globalLock == null) {
                // 拿不到锁，说明已经在运行了
                StartupDialogUtil startupDialogUtil = new StartupDialogUtil();
                startupDialogUtil.showLoading();
                startupDialogUtil.updateStatus(false,"程序已经在运行了，请勿重复启动",true);
                return;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            SpringApplication.run(EHumanCoreApplication.class, args);
        } catch (Exception e) {
            // 判断是不是端口被占用的异常

            if (e.getCause().toString() != null && e.getCause().toString().contains("Port 8081 is already in use")) {
                // 说明之前的程序没关干净，或者用户真的双开了
                StartupDialogUtil startupDialogUtil = new StartupDialogUtil();
                startupDialogUtil.showLoading();
                startupDialogUtil.updateStatus(false,"端口被占用8081，启动失败",true);
            } else {
                // 如果是其他错误，打印堆栈
                e.printStackTrace();
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(null, "ehuman 启动失败：\n" + e.getMessage(), "启动错误", JOptionPane.ERROR_MESSAGE);
                });
                System.exit(1);
            }
        }
    }

}
