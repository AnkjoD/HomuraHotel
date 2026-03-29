package UI.dialogs;

import service.NhanVienService;
import entity.NhanVien;
import entity.TaiKhoan;
import entity.enums.VaiTro;
import UI.components.UIConstants;
import UI.components.RoundedComponents.*;

import javax.swing.*;
import java.awt.*;

public class NhanVienDialog extends JDialog {

    private final NhanVienService service = new NhanVienService();
    private final NhanVien        entity;
    private boolean confirmed = false;

    private JTextField    txtTen, txtSDT;
    private JPasswordField txtMatKhau;
    private JComboBox<String> cboChucVu, cboVaiTro;

    // Regex hợp lệ
    private static final String REGEX_TEN = "^[\\p{L} .'-]{2,50}$";
    private static final String REGEX_SDT = "^(0[35789])[0-9]{8}$";
    private static final String REGEX_MK  = "^.{6,32}$";
    
    private JLabel errTen, errSDT, errMK;

    public NhanVienDialog(Frame parent, NhanVien nv) {
        super(parent, nv == null ? "Thêm nhân viên mới" : "Chỉnh sửa – " + nv.getHoTen(), true);
        this.entity = nv;
        setSize(450, 580);
        setLocationRelativeTo(parent);
        setResizable(false);
        buildUI();
        if (nv != null) fillData();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UIConstants.PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(16, 24, 16, 24));
        JLabel title = new JLabel(entity == null ? "Thêm nhân viên mới" : "Chỉnh sửa nhân viên");
        title.setFont(new Font("Segoe UI", Font.BOLD, 17)); title.setForeground(Color.WHITE);
        JLabel lblMa = new JLabel(entity != null ? "Mã: " + entity.getMaNhanVien() : "Mã sẽ được tự động tạo");
        lblMa.setFont(UIConstants.FONT_SMALL); lblMa.setForeground(new Color(255,255,255,180));
        JPanel hLeft = new JPanel(); hLeft.setOpaque(false); hLeft.setLayout(new BoxLayout(hLeft,BoxLayout.Y_AXIS));
        hLeft.add(title); hLeft.add(lblMa);
        header.add(hLeft, BorderLayout.WEST);

        // Form
        JPanel body = new JPanel(); body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBackground(Color.WHITE);
        body.setBorder(BorderFactory.createEmptyBorder(20, 24, 8, 24));

        txtTen     = field();
        txtSDT     = field();
        txtMatKhau = new JPasswordField();
        styleField(txtMatKhau);
        
        errTen = errLabel();
        errSDT = errLabel();
        errMK  = errLabel();

        txtTen.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusLost(java.awt.event.FocusEvent e) { validateTen(); }
        });
        txtSDT.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusLost(java.awt.event.FocusEvent e) { validateSDT(); }
        });
        txtMatKhau.addFocusListener(new java.awt.event.FocusAdapter() {
            @Override public void focusLost(java.awt.event.FocusEvent e) { validateMK(); }
        });

        // Chức vụ hiển thị & vai trò hệ thống đồng nhất
        cboChucVu = combo("Lễ tân", "Quản lý", "Quản trị viên");
        cboVaiTro = combo("RECEPTIONIST", "MANAGER", "ADMIN");

        // Đồng bộ chức vụ ↔ vai trò
        cboChucVu.addActionListener(e -> {
            int idx = cboChucVu.getSelectedIndex();
            if (idx >= 0 && idx < cboVaiTro.getItemCount())
                cboVaiTro.setSelectedIndex(idx);
        });
        cboVaiTro.addActionListener(e -> {
            int idx = cboVaiTro.getSelectedIndex();
            if (idx >= 0 && idx < cboChucVu.getItemCount())
                cboChucVu.setSelectedIndex(idx);
        });

        String pwHint = entity == null ? " *" : " (để trống = giữ nguyên)";
        body.add(labelField("Họ và tên *",          txtTen, errTen));
        body.add(Box.createVerticalStrut(8));
        body.add(labelField("Số điện thoại *",       txtSDT, errSDT));
        body.add(Box.createVerticalStrut(8));
        body.add(labelField("Chức vụ",               cboChucVu, null));
        body.add(Box.createVerticalStrut(8));
        body.add(labelField("Vai trò hệ thống",      cboVaiTro, null));
        body.add(Box.createVerticalStrut(8));
        body.add(labelField("Mật khẩu" + pwHint,    txtMatKhau, errMK));

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1,0,0,0,UIConstants.BORDER));
        RoundedButton btnCancel = new RoundedButton("Huỷ",  new Color(0xE2E8F0), UIConstants.TEXT_PRIMARY);
        RoundedButton btnSave   = new RoundedButton(entity == null ? "Thêm nhân viên" : "Lưu thay đổi",
                                                    UIConstants.PRIMARY, Color.WHITE);
        btnCancel.addActionListener(e -> dispose());
        btnSave.addActionListener(e   -> doSave());
        getRootPane().setDefaultButton(btnSave);
        footer.add(btnCancel); footer.add(btnSave);

        root.add(header, BorderLayout.NORTH);
        root.add(body,   BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);
        setContentPane(root);
    }

    private void fillData() {
        txtTen.setText(entity.getHoTen());
        txtSDT.setText(entity.getSdt());
        // KHÔNG điền mật khẩu vào trường – để trống khi sửa
        if (entity.getTaiKhoan() != null) {
            VaiTro vaiTro = entity.getTaiKhoan().getVaiTro();
            if (vaiTro != null) {
                setCombo(cboVaiTro, vaiTro.name());
                // Đồng bộ chức vụ hiển thị
                int idx = cboVaiTro.getSelectedIndex();
                if (idx >= 0) cboChucVu.setSelectedIndex(idx);
            }
        } else {
            setCombo(cboChucVu, entity.getChucVu());
        }
    }

    private boolean validateTen() {
        String ten = txtTen.getText().trim();
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

    private boolean validateMK() {
        String pw  = new String(txtMatKhau.getPassword()).trim();
        errMK.setText(" ");
        if (entity == null && pw.isEmpty()) {
            errMK.setText("Mật khẩu không được để trống khi thêm nhân viên!"); return false;
        }
        if (!pw.isEmpty() && !pw.matches(REGEX_MK)) {
            errMK.setText("Mật khẩu phải có từ 6 đến 32 ký tự!"); return false;
        }
        return true;
    }

    private void doSave() {
        boolean ok1 = validateTen();
        boolean ok2 = validateSDT();
        boolean ok3 = validateMK();
        if (!ok1 || !ok2 || !ok3) return;

        String ten = txtTen.getText().trim();
        String sdt = txtSDT.getText().trim();
        String pw  = new String(txtMatKhau.getPassword()).trim();

        NhanVien nv = entity != null ? entity : new NhanVien();
        nv.setHoTen(ten);
        nv.setSdt(sdt);

        // Chức vụ từ combo
        String[] chucVuMap = {"Lễ tân", "Quản lý", "Quản trị viên"};
        int cvIdx = cboChucVu.getSelectedIndex();
        nv.setChucVu(cvIdx >= 0 ? chucVuMap[cvIdx] : "Lễ tân");

        // Tài khoản
        if (nv.getTaiKhoan() == null) {
            TaiKhoan tk = new TaiKhoan();
            tk.setTenDangNhap(nv.getMaNhanVien() != null ? nv.getMaNhanVien() : "");
            tk.setVaiTro(VaiTro.RECEPTIONIST);
            nv.setTaiKhoan(tk);
            tk.setNhanVien(nv);
        }

        String rolStr = (String) cboVaiTro.getSelectedItem();
        try {
            nv.getTaiKhoan().setVaiTro(VaiTro.valueOf(rolStr));
        } catch (Exception ex) {
            nv.getTaiKhoan().setVaiTro(VaiTro.RECEPTIONIST);
        }

        if (!pw.isEmpty()) {
            nv.getTaiKhoan().setMatKhau(pw);
        } else if (entity == null) {
            nv.getTaiKhoan().setMatKhau("123456");
        }

        String err = entity == null ? service.them(nv) : service.sua(nv);
        if (err == null) { confirmed = true; dispose(); }
        else showErr(err);
    }

    private void showErr(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Dữ liệu không hợp lệ", JOptionPane.WARNING_MESSAGE);
    }

    public boolean isConfirmed() { return confirmed; }

    private JTextField field() { JTextField f = new JTextField(); styleField(f); return f; }
    private void styleField(JTextField f) {
        f.setFont(UIConstants.FONT_BODY);
        f.setBorder(BorderFactory.createCompoundBorder(
            new RoundedBorder(UIConstants.BTN_RADIUS, UIConstants.BORDER),
            BorderFactory.createEmptyBorder(5,9,5,9)));
        f.setPreferredSize(new Dimension(0,34));
    }
    private JComboBox<String> combo(String... items) {
        JComboBox<String> c = new JComboBox<>(items);
        c.setFont(UIConstants.FONT_BODY);
        c.setPreferredSize(new Dimension(0,34));
        return c;
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
    private void setCombo(JComboBox<String> c, String val) {
        if (val == null) return;
        for (int i = 0; i < c.getItemCount(); i++)
            if (val.equals(c.getItemAt(i))) { c.setSelectedIndex(i); return; }
    }
}
