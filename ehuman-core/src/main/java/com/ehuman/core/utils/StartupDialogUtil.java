package com.ehuman.core.utils;

import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyledDocument;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

@Slf4j
public class StartupDialogUtil {

    private JDialog dialog;
    private JTextPane textPane;
    private JButton button;
    private boolean isFailed = false;
    private String currentMessage = "";
    /** 需要标红显示的文本（比如 SnowLuma 初始密码），没有则为 null */
    private String highlightText;

    public void showLoading() {
        SwingUtilities.invokeLater(() -> {
            dialog = new JDialog();
            dialog.setTitle("ehuman");
            dialog.setSize(450, 180);
            dialog.setLocationRelativeTo(null);
            dialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
            dialog.setResizable(false);
            dialog.setAlwaysOnTop(true);

            JPanel mainPanel = new JPanel(new GridBagLayout());
            mainPanel.setBackground(new Color(245, 245, 245));

            textPane = new JTextPane();
            textPane.setEditable(false);
            textPane.setOpaque(false);
            textPane.setCursor(new Cursor(Cursor.TEXT_CURSOR));
            // 给文字留一点左右边距，防止长文字贴边
            textPane.setBorder(BorderFactory.createEmptyBorder(0, 20, 0, 20));

            // 监听窗口大小变化，保证实时垂直居中
            textPane.addComponentListener(new ComponentAdapter() {
                @Override
                public void componentResized(ComponentEvent e) {
                    centerText();
                }
            });

            GridBagConstraints gbcText = new GridBagConstraints();
            gbcText.gridx = 0;
            gbcText.gridy = 0;
            gbcText.weightx = 1.0;
            gbcText.weighty = 1.0;
            gbcText.fill = GridBagConstraints.BOTH;
            mainPanel.add(textPane, gbcText);

            button = new JButton("确 定");
            button.setVisible(false);
            button.addActionListener(e -> {
                dialog.dispose();
                if (isFailed) {
                    log.error("启动失败，用户已确认，程序即将退出...");
                    System.exit(0);
                }
            });

            GridBagConstraints gbcBtn = new GridBagConstraints();
            gbcBtn.gridx = 0;
            gbcBtn.gridy = 1;
            gbcBtn.weightx = 1.0;
            gbcBtn.weighty = 0;
            gbcBtn.insets = new Insets(0, 0, 15, 0);
            mainPanel.add(button, gbcBtn);

            dialog.add(mainPanel);
            dialog.setVisible(true);

            updateText("ehuman 正在启动，请稍候...", Color.BLACK);
        });
    }

    public void updateStatus(boolean success, String message, boolean showButton) {
        SwingUtilities.invokeLater(() -> {
            if (dialog == null || textPane == null || button == null) return;

            this.highlightText = null;
            Color color = success ? new Color(0, 153, 51) : Color.RED;
            updateText(message, color);

            this.isFailed = !success;
            button.setVisible(showButton);
            button.setFocusable(true);
            dialog.getRootPane().setDefaultButton(button);
            button.requestFocus();
        });
    }

    /**
     * 更新状态，并把密码部分标红显示
     */
    public void updateStatusWithPassword(boolean success, String message, String password, boolean showButton) {
        SwingUtilities.invokeLater(() -> {
            if (dialog == null || textPane == null || button == null) return;

            this.highlightText = "初始密码是 " + password;
            Color color = success ? new Color(0, 153, 51) : Color.RED;
            updateText(message + "\n" + highlightText, color);

            this.isFailed = !success;
            button.setVisible(showButton);
            button.setFocusable(true);
            dialog.getRootPane().setDefaultButton(button);
            button.requestFocus();
        });
    }

    public void updateProgress(String message) {
        SwingUtilities.invokeLater(() -> {
            if (dialog == null || textPane == null) return;

            this.highlightText = null;
            updateText(message, Color.BLACK);

            if (button != null) {
                button.setVisible(false);
            }
        });
    }

    private void updateText(String message, Color color) {
        if (textPane == null) return;
        this.currentMessage = message;
        textPane.setText(message);
        textPane.setForeground(color);

        StyledDocument doc = textPane.getStyledDocument();
        SimpleAttributeSet center = new SimpleAttributeSet();
        StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
        doc.setParagraphAttributes(0, doc.getLength(), center, false);

        centerText();
    }

    /**
     * 精准计算垂直居中
     */
    private void centerText() {
        if (textPane == null || currentMessage.isEmpty()) return;

        FontMetrics fm = textPane.getFontMetrics(textPane.getFont());
        int lineHeight = fm.getHeight();

        // 获取当前 textPane 可用高度
        int availableHeight = textPane.getHeight() - textPane.getInsets().top - textPane.getInsets().bottom;

        // 先还原成原始文本，计算所需行数
        textPane.setText(currentMessage);

        int textHeight;
        try {
            // 获取文字实际渲染的高度
            Rectangle r = textPane.modelToView2D(textPane.getDocument().getLength()).getBounds();
            textHeight = r.y + r.height; // 内容总高度
        } catch (Exception e) {
            textHeight = lineHeight;
        }

        // 计算上下需要补充的空白高度
        int emptyHeight = availableHeight - textHeight;

        if (emptyHeight > 0) {
            // 一半留上面，一半留下面。只留上面的空行
            int topPaddingPixels = emptyHeight / 2;
            // 转换成空行的数量
            int topPaddingLines = topPaddingPixels / lineHeight;

            // 防止由于误差变成负数
            if (topPaddingLines > 0) {
                StringBuilder paddedText = new StringBuilder();
                for (int i = 0; i < topPaddingLines; i++) {
                    paddedText.append("\n");
                }
                paddedText.append(currentMessage);

                // 重新设置带空行的文本
                textPane.setText(paddedText.toString());

                StyledDocument doc = textPane.getStyledDocument();
                SimpleAttributeSet center = new SimpleAttributeSet();
                StyleConstants.setAlignment(center, StyleConstants.ALIGN_CENTER);
                doc.setParagraphAttributes(0, doc.getLength(), center, false);
            }
        }

        applyHighlight();
    }

    /**
     * 把 highlightText 部分标红
     */
    private void applyHighlight() {
        if (highlightText == null || textPane == null) return;

        String text = textPane.getText();
        int start = text.lastIndexOf(highlightText);
        if (start < 0) return;

        SimpleAttributeSet red = new SimpleAttributeSet();
        StyleConstants.setForeground(red, Color.RED);

        StyledDocument doc = textPane.getStyledDocument();
        doc.setCharacterAttributes(start, highlightText.length(), red, false);
    }
}