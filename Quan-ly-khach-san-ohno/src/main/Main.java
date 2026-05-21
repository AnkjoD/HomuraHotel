package main;

import database.DatabaseConnection;
import javax.swing.*;
import ui.LoginFrame;

/**
 * ỨNG DỤNG QUẢN LÝ KHÁCH SẠN
 * Đây là file chạy chính (GUI Entry Point)
 * Hướng dẫn: Chạy file này để mở giao diện Đăng nhập.
 */
public class Main {
    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            UIManager.put("Button.focus", new java.awt.Color(0, 0, 0, 0));
        } catch (UnsupportedLookAndFeelException | ClassNotFoundException | InstantiationException
                | IllegalAccessException e) {
            // fallback to default
        }

        SwingUtilities.invokeLater(() -> {
            if (!DatabaseConnection.getInstance().testConnection()) {
                int choice = JOptionPane.showConfirmDialog(
                        null,
                        """
                                Không thể kết nối SQL Server!

                                Vui lòng kiểm tra:
                                  • SQL Server đang chạy
                                  • Database 'HotelMS' đã được tạo
                                  • Cấu hình trong DatabaseConnection.java

                                Tiếp tục ở chế độ demo (không có DB)?""",
                        "Lỗi kết nối",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE);
                if (choice == JOptionPane.NO_OPTION) {
                    System.exit(0);
                    return;
                }
            }
            LoginFrame frame = new LoginFrame();
            frame.setVisible(true);
        });
    }
}