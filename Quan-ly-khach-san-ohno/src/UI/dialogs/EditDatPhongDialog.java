package ui.dialogs;

import entity.DatPhong;
import entity.enums.TrangThaiDatPhong;
import service.DatPhongService;
import ui.components.DateTimePicker;
import ui.components.NotificationManager;
import ui.components.RoundedComponents.*;
import ui.components.UIConstants;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDateTime;
import java.util.Date;

/**
 * EditDatPhongDialog - Giao diện sửa thông tin đặt phòng.
 * Khôi phục từ bản thiết kế chuyên nghiệp 11:40 PM.
 */
public class EditDatPhongDialog extends JDialog {

    private final DatPhongService datPhongService = new DatPhongService();
    private boolean confirmed = false;
    private final DatPhong dp;

    private DateTimePicker txtCheckIn, txtCheckOut;
    private JTextField txtSoKhach;
    private JTextArea txtGhiChu;
    private JTextField txtTienCoc;

    public EditDatPhongDialog(Frame parent, DatPhong dp) {
        super(parent, "Sửa thông tin đặt phòng", true);
        this.dp = dp;
        
        setLayout(new BorderLayout());
        setSize(500, 600);
        setLocationRelativeTo(parent);
        
        buildUI();
        loadData();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);
        root.setBorder(new EmptyBorder(20, 20, 20, 20));

        // Header
        JLabel lblHeader = new JLabel("CHỈNH SỬA ĐƠN ĐẶT #" + dp.getMaDatPhong());
        lblHeader.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblHeader.setForeground(UIConstants.PRIMARY);
        root.add(lblHeader, BorderLayout.NORTH);

        // Body
        JPanel body = new JPanel(new GridBagLayout());
        body.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 0, 8, 0);
        gbc.weightx = 1.0;

        int row = 0;

        // Check-in
        gbc.gridy = row++;
        body.add(new JLabel("Thời điểm nhận phòng dự kiến:"), gbc);
        txtCheckIn = new DateTimePicker(new Date());
        gbc.gridy = row++;
        body.add(txtCheckIn, gbc);

        // Check-out
        gbc.gridy = row++;
        body.add(new JLabel("Thời điểm trả phòng dự kiến:"), gbc);
        txtCheckOut = new DateTimePicker(new Date());
        gbc.gridy = row++;
        body.add(txtCheckOut, gbc);

        // Số khách
        gbc.gridy = row++;
        body.add(new JLabel("Số lượng khách:"), gbc);
        txtSoKhach = new JTextField();
        txtSoKhach.setPreferredSize(new Dimension(0, 40));
        gbc.gridy = row++;
        body.add(txtSoKhach, gbc);

        // Tiền cọc
        gbc.gridy = row++;
        body.add(new JLabel("Tiền đặt cọc (VNĐ):"), gbc);
        txtTienCoc = new JTextField();
        txtTienCoc.setPreferredSize(new Dimension(0, 40));
        gbc.gridy = row++;
        body.add(txtTienCoc, gbc);

        // Ghi chú
        gbc.gridy = row++;
        body.add(new JLabel("Ghi chú:"), gbc);
        txtGhiChu = new JTextArea(4, 20);
        txtGhiChu.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER));
        gbc.gridy = row++;
        body.add(new JScrollPane(txtGhiChu), gbc);

        root.add(body, BorderLayout.CENTER);

        // Buttons
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        footer.setOpaque(false);
        footer.setBorder(new EmptyBorder(20, 0, 0, 0));

        RoundedButton btnCancel = new RoundedButton("Hủy bỏ", UIConstants.DANGER_LIGHT, UIConstants.DANGER);
        btnCancel.addActionListener(e -> dispose());
        
        RoundedButton btnSave = new RoundedButton("Lưu thay đổi", UIConstants.SUCCESS, Color.WHITE);
        btnSave.addActionListener(e -> doSave());

        footer.add(btnCancel);
        footer.add(btnSave);
        root.add(footer, BorderLayout.SOUTH);

        add(root);
    }

    private void loadData() {
        if (dp.getNgayNhanDuKien() != null) {
            txtCheckIn.setDate(java.sql.Timestamp.valueOf(dp.getNgayNhanDuKien()));
        }
        if (dp.getNgayTraDuKien() != null) {
            txtCheckOut.setDate(java.sql.Timestamp.valueOf(dp.getNgayTraDuKien()));
        }
        txtSoKhach.setText(String.valueOf(dp.getSoLuongKhach()));
        txtTienCoc.setText(String.format("%.0f", dp.getTienDatCoc()));
        txtGhiChu.setText(dp.getGhiChu());
    }

    private void doSave() {
        try {
            LocalDateTime in = txtCheckIn.getLocalDateTime();
            LocalDateTime out = txtCheckOut.getLocalDateTime();
            
            if (out.isBefore(in)) {
                NotificationManager.showError("Lỗi", "Ngày trả không thể trước ngày nhận!");
                return;
            }

            dp.setNgayNhanDuKien(in);
            dp.setNgayTraDuKien(out);
            dp.setSoLuongKhach(Integer.parseInt(txtSoKhach.getText()));
            dp.setTienDatCoc(Double.parseDouble(txtTienCoc.getText()));
            dp.setGhiChu(txtGhiChu.getText());

            String err = datPhongService.suaDatPhong(dp);
            if (err == null) {
                confirmed = true;
                NotificationManager.showSuccess("Thành công", "Đã cập nhật thông tin đơn đặt phòng!");
                dispose();
            } else {
                NotificationManager.showError("Lỗi", err);
            }
        } catch (NumberFormatException ex) {
            NotificationManager.showError("Lỗi", "Vui lòng nhập số hợp lệ!");
        }
    }

    public boolean isConfirmed() { return confirmed; }
}
