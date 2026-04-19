package ui.dialogs;

import service.DichVuService;
import entity.DichVu;
import ui.components.UIConstants;
import ui.components.RoundedComponents.*;

import javax.swing.*;
import java.awt.*;

public class DichVuDialog extends JDialog {

    private final DichVuService service = new DichVuService();
    private final DichVu        entity;
    private boolean confirmed = false;

    private JTextField    txtTen, txtGia, txtDonVi, txtSLMin, txtMoTa;
    private JComboBox<String> cboLoai, cboTrangThai;

    public DichVuDialog(Frame parent, DichVu dv) {
        super(parent, dv == null ? "Thêm dịch vụ mới" : "Chỉnh sửa – " + dv.getTenDV(), true);
        this.entity = dv;
        setSize(520, 460);
        setLocationRelativeTo(parent);
        setResizable(false);
        buildUI();
        if (dv != null) fillData();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout()); root.setBackground(Color.WHITE);

        // Header
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(UIConstants.PRIMARY);
        header.setBorder(BorderFactory.createEmptyBorder(16,24,16,24));
        JLabel title = new JLabel(entity==null?"Thêm dịch vụ mới":"Chỉnh sửa dịch vụ");
        title.setFont(new Font("Segoe UI",Font.BOLD,17)); title.setForeground(Color.WHITE);
        JLabel sub = new JLabel(entity!=null?"Mã: "+entity.getMaDV():"Mã sẽ được tự động tạo");
        sub.setFont(UIConstants.FONT_SMALL); sub.setForeground(new Color(255,255,255,180));
        JPanel hl = new JPanel(); hl.setOpaque(false); hl.setLayout(new BoxLayout(hl,BoxLayout.Y_AXIS));
        hl.add(title); hl.add(sub);
        header.add(hl, BorderLayout.WEST);

        // Form
        JPanel body = new JPanel(new GridLayout(0,2,14,12));
        body.setBackground(Color.WHITE); body.setBorder(BorderFactory.createEmptyBorder(20,24,8,24));

        txtTen   = f(); txtGia   = f(); txtGia.setText("0");
        txtDonVi = f(); txtDonVi.setText("lần");
        txtSLMin = f(); txtSLMin.setText("1");
        txtMoTa  = f();
        cboLoai      = new JComboBox<>(new String[]{"Ăn uống","Spa & Làm đẹp","Vận chuyển","Dịch vụ phòng","Khác"});
        cboTrangThai = new JComboBox<>(new String[]{"Hoạt động","Tạm ngừng"});
        cboLoai.setFont(UIConstants.FONT_BODY); cboTrangThai.setFont(UIConstants.FONT_BODY);

        body.add(lf("Tên dịch vụ *",  txtTen));    body.add(lf("Loại dịch vụ",   cboLoai));
        body.add(lf("Giá (đ) *",      txtGia));    body.add(lf("Đơn vị tính",    txtDonVi));
        body.add(lf("Số lượng tối thiểu", txtSLMin)); body.add(lf("Trạng thái",  cboTrangThai));
        body.add(lf("Mô tả",          txtMoTa));   body.add(new JLabel());

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT,10,10));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1,0,0,0,UIConstants.BORDER));
        RoundedButton btnCancel = new RoundedButton("Huỷ", new Color(0xE2E8F0), UIConstants.TEXT_PRIMARY);
        RoundedButton btnSave   = new RoundedButton(entity==null?"Thêm dịch vụ":"Lưu thay đổi", UIConstants.PRIMARY, Color.WHITE);
        btnCancel.addActionListener(e -> dispose()); btnSave.addActionListener(e -> doSave());
        getRootPane().setDefaultButton(btnSave);
        footer.add(btnCancel); footer.add(btnSave);

        root.add(header,BorderLayout.NORTH); root.add(body,BorderLayout.CENTER); root.add(footer,BorderLayout.SOUTH);
        setContentPane(root);
    }

    private void fillData() {
        txtTen.setText(entity.getTenDV()); txtGia.setText(String.valueOf((long)entity.getGia()));
        txtDonVi.setText(entity.getDonVi()!=null?entity.getDonVi():"lần");
        txtSLMin.setText(String.valueOf(entity.getSoLuongMin()));
        txtMoTa.setText(entity.getMoTa()!=null?entity.getMoTa():"");
        sc(cboLoai, entity.getLoai()); sc(cboTrangThai, entity.getTrangThai());
    }

    private void doSave() {
        String ten = txtTen.getText().trim();
        if (ten.isEmpty()) { err("Tên dịch vụ không được để trống!"); txtTen.requestFocus(); return; }
        if (ten.length() < 2 || ten.length() > 100) {
            err("Tên dịch vụ phải từ 2 đến 100 ký tự!"); txtTen.requestFocus(); return;
        }
        String donVi = txtDonVi.getText().trim();
        if (donVi.isEmpty()) { err("Đơn vị tính không được để trống!"); txtDonVi.requestFocus(); return; }
        if (!donVi.matches("^[\\p{L}0-9 /]{1,20}$")) {
            err("Đơn vị tính không hợp lệ! (VD: lần, phần, gói, ...)"); txtDonVi.requestFocus(); return;
        }
        long gia;
        try {
            gia = Long.parseLong(txtGia.getText().trim().replace(",","").replace(".",""));
            if(gia<0) throw new Exception();
        } catch(Exception e) { err("Giá phải là số không âm!"); txtGia.requestFocus(); return; }

        int slMin;
        try { slMin = Integer.parseInt(txtSLMin.getText().trim()); if(slMin<1||slMin>9999) throw new Exception(); }
        catch(Exception e) { err("Số lượng tối thiểu phải là số nguyên từ 1–9999!"); return; }

        DichVu dv = entity!=null?entity:new DichVu();
        dv.setTenDV(ten); dv.setGia(gia); dv.setDonVi(donVi); dv.setSoLuongMin(slMin);
        dv.setMoTa(txtMoTa.getText().trim());
        dv.setLoai((String)cboLoai.getSelectedItem()); dv.setTrangThai((String)cboTrangThai.getSelectedItem());

        String error = entity==null?service.them(dv):service.sua(dv);
        if (error==null) { confirmed=true; dispose(); } else err(error);
    }

    public boolean isConfirmed() { return confirmed; }
    private JTextField f() { JTextField t=new JTextField(); t.setFont(UIConstants.FONT_BODY); t.setBorder(BorderFactory.createCompoundBorder(new RoundedBorder(UIConstants.BTN_RADIUS,UIConstants.BORDER),BorderFactory.createEmptyBorder(5,9,5,9))); t.setPreferredSize(new Dimension(0,34)); return t; }
    private JPanel lf(String l, JComponent c) { JPanel p=new JPanel(new BorderLayout(0,3)); p.setOpaque(false); JLabel lb=new JLabel(l); lb.setFont(UIConstants.FONT_SMALL_BOLD); p.add(lb,BorderLayout.NORTH); p.add(c,BorderLayout.CENTER); return p; }
    private void sc(JComboBox<String> c, String v) { if(v==null)return; for(int i=0;i<c.getItemCount();i++) if(v.equals(c.getItemAt(i))){c.setSelectedIndex(i);return;} }
    private void err(String m) { JOptionPane.showMessageDialog(this,m,"Lỗi dữ liệu",JOptionPane.WARNING_MESSAGE); }
}

