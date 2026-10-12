package main;

import database.DatabaseConnection;
import javax.swing.*;
import ui.LoginFrame;
import ui.theme.ThemeManager;

public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            UIManager.put("Button.focus", new java.awt.Color(0, 0, 0, 0));
        } catch (Exception ignored) {
        }

        // Khởi động ThemeManager
        ThemeManager.getCurrentTheme();

        SwingUtilities.invokeLater(() -> {
            boolean connected = DatabaseConnection.getInstance().testConnection();
            if (!connected) {
                int choice = JOptionPane.showConfirmDialog(
                        null,
                        "Không thể kết nối tới SQL Server!\n\n"
                                + "Bạn có muốn mở Homura Hotel ở chế độ Demo (Offline) không?\n"
                                + "(Đầy đủ dữ liệu mẫu phòng, khách hàng, thống kê và tài khoản)",
                        "Homura Hotel – Chế độ hoạt động",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.INFORMATION_MESSAGE);

                if (choice == JOptionPane.YES_OPTION) {
                    DatabaseConnection.setDemoMode(true);
                } else {
                    System.exit(0);
                    return;
                }
            }

            LoginFrame frame = new LoginFrame();
            frame.setVisible(true);
        });
    }
}
