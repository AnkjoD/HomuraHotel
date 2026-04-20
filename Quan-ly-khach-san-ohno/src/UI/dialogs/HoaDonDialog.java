package ui.dialogs;

import service.AuthService;
import service.ThuePhongService;
import service.DichVuService;
import service.PhongService;
import service.KhachHangService;
import entity.*;
import ui.components.UIConstants;
import ui.components.RoundedComponents.*;
import ui.MainFrame;
import dao.KhuyenMaiDAO;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.print.*;
import java.text.*;
import java.time.LocalDateTime;
import java.util.*;

public class HoaDonDialog extends JDialog {

    private final ThuePhongService thuePhongService = new ThuePhongService();
    private final DichVuService dichVuService = new DichVuService();
    private final PhongService phongService = new PhongService();
    private final KhachHangService khService = new KhachHangService();

    private final ChiTietDatPhong thuePhong;
    private boolean confirmed = false;

    private DefaultTableModel tableModel;
    private JLabel lblTongPhong, lblTongDV, lblGiamGia, lblTongCong;
    private JComboBox<String> cboDV;
    private JSpinner spnSL;
    private java.util.List<DichVu> dvList = new ArrayList<>();
    private JComboBox<String> cboHinhThuc;
    private JCheckBox chkDebt;

    /**
     * Lấy ngày nhận phòng hiệu lực: ưu tiên ngayNhanThucTe,
     * fallback sang ngayNhanDuKien từ đặt phòng.
     */
    private java.time.LocalDateTime resolveNgayNhan() {
        if (thuePhong.getNgayNhanThucTe() != null) return thuePhong.getNgayNhanThucTe();
        if (thuePhong.getDatPhong() != null && thuePhong.getDatPhong().getNgayNhanDuKien() != null)
            return thuePhong.getDatPhong().getNgayNhanDuKien();
        return null;
    }

    private Date resolveNgayNhan_Date() {
        java.time.LocalDateTime ldt = resolveNgayNhan();
        return ldt != null ? Date.from(ldt.atZone(java.time.ZoneId.systemDefault()).toInstant()) : null;
    }

    private long tinhSoNgayHieuLuc() {
        return Math.max(1, thuePhongService.tinhSoNgay(resolveNgayNhan_Date(), new Date()));
    }

    public HoaDonDialog(Frame parent, ChiTietDatPhong tp) {
        super(parent, "Hóa đơn – Phòng " + tp.getSoPhong(), true);
        this.thuePhong = tp;
        setSize(660, 700);
        setLocationRelativeTo(parent);
        setResizable(false);
        getContentPane().setBackground(Color.WHITE);
        buildUI();
    }

    private void buildUI() {
        setLayout(new BorderLayout());
        add(buildBanner(), BorderLayout.NORTH);

        JScrollPane scroll = new JScrollPane(buildBody());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        add(scroll, BorderLayout.CENTER);
        add(buildFooter(), BorderLayout.SOUTH);
    }

    // ---- Banner ----
    private JPanel buildBanner() {
        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(UIConstants.PRIMARY);
        banner.setBorder(BorderFactory.createEmptyBorder(18, 28, 18, 28));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        JLabel t1 = new JLabel("HÓA ĐƠN THANH TOÁN");
        t1.setFont(new Font("Segoe UI", Font.BOLD, 20));
        t1.setForeground(Color.WHITE);
        JLabel t2 = new JLabel("P." + thuePhong.getSoPhong() + "  "
                + (thuePhong.getTenLoaiPhong() != null ? thuePhong.getTenLoaiPhong() : ""));
        t2.setFont(UIConstants.FONT_BODY);
        t2.setForeground(new Color(255, 255, 255, 200));
        left.add(t1);
        left.add(Box.createVerticalStrut(2));
        left.add(t2);

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        String maHD = "HD-" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + "-" + thuePhong.getSoPhong();
        JLabel lma = new JLabel("Mã HĐ: " + maHD);
        lma.setFont(UIConstants.FONT_SMALL_BOLD);
        lma.setForeground(Color.WHITE);
        lma.setHorizontalAlignment(SwingConstants.RIGHT);
        JLabel ldt = new JLabel("Xuất: " + new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date()));
        ldt.setFont(UIConstants.FONT_SMALL);
        ldt.setForeground(new Color(255, 255, 255, 180));
        ldt.setHorizontalAlignment(SwingConstants.RIGHT);
        right.add(lma);
        right.add(Box.createVerticalStrut(2));
        right.add(ldt);

        banner.add(left, BorderLayout.WEST);
        banner.add(right, BorderLayout.EAST);
        return banner;
    }

    // ---- Body ----
    private JPanel buildBody() {
        JPanel body = new JPanel();
        body.setBackground(Color.WHITE);
        body.setLayout(new BoxLayout(body, BoxLayout.Y_AXIS));
        body.setBorder(BorderFactory.createEmptyBorder(20, 28, 16, 28));

        body.add(buildCustomerSection());
        body.add(Box.createVerticalStrut(18));
        body.add(buildBillSection());
        body.add(Box.createVerticalStrut(14));
        body.add(buildAddDVSection());
        body.add(Box.createVerticalStrut(16));
        body.add(buildSummarySection());
        return body;
    }

    private JPanel buildCustomerSection() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 24, 0));
        panel.setOpaque(false);

        KhachHang kh = thuePhong.resolveKhachHang();
        if (kh == null && thuePhong.getMaKH() != null) {
            kh = khService.getById(thuePhong.getMaKH());
        }
        if (kh != null && (kh.getHoTen() == null || kh.getHoTen().isBlank()) && kh.getMaKhachHang() != null) {
            KhachHang full = khService.getById(kh.getMaKhachHang());
            if (full != null)
                kh = full;
        }
        String ten = kh != null && kh.getHoTen() != null ? kh.getHoTen() : "—";
        String sdt = kh != null && kh.getSoDienThoai() != null ? kh.getSoDienThoai() : "—";
        String cccd = kh != null && kh.getCccd() != null ? kh.getCccd() : "—";
        String email = kh != null && kh.getEmail() != null ? kh.getEmail() : "—";

        panel.add(infoBlock("KHÁCH HÀNG",
                new String[][] { { "Họ tên:", ten }, { "SĐT:", sdt }, { "CCCD:", cccd }, { "Email:", email } }));

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");

        panel.add(infoBlock("CHI TIẾT LƯU TRÚ", new String[][] {
                { "Nhận phòng:",
                        resolveNgayNhan_Date() != null ? sdf.format(resolveNgayNhan_Date()) : "—" },
                { "Trả phòng:", sdf.format(new Date()) },
                { "Số đêm:", tinhSoNgayHieuLuc() + " đêm" },
                { "Giá/đêm:", String.format("%,.0f đ", thuePhong.getGiaThucTeChot()) }
        }));
        return panel;
    }

    private JPanel infoBlock(String heading, String[][] rows) {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        JLabel lbl = new JLabel(heading);
        lbl.setFont(UIConstants.FONT_SMALL_BOLD);
        lbl.setForeground(UIConstants.TEXT_MUTED);
        panel.add(lbl);
        panel.add(Box.createVerticalStrut(8));
        for (String[] row : rows) {
            JPanel r = new JPanel(new BorderLayout(8, 0));
            r.setOpaque(false);
            r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
            JLabel k = new JLabel(row[0]);
            k.setFont(UIConstants.FONT_SMALL);
            k.setForeground(UIConstants.TEXT_SECONDARY);
            k.setPreferredSize(new Dimension(70, 18));
            JLabel v = new JLabel(row[1]);
            v.setFont(UIConstants.FONT_SMALL);
            r.add(k, BorderLayout.WEST);
            r.add(v, BorderLayout.CENTER);
            panel.add(r);
            panel.add(Box.createVerticalStrut(3));
        }
        return panel;
    }

    private JPanel buildBillSection() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 210));

        JLabel hdr = new JLabel("BẢNG CHI TIẾT THANH TOÁN");
        hdr.setFont(UIConstants.FONT_SMALL_BOLD);
        hdr.setForeground(UIConstants.TEXT_MUTED);
        hdr.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        String[] cols = { "Khoản mục", "Đơn giá", "Số lượng", "Thành tiền" };
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable table = new JTable(tableModel);
        table.setFont(UIConstants.FONT_BODY);
        table.setRowHeight(32);
        table.setShowGrid(false);
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(UIConstants.PRIMARY_LIGHT);
        table.getTableHeader().setFont(UIConstants.FONT_SMALL_BOLD);
        table.getTableHeader().setBackground(UIConstants.BG_TABLE_HEADER);
        table.getTableHeader().setForeground(UIConstants.TEXT_SECONDARY);
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.BORDER));
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row,
                    int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                lbl.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                lbl.setBackground(sel ? UIConstants.PRIMARY_LIGHT : Color.WHITE);
                lbl.setFont(UIConstants.FONT_BODY);
                if (col == 3) {
                    lbl.setFont(UIConstants.FONT_BODY_BOLD);
                    lbl.setForeground(UIConstants.SUCCESS);
                } else
                    lbl.setForeground(UIConstants.TEXT_PRIMARY);
                return lbl;
            }
        });

        // Init summary labels BEFORE loadBillRows
        lblTongPhong = new JLabel("0 đ");
        lblTongPhong.setFont(UIConstants.FONT_BODY);
        lblTongDV = new JLabel("0 đ");
        lblTongDV.setFont(UIConstants.FONT_BODY);
        lblGiamGia = new JLabel("0 đ");
        lblGiamGia.setFont(UIConstants.FONT_BODY);
        lblGiamGia.setForeground(UIConstants.SUCCESS);
        lblTongCong = new JLabel("0 đ");
        lblTongCong.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTongCong.setForeground(UIConstants.PRIMARY);

        loadBillRows();

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER));
        sp.setPreferredSize(new Dimension(0, 140));
        sp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 140));

        panel.add(hdr, BorderLayout.NORTH);
        panel.add(sp, BorderLayout.CENTER);
        return panel;
    }

    private void loadBillRows() {
        tableModel.setRowCount(0);
        service.BangGiaService bangGiaService = new service.BangGiaService();
        Phong phong = phongService.getPhongById(thuePhong.getSoPhong());
        double policyPrice = bangGiaService.layGiaHienHanh(phong != null && phong.getLoaiPhong() != null ? phong.getLoaiPhong().getMaLoaiPhong() : "");
        double donGiaThang = policyPrice > 0 ? policyPrice : (thuePhong.getGiaThucTeChot() > 0 ? thuePhong.getGiaThucTeChot() : (phong != null ? phong.getGiaTheoNgay() : 0.0));

        long soNgay = tinhSoNgayHieuLuc();
        double tienPhong = donGiaThang * soNgay;

        // 1. Kiểm tra xem phụ phí nhận sớm đã được lưu vào danh sách dịch vụ chưa
        java.util.List<SuDungDichVu> dsDichVu = thuePhongService.getDichVuByChiTiet(thuePhong.getMaChiTiet());
        boolean earlySurchargeAdded = dsDichVu.stream().anyMatch(s -> 
            (s.getDichVu() != null && "DV_EXTRA_EARLY".equals(s.getDichVu().getMaDV()))
            || (s.getTenDichVu() != null && s.getTenDichVu().toLowerCase().contains("nhận phòng sớm")));

        // 2. Tính phí phát sinh (nhận sớm/trả trễ)
        service.StandardRoomCalculator calc = new service.StandardRoomCalculator();
        java.time.LocalDateTime ngayNhanResolved = resolveNgayNhan();
        
        double phiCheckinOut = 0;
        if (ngayNhanResolved != null) {
            phiCheckinOut = calc.tinhPhuPhi(thuePhong, ngayNhanResolved, java.time.LocalDateTime.now());
            if (earlySurchargeAdded) {
                // Nếu đã lưu phụ phí nhận sớm vào dịch vụ, ta trừ phần đó ra để tránh tính trùng
                double onlyEarly = calc.tinhPhuPhi(thuePhong, ngayNhanResolved, ngayNhanResolved.withHour(14).withMinute(0).withSecond(0));
                phiCheckinOut = Math.max(0, phiCheckinOut - onlyEarly);
            }
        }
        
        double phiKhac = thuePhong.getPhuPhiPhatSinh();
        double phuPhi = phiCheckinOut + phiKhac;

        tableModel.addRow(new Object[] {
                "Tiền phòng – " + (thuePhong.getTenLoaiPhong() != null ? thuePhong.getTenLoaiPhong() : ""),
                String.format("%,.0f đ/đêm", donGiaThang),
                soNgay + " đêm",
                String.format("%,.0f đ", tienPhong)
        });

        if (phiCheckinOut > 0) {
            tableModel.addRow(new Object[] { "Phụ thu (Nhận sớm / Trả trễ)", "—", "—", String.format("%,.0f đ", phiCheckinOut) });
        }
        if (phiKhac > 0) {
            tableModel.addRow(new Object[] { "Phí phát sinh khác", "—", "—", String.format("%,.0f đ", phiKhac) });
        }

        long totalDV = 0;
        for (SuDungDichVu sddv : dsDichVu) {
            tableModel.addRow(new Object[] {
                    sddv.getTenDichVu() != null ? sddv.getTenDichVu() : "Dịch vụ",
                    String.format("%,.0f đ", (double) sddv.getDonGia()),
                    sddv.getSoLuong() + "x",
                    String.format("%,.0f đ", (double) sddv.tinhThanhTien())
            });
            totalDV += sddv.tinhThanhTien();
        }

        // 3. Xử lý Voucher/Khuyến mãi
        double tienGiam = 0;
        String voucherCode = (thuePhong.getDatPhong() != null) ? thuePhong.getDatPhong().getMaKhuyenMai() : null;
        
        double subTotal = tienPhong + phuPhi + totalDV;
        if (voucherCode != null && !voucherCode.isBlank()) {
            KhuyenMai km = new dao.KhuyenMaiDAO().getByVoucherCode(voucherCode.trim());
            
            LocalDateTime thoiDiemDat = (thuePhong.getDatPhong() != null) 
                ? thuePhong.getDatPhong().getNgayDat() 
                : LocalDateTime.now();
                
            if (km != null && km.kiemTraHopLe(subTotal, thoiDiemDat)) {
                tienGiam = km.tinhSoTienGiam(subTotal);
            }
        }

        java.math.BigDecimal bdTienPhong = java.math.BigDecimal.valueOf(tienPhong);
        java.math.BigDecimal bdPhuPhi = java.math.BigDecimal.valueOf(phuPhi);
        java.math.BigDecimal bdTienDV = java.math.BigDecimal.valueOf(totalDV);
        java.math.BigDecimal bdGiamGia = java.math.BigDecimal.valueOf(tienGiam);

        if (lblTongPhong != null)
            lblTongPhong.setText(String.format("%,.0f đ", bdTienPhong.add(bdPhuPhi).doubleValue()));
        if (lblTongDV != null)
            lblTongDV.setText(String.format("%,.0f đ", bdTienDV.doubleValue()));
        if (lblGiamGia != null)
            lblGiamGia.setText(String.format("-%,.0f đ", bdGiamGia.doubleValue()));
        
        if (lblTongCong != null) {
            double finalTotal = bdTienPhong.add(bdPhuPhi).add(bdTienDV).subtract(bdGiamGia).doubleValue();
            lblTongCong.setText(String.format("%,.0f đ", finalTotal));
        }
    }

    private JPanel buildAddDVSection() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        JLabel hdr = new JLabel("THÊM DỊCH VỤ (nếu có)");
        hdr.setFont(UIConstants.FONT_SMALL_BOLD);
        hdr.setForeground(UIConstants.TEXT_MUTED);
        hdr.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));

        dvList = dichVuService.getActive();
        String[] names = dvList.stream().map(DichVu::getTenDV).toArray(String[]::new);
        cboDV = new JComboBox<>(names.length > 0 ? names : new String[] { "Không có dịch vụ" });
        cboDV.setFont(UIConstants.FONT_BODY);
        spnSL = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        spnSL.setFont(UIConstants.FONT_BODY);
        spnSL.setPreferredSize(new Dimension(65, 32));

        JButton btnAdd = new JButton("+ Thêm");
        btnAdd.setFont(UIConstants.FONT_SMALL_BOLD);
        btnAdd.setBackground(UIConstants.SUCCESS);
        btnAdd.setForeground(Color.WHITE);
        btnAdd.setBorderPainted(false);
        btnAdd.setFocusPainted(false);
        btnAdd.addActionListener(e -> {
            int idx = cboDV.getSelectedIndex();
            if (idx < 0 || dvList.isEmpty())
                return;
            DichVu dv = dvList.get(idx);
            int sl = (int) spnSL.getValue();
            String err = thuePhongService.themDichVu(thuePhong.getMaChiTiet(), dv.getMaDV(), sl, dv.getGia());
            if (err != null) {
                ui.components.NotificationManager.showError("Lỗi", "Không thể thêm dịch vụ: " + err);
                return;
            }
            loadBillRows();
        });

        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.add(cboDV, BorderLayout.CENTER);
        JPanel right = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        right.setOpaque(false);
        JLabel slLbl = new JLabel("SL:");
        slLbl.setFont(UIConstants.FONT_BODY);
        right.add(slLbl);
        right.add(spnSL);
        right.add(btnAdd);
        row.add(right, BorderLayout.EAST);

        panel.add(hdr, BorderLayout.NORTH);
        panel.add(row, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildSummarySection() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 165));

        panel.add(Box.createVerticalStrut(10));
        panel.add(sumRow("Tiền phòng", lblTongPhong, false));
        panel.add(Box.createVerticalStrut(4));
        panel.add(sumRow("Tiền dịch vụ", lblTongDV, false));
        panel.add(Box.createVerticalStrut(4));
        panel.add(sumRow("Giảm giá", lblGiamGia, false));
        panel.add(Box.createVerticalStrut(6));
        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        panel.add(sep);
        panel.add(Box.createVerticalStrut(6));
        panel.add(sumRow("TỔNG CỘNG", lblTongCong, true));
        
        panel.add(Box.createVerticalStrut(12));
        JPanel rowHT = new JPanel(new BorderLayout());
        rowHT.setOpaque(false);
        rowHT.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        JLabel lblHT = new JLabel("Hình thức thanh toán");
        lblHT.setFont(UIConstants.FONT_BODY);
        lblHT.setForeground(UIConstants.TEXT_SECONDARY);
        rowHT.add(lblHT, BorderLayout.WEST);

        String[] options = { "Tiền mặt", "Chuyển khoản", "Thẻ tín dụng" };
        cboHinhThuc = new JComboBox<>(options);
        cboHinhThuc.setFont(UIConstants.FONT_BODY);
        cboHinhThuc.setBackground(Color.WHITE);
        cboHinhThuc.setPreferredSize(new Dimension(140, 28));
        
        JPanel comboPan = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        comboPan.setOpaque(false);
        comboPan.add(cboHinhThuc);
        rowHT.add(comboPan, BorderLayout.EAST);
        
        panel.add(rowHT);
        
        panel.add(Box.createVerticalStrut(8));
        chkDebt = new JCheckBox("Ghi nợ (Thanh toán sau)");
        chkDebt.setFont(UIConstants.FONT_BODY);
        chkDebt.setOpaque(false);
        chkDebt.setForeground(UIConstants.DANGER);
        chkDebt.addActionListener(e -> {
            cboHinhThuc.setEnabled(!chkDebt.isSelected());
            if (chkDebt.isSelected()) cboHinhThuc.setSelectedIndex(0);
        });
        
        JPanel debtPan = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        debtPan.setOpaque(false);
        debtPan.add(chkDebt);
        panel.add(debtPan);

        return panel;
    }

    private JPanel sumRow(String label, JLabel valLabel, boolean bold) {
        JPanel r = new JPanel(new BorderLayout());
        r.setOpaque(false);
        r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        JLabel lbl = new JLabel(label);
        lbl.setFont(bold ? new Font("Segoe UI", Font.BOLD, 15) : UIConstants.FONT_BODY);
        lbl.setForeground(bold ? UIConstants.TEXT_PRIMARY : UIConstants.TEXT_SECONDARY);
        r.add(lbl, BorderLayout.WEST);
        r.add(valLabel, BorderLayout.EAST);
        return r;
    }

    // ---- Footer ----
    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER),
                BorderFactory.createEmptyBorder(12, 28, 12, 28)));

        JButton btnPrint = new JButton(" In hóa đơn");
        try {
            java.io.File printIconFile = new java.io.File("icon/print.png");
            if (printIconFile.exists()) {
                javax.swing.ImageIcon icon = new javax.swing.ImageIcon(printIconFile.getAbsolutePath());
                java.awt.Image scaled = icon.getImage().getScaledInstance(18, 18, java.awt.Image.SCALE_SMOOTH);
                btnPrint.setIcon(new javax.swing.ImageIcon(scaled));
            } else {
                btnPrint.setText("🖨 In hóa đơn"); // Fallback nếu không có file ảnh
            }
        } catch (Exception e) {
            btnPrint.setText("🖨 In hóa đơn");
        }
        btnPrint.setFont(UIConstants.FONT_BODY);
        btnPrint.setForeground(UIConstants.TEXT_SECONDARY);
        btnPrint.setBorderPainted(false);
        btnPrint.setContentAreaFilled(false);
        btnPrint.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnPrint.addActionListener(e -> printHoaDon());

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);
        RoundedButton btnCancel = new RoundedButton("Huỷ", new Color(0xE2E8F0), UIConstants.TEXT_PRIMARY);
        RoundedButton btnConfirm = new RoundedButton("Xác nhận trả phòng", UIConstants.SUCCESS, Color.WHITE);
        btnConfirm.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnCancel.addActionListener(e -> dispose());
        btnConfirm.addActionListener(e -> doCheckout());
        btns.add(btnCancel);
        btns.add(btnConfirm);

        footer.add(btnPrint, BorderLayout.WEST);
        footer.add(btns, BorderLayout.EAST);
        return footer;
    }

    // ---- CHECK-OUT ----
    private void doCheckout() {
        // Lấy hình thức thanh toán từ ComboBox trên giao diện
        String hinhThuc = (String) cboHinhThuc.getSelectedItem();
        if (hinhThuc == null)
            return;

        // ── KIỂM TRA CẢNH BÁO NGHIỆP VỤ: TRÙNG NGƯỜI ĐẠI DIỆN VỚI PHÒNG KHÁC ──
        if (thuePhong.getDatPhong() != null) {
            java.util.List<ChiTietDatPhong> otherRooms = thuePhongService.getChiTietByDatPhong(thuePhong.getDatPhong().getMaDatPhong());
            java.util.List<String> sharedRooms = new ArrayList<>();
            String currentKH = thuePhong.resolveKhachHang() != null ? thuePhong.resolveKhachHang().getMaKhachHang() : null;
            
            if (currentKH != null) {
                for (ChiTietDatPhong c : otherRooms) {
                    if (!c.getMaChiTiet().equals(thuePhong.getMaChiTiet()) && c.getNgayTraThucTe() == null) {
                        String cKH = c.resolveKhachHang() != null ? c.resolveKhachHang().getMaKhachHang() : null;
                        if (currentKH.equals(cKH)) {
                            sharedRooms.add((c.getPhong() != null ? c.getPhong().getMaPhong() : "Unknown"));
                        }
                    }
                }
            }
            
            if (!sharedRooms.isEmpty()) {
                int warn = JOptionPane.showConfirmDialog(this,
                    "CẢNH BÁO NGHIỆP VỤ LƯU TRÚ:\n"
                    + "Phòng " + thuePhong.getSoPhong() + " đang trả phòng.\n"
                    + "Các phòng phụ thuộc: " + String.join(", ", sharedRooms) + " đang dùng CHUNG thông tin người đại diện này.\n\n"
                    + "Khi người đại diện rời đi, bạn NÊN bổ sung giấy tờ/CCCD cho người quản lý các phòng còn lại.\n"
                    + "Bạn có muốn tiếp tục thanh toán phòng " + thuePhong.getSoPhong() + " ngay lúc này?",
                    "Cảnh báo người đại diện RỜI ĐI", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (warn != JOptionPane.YES_OPTION) {
                    return;
                }
            }
        }
        // ────────────────────────────────────────────────────────

        // ── NẾU CHUYỂN KHOẢN → hiện QR ──────────────────────────
        if ("Chuyển khoản".equals(hinhThuc)) {
            // Tính tổng tiền để truyền vào QR
            Phong phong = phongService.getPhongById(thuePhong.getSoPhong());
            double donGiaThang = thuePhong.getGiaThucTeChot() > 0 ? thuePhong.getGiaThucTeChot()
                    : (phong != null ? phong.getGiaTheoNgay() : 0.0);
            long soNgay = tinhSoNgayHieuLuc();
            double tienPhong = donGiaThang * soNgay;

            // Tính phí
            service.StandardRoomCalculator calc = new service.StandardRoomCalculator();
            java.util.List<SuDungDichVu> dsDichVu = thuePhongService.getDichVuByChiTiet(thuePhong.getMaChiTiet());
            boolean earlyAdded = dsDichVu.stream().anyMatch(s -> 
                (s.getDichVu() != null && "DV_EXTRA_EARLY".equals(s.getDichVu().getMaDV()))
                || (s.getTenDichVu() != null && s.getTenDichVu().toLowerCase().contains("nhận phòng sớm")));

            java.time.LocalDateTime ngayNhanR = resolveNgayNhan();
            double phiCheckinOut = (ngayNhanR != null) ? calc.tinhPhuPhi(thuePhong, ngayNhanR, java.time.LocalDateTime.now()) : 0;
            if (earlyAdded && ngayNhanR != null) {
                double onlyEarly = calc.tinhPhuPhi(thuePhong, ngayNhanR, ngayNhanR.withHour(14).withMinute(0).withSecond(0));
                phiCheckinOut = Math.max(0, phiCheckinOut - onlyEarly);
            }

            double phuPhi = phiCheckinOut + thuePhong.getPhuPhiPhatSinh();
            double tienDV = dsDichVu.stream().mapToDouble(SuDungDichVu::tinhThanhTien).sum();
            
            double subTotal = tienPhong + phuPhi + tienDV;
            double tienGiam = 0;
            String vc = thuePhong.getDatPhong() != null ? thuePhong.getDatPhong().getMaKhuyenMai() : null;
            if (vc != null && !vc.isBlank()) {
                KhuyenMai km = new dao.KhuyenMaiDAO().getByVoucherCode(vc.trim());
                LocalDateTime thoiDiemDat = (thuePhong.getDatPhong() != null) 
                    ? thuePhong.getDatPhong().getNgayDat() 
                    : LocalDateTime.now();
                if (km != null && km.kiemTraHopLe(subTotal, thoiDiemDat)) tienGiam = km.tinhSoTienGiam(subTotal);
            }

            double tongCong = subTotal - tienGiam;

            String maHDLabel = "HD-" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + "-" + thuePhong.getSoPhong();
            // Hiện popup QR
            QRPaymentDialog qrDialog = new QRPaymentDialog(
                    (Frame) getOwner(), (long) tongCong, maHDLabel);
            qrDialog.setVisible(true);

            if (!qrDialog.isConfirmed())
                return; // người dùng bấm Huỷ
            // Nếu confirmed → tiếp tục checkout bình thường bên dưới
        }
        // ────────────────────────────────────────────────────────

        int ok = JOptionPane.showConfirmDialog(this,
                "Xác nhận trả phòng " + thuePhong.getSoPhong() + "?\nTổng tiền: " + lblTongCong.getText(),
                "Xác nhận", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION)
            return;

        String status = chkDebt.isSelected() ? "UNPAID" : "PAID";
        // Pass explicitly validated voucher from UI if available
        String currentVoucher = thuePhong.getDatPhong() != null ? thuePhong.getDatPhong().getMaKhuyenMai() : null;
        
        String err = thuePhongService.checkOut(
                thuePhong.getMaChiTiet(),
                AuthService.getInstance().getCurrentMaNV(),
                status,
                currentVoucher);

        if (err == null) {
            confirmed = true;
            ui.components.NotificationManager.showSuccess("Trả phòng thành công", 
                "Phòng " + thuePhong.getSoPhong() + " đã được thanh toán.");
            
            // Thông báo làm mới toàn bộ hệ thống (Sơ đồ phòng, Thống kê, Lịch sử)
            if (getOwner() instanceof MainFrame) {
                ((MainFrame) getOwner()).notifyDataChanged();
            }
            
            dispose();
        } else {
            ui.components.NotificationManager.showError("Lỗi", err);
        }
    }

    // ---- PRINT ----
    private void printHoaDon() {
        // Tạo panel in
        JPanel printPanel = buildPrintPanel();
        
        // Kích hoạt Layout Manager chìm bằng 1 frame ảo để load các Sub-Panel
        JWindow dummyWindow = new JWindow();
        dummyWindow.getContentPane().add(printPanel);
        dummyWindow.pack();

        PrinterJob job = PrinterJob.getPrinterJob();
        job.setJobName("HoaDon_" + thuePhong.getSoPhong());

        job.setPrintable((graphics, pageFormat, pageIndex) -> {
            if (pageIndex > 0)
                return Printable.NO_SUCH_PAGE;
            Graphics2D g2 = (Graphics2D) graphics;
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            // Scale to fit page
            double scaleX = pageFormat.getImageableWidth() / printPanel.getPreferredSize().getWidth();
            double scaleY = pageFormat.getImageableHeight() / printPanel.getPreferredSize().getHeight();
            double scale = Math.min(scaleX, scaleY);
            g2.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
            g2.scale(scale, scale);
            
            printPanel.printAll(g2);
            return Printable.PAGE_EXISTS;
        });

        if (job.printDialog()) {
            try {
                job.print();
            } catch (PrinterException ex) {
                ui.components.NotificationManager.showError("Lỗi in", ex.getMessage());
            }
        }
        
        dummyWindow.dispose();
    }

    private JPanel buildPrintPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setPreferredSize(new Dimension(550, 800)); // Slightly wider for better table layout
        panel.setBorder(BorderFactory.createEmptyBorder(25, 35, 25, 35));

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        KhachHang kh = thuePhong.resolveKhachHang();
        Phong phong = phongService.getPhongById(thuePhong.getSoPhong());
        
        // 1. BRANDING HEADER
        panel.add(buildPrintHeader());
        panel.add(Box.createVerticalStrut(15));
        
        // 2. INVOICE TITLE & META
        JLabel lblTitle = new JLabel("HÓA ĐƠN THANH TOÁN", SwingConstants.CENTER);
        lblTitle.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblTitle.setForeground(new Color(0x1E293B));
        panel.add(lblTitle);
        
        String maHD = "HD-" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + "-" + thuePhong.getSoPhong();
        JLabel lblMa = new JLabel("Số HĐ: " + maHD, SwingConstants.CENTER);
        lblMa.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        lblMa.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblMa.setForeground(new Color(0x64748B));
        panel.add(lblMa);
        
        JLabel lblDate = new JLabel("Ngày lập: " + sdf.format(new Date()), SwingConstants.CENTER);
        lblDate.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        lblDate.setAlignmentX(Component.CENTER_ALIGNMENT);
        lblDate.setForeground(new Color(0x64748B));
        panel.add(lblDate);
        panel.add(Box.createVerticalStrut(20));

        // 3. SECTIONS: CUSTOMER & STAY INFO
        JPanel infoGrid = new JPanel(new GridLayout(1, 2, 20, 0));
        infoGrid.setOpaque(false);
        infoGrid.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));
        
        // Left Column: Customer
        JPanel custCol = new JPanel();
        custCol.setOpaque(false);
        custCol.setLayout(new BoxLayout(custCol, BoxLayout.Y_AXIS));
        custCol.add(printSectionHeader("THÔNG TIN KHÁCH HÀNG"));
        custCol.add(printLine("Khách hàng:", kh != null ? kh.getHoTen() : "—"));
        custCol.add(printLine("SĐT:", kh != null && kh.getSoDienThoai() != null ? kh.getSoDienThoai() : "—"));
        custCol.add(printLine("CCCD:", kh != null && kh.getCccd() != null ? kh.getCccd() : "—"));
        infoGrid.add(custCol);
        
        // Right Column: Stay
        JPanel stayCol = new JPanel();
        stayCol.setOpaque(false);
        stayCol.setLayout(new BoxLayout(stayCol, BoxLayout.Y_AXIS));
        stayCol.add(printSectionHeader("CHI TIẾT LƯU TRÚ"));
        stayCol.add(printLine("Phòng:", thuePhong.getSoPhong() + " - " + (thuePhong.getTenLoaiPhong() != null ? thuePhong.getTenLoaiPhong() : "")));
        stayCol.add(printLine("Nhận phòng:", resolveNgayNhan_Date() != null ? sdf.format(resolveNgayNhan_Date()) : "—"));
        stayCol.add(printLine("Trả phòng:", sdf.format(new Date())));
        infoGrid.add(stayCol);
        
        panel.add(infoGrid);
        panel.add(Box.createVerticalStrut(25));

        // 4. BILLING TABLE
        panel.add(printSectionHeader("CHI TIẾT THANH TOÁN"));
        panel.add(Box.createVerticalStrut(8));
        
        // Table Header
        panel.add(buildPrintTableHeader());
        
        // Table Rows
        double donGiaThang = thuePhong.getGiaThucTeChot() > 0 ? thuePhong.getGiaThucTeChot()
                : (phong != null ? phong.getGiaTheoNgay() : 0.0);
        long soNgay = tinhSoNgayHieuLuc();
        double tienPhong = donGiaThang * soNgay;
        
        panel.add(buildPrintTableRow("01", "Tiền phòng (" + soNgay + " đêm)", String.format("%,.0f", donGiaThang), String.valueOf(soNgay), String.format("%,.0f đ", tienPhong)));
        
        int stt = 2;
        service.StandardRoomCalculator calc = new service.StandardRoomCalculator();
        java.util.List<SuDungDichVu> dsDichVu = thuePhongService.getDichVuByChiTiet(thuePhong.getMaChiTiet());
        
        // Surcharges
        java.time.LocalDateTime ngayNhanP = resolveNgayNhan();
        double phiCheckinOut = (ngayNhanP != null) ? calc.tinhPhuPhi(thuePhong, ngayNhanP, java.time.LocalDateTime.now()) : 0;
        if (phiCheckinOut > 0) {
            panel.add(buildPrintTableRow(String.format("%02d", stt++), "Phụ thu (Nhận sớm / Trả trễ)", "—", "—", String.format("%,.0f đ", phiCheckinOut)));
        }
        
        double phiKhac = thuePhong.getPhuPhiPhatSinh();
        if (phiKhac > 0) {
             panel.add(buildPrintTableRow(String.format("%02d", stt++), "Phí phát sinh khác", "—", "—", String.format("%,.0f đ", phiKhac)));
        }

        // Services
        for (SuDungDichVu sddv : dsDichVu) {
            panel.add(buildPrintTableRow(String.format("%02d", stt++), sddv.getTenDichVu(), String.format("%,.0f", (double) sddv.getDonGia()), String.valueOf(sddv.getSoLuong()), String.format("%,.0f đ", (double) sddv.tinhThanhTien())));
        }
        
        panel.add(Box.createVerticalGlue()); // Push rest to bottom
        
        // 5. SUMMARY
        double totalDV = dsDichVu.stream().mapToDouble(SuDungDichVu::tinhThanhTien).sum();
        double subTotal = tienPhong + phiCheckinOut + phiKhac + totalDV;
        
        double tienGiam = 0;
        String vc = thuePhong.getDatPhong() != null ? thuePhong.getDatPhong().getMaKhuyenMai() : null;
        if (vc != null && !vc.isBlank()) {
            KhuyenMai km = new dao.KhuyenMaiDAO().getByVoucherCode(vc.trim());
            if (km != null && km.kiemTraHopLe(subTotal)) tienGiam = km.tinhSoTienGiam(subTotal);
        }
        
        double tongFinal = subTotal - tienGiam;
        
        panel.add(printSep());
        panel.add(Box.createVerticalStrut(10));
        
        JPanel summaryBox = new JPanel(new BorderLayout());
        summaryBox.setOpaque(false);
        summaryBox.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        
        JPanel rightSummary = new JPanel(new GridLayout(3, 1, 0, 5));
        rightSummary.setOpaque(false);
        rightSummary.add(printSummaryLine("Tổng tiền dịch vụ:", String.format("%,.0f đ", totalDV + phiCheckinOut + phiKhac)));
        if (tienGiam > 0) rightSummary.add(printSummaryLine("Giảm giá (Voucher):", String.format("-%,.0f đ", tienGiam)));
        
        JLabel lblTongFinal = new JLabel("TỔNG CỘNG:   " + String.format("%,.0f đ", tongFinal));
        lblTongFinal.setFont(new Font("Segoe UI", Font.BOLD, 18));
        lblTongFinal.setForeground(new Color(0x1E293B));
        lblTongFinal.setHorizontalAlignment(SwingConstants.RIGHT);
        rightSummary.add(lblTongFinal);
        
        summaryBox.add(rightSummary, BorderLayout.EAST);
        panel.add(summaryBox);
        
        panel.add(Box.createVerticalStrut(30));
        
        // 6. SIGNATURE AREAS
        panel.add(buildSignatureArea());
        
        panel.add(Box.createVerticalStrut(30));
        JLabel thanks = new JLabel("Cảm ơn quý khách! Hẹn gặp lại.", SwingConstants.CENTER);
        thanks.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        thanks.setForeground(new Color(0x64748B));
        thanks.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(thanks);
        
        return panel;
    }

    private JPanel buildPrintHeader() {
        JPanel h = new JPanel(new BorderLayout());
        h.setOpaque(false);
        h.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        
        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        
        JLabel hotelName = new JLabel("HOTEL OHNO");
        hotelName.setFont(new Font("Segoe UI", Font.BOLD, 20));
        hotelName.setForeground(new Color(0x2563EB));
        
        JLabel hotelAddr = new JLabel("12 Quang Trung, Gò Vấp, TP. HCM");
        hotelAddr.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        hotelAddr.setForeground(new Color(0x64748B));
        
        JLabel hotelContact = new JLabel("SĐT: 028.1234.5678 | Email: contact@ohno.com");
        hotelContact.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        hotelContact.setForeground(new Color(0x64748B));

        left.add(hotelName);
        left.add(hotelAddr);
        left.add(hotelContact);
        
        h.add(left, BorderLayout.WEST);
        return h;
    }

    private JPanel printSectionHeader(String title) {
        JPanel p = new JPanel(new BorderLayout());
        p.setBackground(new Color(0xF1F5F9));
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        p.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        JLabel lbl = new JLabel(title);
        lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lbl.setForeground(new Color(0x334155));
        p.add(lbl, BorderLayout.WEST);
        return p;
    }

    private JPanel printLine(String label, String value) {
        JPanel p = new JPanel(new BorderLayout(5, 0));
        p.setOpaque(false);
        p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        p.setBorder(BorderFactory.createEmptyBorder(2, 10, 2, 0));
        JLabel l = new JLabel(label);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        l.setForeground(new Color(0x64748B));
        l.setPreferredSize(new Dimension(100, 18));
        JLabel v = new JLabel(value);
        v.setFont(new Font("Segoe UI", Font.BOLD, 11));
        v.setForeground(new Color(0x334155));
        p.add(l, BorderLayout.WEST);
        p.add(v, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildPrintTableHeader() {
        JPanel h = new JPanel(new GridBagLayout());
        h.setBackground(new Color(0x334155));
        h.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        
        String[] headers = {"STT", "Nội dung", "Đơn giá", "SL", "Thành tiền"};
        double[] weights = {0.08, 0.47, 0.15, 0.10, 0.20};
        
        for (int i = 0; i < headers.length; i++) {
            gbc.weightx = weights[i];
            JLabel lbl = new JLabel(headers[i], SwingConstants.CENTER);
            lbl.setFont(new Font("Segoe UI", Font.BOLD, 11));
            lbl.setForeground(Color.WHITE);
            lbl.setBorder(BorderFactory.createMatteBorder(0, 0, 0, i == headers.length - 1 ? 0 : 1, Color.GRAY));
            h.add(lbl, gbc);
        }
        return h;
    }

    private JPanel buildPrintTableRow(String stt, String content, String price, String qty, String total) {
        JPanel r = new JPanel(new GridBagLayout());
        r.setBackground(Color.WHITE);
        r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
        r.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xF1F5F9)));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1.0;
        
        String[] vals = {stt, content, price, qty, total};
        double[] weights = {0.08, 0.47, 0.15, 0.10, 0.20};
        int[] aligns = {SwingConstants.CENTER, SwingConstants.LEFT, SwingConstants.RIGHT, SwingConstants.CENTER, SwingConstants.RIGHT};
        
        for (int i = 0; i < vals.length; i++) {
            gbc.weightx = weights[i];
            JLabel lbl = new JLabel(vals[i], aligns[i]);
            lbl.setFont(new Font("Segoe UI", i == 1 ? Font.BOLD : Font.PLAIN, 11));
            lbl.setForeground(new Color(0x334155));
            lbl.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
            r.add(lbl, gbc);
        }
        return r;
    }

    private JPanel printSummaryLine(String label, String value) {
        JPanel p = new JPanel(new BorderLayout(10, 0));
        p.setOpaque(false);
        p.setPreferredSize(new Dimension(250, 20));
        JLabel l = new JLabel(label);
        l.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        l.setForeground(new Color(0x64748B));
        JLabel v = new JLabel(value, SwingConstants.RIGHT);
        v.setFont(new Font("Segoe UI", Font.BOLD, 12));
        v.setForeground(new Color(0x334155));
        p.add(l, BorderLayout.WEST);
        p.add(v, BorderLayout.CENTER);
        return p;
    }

    private JPanel buildSignatureArea() {
        JPanel s = new JPanel(new GridLayout(1, 2));
        s.setOpaque(false);
        s.setMaximumSize(new Dimension(Integer.MAX_VALUE, 100));
        
        s.add(buildSignatureBlock("Khách hàng", "(Ký và ghi rõ họ tên)"));
        s.add(buildSignatureBlock("Thủ quỹ / Lễ tân", "(Ký và ghi rõ họ tên)"));
        
        return s;
    }

    private JPanel buildSignatureBlock(String title, String sub) {
        JPanel p = new JPanel();
        p.setOpaque(false);
        p.setLayout(new BoxLayout(p, BoxLayout.Y_AXIS));
        
        JLabel t = new JLabel(title);
        t.setFont(new Font("Segoe UI", Font.BOLD, 12));
        t.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        JLabel s = new JLabel(sub);
        s.setFont(new Font("Segoe UI", Font.ITALIC, 10));
        s.setForeground(Color.GRAY);
        s.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        p.add(t);
        p.add(s);
        p.add(Box.createVerticalStrut(50)); // Signature space
        
        return p;
    }

    private JSeparator printSep() {
        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        sep.setForeground(new Color(0xCBD5E1));
        return sep;
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}
