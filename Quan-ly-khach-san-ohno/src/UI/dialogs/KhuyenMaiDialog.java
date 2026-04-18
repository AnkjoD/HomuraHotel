package ui.dialogs;

import dao.KhuyenMaiDAO;
import entity.KhuyenMai;
import entity.enums.LoaiGiam;
import ui.components.RoundedComponents;
import ui.components.RoundedComponents.RoundedButton;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class KhuyenMaiDialog extends JDialog {

    private final KhuyenMaiDAO kmDAO = new KhuyenMaiDAO();
    private KhuyenMai km;
    private final boolean isEdit;

    // UI Components
    private JTextField txtMa, txtTen, txtGiaTri, txtBatDau, txtKetThuc, txtSoLuong, txtMinBill;
    private JComboBox<LoaiGiam> cbLoai;
    private JCheckBox chkActive;

    public KhuyenMaiDialog(Window owner, KhuyenMai km, boolean isEdit) {
        super(owner, isEdit ? "Chỉnh sửa khuyến mãi" : "Thêm khuyến mãi mới", ModalityType.APPLICATION_MODAL);
        this.km = km;
        this.isEdit = isEdit;
        setLayout(new BorderLayout());
        setBackground(Color.WHITE);

        initComponents();
        if (isEdit && km != null) fillData();
        
        pack();
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        JPanel mainPanel = new JPanel(new GridBagLayout());
        mainPanel.setBackground(Color.WHITE);
        mainPanel.setBorder(new EmptyBorder(25, 25, 25, 25));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(8, 8, 8, 8);

        // Header Title
        JLabel lblHeader = new JLabel(isEdit ? "CHỈNH SỬA" : "TẠO MỚI KHUYẾN MÃI");
        lblHeader.setFont(new Font("Inter", Font.BOLD, 22));
        lblHeader.setForeground(new Color(33, 37, 41));
        gbc.gridx = 0; gbc.gridy = 0; gbc.gridwidth = 2;
        gbc.insets = new Insets(0, 8, 20, 8);
        mainPanel.add(lblHeader, gbc);

        gbc.gridwidth = 1;
        gbc.insets = new Insets(8, 8, 8, 8);

        // Code
        txtMa = createField(mainPanel, "Mã Khuyến mãi/Voucher:", 1);
        if (isEdit) txtMa.setEditable(false);

        // Name
        txtTen = createField(mainPanel, "Tên chương trình:", 2);

        // Type & Value
        mainPanel.add(new JLabel("Loại giảm giá:"), getGbc(0, 3));
        cbLoai = new JComboBox<>(LoaiGiam.values());
        cbLoai.setPreferredSize(new Dimension(200, 40));
        mainPanel.add(cbLoai, getGbc(1, 3));

        txtGiaTri = createField(mainPanel, "Giá trị (% hoặc VNĐ):", 4);

        // Dates
        txtBatDau = createField(mainPanel, "Ngày bắt đầu (dd/MM/yyyy HH:mm):", 5);
        if (!isEdit) txtBatDau.setText(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        
        txtKetThuc = createField(mainPanel, "Ngày kết thúc (dd/MM/yyyy HH:mm):", 6);
        if (!isEdit) txtKetThuc.setText(LocalDateTime.now().plusMonths(1).format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));

        // Quota & Min Bill
        txtSoLuong = createField(mainPanel, "Số lượng phát hành:", 7);
        if (!isEdit) txtSoLuong.setText("100");
        
        txtMinBill = createField(mainPanel, "Đơn hàng tối thiểu (VNĐ):", 8);
        if (!isEdit) txtMinBill.setText("0");

        // Status
        mainPanel.add(new JLabel("Trạng thái:"), getGbc(0, 9));
        chkActive = new JCheckBox("Đang kích hoạt");
        chkActive.setSelected(true);
        chkActive.setBackground(Color.WHITE);
        mainPanel.add(chkActive, getGbc(1, 9));

        add(mainPanel, BorderLayout.CENTER);

        // Buttons
        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 15));
        btnPanel.setBackground(new Color(248, 249, 250));
        
        RoundedButton btnCancel = new RoundedButton("Hủy bỏ", new Color(220, 53, 69), Color.WHITE);
        btnCancel.addActionListener(e -> dispose());
        btnPanel.add(btnCancel);

        RoundedButton btnSave = new RoundedButton("Lưu lại", new Color(40, 167, 69), Color.WHITE);
        btnSave.addActionListener(e -> save());
        btnPanel.add(btnSave);

        add(btnPanel, BorderLayout.SOUTH);
    }

    private JTextField createField(JPanel p, String label, int y) {
        p.add(new JLabel(label), getGbc(0, y));
        JTextField txt = new JTextField();
        txt.setPreferredSize(new Dimension(300, 40));
        p.add(txt, getGbc(1, y));
        return txt;
    }

    private GridBagConstraints getGbc(int x, int y) {
        GridBagConstraints g = new GridBagConstraints();
        g.gridx = x; g.gridy = y;
        g.fill = GridBagConstraints.HORIZONTAL;
        g.insets = new Insets(8, 8, 8, 8);
        if (x == 1) g.weightx = 1.0;
        return g;
    }

    private void fillData() {
        txtMa.setText(km.getMaKhuyenMai());
        txtTen.setText(km.getTenKhuyenMai());
        cbLoai.setSelectedItem(km.getLoaiGiam());
        txtGiaTri.setText(String.valueOf(km.getGiaTriGiam()));
        txtBatDau.setText(km.getNgayBatDau().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        txtKetThuc.setText(km.getNgayKetThuc().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
        txtSoLuong.setText(String.valueOf(km.getSoLuong()));
        txtMinBill.setText(String.valueOf(km.getDieuKienToiThieu()));
        chkActive.setSelected(km.isTrangThai());
    }

    private void save() {
        try {
            if (km == null) km = new KhuyenMai();
            
            km.setMaKhuyenMai(txtMa.getText().trim());
            km.setTenKhuyenMai(txtTen.getText().trim());
            km.setLoaiGiam((LoaiGiam) cbLoai.getSelectedItem());
            km.setGiaTriGiam(Double.parseDouble(txtGiaTri.getText()));
            
            DateTimeFormatter df = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            km.setNgayBatDau(LocalDateTime.parse(txtBatDau.getText(), df));
            km.setNgayKetThuc(LocalDateTime.parse(txtKetThuc.getText(), df));
            
            km.setSoLuong(Integer.parseInt(txtSoLuong.getText()));
            km.setDieuKienToiThieu(Double.parseDouble(txtMinBill.getText()));
            km.setTrangThai(chkActive.isSelected());

            if (km.getMaKhuyenMai().isEmpty()) {
                JOptionPane.showMessageDialog(this, "Mã không được để trống!");
                return;
            }

            boolean success = isEdit ? kmDAO.update(km) : kmDAO.insert(km);
            if (success) {
                JOptionPane.showMessageDialog(this, "Đã lưu thành công!");
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Lỗi khi lưu dữ liệu!");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Lỗi định dạng dữ liệu: " + e.getMessage());
        }
    }
}
