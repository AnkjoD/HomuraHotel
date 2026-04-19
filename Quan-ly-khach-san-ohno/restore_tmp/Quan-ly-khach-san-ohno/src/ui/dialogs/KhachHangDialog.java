package ui.dialogs;

import ui.components.RoundedComponents.RoundedBorder;
import ui.components.RoundedComponents.RoundedButton;
import ui.components.UIConstants;
import entity.KhachHang;
import java.awt.*;
import javax.swing.*;
import service.KhachHangService;

public class KhachHangDialog extends JDialog {

    private final KhachHangService service = new KhachHangService();
    private final KhachHang        entity;
    private boolean confirmed = false;

    private JTextField    txtTen, txtSDT, txtCCCD, txtQuocTich;
    private ui.components.DatePicker dtNgaySinh;
    private JComboBox<String> cboGioiTinh;
    private JLabel        lblMa;
    private RoundedButton btnSave;

    // Regex validation
    private static final String REGEX_TEN  = "^[\\p{L} .'-]{2,50}$";
    private static final String REGEX_SDT  = "^(0[35789])[0-9]{8}$";
    private static final String REGEX_CCCD = "^([0-9]{9}|[0-9]{12}|[A-Z][0-9]{7,8})$";
    
    private JLabel errTen, errSDT, errCCCD, errNgaySinh;

    public KhachHangDialog(Frame parent, KhachHang kh) {
        super(parent, kh == null ? "Thêm khách hàng mới" : "Chỉnh sửa – " + kh.getHoTen(), true);
        this.entity = kh;
        setSize(430, 680);
        setLocationRelativeTo(parent);
        setResizable(false);
        buildUI();
        if (kh != null) fillData();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UIConstants.PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));
        JLabel title = new JLabel(entity == null ? "Thêm khách hàng mới" : "Chỉnh sửa khách hàng");
        title.setFont(new Font("Segoe UI", Font.BOLD, 17));
        title.setForeground(Color.WHITE);
        lblMa = new JLabel(entity != null ? "Mã: " + entity.getMaKhachHang() : "Mã sẽ được tự động tạo");
        lblMa.setFont(UIConstants.FONT_SMALL); lblMa.setForeground(new Color(255,255,255,180));
        JPanel hLeft = new JPanel(); hLeft.setOpaque(false);
        hLeft.setLayout(new BoxLayout(hLeft, BoxLayout.Y_AXIS));
        hLeft.add(title); hLeft.add(lblMa);
        header.add(hLeft, BorderLayout.WEST);

        // Form body
        JPanel body = new JPanel();
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Color.WHITE);
        body.setBorder(BorderFactory.createEmptyBorder(20, 24, 8, 24));

        txtTen  = field(); txtSDT = field(); txtCCCD = field();
        dtNgaySinh = new ui.components.DatePicker(null); txtQuocTich = field(); txtQuocTich.setText("Việt Nam");
        cboGioiTinh = new JComboBox<>(new String[]{"Nam", "Nữ", "Khác"});
        cboGioiTinh.setFont(UIConstants.FONT_BODY);
        cboGioiTinh.setBackground(Color.WHITE);
        cboGioiTinh.setPreferredSize(new Dimension(0, 34));
        
        errTen = errLabel();
        errSDT = errLabel();
        errCCCD = errLabel();
        errNgaySinh = errLabel();

        txtTen.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusLost(java.awt.event.FocusEvent e) { validateTen(); }
        });
        txtSDT.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusLost(java.awt.event.FocusEvent e) { validateSDT(); }
        });
        txtCCCD.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusLost(java.awt.event.FocusEvent e) { validateCCCD(); }
        });

        // Gợi ý placeholder
        setPlaceholder(txtSDT,  "VD: 0901234567");
        setPlaceholder(txtCCCD, "VD: 012345678901 (9 hoặc 12 số)");

        body.add(labelField("Họ và tên *",       txtTen, errTen));
        body.add(Box.createVerticalStrut(6));
        body.add(labelField("Số điện thoại *",    txtSDT, errSDT));
        body.add(Box.createVerticalStrut(6));
        body.add(labelField("CCCD / Passport",    txtCCCD, errCCCD));
        body.add(Box.createVerticalStrut(6));
        body.add(labelField("Ngày sinh", dtNgaySinh, errNgaySinh));
        body.add(Box.createVerticalStrut(6));
        body.add(labelField("Giới tính", cboGioiTinh, null));
        body.add(Box.createVerticalStrut(6));
        body.add(labelField("Quốc tịch", txtQuocTich, null));

        // Footer buttons
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER));

        RoundedButton btnCancel = new RoundedButton("Huỷ",  new Color(0xE2E8F0), UIConstants.TEXT_PRIMARY);
        btnSave = new RoundedButton(entity == null ? "Thêm khách hàng mới" : "Lưu thay đổi",
                                                    UIConstants.PRIMARY, Color.WHITE);
        btnCancel.addActionListener(e -> dispose());
        btnSave.addActionListener(e   -> doSave());

        getRootPane().setDefaultButton(btnSave);

        footer.add(btnCancel);
        footer.add(btnSave);

        root.add(header, BorderLayout.NORTH);
        root.add(body,   BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private void fillData() {
        txtTen.setText(entity.getHoTen());
        txtSDT.setText(entity.getSdt());
        txtCCCD.setText(entity.getCccd() != null ? entity.getCccd() : "");
        if (entity.getNgaySinh() != null) dtNgaySinh.setDate(java.sql.Date.valueOf(entity.getNgaySinh()));
        if (entity.getGioiTinh() != null) cboGioiTinh.setSelectedItem(entity.getGioiTinh());
        txtQuocTich.setText(entity.getQuocTich() != null ? entity.getQuocTich() : "Việt Nam");
    }

    private String formatName(String name) {
        if (name == null || name.isBlank()) return "";
        String[] words = name.trim().split("\\s+");
        StringBuilder sb = new StringBuilder();
        for (String w : words) {
            if (w.length() > 0) {
                sb.append(Character.toUpperCase(w.charAt(0)));
                if (w.length() > 1) {
                    sb.append(w.substring(1).toLowerCase());
                }
                sb.append(" ");
            }
        }
        return sb.toString().trim();
    }

    private boolean validateTen() {
        String ten = txtTen.getText().trim();
        ten = formatName(ten);
        txtTen.setText(ten);
        
        errTen.setText(" ");
        if (ten.isEmpty()) { errTen.setText("Bắt buộc - Vui lòng nhập họ và tên"); return false; }
        if (!ten.matches(REGEX_TEN)) { errTen.setText("Họ tên không hợp lệ (2-50 ký tự, chỉ gồm chữ cái)"); return false; }
        return true;
    }

    private boolean validateSDT() {
        String sdt = txtSDT.getText().trim();
        errSDT.setText(" ");
        if (sdt.isEmpty()) { errSDT.setText("Bắt buộc - Vui lòng nhập số điện thoại"); return false; }
        if (!sdt.matches(REGEX_SDT)) { errSDT.setText("SĐT không đúng định dạng (10 số, bắt đầu 03/05/07/08/09)"); return false; }
        return true;
    }

    private boolean validateCCCD() {
        String cccd = txtCCCD.getText().trim();
        errCCCD.setText(" ");
        if (!cccd.isEmpty() && !cccd.matches(REGEX_CCCD)) {
            errCCCD.setText("CCCD/Passport không hợp lệ (9 số, 12 số, hoặc passport)"); return false;
        }
        return true;
    }

    private boolean validateNgaySinh() {
        errNgaySinh.setText(" ");
        return true;
    }

    private void doSave() {
        boolean ok1 = validateTen();
        boolean ok2 = validateSDT();
        boolean ok3 = validateCCCD();
        boolean ok4 = validateNgaySinh();
        if (!ok1 || !ok2 || !ok3 || !ok4) return;

        String ten  = txtTen.getText().trim();
        String sdt  = txtSDT.getText().trim();
        String cccd = txtCCCD.getText().trim();

        KhachHang kh = entity != null ? entity : new KhachHang();
        kh.setHoTen(ten);
        kh.setSdt(sdt);
        kh.setCccd(cccd.isEmpty() ? null : cccd);
        
        try {
            if (dtNgaySinh.getDate() != null) {
                kh.setNgaySinh(dtNgaySinh.getDate().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDate());
            } else {
                kh.setNgaySinh(null);
            }
        } catch (Exception ignored) {}
        
        kh.setGioiTinh((String) cboGioiTinh.getSelectedItem());
        kh.setQuocTich(txtQuocTich.getText().trim().isEmpty() ? "Việt Nam" : txtQuocTich.getText().trim());

        String err = entity == null ? service.them(kh) : service.sua(kh);
        if (err == null) {
            confirmed = true;
            dispose();
        } else {
            showError(err);
        }
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Dữ liệu không hợp lệ", JOptionPane.WARNING_MESSAGE);
    }

    public boolean isConfirmed() { return confirmed; }

    // ---- Helpers ----
    private JTextField field() {
        JTextField f = new JTextField();
        f.setFont(UIConstants.FONT_BODY);
        f.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(UIConstants.BTN_RADIUS, UIConstants.BORDER),
            BorderFactory.createEmptyBorder(5, 9, 5, 9)));
        f.setPreferredSize(new Dimension(0, 34));
        return f;
    }

    private void setPlaceholder(JTextField f, String hint) {
        f.setToolTipText(hint);
        f.putClientProperty("JTextField.placeholderText", hint);
    }

    private JPanel labelField(String label, JComponent comp, JLabel errLbl) {
        JPanel p = new JPanel(new BorderLayout(0,3)); p.setOpaque(false);
        JLabel lbl = new JLabel(label); lbl.setFont(UIConstants.FONT_SMALL_BOLD); lbl.setForeground(UIConstants.TEXT_PRIMARY);
        p.add(lbl, BorderLayout.NORTH); 
        JPanel c = new JPanel(new BorderLayout()); c.setOpaque(false);
        c.add(comp, BorderLayout.NORTH);
        if (errLbl != null) c.add(errLbl, BorderLayout.CENTER);
        p.add(c, BorderLayout.CENTER);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, errLbl != null ? 64 : 54));
        return p;
    }

    private JLabel errLabel() {
        JLabel err = new JLabel(" ");
        err.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        err.setForeground(UIConstants.DANGER);
        return err;
    }
}


