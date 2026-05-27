package ui.dialogs;

import ui.components.RoundedComponents.*;
import ui.components.UIConstants;
import ui.components.DatePicker;
import entity.*;
import java.awt.*;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.*;
import service.BangGiaService;
import service.PhongService;

/**
 * Dialog thêm/sửa Bảng Giá (Rate Plan).
 * Bao gồm:
 * - Thông tin bảng giá (tên, loại, đối tượng, ưu tiên, ngày, trạng thái)
 * - Bảng chi tiết giá cho TỪNG loại phòng (giá/ngày, giá/giờ, giá cuối tuần, phụ phí trả trễ)
 */
public class BangGiaDialog extends JDialog {

    private final BangGiaService service = new BangGiaService();
    private final PhongService phongService = new PhongService();
    private final BangGia entity;
    private boolean confirmed = false;

    private ModernTextField txtTen, txtMoTa;
    private DatePicker spnBatDau, spnKetThuc;
    private ModernSpinner spnUuTien;
    private JCheckBox cbTrangThai;
    private ModernComboBox<String> cboLoaiBangGia, cboDoiTuong;
    private JCheckBox chkMon, chkTue, chkWed, chkThu, chkFri, chkSat, chkSun;
    private DefaultTableModel chiTietModel;
    private List<LoaiPhong> allLoaiPhong;

    public BangGiaDialog(Frame parent, BangGia bg) {
        super(parent, bg == null ? "Thêm bảng giá mới" : "Chỉnh sửa – " + bg.getTenBangGia(), true);
        this.entity = bg;
        setSize(780, 680);
        setLocationRelativeTo(parent);
        setResizable(false);
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        allLoaiPhong = phongService.getAllLoaiPhong();
        buildUI();
        if (bg != null)
            fillData();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);

        // ===== HEADER =====
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

        JLabel title = new JLabel(entity == null ? "Thêm Rate Plan mới" : "Chỉnh sửa Rate Plan");
        title.setFont(new Font("Segoe UI", Font.BOLD, 24));
        title.setForeground(Color.WHITE);

        JLabel subtitle = new JLabel(entity == null
                ? "Thiết lập giá theo mùa/sự kiện/đối tượng cho từng loại phòng"
                : "Mã: " + entity.getMaBangGia() + " | " + entity.getLoaiBangGiaLabel());
        subtitle.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitle.setForeground(new Color(220, 230, 255));

        JPanel headerContent = new JPanel();
        headerContent.setOpaque(false);
        headerContent.setLayout(new BoxLayout(headerContent, BoxLayout.Y_AXIS));
        headerContent.add(title);
        headerContent.add(Box.createVerticalStrut(2));
        headerContent.add(subtitle);
        header.add(headerContent, BorderLayout.WEST);

        // ===== BODY =====
        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setBackground(Color.WHITE);
        body.setBorder(BorderFactory.createEmptyBorder(16, 24, 8, 24));

        // -- Top form: 4 rows --
        JPanel topForm = new JPanel(new GridLayout(4, 2, 14, 8));
        topForm.setBackground(Color.WHITE);

        txtTen = new ModernTextField("Bảng giá mới...");
        txtTen.setText("Bảng giá tháng " + java.time.YearMonth.now());
        txtTen.setPreferredSize(new Dimension(0, 40));

        // Loại bảng giá
        cboLoaiBangGia = new ModernComboBox<>(new String[]{"RACK", "SEASONAL", "CORPORATE", "OTA", "PROMOTION"});
        cboLoaiBangGia.setPreferredSize(new Dimension(0, 40));
        cboLoaiBangGia.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean sel, boolean focus) {
                super.getListCellRendererComponent(list, value, index, sel, focus);
                if (value != null) {
                    switch (value.toString()) {
                        case "RACK": setText("Rack Rate (Giá gốc)"); break;
                        case "SEASONAL": setText("Giá theo mùa"); break;
                        case "CORPORATE": setText("Giá doanh nghiệp"); break;
                        case "OTA": setText("Giá OTA"); break;
                        case "PROMOTION": setText("Khuyến mãi"); break;
                    }
                }
                return this;
            }
        });

        // Đối tượng áp dụng
        cboDoiTuong = new ModernComboBox<>(new String[]{"ALL", "CA_NHAN", "DOAN"});
        cboDoiTuong.setPreferredSize(new Dimension(0, 40));
        cboDoiTuong.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean sel, boolean focus) {
                super.getListCellRendererComponent(list, value, index, sel, focus);
                if (value != null) {
                    switch (value.toString()) {
                        case "ALL": setText("Tất cả khách"); break;
                        case "CA_NHAN": setText("Khách lẻ"); break;
                        case "DOAN": setText("Khách đoàn / Doanh nghiệp"); break;
                    }
                }
                return this;
            }
        });

        cboLoaiBangGia.addActionListener(e -> {
            String selectedLoai = (String) cboLoaiBangGia.getSelectedItem();
            if ("CORPORATE".equals(selectedLoai)) {
                cboDoiTuong.setSelectedItem("DOAN");
                cboDoiTuong.setEnabled(false);
            } else {
                cboDoiTuong.setEnabled(true);
            }
        });

        spnBatDau = new DatePicker(new java.util.Date());
        spnKetThuc = new DatePicker(new java.util.Date());

        // Mức ưu tiên
        spnUuTien = new ModernSpinner(new SpinnerNumberModel(100, 0, 999, 1));
        spnUuTien.setPreferredSize(new Dimension(0, 40));
        spnUuTien.setToolTipText("Số nhỏ = ưu tiên cao. VD: 30 (mùa cao điểm) > 50 (doanh nghiệp) > 100 (rack rate)");

        cbTrangThai = new JCheckBox("Kích hoạt ngay");
        cbTrangThai.setFont(UIConstants.FONT_BODY);
        cbTrangThai.setOpaque(false);
        cbTrangThai.setSelected(true);
        cbTrangThai.setForeground(UIConstants.TEXT_PRIMARY);

        topForm.add(createFormField("Tên bảng giá *", txtTen));
        topForm.add(createFormField("Loại Rate Plan *", cboLoaiBangGia));
        topForm.add(createFormField("Ngày bắt đầu *", spnBatDau));
        topForm.add(createFormField("Ngày kết thúc *", spnKetThuc));
        topForm.add(createFormField("Đối tượng áp dụng", cboDoiTuong));

        // Ưu tiên + trạng thái trên cùng dòng
        JPanel priorityRow = new JPanel(new GridLayout(1, 2, 10, 0));
        priorityRow.setOpaque(false);
        priorityRow.add(createFormField("Mức ưu tiên", spnUuTien));
        priorityRow.add(createFormField("Trạng thái", cbTrangThai));
        topForm.add(priorityRow);

        // Checkbox ngày cuối tuần
        chkMon = new JCheckBox("T2");
        chkTue = new JCheckBox("T3");
        chkWed = new JCheckBox("T4");
        chkThu = new JCheckBox("T5");
        chkFri = new JCheckBox("T6");
        chkSat = new JCheckBox("T7");
        chkSun = new JCheckBox("CN");

        JCheckBox[] checkboxes = { chkMon, chkTue, chkWed, chkThu, chkFri, chkSat, chkSun };
        for (JCheckBox cb : checkboxes) {
            cb.setFont(UIConstants.FONT_BODY);
            cb.setOpaque(false);
            cb.setForeground(UIConstants.TEXT_PRIMARY);
        }
        chkSat.setSelected(true);
        chkSun.setSelected(true);

        JPanel weekendPanel = new JPanel(new GridLayout(1, 7, 2, 0));
        weekendPanel.setOpaque(false);
        weekendPanel.add(chkMon);
        weekendPanel.add(chkTue);
        weekendPanel.add(chkWed);
        weekendPanel.add(chkThu);
        weekendPanel.add(chkFri);
        weekendPanel.add(chkSat);
        weekendPanel.add(chkSun);
        topForm.add(createFormField("Ngày cuối tuần *", weekendPanel));

        // Mô tả
        txtMoTa = new ModernTextField("Mô tả / ghi chú...");
        txtMoTa.setPreferredSize(new Dimension(0, 40));
        topForm.add(createFormField("Mô tả / Ghi chú", txtMoTa));

        // -- Bottom: Chi tiết giá cho từng loại phòng --
        JPanel tablePanel = new JPanel(new BorderLayout(0, 8));
        tablePanel.setBackground(Color.WHITE);

        JLabel lblTable = new JLabel("Chi tiết giá theo loại phòng");
        lblTable.setFont(UIConstants.FONT_BODY_BOLD);
        lblTable.setForeground(UIConstants.TEXT_PRIMARY);

        String[] cols = { "Mã", "Loại phòng", "Giá cơ sở (đ)", "Giá ngày mới (đ)", "Giá cuối tuần (đ)" };
        chiTietModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return col >= 3; // Chỉ cho sửa Giá ngày mới, Giá cuối tuần
            }
        };

        JTable table = new JTable(chiTietModel);
        table.setFont(UIConstants.FONT_BODY);
        table.setRowHeight(40); // Expanded for premium field breathing room
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(UIConstants.PRIMARY_LIGHT);
        
        // Right-align column headers for all price columns (col >= 2)
        TableCellRenderer baseHeaderRenderer = table.getTableHeader().getDefaultRenderer();
        table.getTableHeader().setDefaultRenderer(new TableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                JLabel lbl = (JLabel) baseHeaderRenderer.getTableCellRendererComponent(t, val, sel, foc, row, col);
                lbl.setFont(UIConstants.FONT_SMALL_BOLD);
                lbl.setBackground(UIConstants.BG_TABLE_HEADER);
                lbl.setForeground(UIConstants.TEXT_SECONDARY);
                lbl.setBorder(BorderFactory.createCompoundBorder(
                        BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.BORDER),
                        BorderFactory.createEmptyBorder(0, 10, 0, 10)
                ));
                if (col >= 2) {
                    lbl.setHorizontalAlignment(SwingConstants.RIGHT);
                } else {
                    lbl.setHorizontalAlignment(SwingConstants.LEFT);
                }
                return lbl;
            }
        });
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.BORDER));

        // Hide mã column
        table.getColumn("Mã").setMaxWidth(0);
        table.getColumn("Mã").setMinWidth(0);
        table.getColumn("Mã").setPreferredWidth(0);

        // Premium input-like cell renderer
        DefaultTableCellRenderer renderer = new DefaultTableCellRenderer() {
            private final Border nonEditableBorder = BorderFactory.createEmptyBorder(0, 10, 0, 10);
            
            // Outer margin + light input border + inner padding
            private final Border editableBorderNormal = BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(4, 4, 4, 4),
                    BorderFactory.createCompoundBorder(
                            new RoundedBorder(6, UIConstants.BORDER),
                            BorderFactory.createEmptyBorder(0, 8, 0, 8)
                    )
            );
            
            private final Border editableBorderActive = BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(4, 4, 4, 4),
                    BorderFactory.createCompoundBorder(
                            new RoundedBorder(6, UIConstants.PRIMARY),
                            BorderFactory.createEmptyBorder(0, 8, 0, 8)
                    )
            );

            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean foc, int row,
                    int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                lbl.setFont(UIConstants.FONT_BODY);
                
                // Alignment: Right-aligned for money values
                if (col >= 2) {
                    lbl.setHorizontalAlignment(SwingConstants.RIGHT);
                } else {
                    lbl.setHorizontalAlignment(SwingConstants.LEFT);
                }

                // Styling based on column editability
                if (col >= 3) {
                    lbl.setBorder(foc ? editableBorderActive : editableBorderNormal);
                    lbl.setBackground(Color.WHITE); // Pop editable cells out as real input boxes
                    
                    String text = val != null ? val.toString().trim() : "";
                    if (!text.isEmpty()) {
                        try {
                            double num = Double.parseDouble(text.replace(",", "").replace(".", ""));
                            lbl.setText(String.format("%,.0f", num));
                            if (col == 4) { // Giá cuối tuần
                                lbl.setForeground(UIConstants.ORANGE);
                                lbl.setFont(UIConstants.FONT_BODY_BOLD);
                            } else {
                                lbl.setForeground(UIConstants.SUCCESS);
                                lbl.setFont(UIConstants.FONT_BODY_BOLD);
                            }
                        } catch (NumberFormatException e) {
                            lbl.setText(text);
                            lbl.setForeground(UIConstants.TEXT_PRIMARY);
                        }
                    } else {
                        lbl.setText("Nhập giá..."); // Explicit text placeholder
                        lbl.setForeground(UIConstants.TEXT_MUTED);
                        lbl.setFont(UIConstants.FONT_BODY);
                    }
                } else {
                    lbl.setBorder(nonEditableBorder);
                    lbl.setBackground(sel ? UIConstants.PRIMARY_LIGHT : (row % 2 == 0 ? Color.WHITE : new Color(0xFAFAFA)));
                    
                    if (col == 2) { // Giá cơ sở
                        lbl.setForeground(UIConstants.TEXT_MUTED);
                        lbl.setFont(UIConstants.FONT_SMALL);
                    } else {
                        lbl.setForeground(UIConstants.TEXT_PRIMARY);
                    }
                }
                return lbl;
            }
        };
        table.setDefaultRenderer(Object.class, renderer);

        // Apply custom modern editor for seamless inputs
        ModernTableEditor modernEditor = new ModernTableEditor();
        table.getColumnModel().getColumn(3).setCellEditor(modernEditor);
        table.getColumnModel().getColumn(4).setCellEditor(modernEditor);

        // Populate with all loai phong
        loadLoaiPhongRows(null);

        JScrollPane scrollTable = new JScrollPane(table);
        scrollTable.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER));
        scrollTable.setPreferredSize(new Dimension(0, 200));

        JLabel hintLbl = new JLabel(
                "<html><b>Giá cơ sở</b> từ Loại phòng. <b>Giá cuối tuần</b> áp dụng theo cấu hình lựa chọn ở trên. Để trống = dùng giá cơ sở. <b>Mức ưu tiên</b>: số nhỏ = ưu tiên cao hơn.</html>");
        hintLbl.setFont(UIConstants.FONT_SMALL);
        hintLbl.setForeground(UIConstants.TEXT_SECONDARY);

        tablePanel.add(lblTable, BorderLayout.NORTH);
        tablePanel.add(scrollTable, BorderLayout.CENTER);
        tablePanel.add(hintLbl, BorderLayout.SOUTH);

        body.add(topForm, BorderLayout.NORTH);
        body.add(tablePanel, BorderLayout.CENTER);

        // Footer
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 14));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xF1F5F9)));

        RoundedButton btnCancel = new RoundedButton("Huỷ bỏ", new Color(0xF1F5F9), UIConstants.TEXT_SECONDARY);
        btnCancel.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnCancel.setPreferredSize(new Dimension(100, 42));
        btnCancel.addActionListener(e -> dispose());

        RoundedButton btnSave = new RoundedButton(
                entity == null ? "Thêm bảng giá" : "Lưu thay đổi",
                UIConstants.PRIMARY, Color.WHITE);
        btnSave.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnSave.setPreferredSize(new Dimension(160, 42));
        btnSave.addActionListener(e -> doSave());
        getRootPane().setDefaultButton(btnSave);

        footer.add(btnCancel);
        footer.add(btnSave);

        root.add(header, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);
        root.add(footer, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private void loadLoaiPhongRows(List<ChiTietBangGia> existingCT) {
        chiTietModel.setRowCount(0);
        for (LoaiPhong lp : allLoaiPhong) {
            String giaCoSo = String.format("%,.0f", lp.getGiaTheoNgay());
            String giaMoi = "";
            String giaCuoiTuan = "";

            // If editing, fill existing values
            if (existingCT != null) {
                for (ChiTietBangGia ct : existingCT) {
                    if (ct.getLoaiPhong() != null && lp.getMaLoaiPhong().equals(ct.getLoaiPhong().getMaLoaiPhong())) {
                        giaMoi = ct.getGiaNgay() > 0 ? String.valueOf((long) ct.getGiaNgay()) : "";
                        giaCuoiTuan = ct.getGiaCuoiTuan() > 0 ? String.valueOf((long) ct.getGiaCuoiTuan()) : "";
                        break;
                    }
                }
            }

            chiTietModel.addRow(new Object[] {
                    lp.getMaLoaiPhong(),
                    lp.getTenLoaiPhong(),
                    giaCoSo,
                    giaMoi,
                    giaCuoiTuan
            });
        }
    }

    private void fillData() {
        txtTen.setText(entity.getTenBangGia());
        if (entity.getNgayBatDau() != null) {
            java.util.Date dateBD = java.util.Date.from(
                    entity.getNgayBatDau().atZone(ZoneId.systemDefault()).toInstant());
            spnBatDau.setDate(dateBD);
        }
        if (entity.getNgayKetThuc() != null) {
            java.util.Date dateKT = java.util.Date.from(
                    entity.getNgayKetThuc().atZone(ZoneId.systemDefault()).toInstant());
            spnKetThuc.setDate(dateKT);
        }
        cbTrangThai.setSelected(entity.isTrangThai());

        // Rate Plan fields
        if (entity.getLoaiBangGia() != null) cboLoaiBangGia.setSelectedItem(entity.getLoaiBangGia());
        if (entity.getDoiTuongApDung() != null) cboDoiTuong.setSelectedItem(entity.getDoiTuongApDung());
        spnUuTien.setValue(entity.getMucUuTien());
        if (entity.getMoTa() != null) {
            txtMoTa.setText(entity.getMoTa());
        } else {
            txtMoTa.setText("");
        }

        String nct = entity.getNgayCuoiTuan();
        if (nct != null) {
            chkMon.setSelected(nct.contains("MONDAY"));
            chkTue.setSelected(nct.contains("TUESDAY"));
            chkWed.setSelected(nct.contains("WEDNESDAY"));
            chkThu.setSelected(nct.contains("THURSDAY"));
            chkFri.setSelected(nct.contains("FRIDAY"));
            chkSat.setSelected(nct.contains("SATURDAY"));
            chkSun.setSelected(nct.contains("SUNDAY"));
        } else {
            chkMon.setSelected(false);
            chkTue.setSelected(false);
            chkWed.setSelected(false);
            chkThu.setSelected(false);
            chkFri.setSelected(false);
            chkSat.setSelected(true);
            chkSun.setSelected(true);
        }

        // Load existing chi tiet
        List<ChiTietBangGia> existingCT = service.getChiTiet(entity.getMaBangGia());
        loadLoaiPhongRows(existingCT);
    }

    private void doSave() {
        String ten = txtTen.getText().trim();
        if (ten.isEmpty()) {
            err("Tên bảng giá không được để trống!");
            return;
        }

        java.util.Date d1 = spnBatDau.getDate();
        java.util.Date d2 = spnKetThuc.getDate();
        if (d1 == null || d2 == null) {
            err("Vui lòng chọn ngày bắt đầu và ngày kết thúc!");
            return;
        }
        if (!d2.after(d1)) {
            err("Ngày kết thúc phải sau ngày bắt đầu!");
            return;
        }

        // Collect chi tiet from table
        List<ChiTietBangGia> dsCT = new ArrayList<>();
        for (int i = 0; i < chiTietModel.getRowCount(); i++) {
            String maLoai = (String) chiTietModel.getValueAt(i, 0);
            String giaMoiStr = chiTietModel.getValueAt(i, 3) != null ? chiTietModel.getValueAt(i, 3).toString().trim() : "";
            String giaCTuanStr = chiTietModel.getValueAt(i, 4) != null ? chiTietModel.getValueAt(i, 4).toString().trim() : "";

            // Skip empty rows
            if (giaMoiStr.isEmpty() && giaCTuanStr.isEmpty())
                continue;

            ChiTietBangGia ct = new ChiTietBangGia();
            LoaiPhong lp = new LoaiPhong();
            lp.setMaLoaiPhong(maLoai);
            ct.setLoaiPhong(lp);

            try {
                if (!giaMoiStr.isEmpty())
                    ct.setGiaNgay(Double.parseDouble(giaMoiStr.replace(",", "").replace(".", "")));
                if (!giaCTuanStr.isEmpty())
                    ct.setGiaCuoiTuan(Double.parseDouble(giaCTuanStr.replace(",", "").replace(".", "")));
            } catch (NumberFormatException e) {
                err("Giá trị không hợp lệ ở dòng " + (i + 1) + "! Chỉ nhập số.");
                return;
            }

            if (ct.getGiaNgay() < 0 || ct.getGiaCuoiTuan() < 0) {
                err("Giá trị không được âm!");
                return;
            }

            dsCT.add(ct);
        }

        // Save BangGia header
        BangGia bg = entity != null ? entity : new BangGia();
        bg.setTenBangGia(ten);
        bg.setNgayBatDau(d1.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        bg.setNgayKetThuc(d2.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        bg.setTrangThai(cbTrangThai.isSelected());

        // Collect weekend days
        List<String> listDays = new ArrayList<>();
        if (chkMon.isSelected()) listDays.add("MONDAY");
        if (chkTue.isSelected()) listDays.add("TUESDAY");
        if (chkWed.isSelected()) listDays.add("WEDNESDAY");
        if (chkThu.isSelected()) listDays.add("THURSDAY");
        if (chkFri.isSelected()) listDays.add("FRIDAY");
        if (chkSat.isSelected()) listDays.add("SATURDAY");
        if (chkSun.isSelected()) listDays.add("SUNDAY");

        if (listDays.isEmpty()) {
            err("Vui lòng chọn ít nhất một ngày áp dụng giá cuối tuần!");
            return;
        }
        String ngayCuoiTuanStr = String.join(",", listDays);

        // Rate Plan fields
        bg.setLoaiBangGia((String) cboLoaiBangGia.getSelectedItem());
        bg.setDoiTuongApDung((String) cboDoiTuong.getSelectedItem());
        bg.setMucUuTien((Integer) spnUuTien.getValue());
        bg.setMoTa(txtMoTa.getText().trim());
        bg.setNgayCuoiTuan(ngayCuoiTuanStr);

        String error;
        if (entity == null) {
            error = service.them(bg);
        } else {
            error = service.sua(bg);
        }

        if (error != null) {
            err(error);
            return;
        }

        // Save chi tiet
        String maBG = bg.getMaBangGia();
        if (!dsCT.isEmpty()) {
            String ctError = service.saveChiTiet(maBG, dsCT);
            if (ctError != null) {
                err(ctError);
                return;
            }
        }

        confirmed = true;
        dispose();
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    private void err(String m) {
        JOptionPane.showMessageDialog(this, m, "Lỗi dữ liệu", JOptionPane.WARNING_MESSAGE);
    }

    private JTextField createModernTextField() {
        JTextField t = new JTextField();
        t.setFont(UIConstants.FONT_BODY);
        t.setBackground(new Color(249, 250, 251));
        t.setForeground(UIConstants.TEXT_PRIMARY);
        t.setCaretColor(UIConstants.PRIMARY);
        t.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(203, 213, 225), 1),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));
        t.setPreferredSize(new Dimension(0, 36));
        return t;
    }

    private JSpinner createDateSpinner() {
        JSpinner spinner = new JSpinner();
        spinner.setModel(new SpinnerDateModel(new java.util.Date(), null, null, java.util.Calendar.DAY_OF_MONTH));
        JSpinner.DateEditor editor = new JSpinner.DateEditor(spinner, "dd/MM/yyyy");
        editor.getTextField().setFont(UIConstants.FONT_BODY);
        spinner.setEditor(editor);
        spinner.setPreferredSize(new Dimension(0, 36));
        spinner.setBackground(new Color(249, 250, 251));
        spinner.setBorder(BorderFactory.createLineBorder(new Color(203, 213, 225), 1));
        return spinner;
    }

    private JPanel createFormField(String label, JComponent component) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        JLabel lb = new JLabel(label);
        lb.setFont(UIConstants.FONT_SMALL_BOLD);
        lb.setForeground(UIConstants.TEXT_PRIMARY);
        p.add(lb, BorderLayout.NORTH);
        p.add(component, BorderLayout.CENTER);
        return p;
    }

    private static class ModernTableEditor extends DefaultCellEditor {
        private final JTextField textField;

        public ModernTableEditor() {
            super(new JTextField());
            this.textField = (JTextField) getComponent();
            this.textField.setFont(UIConstants.FONT_BODY_BOLD);
            this.textField.setHorizontalAlignment(SwingConstants.RIGHT);
            this.textField.setForeground(UIConstants.SUCCESS);
            
            // Outer margin + focus border + inner padding
            this.textField.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createEmptyBorder(4, 4, 4, 4),
                    BorderFactory.createCompoundBorder(
                            new RoundedBorder(6, UIConstants.PRIMARY),
                            BorderFactory.createEmptyBorder(0, 8, 0, 8)
                    )
            ));
            
            // Select all text on focus
            this.textField.addFocusListener(new java.awt.event.FocusAdapter() {
                @Override
                public void focusGained(java.awt.event.FocusEvent e) {
                    SwingUtilities.invokeLater(() -> textField.selectAll());
                }
            });
            
            setClickCountToStart(1); // Single click to initiate edit instantly
        }
        
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column) {
            JTextField tf = (JTextField) super.getTableCellEditorComponent(table, value, isSelected, row, column);
            if (column == 4) { // Giá cuối tuần
                tf.setForeground(UIConstants.ORANGE);
            } else {
                tf.setForeground(UIConstants.SUCCESS);
            }
            tf.setText(value != null ? value.toString() : "");
            return tf;
        }
    }
}
