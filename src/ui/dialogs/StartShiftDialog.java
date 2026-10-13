package ui.dialogs;

import entity.GiaoCa;
import entity.NhanVien;
import service.AuthService;
import service.GiaoCaService;
import ui.components.RoundedComponents.*;
import ui.components.UIConstants;
import java.awt.*;
import java.time.format.DateTimeFormatter;
import javax.swing.*;
import javax.swing.border.EmptyBorder;

public class StartShiftDialog extends JDialog {
    private final GiaoCaService service = new GiaoCaService();
    private final Frame parentFrame;
    private GiaoCa lastClosedShift;
    private boolean confirmed = false;

    private JTextField txtTienThucTe;
    private JLabel lblVariance;
    private JTextArea txtGhiChu;
    private double expectedHandoverAmount = 0.0;

    public StartShiftDialog(Frame owner) {
        super(owner, "Khởi động ca làm việc mới", true);
        this.parentFrame = owner;
        // Luôn lấy ca CLOSED mới nhất bất kỳ — chỉ có 1 két tiền vật lý,
        // tất cả nhân viên đều phải đối chiếu từ cùng 1 mốc bàn giao cuối cùng.
        this.lastClosedShift = service.getLastClosedShift();
        if (lastClosedShift != null) {
            expectedHandoverAmount = lastClosedShift.getTienMatBanGiao();
        }

        setupUI();
    }

    private void setupUI() {
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        // Ngăn không cho đóng bằng nút X trên thanh tiêu đề
        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent e) {
                handleLogout();
            }
        });

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);

        // ===== 1. HEADER GRADIENT =====
        JPanel header = new JPanel(new BorderLayout()) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setPaint(new GradientPaint(0, 0, UIConstants.PRIMARY, 0, getHeight(), UIConstants.PRIMARY_DARK));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.dispose();
            }
        };
        header.setPreferredSize(new Dimension(0, 90));
        header.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

        JLabel title = new JLabel("Bắt đầu ca trực mới");
        title.setFont(new Font("Segoe UI", Font.BOLD, 22));
        title.setForeground(Color.WHITE);

        NhanVien user = AuthService.getInstance().getCurrentUser();
        String staffName = user != null ? user.getHoTen() : "Nhân viên";
        String staffId = user != null ? user.getMaNhanVien() : "—";
        JLabel subtitle = new JLabel("Nhân viên trực ca: " + staffName + " (" + staffId + ")");
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(new Color(220, 230, 255));

        JPanel headerText = new JPanel();
        headerText.setOpaque(false);
        headerText.setLayout(new BoxLayout(headerText, BoxLayout.Y_AXIS));
        headerText.add(title);
        headerText.add(Box.createVerticalStrut(2));
        headerText.add(subtitle);
        header.add(headerText, BorderLayout.WEST);
        root.add(header, BorderLayout.NORTH);

        // ===== 2. BODY CONTENT =====
        JPanel body = new JPanel(new GridBagLayout());
        body.setBackground(Color.WHITE);
        body.setBorder(BorderFactory.createEmptyBorder(20, 24, 15, 24));
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;
        g.weightx = 1.0;
        g.insets = new Insets(0, 0, 14, 0);

        int gridY = 0;

        // -- Dashboard Ca Trước Bàn Giao --
        JPanel pnlLastShift = new JPanel(new GridBagLayout());
        pnlLastShift.setBackground(new Color(0xF8FAFC));
        pnlLastShift.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE2E8F0), 1),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));

        GridBagConstraints gcSub = new GridBagConstraints();
        gcSub.fill = GridBagConstraints.HORIZONTAL;
        gcSub.weightx = 1.0;
        gcSub.gridy = 0;

        JLabel lblLastTitle = new JLabel("Thông tin két tiền bàn giao ca trước");
        lblLastTitle.setFont(UIConstants.FONT_BODY_BOLD);
        lblLastTitle.setForeground(UIConstants.TEXT_PRIMARY);
        pnlLastShift.add(lblLastTitle, gcSub);

        gcSub.gridy++;
        gcSub.insets = new Insets(8, 0, 0, 0);

        if (lastClosedShift != null) {
            String timeStr = lastClosedShift.getThoiGianKetThuc() != null
                    ? lastClosedShift.getThoiGianKetThuc().format(DateTimeFormatter.ofPattern("HH:mm - dd/MM/yyyy"))
                    : "—";
            String giverName = lastClosedShift.getNhanVien() != null ? lastClosedShift.getNhanVien().getHoTen() : "—";
            String giverId = lastClosedShift.getNhanVien() != null ? lastClosedShift.getNhanVien().getMaNhanVien() : "—";

            JLabel lblGiver = new JLabel("<html>• Người bàn giao ca trước: <b>" + giverName + "</b> (" + giverId + ")</html>");
            lblGiver.setFont(UIConstants.FONT_SMALL);
            lblGiver.setForeground(UIConstants.TEXT_SECONDARY);
            pnlLastShift.add(lblGiver, gcSub);

            gcSub.gridy++;
            JLabel lblTime = new JLabel("• Thời gian bàn giao két: " + timeStr);
            lblTime.setFont(UIConstants.FONT_SMALL);
            lblTime.setForeground(UIConstants.TEXT_SECONDARY);
            pnlLastShift.add(lblTime, gcSub);

            // Kiểm tra người được chỉ định nhận bàn giao
            String assignedRecipientId = lastClosedShift.getMaNhanVienNhan();
            if (assignedRecipientId != null && !assignedRecipientId.equals(AuthService.getInstance().getCurrentMaNV())) {
                dao.NhanVienDAO nvDAO = new dao.NhanVienDAO();
                NhanVien assignedRecipient = nvDAO.getById(assignedRecipientId);
                String assignedName = assignedRecipient != null ? assignedRecipient.getHoTen() : assignedRecipientId;

                gcSub.gridy++;
                JLabel lblWarning = new JLabel("<html>⚠️ <font color='#D97706'><b>Cảnh báo:</b> Ca trước chỉ định bàn giao cho <b>" + assignedName + "</b>, nhưng bạn đang đăng nhập nhận ca.</font></html>");
                lblWarning.setFont(UIConstants.FONT_SMALL);
                pnlLastShift.add(lblWarning, gcSub);
            }

            gcSub.gridy++;
            gcSub.insets = new Insets(10, 0, 0, 0);
            JLabel lblAmount = new JLabel(String.format("Số tiền mặt bàn giao ca trước: %,.0f VNĐ", expectedHandoverAmount));
            lblAmount.setFont(new Font("Segoe UI", Font.BOLD, 14));
            lblAmount.setForeground(UIConstants.PRIMARY);
            pnlLastShift.add(lblAmount, gcSub);
        } else {
            JLabel lblNoShift = new JLabel("• Hệ thống chưa ghi nhận ca làm việc bàn giao trước đó.");
            lblNoShift.setFont(UIConstants.FONT_SMALL);
            lblNoShift.setForeground(UIConstants.TEXT_MUTED);
            pnlLastShift.add(lblNoShift, gcSub);

            gcSub.gridy++;
            gcSub.insets = new Insets(10, 0, 0, 0);
            JLabel lblAmount = new JLabel("Số tiền mặt bàn giao ca trước: 0 VNĐ");
            lblAmount.setFont(new Font("Segoe UI", Font.BOLD, 14));
            lblAmount.setForeground(UIConstants.PRIMARY);
            pnlLastShift.add(lblAmount, gcSub);
        }

        g.gridy = gridY++;
        body.add(pnlLastShift, g);

        // -- Ô nhập tiền mặt thực tế đầu ca --
        g.gridy = gridY++;
        JLabel lblTien = new JLabel("Số tiền mặt thực tế trong két đầu ca (VNĐ) *");
        lblTien.setFont(UIConstants.FONT_BODY_BOLD);
        lblTien.setForeground(UIConstants.TEXT_PRIMARY);
        body.add(lblTien, g);

        g.gridy = gridY++;
        txtTienThucTe = new JTextField(String.format("%.0f", expectedHandoverAmount));
        txtTienThucTe.setFont(new Font("Segoe UI", Font.BOLD, 16));
        txtTienThucTe.setPreferredSize(new Dimension(0, 42));
        txtTienThucTe.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225), 1),
                BorderFactory.createEmptyBorder(4, 12, 4, 12)));
        body.add(txtTienThucTe, g);

        // -- Nhãn hiển thị chênh lệch két --
        g.gridy = gridY++;
        lblVariance = new JLabel("Chênh lệch két đầu ca: 0 VNĐ (Khớp)");
        lblVariance.setFont(UIConstants.FONT_BODY_BOLD);
        lblVariance.setForeground(new Color(0x10B981)); // Khớp = Xanh lá
        body.add(lblVariance, g);

        // Live calculation on key type
        txtTienThucTe.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyReleased(java.awt.event.KeyEvent e) {
                updateDiscrepancy();
            }
        });

        // -- Ô nhập Ghi chú đầu ca --
        g.gridy = gridY++;
        JLabel lblNote = new JLabel("Ghi chú / Lưu ý đầu ca");
        lblNote.setFont(UIConstants.FONT_BODY_BOLD);
        lblNote.setForeground(UIConstants.TEXT_PRIMARY);
        body.add(lblNote, g);

        g.gridy = gridY++;
        txtGhiChu = new JTextArea(3, 20);
        txtGhiChu.setFont(UIConstants.FONT_BODY);
        txtGhiChu.setLineWrap(true);
        txtGhiChu.setWrapStyleWord(true);
        txtGhiChu.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225), 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        body.add(new JScrollPane(txtGhiChu), g);

        // -- Dòng nhắc nhở đếm két --
        g.gridy = gridY++;
        g.insets = new Insets(0, 0, 5, 0);
        JLabel lblPrompt = new JLabel("<html><i>* Lưu ý: Lễ tân bắt buộc phải đếm kỹ tiền mặt trong két trước khi nhận ca trực để chịu trách nhiệm số dư tài chính trong ca.</i></html>");
        lblPrompt.setFont(UIConstants.FONT_SMALL);
        lblPrompt.setForeground(UIConstants.TEXT_SECONDARY);
        body.add(lblPrompt, g);

        root.add(body, BorderLayout.CENTER);

        // ===== 3. FOOTER BUTTONS =====
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 14));
        footer.setBackground(new Color(0xF8FAFC));
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xE2E8F0)));

        RoundedButton btnLogout = RoundedButton.outline("Hủy & Đăng xuất", UIConstants.DANGER);
        btnLogout.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnLogout.setPreferredSize(new Dimension(150, 40));
        btnLogout.addActionListener(e -> handleLogout());

        RoundedButton btnStart = new RoundedButton("Xác nhận bắt đầu ca", UIConstants.PRIMARY, Color.WHITE);
        btnStart.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnStart.setPreferredSize(new Dimension(180, 40));
        btnStart.addActionListener(e -> handleConfirmStart());
        getRootPane().setDefaultButton(btnStart);

        footer.add(btnLogout);
        footer.add(btnStart);
        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
        pack();
        setSize(460, 610);
        setResizable(false);
        setLocationRelativeTo(parentFrame);
    }

    private void updateDiscrepancy() {
        try {
            String cleanText = txtTienThucTe.getText().trim().replace(",", "").replace(".", "");
            if (cleanText.isEmpty()) {
                lblVariance.setText("Vui lòng nhập tiền mặt đầu ca!");
                lblVariance.setForeground(UIConstants.DANGER);
                return;
            }
            double actual = Double.parseDouble(cleanText);
            double diff = actual - expectedHandoverAmount;

            if (diff == 0) {
                lblVariance.setText("Chênh lệch két đầu ca: 0 VNĐ (Khớp)");
                lblVariance.setForeground(new Color(0x10B981));
            } else if (diff > 0) {
                lblVariance.setText(String.format("Chênh lệch két đầu ca: +%,.0f VNĐ (Thừa)", diff));
                lblVariance.setForeground(UIConstants.WARNING);
            } else {
                lblVariance.setText(String.format("Chênh lệch két đầu ca: -%,.0f VNĐ (Thiếu)", Math.abs(diff)));
                lblVariance.setForeground(UIConstants.DANGER);
            }
        } catch (NumberFormatException e) {
            lblVariance.setText("Số tiền nhập vào không hợp lệ!");
            lblVariance.setForeground(UIConstants.DANGER);
        }
    }

    private void handleConfirmStart() {
        try {
            String cleanText = txtTienThucTe.getText().trim().replace(",", "").replace(".", "");
            if (cleanText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Số tiền mặt đầu ca không được để trống!", "Lỗi nhập liệu", JOptionPane.WARNING_MESSAGE);
                return;
            }
            double actual = Double.parseDouble(cleanText);
            if (actual < 0) {
                JOptionPane.showMessageDialog(this, "Số tiền mặt không được là số âm!", "Lỗi nhập liệu", JOptionPane.WARNING_MESSAGE);
                return;
            }

            NhanVien user = AuthService.getInstance().getCurrentUser();
            String ghiChuText = txtGhiChu.getText().trim();
            double diff = actual - expectedHandoverAmount;
            if (diff != 0 && ghiChuText.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Có chênh lệch tiền mặt so với ca trước. Vui lòng nhập lý do giải trình vào mục Ghi chú!", "Yêu cầu giải trình", JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Ghi chép chênh lệch tự động vào ghi chú
            String finalGhiChu = ghiChuText;
            if (diff != 0) {
                finalGhiChu = "[Lưu ý đầu ca] Chênh lệch két đầu ca: " + 
                              String.format("%,.0f VNĐ %s. ", diff, diff > 0 ? "thừa" : "thiếu") + 
                              (ghiChuText.isEmpty() ? "" : "\nLý do: " + ghiChuText);
            }

            GiaoCa gc = service.moCa(user, actual, finalGhiChu);
            if (gc != null) {
                AuthService.getInstance().setCurrentShift(gc);
                confirmed = true;
                JOptionPane.showMessageDialog(this, "Mở ca trực thành công! Chúc bạn một ngày làm việc hiệu quả.", "Mở ca thành công", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Đã có lỗi xảy ra khi tạo ca làm việc trên cơ sở dữ liệu!", "Lỗi hệ thống", JOptionPane.ERROR_MESSAGE);
            }
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Số tiền nhập vào không đúng định dạng số hợp lệ!", "Lỗi nhập liệu", JOptionPane.WARNING_MESSAGE);
        }
    }

    private void handleLogout() {
        int choice = JOptionPane.showConfirmDialog(this, 
                "Bạn bắt buộc phải bắt đầu ca trực để sử dụng hệ thống.\nHủy bỏ quy trình này sẽ tự động đăng xuất tài khoản. Bạn có muốn đăng xuất không?", 
                "Xác nhận đăng xuất", 
                JOptionPane.YES_NO_OPTION, 
                JOptionPane.WARNING_MESSAGE);
        
        if (choice == JOptionPane.YES_OPTION) {
            AuthService.getInstance().dangXuat();
            dispose();
            if (parentFrame != null) {
                parentFrame.dispose();
            }
            new ui.LoginFrame().setVisible(true);
        }
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}
