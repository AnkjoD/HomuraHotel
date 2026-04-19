package ui.dialogs;

import entity.DatPhong;
import entity.ChiTietDatPhong;
import entity.Phong;
import entity.enums.TrangThaiDatPhong;
import service.ThuePhongService;
import ui.components.UIConstants;
import ui.components.RoundedComponents.*;
import ui.panels.DatPhongPanel;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.List;

public class DatPhongDetailDialog extends JDialog {

    private final DatPhong datPhong;
    private final DatPhongPanel parentPanel;

    public DatPhongDetailDialog(Frame parent, DatPhong dp, DatPhongPanel parentPanel) {
        super(parent, "Chi tiết Đơn đặt phòng", true);
        this.datPhong = dp;
        this.parentPanel = parentPanel;
        
        setSize(520, 600);
        setLocationRelativeTo(parent);
        setResizable(false);
        setBackground(new Color(0xF8FAFC));
        
        buildUI();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(0xF8FAFC));

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UIConstants.PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));
        
        JLabel lblTitle = new JLabel("Mã đặt phòng: " + datPhong.getMaDatPhong());
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTitle.setForeground(Color.WHITE);
        
        JLabel lblStatus = new JLabel(UIConstants.getTrangThaiDatPhongLabel(datPhong.getTrangThai()));
        lblStatus.setFont(UIConstants.FONT_SMALL_BOLD);
        lblStatus.setForeground(UIConstants.getTrangThaiDatPhongColor(datPhong.getTrangThai()));
        lblStatus.setOpaque(true);
        lblStatus.setBackground(new Color(255, 255, 255, 230));
        lblStatus.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        header.add(lblTitle, BorderLayout.WEST);
        header.add(lblStatus, BorderLayout.EAST);
        
        // Content
        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setBackground(Color.WHITE);
        content.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        content.add(createSectionTitle("THÔNG TIN KHÁCH HÀNG"));
        content.add(createDetailRow("Họ và tên:", datPhong.getTenKhachHang()));
        content.add(createDetailRow("Số điện thoại:", datPhong.getKhachHang() != null ? datPhong.getKhachHang().getSoDienThoai() : "—"));
        content.add(createDetailRow("Số lượng khách:", datPhong.getSoLuongKhach() + " người"));
        content.add(createDetailRow("Loại khách:", datPhong.getLoaiKhachLabel()));
        if (datPhong.isDoan() && datPhong.getTenDoan() != null && !datPhong.getTenDoan().isEmpty()) {
            content.add(createDetailRow("Tên đoàn:", datPhong.getTenDoan()));
        }
        content.add(Box.createVerticalStrut(15));
        
        List<ChiTietDatPhong> chiTietList = datPhong.getDsChiTiet();

        content.add(createSectionTitle("THÔNG TIN PHÒNG"));
        if (chiTietList != null && !chiTietList.isEmpty()) {
            boolean first = true;
            for (ChiTietDatPhong ct : chiTietList) {
                Phong p = ct.getPhong();
                if (p != null) {
                    content.add(createDetailRow(first ? "Phòng:" : "", p.getMaPhong() + " (" + p.getTenLoaiPhong() + ")"));
                    first = false;
                }
            }
        } else {
            String phongStr = datPhong.getSoPhong() != null ? datPhong.getSoPhong() : "—";
            String loaiPhongStr = datPhong.getTenLoaiPhong() != null ? datPhong.getTenLoaiPhong() : "—";
            content.add(createDetailRow("Phòng:", phongStr + " (" + loaiPhongStr + ")"));
        }
        
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        String checkin = datPhong.getNgayNhanDK_Date() != null ? sdf.format(datPhong.getNgayNhanDK_Date()) : "—";
        String checkout = datPhong.getNgayTraDK_Date() != null ? sdf.format(datPhong.getNgayTraDK_Date()) : "—";
        
        content.add(createDetailRow("Check-in dự kiến:", checkin));
        content.add(createDetailRow("Check-out dự kiến:", checkout));
        
        long soNgay = (datPhong.getNgayNhanDK_Date() != null && datPhong.getNgayTraDK_Date() != null)
                ? new ThuePhongService().tinhSoNgay(datPhong.getNgayNhanDK_Date(), datPhong.getNgayTraDK_Date())
                : 0;
        content.add(createDetailRow("Thời gian lưu trú:", soNgay + " đêm"));
        content.add(Box.createVerticalStrut(15));

        content.add(createSectionTitle("THANH TOÁN"));
        if (chiTietList != null && !chiTietList.isEmpty()) {
            boolean first = true;
            for (ChiTietDatPhong ct : chiTietList) {
                String labelStr = first ? "Giá phòng:" : "";
                String valueStr = String.format("%,.0f đ/đêm", ct.getGiaThucTeChot());
                if (ct.getPhong() != null) {
                    valueStr += " (P." + ct.getPhong().getMaPhong() + ")";
                }
                content.add(createDetailRow(labelStr, valueStr));
                first = false;
            }
        } else {
            content.add(createDetailRow("Giá phòng:", "—"));
        }
        content.add(createDetailRow("Tiền cọc:", datPhong.getTienDatCoc() > 0 ? String.format("%,.0f đ", datPhong.getTienDatCoc()) : "Không cọc"));
        content.add(Box.createVerticalStrut(15));

        content.add(createSectionTitle("GHI CHÚ"));
        JTextArea txtGhiChu = new JTextArea(datPhong.getGhiChu() != null && !datPhong.getGhiChu().trim().isEmpty() ? datPhong.getGhiChu() : "Không có ghi chú");
        txtGhiChu.setFont(UIConstants.FONT_BODY);
        txtGhiChu.setLineWrap(true);
        txtGhiChu.setWrapStyleWord(true);
        txtGhiChu.setEditable(false);
        txtGhiChu.setBackground(new Color(0xF8FAFC));
        txtGhiChu.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        txtGhiChu.setAlignmentX(Component.LEFT_ALIGNMENT);
        content.add(txtGhiChu);

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setBorder(null);

        // Footer buttons
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER));

        RoundedButton btnClose = new RoundedButton("Đóng", new Color(0xE2E8F0), UIConstants.TEXT_PRIMARY);
        btnClose.setPreferredSize(new Dimension(100, 36));
        btnClose.addActionListener(e -> dispose());
        
        footer.add(btnClose);

        root.add(header, BorderLayout.NORTH);
        root.add(scrollPane, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private JLabel createSectionTitle(String title) {
        JLabel lbl = new JLabel(title);
        lbl.setFont(UIConstants.FONT_BODY_BOLD);
        lbl.setForeground(UIConstants.PRIMARY);
        lbl.setBorder(BorderFactory.createEmptyBorder(10, 0, 8, 0));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JPanel createDetailRow(String label, String value) {
        JPanel row = new JPanel(new BorderLayout());
        row.setBackground(Color.WHITE);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        row.setAlignmentX(Component.LEFT_ALIGNMENT);

        JLabel lblLeft = new JLabel(label);
        lblLeft.setFont(UIConstants.FONT_BODY);
        lblLeft.setForeground(UIConstants.TEXT_SECONDARY);
        lblLeft.setPreferredSize(new Dimension(140, 20));

        JLabel lblRight = new JLabel(value);
        lblRight.setFont(new Font("Segoe UI", Font.BOLD, 14));
        lblRight.setForeground(UIConstants.TEXT_PRIMARY);

        row.add(lblLeft, BorderLayout.WEST);
        row.add(lblRight, BorderLayout.CENTER);
        row.setBorder(BorderFactory.createEmptyBorder(3, 0, 3, 0));
        return row;
    }
}
