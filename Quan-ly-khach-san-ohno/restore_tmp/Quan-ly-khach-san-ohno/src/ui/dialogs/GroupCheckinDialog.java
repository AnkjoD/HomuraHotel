package ui.dialogs;

import service.ThuePhongService;
import entity.DatPhong;
import entity.ChiTietDatPhong;
import entity.KhachHang;
import ui.components.UIConstants;
import ui.components.RoundedComponents.RoundedButton;
import ui.components.RoundedComponents.RoundedPanel;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class GroupCheckinDialog extends JDialog {

    private final ThuePhongService thuePhongService = new ThuePhongService();
    private final DatPhong datPhong;
    private boolean isConfirmed = false;

    public GroupCheckinDialog(Frame parent, DatPhong dp) {
        super(parent, "Group Check-in - Mã đoàn: " + dp.getMaDatPhong(), true);
        this.datPhong = dp;
        setSize(540, 480);
        setLocationRelativeTo(parent);
        setResizable(false);
        getContentPane().setBackground(Color.WHITE);
        buildUI();
    }

    private void buildUI() {
        setLayout(new BorderLayout());

        // Top info
        JPanel top = new JPanel(new BorderLayout());
        top.setBackground(Color.WHITE);
        top.setBorder(BorderFactory.createEmptyBorder(20, 24, 10, 24));

        JLabel lblTitle = new JLabel("Tiến hành Check-in Hàng loạt");
        lblTitle.setFont(UIConstants.FONT_TITLE);
        
        String loaiKhachText = datPhong.isDoan() ? "Đoàn: " + datPhong.getTenDoan() : "Đa phòng";
        JLabel lblInfo = new JLabel(loaiKhachText + " | Nhận phòng: " + datPhong.getDanhSachTenPhong());
        lblInfo.setFont(UIConstants.FONT_BODY);
        lblInfo.setForeground(UIConstants.PRIMARY);

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        titleBox.add(lblTitle);
        titleBox.add(Box.createVerticalStrut(5));
        titleBox.add(lblInfo);
        top.add(titleBox, BorderLayout.WEST);
        add(top, BorderLayout.NORTH);

        // Center Box
        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.setBorder(BorderFactory.createEmptyBorder(10, 24, 10, 24));

        RoundedPanel infoCard = new RoundedPanel(UIConstants.CARD_RADIUS);
        infoCard.setBackground(new Color(0xF8FAFC));
        infoCard.setLayout(new BoxLayout(infoCard, BoxLayout.Y_AXIS));
        infoCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(new Color(0xE2E8F0), 1),
            BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));

        KhachHang kh = datPhong.getKhachHang();
        String phuTrach = kh != null ? kh.getHoTen() : "Không rõ";
        String sdt = kh != null ? kh.getSoDienThoai() : "Không rõ";

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        Date in = datPhong.getNgayNhanDK_Date();
        Date out = datPhong.getNgayTraDK_Date();

        List<ChiTietDatPhong> ds = datPhong.getDsChiTiet();
        int roomCount = ds != null ? ds.size() : 0;

        infoCard.add(summaryRow("Người phụ trách:", phuTrach + " (" + sdt + ")", UIConstants.TEXT_PRIMARY));
        infoCard.add(Box.createVerticalStrut(8));
        infoCard.add(summaryRow("Số lượng phòng:", roomCount + " phòng", new Color(0x059669)));
        infoCard.add(Box.createVerticalStrut(8));
        infoCard.add(summaryRow("Ngày nhận (Dự kiến):", in != null ? sdf.format(in) : "—", UIConstants.TEXT_PRIMARY));
        infoCard.add(Box.createVerticalStrut(8));
        infoCard.add(summaryRow("Ngày trả (Dự kiến):", out != null ? sdf.format(out) : "—", UIConstants.TEXT_PRIMARY));
        
        infoCard.add(Box.createVerticalStrut(15));
        JSeparator sep = new JSeparator();
        sep.setForeground(new Color(0xCBD5E1));
        sep.setMaximumSize(new Dimension(800, 1));
        infoCard.add(sep);
        infoCard.add(Box.createVerticalStrut(15));
        
        JLabel warning = new JLabel("<html><body style='width: 380px;'>"
            + "<b style='color:#b45309;'>QUAN TRỌNG:</b> Hành động này sẽ thay đổi trạng thái của <b>" + roomCount + " phòng</b> sang trạng thái Đang ở."
            + " Thông tin người lưu trú từng phòng sẽ lấy theo tên người phụ trách chung. Bạn có thể cập nhật lại từng cá nhân ở Quản lý Khách sạn sau."
            + "</body></html>");
        warning.setFont(UIConstants.FONT_SMALL);
        warning.setForeground(new Color(0x92400E));
        infoCard.add(warning);

        center.add(infoCard, BorderLayout.NORTH);
        add(center, BorderLayout.CENTER);

        // Bottom
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        bottom.setBackground(Color.WHITE);
        bottom.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER),
            BorderFactory.createEmptyBorder(15, 24, 15, 24)
        ));

        RoundedButton btnCancel = new RoundedButton("Hủy bỏ", new Color(0xE2E8F0), UIConstants.TEXT_PRIMARY);
        RoundedButton btnOk = new RoundedButton("✓ Xác nhận Check-in (" + roomCount + " phòng)", UIConstants.SUCCESS, Color.WHITE);
        btnOk.setFont(UIConstants.FONT_BODY_BOLD);

        btnCancel.addActionListener(e -> dispose());
        btnOk.addActionListener(e -> doGroupCheckin());

        bottom.add(btnCancel);
        bottom.add(btnOk);
        add(bottom, BorderLayout.SOUTH);
    }

    private JPanel summaryRow(String key, String val, Color valColor) {
        JPanel r = new JPanel(new BorderLayout(10, 0));
        r.setOpaque(false);
        r.setMaximumSize(new Dimension(800, 22));
        r.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel k = new JLabel(key);
        k.setFont(UIConstants.FONT_SMALL_BOLD);
        k.setForeground(new Color(0x64748B));
        k.setPreferredSize(new Dimension(140, 20));
        JLabel v = new JLabel(val);
        v.setFont(UIConstants.FONT_BODY);
        v.setForeground(valColor);
        r.add(k, BorderLayout.WEST);
        r.add(v, BorderLayout.CENTER);
        return r;
    }

    private void doGroupCheckin() {
        // Do the batch check in
        String err = thuePhongService.checkInGroup(datPhong);
        
        if (err != null) {
            JOptionPane.showMessageDialog(this, "Có lỗi xảy ra trong quá trình Check-in Hàng loạt:\n" + err, "Lỗi Check-in", JOptionPane.ERROR_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this, "Đã check-in thành công toàn bộ " + datPhong.getDsChiTiet().size() + " phòng!", "Hoàn tất", JOptionPane.INFORMATION_MESSAGE);
            isConfirmed = true;
            dispose();
        }
    }

    public boolean isConfirmed() {
        return isConfirmed;
    }
}
