package ui.dialogs;

import ui.components.UIConstants;
import ui.components.RoundedComponents.RoundedButton;
import ui.components.RoundedComponents.ModernTextField;
import service.DatPhongService;
import entity.DatPhong;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;

/**
 * Hộp thoại xác nhận No-show với thiết kế hiện đại.
 */
public class NoShowDialog extends JDialog {

    private final DatPhongService datPhongService = new DatPhongService();
    private final String maDatPhong;
    private final String tenKhach;
    private double phiPhat = 0;
    private double tienDatCoc = 0;
    private boolean confirmed = false;

    private ModernTextField txtPhiPhat;

    public NoShowDialog(Frame parent, String maDatPhong, String tenKhach) {
        super(parent, "Xác nhận Khách không đến", true);
        this.maDatPhong = maDatPhong;
        this.tenKhach = tenKhach;

        DatPhong dp = datPhongService.getById(maDatPhong);
        if (dp != null) {
            this.tienDatCoc = dp.getTienDatCoc();
        }

        setSize(450, 410); // Tăng thêm chiều cao để chứa gợi ý
        setLocationRelativeTo(parent);
        setResizable(false);
        setLayout(new BorderLayout());
        getContentPane().setBackground(Color.WHITE);

        buildUI();
    }

    private void buildUI() {
        // --- Header ---
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(new Color(0x334155));
        header.setPreferredSize(new Dimension(0, 65));
        header.setBorder(new EmptyBorder(0, 25, 0, 25));

        JLabel lblTitle = new JLabel("XÁC NHẬN KHÁCH KHÔNG ĐẾN");
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblTitle.setForeground(Color.WHITE);
        header.add(lblTitle, BorderLayout.CENTER);
        
        JLabel lblSub = new JLabel("Đánh dấu khách không đến và áp dụng phí phạt");
        lblSub.setFont(UIConstants.FONT_TINY);
        lblSub.setForeground(new Color(0xCBD5E1));
        header.add(lblSub, BorderLayout.SOUTH);
        
        add(header, BorderLayout.NORTH);

        // --- Main Content ---
        JPanel main = new JPanel();
        main.setLayout(new BoxLayout(main, BoxLayout.Y_AXIS));
        main.setOpaque(false);
        main.setBorder(new EmptyBorder(20, 30, 20, 30));

        // Info Card
        JPanel infoCard = new JPanel(new GridLayout(2, 1, 0, 5));
        infoCard.setBackground(new Color(0xF8FAFC));
        infoCard.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(UIConstants.BORDER),
            new EmptyBorder(12, 15, 12, 15)
        ));

        JLabel lblMa = new JLabel("Mã đơn: " + maDatPhong);
        lblMa.setFont(UIConstants.FONT_SMALL_BOLD);
        lblMa.setForeground(UIConstants.TEXT_SECONDARY);
        
        JLabel lblTen = new JLabel("Khách hàng: " + tenKhach);
        lblTen.setFont(UIConstants.FONT_BODY_BOLD);
        lblTen.setForeground(UIConstants.TEXT_PRIMARY);

        infoCard.add(lblMa);
        infoCard.add(lblTen);
        main.add(infoCard);
        main.add(Box.createVerticalStrut(15)); // Giảm bớt khoảng cách

        // Input Field
        JLabel lblInput = new JLabel("Nhập phí phạt khách không đến (VNĐ):");
        lblInput.setFont(UIConstants.FONT_BODY);
        lblInput.setForeground(UIConstants.TEXT_SECONDARY);
        main.add(lblInput);
        main.add(Box.createVerticalStrut(8));

        JPanel inputPanel = new JPanel(new BorderLayout());
        inputPanel.setOpaque(false);
        inputPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        
        txtPhiPhat = new ModernTextField("Nhập số tiền...");
        txtPhiPhat.setText("0");
        txtPhiPhat.setFont(new Font("Segoe UI", Font.BOLD, 15));
        txtPhiPhat.setForeground(UIConstants.PRIMARY);
        txtPhiPhat.setHorizontalAlignment(JTextField.RIGHT);
        inputPanel.add(txtPhiPhat, BorderLayout.CENTER);
        main.add(inputPanel);

        // Quick suggestions based on deposit
        if (tienDatCoc > 0) {
            main.add(Box.createVerticalStrut(8));
            JPanel suggestPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
            suggestPanel.setOpaque(false);
            
            JLabel lblSuggest = new JLabel("Gợi ý theo cọc (" + String.format("%,.0fđ", tienDatCoc) + "):");
            lblSuggest.setFont(UIConstants.FONT_TINY);
            lblSuggest.setForeground(UIConstants.TEXT_MUTED);
            suggestPanel.add(lblSuggest);
            
            double[] percentages = { 0.3, 0.5, 1.0 };
            String[] labels = { "30%", "50%", "100%" };
            
            for (int i = 0; i < percentages.length; i++) {
                double pct = percentages[i];
                String lbl = labels[i];
                double value = tienDatCoc * pct;
                
                JButton btnPct = new JButton(lbl) {
                    @Override
                    protected void paintComponent(Graphics g) {
                        Graphics2D g2 = (Graphics2D) g.create();
                        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                        g2.setColor(new Color(0xF8FAFC));
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                        g2.setColor(new Color(0xCBD5E1));
                        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                        g2.dispose();
                        super.paintComponent(g);
                    }
                };
                btnPct.setFont(UIConstants.FONT_TINY);
                btnPct.setForeground(UIConstants.TEXT_SECONDARY);
                btnPct.setContentAreaFilled(false);
                btnPct.setBorderPainted(false);
                btnPct.setFocusPainted(false);
                btnPct.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                btnPct.setPreferredSize(new Dimension(50, 22));
                btnPct.addActionListener(e -> {
                    txtPhiPhat.setText(String.format("%.0f", value));
                });
                suggestPanel.add(btnPct);
            }
            main.add(suggestPanel);
        }

        JLabel lblNotice = new JLabel("* Hệ thống sẽ chuyển trạng thái đơn sang NO_SHOW (Khách không đến).");
        lblNotice.setFont(UIConstants.FONT_TINY);
        lblNotice.setForeground(UIConstants.TEXT_MUTED);
        main.add(Box.createVerticalStrut(10));
        main.add(lblNotice);

        add(main, BorderLayout.CENTER);

        // --- Footer ---
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 12));
        footer.setBackground(new Color(0xF1F5F9));
        
        RoundedButton btnCancel = new RoundedButton("Hủy bỏ", new Color(0xE2E8F0), UIConstants.TEXT_PRIMARY);
        RoundedButton btnConfirm = new RoundedButton("Xác nhận vắng mặt", UIConstants.PRIMARY, Color.WHITE);
        
        btnCancel.addActionListener(e -> dispose());
        btnConfirm.addActionListener(e -> handleConfirm());
        
        footer.add(btnCancel);
        footer.add(btnConfirm);
        add(footer, BorderLayout.SOUTH);
    }

    private void handleConfirm() {
        String input = txtPhiPhat.getText().trim().replace(",", "").replace(".", "");
        double penalty = 0;
        if (!input.isEmpty()) {
            try {
                penalty = Double.parseDouble(input);
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Số tiền không hợp lệ!", "Lỗi", JOptionPane.WARNING_MESSAGE);
                return;
            }
        }

        if (penalty < 0) {
            JOptionPane.showMessageDialog(this, "Số tiền phạt không được là số âm!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        if (penalty > tienDatCoc) {
            JOptionPane.showMessageDialog(this, "Số tiền phạt không được vượt quá số tiền đặt cọc (" + String.format("%,.0f VNĐ", tienDatCoc) + ")!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        phiPhat = penalty;
        confirmed = true;
        dispose();
    }

    public boolean isConfirmed() { return confirmed; }
    public double getPhiPhat() { return phiPhat; }
}
