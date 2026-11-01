package com.ehuman.core.utils;

import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import java.awt.*;

@Slf4j
public class StartupDialogUtil {

    private JDialog dialog;
    private JLabel label;
    private JButton button;
    private boolean isFailed = false;

    public void showLoading() {
        SwingUtilities.invokeLater(() -> {
            dialog = new JDialog();
            dialog.setTitle("ehuman");
            dialog.setSize(450, 180);
            dialog.setLocationRelativeTo(null);
            dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            dialog.setResizable(false);

            // 使用 GridBagLayout 控制全局
            JPanel mainPanel = new JPanel(new GridBagLayout());

            // 1. 初始化 JLabel
            label = new JLabel("ehuman 正在启动，请稍候...");
            label.setFont(new Font("微软雅黑", Font.PLAIN, 14));
            label.setHorizontalAlignment(SwingConstants.CENTER); // 文字自身居中

            // 添加文字（占满所有空间，绝对居中）
            GridBagConstraints gbcText = new GridBagConstraints();
            gbcText.gridx = 0;
            gbcText.gridy = 0;
            gbcText.weightx = 1.0;
            gbcText.weighty = 1.0;
            gbcText.fill = GridBagConstraints.BOTH; // 核心：让 label 占满整个区域
            mainPanel.add(label, gbcText);

            // 2. 初始化按钮
            button = new JButton("确 定");
            button.setVisible(false);
            button.addActionListener(e -> {
                dialog.dispose();
                if (isFailed) {
                    log.error("启动失败，用户已确认，程序即将退出...");
                    System.exit(0);
                }
            });

            // 添加按钮（贴在最下方）
            GridBagConstraints gbcBtn = new GridBagConstraints();
            gbcBtn.gridx = 0;
            gbcBtn.gridy = 1;
            gbcBtn.weightx = 1.0;
            gbcBtn.weighty = 0;
            gbcBtn.insets = new Insets(0, 0, 20, 0);
            mainPanel.add(button, gbcBtn);

            dialog.add(mainPanel);
            dialog.setVisible(true);
        });
    }

    public void updateStatus(boolean success, String message, boolean showButton) {
        SwingUtilities.invokeLater(() -> {
            if (dialog == null || label == null || button == null) return;

            // 【关键修改】HTML 不要写固定宽度，让它自适应。用 div 包裹并居中
            String safeMessage = message.replace("<", "&lt;").replace(">", "&gt;");
            // 新增：把换行符替换成 HTML 的换行标签
            safeMessage = safeMessage.replace("\n", "<br>");

            label.setText("<html><div style='text-align: center; width: 100%;'>" + safeMessage + "</div></html>");
            label.setForeground(success ? new Color(0, 153, 51) : Color.RED);

            this.isFailed = !success;

            button.setVisible(showButton);
            button.setFocusable(true);
            dialog.getRootPane().setDefaultButton(button);
            button.requestFocus();
        });
    }

    public void updateProgress(String message) {
        SwingUtilities.invokeLater(() -> {
            if (dialog == null || label == null) return;

            // 【关键修改】HTML 不要写固定宽度，让它自适应。用 div 包裹并居中
            String safeMessage = message.replace("<", "&lt;").replace(">", "&gt;");
            // 新增：把换行符替换成 HTML 的换行标签
            safeMessage = safeMessage.replace("\n", "<br>");

            label.setText("<html><div style='text-align: center; width: 100%;'>" + safeMessage + "</div></html>");
            label.setForeground(Color.BLACK);

            if (button != null) {
                button.setVisible(false);
            }
        });
    }
}