package ui.dialogs;

import service.AuthService;
import service.ThuePhongService;
import service.DichVuService;
import service.PhongService;
import service.KhachHangService;
import entity.*;
import ui.components.UIConstants;
import ui.components.RoundedComponents.*;
import ui.components.RoundedComponents.*;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.print.*;
import java.text.*;
import java.util.*;

public class HoaDonDialog extends JDialog {

    private final ThuePhongService thuePhongService = new ThuePhongService();
    private final DichVuService dichVuService = new DichVuService();
    private final PhongService phongService = new PhongService();
    private final KhachHangService khService = new KhachHangService();

    private final ChiTietDatPhong thuePhong;
    private boolean confirmed = false;

    private DefaultTableModel tableModel;
    private JLabel lblTongPhong, lblTongDV, lblTongCong;
    private JComboBox<String> cboDV;
    private JSpinner spnSL;
    private java.util.List<DichVu> dvList = new ArrayList<>();
    private JComboBox<String> cboHinhThuc;

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
        JLabel t2 = new JLabel("P." + thuePhong.getSoPhong() + " · "
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

        KhachHang kh = thuePhong.getDatPhong() != null ? thuePhong.getDatPhong().getKhachHang()
                : khService.getById(thuePhong.getMaKH());
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
                        thuePhong.getNgayNhanThucTe() != null ? sdf.format(thuePhong.getNgayNhanThucTe_Date()) : "—" },
                { "Trả phòng:", sdf.format(new Date()) },
                { "Số đêm:",
                        thuePhongService.tinhSoNgay(thuePhong.getNgayNhanThucTe(), java.time.LocalDateTime.now())
                                + " đêm" },
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
        Phong phong = phongService.getPhongById(thuePhong.getSoPhong());
        double donGiaThang = thuePhong.getGiaThucTeChot() > 0 ? thuePhong.getGiaThucTeChot()
                : (phong != null ? phong.getGiaTheoNgay() : 0.0);
        long soNgay = thuePhongService.tinhSoNgay(thuePhong.getNgayNhanThucTe_Date(), new Date());
        double tienPhong = donGiaThang * soNgay;

        // Tính phí phát sinh (check-in sớm/check-out muộn)
        double phuPhi = 0;
        double phiCheckin = 0;
        double phiCheckout = 0;
        double phiKhac = thuePhong.getPhuPhiPhatSinh();

        if (thuePhong.getNgayNhanThucTe() != null) {
            int gioNhan = thuePhong.getNgayNhanThucTe().getHour();
            if (gioNhan < 12) {
                phiCheckin = 50000; // Check-in trước 12h tính phí
                phuPhi += phiCheckin;
            }
        }
        int gioNow = java.time.LocalDateTime.now().getHour();
        if (gioNow > 13) {
            phiCheckout = 50000; // Check-out sau 13h tính phí
            phuPhi += phiCheckout;
        }
        phuPhi += phiKhac; // Cộng phí khác

        tableModel.addRow(new Object[] {
                "Tiền phòng – " + (thuePhong.getTenLoaiPhong() != null ? thuePhong.getTenLoaiPhong() : ""),
                String.format("%,.0f đ/đêm", donGiaThang),
                soNgay + " đêm",
                String.format("%,.0f đ", tienPhong)
        });

        if (phiCheckin > 0) {
            tableModel.addRow(new Object[] { "Phụ thu nhận phòng sớm", "—", "—", String.format("%,.0f đ", phiCheckin) });
        }
        if (phiCheckout > 0) {
            tableModel.addRow(new Object[] { "Phụ thu trả phòng muộn", "—", "—", String.format("%,.0f đ", phiCheckout) });
        }
        if (phiKhac > 0) {
            tableModel.addRow(new Object[] { "Phí phát sinh khác", "—", "—", String.format("%,.0f đ", phiKhac) });
        }

        java.util.List<SuDungDichVu> dsDichVu = thuePhongService.getDichVuByThue(thuePhong.getMaChiTiet());
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

        if (lblTongPhong != null)
            lblTongPhong.setText(String.format("%,.0f đ", tienPhong + phuPhi));
        if (lblTongDV != null)
            lblTongDV.setText(String.format("%,.0f đ", (double) totalDV));
        if (lblTongCong != null)
            lblTongCong.setText(String.format("%,.0f đ", tienPhong + phuPhi + totalDV));
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
                JOptionPane.showMessageDialog(this, "Không thể thêm dịch vụ: " + err, "Lỗi", JOptionPane.ERROR_MESSAGE);
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

        // ── NẾU CHUYỂN KHOẢN → hiện QR ──────────────────────────
        if ("Chuyển khoản".equals(hinhThuc)) {
            // Tính tổng tiền để truyền vào QR
            Phong phong = phongService.getPhongById(thuePhong.getSoPhong());
            double donGiaThang = thuePhong.getGiaThucTeChot() > 0 ? thuePhong.getGiaThucTeChot()
                    : (phong != null ? phong.getGiaTheoNgay() : 0.0);
            long soNgay = thuePhongService.tinhSoNgay(thuePhong.getNgayNhanThucTe_Date(), new Date());
            double tienPhong = donGiaThang * soNgay;

            // Tính phí
            double phuPhi = 0;
            if (thuePhong.getNgayNhanThucTe() != null && thuePhong.getNgayNhanThucTe().getHour() < 12)
                phuPhi += 50000;
            if (java.time.LocalDateTime.now().getHour() > 13)
                phuPhi += 50000;
            phuPhi += thuePhong.getPhuPhiPhatSinh();

            java.util.List<SuDungDichVu> dsDichVu = thuePhongService.getDichVuByThue(thuePhong.getMaChiTiet());
            double tienDV = dsDichVu.stream().mapToDouble(SuDungDichVu::tinhThanhTien).sum();
            double tongCong = tienPhong + phuPhi + tienDV;

            String maHD = "HD-" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + "-" + thuePhong.getSoPhong();
            // Hiện popup QR
            QRPaymentDialog qrDialog = new QRPaymentDialog(
                    (Frame) getOwner(), (long) tongCong, maHD);
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

        String err = thuePhongService.checkOut(
                thuePhong.getMaChiTiet(),
                AuthService.getInstance().getCurrentMaNV(),
                hinhThuc);

        if (err == null) {
            confirmed = true;
            JOptionPane.showMessageDialog(this,
                    "✓ Trả phòng thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Lỗi: " + err, "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ---- PRINT ----
    private void printHoaDon() {
        // Tạo panel in
        JPanel printPanel = buildPrintPanel();
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
            printPanel.setSize(printPanel.getPreferredSize());
            printPanel.doLayout();
            printPanel.printAll(g2);
            return Printable.PAGE_EXISTS;
        });

        if (job.printDialog()) {
            try {
                job.print();
            } catch (PrinterException ex) {
                JOptionPane.showMessageDialog(this, "Lỗi in: " + ex.getMessage(), "Lỗi in", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private JPanel buildPrintPanel() {
        JPanel panel = new JPanel();
        panel.setBackground(Color.WHITE);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setPreferredSize(new Dimension(500, 700));
        panel.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        KhachHang kh = khService.getById(thuePhong.getMaKH());
        Phong phong = phongService.getPhongById(thuePhong.getSoPhong());
        double donGiaThang = thuePhong.getGiaThucTeChot() > 0 ? thuePhong.getGiaThucTeChot()
                : (phong != null ? phong.getGiaTheoNgay() : 0.0);
        long soNgay = thuePhongService.tinhSoNgay(thuePhong.getNgayNhanThucTe_Date(), new Date());
        double tienPhong = donGiaThang * soNgay;

        // Tính phí
        double phuPhi = 0;
        double phiCheckin = 0;
        double phiCheckout = 0;
        double phiKhac = thuePhong.getPhuPhiPhatSinh();

        if (thuePhong.getNgayNhanThucTe() != null && thuePhong.getNgayNhanThucTe().getHour() < 12) {
            phiCheckin = 50000;
            phuPhi += phiCheckin;
        }
        if (java.time.LocalDateTime.now().getHour() > 13) {
            phiCheckout = 50000;
            phuPhi += phiCheckout;
        }
        phuPhi += phiKhac;

        java.util.List<SuDungDichVu> dsDichVu = thuePhongService.getDichVuByThue(thuePhong.getMaChiTiet());
        double totalDV = dsDichVu.stream().mapToDouble(SuDungDichVu::tinhThanhTien).sum();

        // Header
        JLabel title = new JLabel("HÓA ĐƠN THANH TOÁN", SwingConstants.CENTER);
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel maHD = new JLabel(
                "Mã HĐ: HD-" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + "-" + thuePhong.getSoPhong(),
                SwingConstants.CENTER);
        maHD.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        maHD.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel ngay = new JLabel("Ngày: " + sdf.format(new Date()), SwingConstants.CENTER);
        ngay.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        ngay.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(title);
        panel.add(Box.createVerticalStrut(2));
        panel.add(maHD);
        panel.add(ngay);
        panel.add(Box.createVerticalStrut(12));
        panel.add(printSep());
        panel.add(Box.createVerticalStrut(8));

        // Khách hàng
        panel.add(printRow("Phòng:", thuePhong.getSoPhong() + " – "
                + (thuePhong.getTenLoaiPhong() != null ? thuePhong.getTenLoaiPhong() : "")));
        panel.add(printRow("Khách hàng:", kh != null ? kh.getHoTen() : "—"));
        panel.add(printRow("SĐT:", kh != null && kh.getSoDienThoai() != null ? kh.getSoDienThoai() : "—"));
        panel.add(printRow("Nhận phòng:",
                thuePhong.getNgayNhanThucTe_Date() != null ? sdf.format(thuePhong.getNgayNhanThucTe_Date()) : "—"));
        panel.add(printRow("Trả phòng:", sdf.format(new Date())));
        panel.add(printRow("Số đêm:", soNgay + " đêm"));
        panel.add(Box.createVerticalStrut(8));
        panel.add(printSep());
        panel.add(Box.createVerticalStrut(8));

        // Chi tiết
        panel.add(printRow("Tiền phòng:", String.format("%,.0f đ", (double) tienPhong)));
        if (phiCheckin > 0) {
            panel.add(printRow("Phụ thu nhận phòng sớm:", String.format("%,.0f đ", phiCheckin)));
        }
        if (phiCheckout > 0) {
            panel.add(printRow("Phụ thu trả phòng muộn:", String.format("%,.0f đ", phiCheckout)));
        }
        if (phiKhac > 0) {
            panel.add(printRow("Phí phát sinh khác:", String.format("%,.0f đ", phiKhac)));
        }
        for (SuDungDichVu sddv : dsDichVu)
            panel.add(printRow(sddv.getTenDichVu() + "×" + sddv.getSoLuong() + ":",
                    String.format("%,.0f đ", (double) sddv.tinhThanhTien())));
        panel.add(Box.createVerticalStrut(8));
        panel.add(printSep());
        panel.add(Box.createVerticalStrut(8));

        JLabel total = new JLabel("TỔNG CỘNG:   " + String.format("%,.0f đ", tienPhong + phuPhi + totalDV));
        total.setFont(new Font("Segoe UI", Font.BOLD, 15));
        total.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(total);
        panel.add(Box.createVerticalStrut(16));
        JLabel thanks = new JLabel("Cảm ơn quý khách! Hẹn gặp lại.", SwingConstants.CENTER);
        thanks.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        thanks.setForeground(Color.GRAY);
        thanks.setAlignmentX(Component.CENTER_ALIGNMENT);
        panel.add(thanks);
        return panel;
    }

    private JPanel printRow(String label, String value) {
        JPanel r = new JPanel(new BorderLayout());
        r.setOpaque(false);
        r.setMaximumSize(new Dimension(Integer.MAX_VALUE, 22));
        JLabel k = new JLabel(label);
        k.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        k.setPreferredSize(new Dimension(130, 18));
        JLabel v = new JLabel(value, SwingConstants.RIGHT);
        v.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        r.add(k, BorderLayout.WEST);
        r.add(v, BorderLayout.EAST);
        return r;
    }

    private JSeparator printSep() {
        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        return sep;
    }

    public boolean isConfirmed() {
        return confirmed;
    }
}
