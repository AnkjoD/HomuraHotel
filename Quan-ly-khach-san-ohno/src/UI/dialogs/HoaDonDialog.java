package UI.dialogs;

import service.AuthService;
import service.ThuePhongService;
import service.DichVuService;
import service.PhongService;
import service.KhachHangService;
import entity.*;
import UI.components.UIConstants;
import UI.components.RoundedComponents.*;
import UI.components.RoundedComponents.*;

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
    private JLabel lblTongPhong, lblTongDV, lblTongCong, lblDatCoc, lblConLai;
    private JComboBox<String> cboDV;
    private JComboBox<String> cboHinhThucThanhToan; // ★ Thêm combobox hình thức
    private JSpinner spnSL;
    private java.util.List<DichVu> dvList = new ArrayList<>();

    public HoaDonDialog(Frame parent, ChiTietDatPhong tp) {
        super(parent, "Hóa đơn – Phòng " + tp.getSoPhong(), true);
        this.thuePhong = tp;
        setSize(660, 700);
        setLocationRelativeTo(parent);
        setResizable(false);
        getContentPane().setBackground(Color.WHITE);
        buildUI();
    }

    // ★ Helper: tính ngày nhận thực tế (fallback khi NULL)
    private Date getNgayNhanFallback() {
        if (thuePhong.getNgayNhanThucTe_Date() != null)
            return thuePhong.getNgayNhanThucTe_Date();
        // Fallback 1: dùng ngày nhận dự kiến từ DatPhong
        if (thuePhong.getDatPhong() != null && thuePhong.getDatPhong().getNgayNhanDK_Date() != null)
            return thuePhong.getDatPhong().getNgayNhanDK_Date();
        // Fallback 2: dùng ngày đặt
        if (thuePhong.getDatPhong() != null && thuePhong.getDatPhong().getNgayDat() != null)
            return java.sql.Timestamp.valueOf(thuePhong.getDatPhong().getNgayDat());
        return new Date(); // cuối cùng dùng NOW
    }

    // ★ Helper: tính số đêm (tối thiểu 1)
    private long getSoNgay() {
        return Math.max(1, thuePhongService.tinhSoNgay(getNgayNhanFallback(), new Date()));
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
        Date ngayNhanDisplay = getNgayNhanFallback();

        panel.add(infoBlock("CHI TIẾT LƯU TRÚ", new String[][] {
                { "Nhận phòng:", sdf.format(ngayNhanDisplay) },
                { "Trả phòng:", sdf.format(new Date()) },
                { "Số đêm:", getSoNgay() + " đêm" },
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
        lblDatCoc = new JLabel("0 đ");
        lblDatCoc.setFont(UIConstants.FONT_BODY);
        lblDatCoc.setForeground(UIConstants.WARNING);
        lblTongCong = new JLabel("0 đ");
        lblTongCong.setFont(UIConstants.FONT_BODY_BOLD);
        lblTongCong.setForeground(UIConstants.TEXT_PRIMARY);
        lblConLai = new JLabel("0 đ");
        lblConLai.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblConLai.setForeground(UIConstants.PRIMARY);

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
        long soNgay = getSoNgay(); // ★ Dùng helper (min 1, có fallback)
        double tienPhong = donGiaThang * soNgay;

        // Tính phí phát sinh (check-in sớm/check-out muộn)
        double phuPhi = 0;
        if (thuePhong.getNgayNhanThucTe() != null) {
            int gioNhan = thuePhong.getNgayNhanThucTe().getHour();
            if (gioNhan < 12)
                phuPhi += 50000;
        }
        int gioNow = java.time.LocalDateTime.now().getHour();
        if (gioNow > 13) {
            phuPhi += 50000;
        }
        phuPhi += thuePhong.getPhuPhiPhatSinh();

        tableModel.addRow(new Object[] {
                "Tiền phòng – " + (thuePhong.getTenLoaiPhong() != null ? thuePhong.getTenLoaiPhong() : ""),
                String.format("%,.0f đ/đêm", donGiaThang),
                soNgay + " đêm",
                String.format("%,.0f đ", tienPhong)
        });

        if (phuPhi > 0) {
            tableModel.addRow(new Object[] {
                    "Phí phát sinh",
                    "—",
                    "—",
                    String.format("%,.0f đ", phuPhi)
            });
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

        double tongGop = tienPhong + phuPhi + totalDV;
        // ★ Lấy đặt cọc từ DatPhong
        double datCoc = 0;
        if (thuePhong.getDatPhong() != null) {
            datCoc = thuePhong.getDatPhong().getTienDatCoc();
        }
        double conLai = tongGop - datCoc;

        if (lblTongPhong != null)
            lblTongPhong.setText(String.format("%,.0f đ", tienPhong + phuPhi));
        if (lblTongDV != null)
            lblTongDV.setText(String.format("%,.0f đ", (double) totalDV));
        if (lblTongCong != null)
            lblTongCong.setText(String.format("%,.0f đ", tongGop));
        if (lblDatCoc != null)
            lblDatCoc.setText(datCoc > 0 ? String.format("- %,.0f đ", datCoc) : "Không cọc");
        if (lblConLai != null) {
            if (conLai >= 0) {
                lblConLai.setText(String.format("%,.0f đ", conLai));
                lblConLai.setForeground(UIConstants.PRIMARY);
            } else {
                // Đặt cọc > tổng → cần hoàn lại
                lblConLai.setText(String.format("Hoàn lại %,.0f đ", Math.abs(conLai)));
                lblConLai.setForeground(UIConstants.SUCCESS);
            }
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
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 120));

        panel.add(Box.createVerticalStrut(10));
        panel.add(sumRow("Tiền phòng", lblTongPhong, false));
        panel.add(Box.createVerticalStrut(4));
        panel.add(sumRow("Tiền dịch vụ", lblTongDV, false));
        panel.add(Box.createVerticalStrut(6));
        JSeparator sep1 = new JSeparator();
        sep1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        panel.add(sep1);
        panel.add(Box.createVerticalStrut(6));
        panel.add(sumRow("Tổng cộng", lblTongCong, false));
        panel.add(Box.createVerticalStrut(4));
        panel.add(sumRow("Đặt cọc", lblDatCoc, false));
        panel.add(Box.createVerticalStrut(6));

        // ★ Hình thức thanh toán
        JPanel methodRow = new JPanel(new BorderLayout());
        methodRow.setOpaque(false);
        methodRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        JLabel lblMethod = new JLabel("Hình thức:");
        lblMethod.setFont(UIConstants.FONT_BODY);
        lblMethod.setForeground(UIConstants.TEXT_SECONDARY);
        cboHinhThucThanhToan = new JComboBox<>(new String[] { "Tiền mặt", "Chuyển khoản", "Thẻ tín dụng" });
        cboHinhThucThanhToan.setFont(UIConstants.FONT_BODY);
        cboHinhThucThanhToan.setPreferredSize(new Dimension(140, 32));
        methodRow.add(lblMethod, BorderLayout.WEST);
        methodRow.add(cboHinhThucThanhToan, BorderLayout.EAST);
        panel.add(methodRow);

        panel.add(Box.createVerticalStrut(6));
        JSeparator sep2 = new JSeparator();
        sep2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        panel.add(sep2);
        panel.add(Box.createVerticalStrut(6));
        panel.add(sumRow("CÒN PHẢI TRẢ", lblConLai, true));
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

        JButton btnPrint = new JButton("In hóa đơn");
        try {
            // Tải ảnh và resize tự động
            java.awt.Image imgPrint = javax.imageio.ImageIO.read(new java.io.File("icon/print.png"));
            btnPrint.setIcon(new ImageIcon(imgPrint.getScaledInstance(18, 18, java.awt.Image.SCALE_SMOOTH)));
        } catch (Exception ex) {
            btnPrint.setText("In hóa đơn"); // Fallback emoji nếu không tìm thấy file
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

        try {
            // Tải ảnh cho nút xác nhận trả phòng
            java.awt.Image imgConfirm = javax.imageio.ImageIO.read(new java.io.File("icon/accept.png"));
            btnConfirm.setIcon(new ImageIcon(imgConfirm.getScaledInstance(18, 18, java.awt.Image.SCALE_SMOOTH)));
        } catch (Exception ex) {
            try {
                // Thử dùng icon accept nếu không có traphong.png
                java.awt.Image imgAccept = javax.imageio.ImageIO.read(new java.io.File("icon/accept.pn"));
                btnConfirm.setIcon(new ImageIcon(imgAccept.getScaledInstance(18, 18, java.awt.Image.SCALE_SMOOTH)));
            } catch (Exception ex2) {
                btnConfirm.setText("Xác nhận trả phòng"); // Fallback
            }
        }

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
        // Lấy hình thức thanh toán từ combobox
        String hinhThuc = (String) cboHinhThucThanhToan.getSelectedItem();
        if (hinhThuc == null)
            return;

        // ── NẾU CHUYỂN KHOẢN → hiện QR ──────────────────────────
        if ("Chuyển khoản".equals(hinhThuc)) {
            // Tính tổng tiền để truyền vào QR
            Phong phong = phongService.getPhongById(thuePhong.getSoPhong());
            double donGiaThang = thuePhong.getGiaThucTeChot() > 0 ? thuePhong.getGiaThucTeChot()
                    : (phong != null ? phong.getGiaTheoNgay() : 0.0);
            long soNgay = getSoNgay(); // ★ Dùng helper
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
            double tongGop = tienPhong + phuPhi + tienDV;
            double datCoc = thuePhong.getDatPhong() != null ? thuePhong.getDatPhong().getTienDatCoc() : 0;
            double conLai = Math.max(0, tongGop - datCoc);

            String maHD = "HD-" + new SimpleDateFormat("yyyyMMdd").format(new Date()) + "-" + thuePhong.getSoPhong();
            QRPaymentDialog qrDialog = new QRPaymentDialog(
                    (Frame) getOwner(), (long) conLai, maHD);
            qrDialog.setVisible(true);

            if (!qrDialog.isConfirmed())
                return; // người dùng bấm Huỷ
            // Nếu confirmed → tiếp tục checkout bình thường bên dưới
        }
        // ────────────────────────────────────────────────────────

        int ok = JOptionPane.showConfirmDialog(this,
                "Xác nhận trả phòng " + thuePhong.getSoPhong() + "?\nCòn phải trả: " + lblConLai.getText(),
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
        long soNgay = getSoNgay(); // ★ Dùng helper (min 1, có fallback)
        double tienPhong = donGiaThang * soNgay;

        // Tính phí
        double phuPhi = 0;
        if (thuePhong.getNgayNhanThucTe() != null && thuePhong.getNgayNhanThucTe().getHour() < 12)
            phuPhi += 50000;
        if (java.time.LocalDateTime.now().getHour() > 13)
            phuPhi += 50000;
        phuPhi += thuePhong.getPhuPhiPhatSinh();

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
        Date ngayNhanDisplay = getNgayNhanFallback(); // ★ Fallback
        panel.add(printRow("Phòng:", thuePhong.getSoPhong() + " – "
                + (thuePhong.getTenLoaiPhong() != null ? thuePhong.getTenLoaiPhong() : "")));
        panel.add(printRow("Khách hàng:", kh != null ? kh.getHoTen() : "—"));
        panel.add(printRow("SĐT:", kh != null && kh.getSoDienThoai() != null ? kh.getSoDienThoai() : "—"));
        panel.add(printRow("Nhận phòng:", sdf.format(ngayNhanDisplay)));
        panel.add(printRow("Trả phòng:", sdf.format(new Date())));
        panel.add(printRow("Số đêm:", soNgay + " đêm"));
        panel.add(Box.createVerticalStrut(8));
        panel.add(printSep());
        panel.add(Box.createVerticalStrut(8));

        // Chi tiết
        panel.add(printRow("Tiền phòng:", String.format("%,.0f đ", (double) tienPhong)));
        if (phuPhi > 0) {
            panel.add(printRow("Phí phát sinh:", String.format("%,.0f đ", (double) phuPhi)));
        }
        for (SuDungDichVu sddv : dsDichVu)
            panel.add(printRow(sddv.getTenDichVu() + "×" + sddv.getSoLuong() + ":",
                    String.format("%,.0f đ", (double) sddv.tinhThanhTien())));
        // Đặt cọc
        double datCoc = thuePhong.getDatPhong() != null ? thuePhong.getDatPhong().getTienDatCoc() : 0;
        double conLai = tienPhong + phuPhi + totalDV - datCoc;

        if (datCoc > 0) {
            panel.add(printRow("Đặt cọc:", String.format("- %,.0f đ", datCoc)));
        }
        panel.add(Box.createVerticalStrut(8));
        panel.add(printSep());
        panel.add(Box.createVerticalStrut(8));

        String totalText = conLai >= 0
                ? "CÒN PHẢI TRẢ:   " + String.format("%,.0f đ", conLai)
                : "HOÀN LẠI:   " + String.format("%,.0f đ", Math.abs(conLai));
        JLabel total = new JLabel(totalText);
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
