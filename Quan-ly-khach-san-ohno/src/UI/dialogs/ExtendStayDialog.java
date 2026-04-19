package ui.dialogs;

import entity.ChiTietDatPhong;
import entity.DatPhong;
import entity.Phong;
import service.ThuePhongService;
import service.DatPhongService;
import ui.components.UIConstants;
import ui.components.RoundedComponents.*;
import ui.components.DateTimePicker;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public class ExtendStayDialog extends JDialog {

    private final ChiTietDatPhong chiTietDatPhong;
    private final DatPhong datPhong;
    private final ThuePhongService thuePhongService = new ThuePhongService();
    private final DatPhongService datPhongService = new DatPhongService();

    private DateTimePicker dtpNewCheckout;
    private JLabel lblCurrentStay, lblExtractedInfo, lblExtraCost, lblTotalEstimate;
    private JLabel lblAvailability;
    
    private boolean confirmed = false;
    private LocalDateTime newCheckoutTime;
    private final DateTimeFormatter dtf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public ExtendStayDialog(Frame parent, ChiTietDatPhong ctdp) {
        super(parent, "Gia hạn lưu trú - EXTEND STAY", true);
        this.chiTietDatPhong = ctdp;
        this.datPhong = ctdp.getDatPhong();

        setSize(600, 520);
        setLocationRelativeTo(parent);
        setResizable(false);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        buildUI();
        calculateImpact();
    }

    private void buildUI() {
        // --- Header ---
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(0x334155));
        header.setPreferredSize(new Dimension(0, 70));
        header.setBorder(new EmptyBorder(0, 25, 0, 25));

        JLabel lblTitle = new JLabel("GIA HẠN LƯU TRÚ (STAY EXTENSION)");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(Color.WHITE);
        header.add(lblTitle, BorderLayout.CENTER);
        
        Phong p = chiTietDatPhong.getPhong();
        JLabel lblRoom = new JLabel("Phòng " + (p != null ? p.getSoPhong() : "N/A") + " - Khách: " + 
                                   (datPhong.getKhachHang() != null ? datPhong.getKhachHang().getHoTen() : "Khách lẻ"));
        lblRoom.setFont(UIConstants.FONT_SMALL);
        lblRoom.setForeground(new Color(0xCBD5E1));
        header.add(lblRoom, BorderLayout.SOUTH);
        
        add(header, BorderLayout.NORTH);

        // --- Content ---
        JPanel main = new JPanel();
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.setOpaque(false);
        main.setBorder(new EmptyBorder(25, 30, 25, 30));

        // 1. Current Stay Info
        JPanel currentPanel = new JPanel(new BorderLayout());
        currentPanel.setOpaque(false);
        currentPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        lblCurrentStay = new JLabel("Ngày trả hiện tại: " + datPhong.getNgayTraDuKien().format(dtf));
        lblCurrentStay.setFont(UIConstants.FONT_BODY_BOLD);
        lblCurrentStay.setForeground(new Color(0xD97706));
        currentPanel.add(lblCurrentStay, BorderLayout.WEST);
        main.add(currentPanel);
        main.add(Box.createVerticalStrut(20));

        // 2. New Checkout Picker
        main.add(new JLabel("Chọn ngày trả mới:"));
        main.add(Box.createVerticalStrut(8));
        dtpNewCheckout = new DateTimePicker(java.sql.Timestamp.valueOf(datPhong.getNgayTraDuKien().plusDays(1)));
        dtpNewCheckout.addChangeListener(e -> calculateImpact());
        main.add(dtpNewCheckout);
        main.add(Box.createVerticalStrut(15));
        
        // 3. Availability Check
        lblAvailability = new JLabel("✓ Phòng sẵn sàng cho khoảng thời gian này");
        lblAvailability.setFont(UIConstants.FONT_SMALL_BOLD);
        lblAvailability.setForeground(UIConstants.SUCCESS);
        main.add(lblAvailability);
        main.add(Box.createVerticalStrut(25));

        // 4. Summary Box
        JPanel summaryBox = new JPanel();
        summaryBox.setLayout(new BoxLayout(summaryBox, BoxLayout.Y_AXIS));
        summaryBox.setBackground(new Color(0xF8FAFC));
        summaryBox.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.BORDER),
            new EmptyBorder(15, 20, 15, 20)
        ));
        
        lblExtractedInfo = new JLabel("Gia hạn thêm: 0 đêm");
        lblExtraCost = new JLabel("Phí phát sinh dự kiến: 0đ");
        lblTotalEstimate = new JLabel("Tổng tiền phòng sau gia hạn: 0đ");
        
        lblExtractedInfo.setFont(UIConstants.FONT_BODY);
        lblExtraCost.setFont(UIConstants.FONT_BODY);
        lblTotalEstimate.setFont(UIConstants.FONT_BODY_BOLD);
        lblTotalEstimate.setForeground(UIConstants.PRIMARY);

        summaryBox.add(lblExtractedInfo);
        summaryBox.add(Box.createVerticalStrut(8));
        summaryBox.add(lblExtraCost);
        summaryBox.add(Box.createVerticalStrut(15));
        summaryBox.add(new JSeparator());
        summaryBox.add(Box.createVerticalStrut(10));
        summaryBox.add(lblTotalEstimate);
        
        main.add(summaryBox);
        add(main, BorderLayout.CENTER);

        // --- Footer Buttons ---
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        footer.setBackground(new Color(0xF1F5F9));
        RoundedButton btnCancel = new RoundedButton("Hủy bỏ", new Color(0xE2E8F0), UIConstants.TEXT_PRIMARY);
        RoundedButton btnConfirm = new RoundedButton("✓ Xác nhận gia hạn", UIConstants.PRIMARY, Color.WHITE);
        
        btnCancel.addActionListener(e -> dispose());
        btnConfirm.addActionListener(e -> handleConfirm());
        
        footer.add(btnCancel);
        footer.add(btnConfirm);
        add(footer, BorderLayout.SOUTH);
    }

    private void calculateImpact() {
        java.util.Date selectedDate = dtpNewCheckout.getDate();
        LocalDateTime newDate = selectedDate.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime();
        LocalDateTime oldDate = datPhong.getNgayTraDuKien();
        LocalDateTime arrival = datPhong.getNgayNhanDuKien();

        if (newDate.isBefore(oldDate)) {
            lblAvailability.setText("⚠ Ngày trả mới phải sau ngày trả hiện tại!");
            lblAvailability.setForeground(Color.RED);
            return;
        }

        // 1. Check Availability (Real check)
        boolean isAvailable = new dao.PhongDAO().isRoomAvailable(
                chiTietDatPhong.getPhong().getMaPhong(), 
                oldDate, 
                newDate, 
                datPhong.getMaDatPhong());
                
        if (!isAvailable) {
            lblAvailability.setText("⚠ XUNG ĐỘT: Phòng đã có khách đặt trong khoảng thời gian này!");
            lblAvailability.setForeground(java.awt.Color.RED);
            this.newCheckoutTime = null; // Chặn xác nhận
            return;
        } else {
            lblAvailability.setText("✓ Phòng trống sẵn sàng cho gia hạn");
            lblAvailability.setForeground(UIConstants.SUCCESS);
        }

        // 2. Calculate nights
        long oldNights = ChronoUnit.DAYS.between(arrival.toLocalDate(), oldDate.toLocalDate());
        long newNights = ChronoUnit.DAYS.between(arrival.toLocalDate(), newDate.toLocalDate());
        if (newNights == oldNights && newDate.isAfter(oldDate)) newNights++; // Handle same-day extension logic if needed

        long extraNights = newNights - oldNights;
        double rate = chiTietDatPhong.getGiaThucTeChot();
        double extraCost = extraNights * rate;
        double totalCost = newNights * rate;

        lblExtractedInfo.setText("Gia hạn thêm: " + extraNights + " đêm (Tổng " + newNights + " đêm)");
        lblExtraCost.setText("Phí phát sinh dự kiến: " + String.format("%,.0fđ", extraCost));
        lblTotalEstimate.setText("Tổng tiền phòng ước tính: " + String.format("%,.0fđ", totalCost));
        
        this.newCheckoutTime = newDate;
    }

    private void handleConfirm() {
        if (newCheckoutTime == null || newCheckoutTime.isBefore(datPhong.getNgayTraDuKien())) {
            JOptionPane.showMessageDialog(this, "Ngày gia hạn không hợp lệ!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }
        confirmed = true;
        dispose();
    }

    public boolean isConfirmed() { return confirmed; }
    public LocalDateTime getNewCheckoutTime() { return newCheckoutTime; }
}
