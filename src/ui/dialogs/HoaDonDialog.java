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
    private JTable billTable;
    private JLabel lblTongPhong, lblTongDV, lblGiamGia, lblTienCoc, lblTongCong;
    private JTextField txtTienKhachDua;
    private JLabel lblTienThua;
    private JComboBox<String> cboDV;
    private JSpinner spnSL;
    private java.util.List<DichVu> dvList = new ArrayList<>();
    private java.util.List<SuDungDichVu> dsDichVuHienTai = new ArrayList<>();
    private int dvStartRow = 0; // chỉ số hàng đầu tiên là dịch vụ trong bảng
    private int hoveredRow = -1;
    private JComboBox<String> cboHinhThuc;
    private JCheckBox chkDebt;

    private JPanel rEarly;
    private JLabel lblPhiPhatEarly;
    private JLabel lEarly;
    private double earlyCheckoutPenalty = 0;

    /**
     * Lấy ngày nhận phòng hiệu lực: ưu tiên ngayNhanThucTe,
     * fallback sang ngayNhanDuKien từ đặt phòng.
     */
    private java.time.LocalDateTime resolveNgayNhan() {
        if (thuePhong.getNgayNhanThucTe() != null) return thuePhongService.billingCheckIn(thuePhong);
        if (thuePhong.getNgayNhanDuKien() != null) return thuePhong.getNgayNhanDuKien();
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

    private double customDeductedDeposit = -1;

    public HoaDonDialog(Frame parent, ChiTietDatPhong tp) {
        super(parent, "Hóa đơn – Phòng " + tp.getSoPhongDisplay(), true);
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
        loadBillRows();
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
        JLabel t2 = new JLabel("P." + thuePhong.getSoPhongDisplay() + "  "
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

        String[] cols = { "Khoản mục", "Đơn giá", "Số lượng", "Thành tiền", "" };
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        billTable = new JTable(tableModel);
        billTable.setFont(UIConstants.FONT_BODY);
        billTable.setRowHeight(34);
        billTable.setShowGrid(false);
        billTable.setBackground(Color.WHITE);
        billTable.setSelectionBackground(UIConstants.PRIMARY_LIGHT);
        billTable.getTableHeader().setFont(UIConstants.FONT_SMALL_BOLD);
        billTable.getTableHeader().setBackground(UIConstants.BG_TABLE_HEADER);
        billTable.getTableHeader().setForeground(UIConstants.TEXT_SECONDARY);
        billTable.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.BORDER));

        // Ẩn header cột action (cột 4)
        billTable.getColumnModel().getColumn(4).setMaxWidth(0);
        billTable.getColumnModel().getColumn(4).setMinWidth(0);
        billTable.getColumnModel().getColumn(4).setWidth(0);
        billTable.getColumnModel().getColumn(4).setPreferredWidth(0);

        // Renderer chính — highlight hover + hiện nút khi là dòng dịch vụ
        billTable.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row,
                    int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                lbl.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                boolean isDvRow = isDichVuRow(row);
                boolean isHovered = (row == hoveredRow);

                if (isHovered && isDvRow) {
                    lbl.setBackground(new Color(0xEFF6FF)); // xanh nhạt khi hover
                } else if (sel) {
                    lbl.setBackground(UIConstants.PRIMARY_LIGHT);
                } else {
                    lbl.setBackground(Color.WHITE);
                }

                lbl.setFont(UIConstants.FONT_BODY);
                if (col == 3) {
                    lbl.setFont(UIConstants.FONT_BODY_BOLD);
                    lbl.setForeground(UIConstants.SUCCESS);
                } else {
                    lbl.setForeground(UIConstants.TEXT_PRIMARY);
                }
                return lbl;
            }
        });

        // Renderer riêng cho cột action — JLabel opaque để tránh bị "..."
        billTable.getColumnModel().getColumn(4).setCellRenderer(new javax.swing.table.TableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                JPanel panel = new JPanel(new FlowLayout(FlowLayout.CENTER, 4, 5));
                boolean dvRow = isDichVuRow(row);
                boolean hovered = (row == hoveredRow);
                panel.setBackground(dvRow && hovered ? new Color(0xEFF6FF) : Color.WHITE);

                if (dvRow && hovered) {
                    JLabel lblSua = new JLabel("S\u1eeda");
                    lblSua.setFont(new Font("Segoe UI", Font.BOLD, 11));
                    lblSua.setOpaque(true);
                    lblSua.setBackground(UIConstants.PRIMARY);
                    lblSua.setForeground(Color.WHITE);
                    lblSua.setBorder(BorderFactory.createEmptyBorder(3, 7, 3, 7));
                    lblSua.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                    JLabel lblXoa = new JLabel("X\u00f3a");
                    lblXoa.setFont(new Font("Segoe UI", Font.BOLD, 11));
                    lblXoa.setOpaque(true);
                    lblXoa.setBackground(UIConstants.DANGER);
                    lblXoa.setForeground(Color.WHITE);
                    lblXoa.setBorder(BorderFactory.createEmptyBorder(3, 7, 3, 7));
                    lblXoa.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

                    panel.add(lblSua);
                    panel.add(lblXoa);
                }
                return panel;
            }
        });

        // Mouse listener: hover tracking + click để sửa/xóa
        billTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                int row = billTable.rowAtPoint(e.getPoint());
                if (row != hoveredRow) {
                    hoveredRow = row;
                    billTable.repaint();
                    // Bật/tắt cột action
                    boolean show = isDichVuRow(row);
                    int w = show ? 90 : 0;
                    billTable.getColumnModel().getColumn(4).setMaxWidth(w);
                    billTable.getColumnModel().getColumn(4).setMinWidth(show ? 90 : 0);
                    billTable.getColumnModel().getColumn(4).setPreferredWidth(w);
                }
            }
        });
        billTable.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                hoveredRow = -1;
                billTable.repaint();
                billTable.getColumnModel().getColumn(4).setMaxWidth(0);
                billTable.getColumnModel().getColumn(4).setMinWidth(0);
                billTable.getColumnModel().getColumn(4).setPreferredWidth(0);
            }
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = billTable.rowAtPoint(e.getPoint());
                int col = billTable.columnAtPoint(e.getPoint());
                if (col == 4 && isDichVuRow(row)) {
                    int dvIdx = row - dvStartRow;
                    if (dvIdx < 0 || dvIdx >= dsDichVuHienTai.size()) return;
                    SuDungDichVu sddv = dsDichVuHienTai.get(dvIdx);
                    // Xác định xem click vào nút sửa hay xóa dựa vào vị trí X trong cell
                    java.awt.Rectangle cellRect = billTable.getCellRect(row, col, false);
                    int relX = e.getX() - cellRect.x;
                    if (relX > cellRect.width / 2) {
                        // Click nút xóa (bên phải)
                        onXoaDichVu(sddv);
                    } else {
                        // Click nút sửa (bên trái)
                        onSuaDichVu(sddv);
                    }
                }
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
        lblTienCoc = new JLabel("0 đ");
        lblTienCoc.setFont(UIConstants.FONT_BODY);
        lblTienCoc.setForeground(new java.awt.Color(0xF97316)); // cam — deposit highlight
        lblTongCong = new JLabel("0 đ");
        lblTongCong.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTongCong.setForeground(UIConstants.PRIMARY);

        loadBillRows();

        JScrollPane sp = new JScrollPane(billTable);
        sp.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER));
        sp.setPreferredSize(new Dimension(0, 150));
        sp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 150));

        JLabel hintLbl = new JLabel("(*) Rê chuột vào dịch vụ → hiện nút [Sửa] [Xóa]");
        hintLbl.setFont(new Font("Segoe UI", Font.ITALIC, 11));
        hintLbl.setForeground(new Color(0x94A3B8));
        hintLbl.setBorder(BorderFactory.createEmptyBorder(4, 2, 0, 0));

        JPanel tableWrap = new JPanel(new BorderLayout());
        tableWrap.setOpaque(false);
        tableWrap.add(sp, BorderLayout.CENTER);
        tableWrap.add(hintLbl, BorderLayout.SOUTH);

        panel.add(hdr, BorderLayout.NORTH);
        panel.add(tableWrap, BorderLayout.CENTER);
        return panel;
    }

    /** Kiểm tra row trong bảng có phải là dòng dịch vụ (có thể sửa/xóa) không */
    private boolean isDichVuRow(int row) {
        if (row < dvStartRow || dsDichVuHienTai.isEmpty()) return false;
        int dvIdx = row - dvStartRow;
        return dvIdx >= 0 && dvIdx < dsDichVuHienTai.size();
    }

    /** Hiện dialog sửa số lượng dịch vụ */
    private void onSuaDichVu(SuDungDichVu sddv) {
        String tenDV = sddv.getTenDichVu();
        JPanel pnl = new JPanel(new BorderLayout(0, 10));
        pnl.setBackground(Color.WHITE);
        pnl.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel lTitle = new JLabel("Sua dich vu: " + tenDV);
        lTitle.setFont(UIConstants.FONT_SMALL_BOLD);
        lTitle.setForeground(UIConstants.PRIMARY);

        JLabel lInfo = new JLabel(String.format("Đơn giá: %,.0f đ  |  SL hiện tại: %d", sddv.getDonGia(), sddv.getSoLuong()));
        lInfo.setFont(UIConstants.FONT_SMALL);
        lInfo.setForeground(UIConstants.TEXT_MUTED);

        JLabel lPrompt = new JLabel("Nhập số lượng mới:");
        lPrompt.setFont(UIConstants.FONT_BODY);

        JSpinner spn = new JSpinner(new SpinnerNumberModel(sddv.getSoLuong(), 1, 999, 1));
        spn.setFont(new Font("Segoe UI", Font.BOLD, 16));
        spn.setPreferredSize(new Dimension(120, 40));

        JPanel north = new JPanel();
        north.setOpaque(false);
        north.setLayout(new BoxLayout(north, BoxLayout.Y_AXIS));
        north.add(lTitle);
        north.add(Box.createVerticalStrut(4));
        north.add(lInfo);

        JPanel center = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        center.setOpaque(false);
        center.add(lPrompt);
        center.add(spn);

        pnl.add(north, BorderLayout.NORTH);
        pnl.add(center, BorderLayout.CENTER);

        Object[] opts = {"Lưu", "Huỷ"};
        int res = JOptionPane.showOptionDialog(this, pnl, "Sửa số lượng dịch vụ",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, opts, opts[0]);

        if (res == 0) {
            int newSl = (int) spn.getValue();
            String err = thuePhongService.suaSoLuongDichVu(sddv, newSl);
            if (err != null) {
                ui.components.NotificationManager.showError("Lỗi", err);
            } else {
                loadBillRows();
            }
        }
    }

    /** Xác nhận và xóa dịch vụ */
    private void onXoaDichVu(SuDungDichVu sddv) {
        String tenDV = sddv.getTenDichVu();
        int ok = JOptionPane.showConfirmDialog(this,
            "Xóa dịch vụ \"" + tenDV + "\" (" + sddv.getSoLuong() + "x) khỏi hóa đơn?",
            "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok == JOptionPane.YES_OPTION) {
            String err = thuePhongService.xoaDichVu(sddv);
            if (err != null) {
                ui.components.NotificationManager.showError("Lỗi", err);
            } else {
                loadBillRows();
            }
        }
    }

    private void loadBillRows() {
        tableModel.setRowCount(0);
        service.BangGiaService bangGiaService = new service.BangGiaService();
        Phong phong = phongService.getPhongById(thuePhong.getSoPhong());
        double policyPrice = bangGiaService.layGiaHienHanh(phong != null && phong.getLoaiPhong() != null ? phong.getLoaiPhong().getMaLoaiPhong() : "");
        double donGiaThang = thuePhong.getGiaThucTeChot() > 0 ? thuePhong.getGiaThucTeChot() : (policyPrice > 0 ? policyPrice : (phong != null ? phong.getGiaTheoNgay() : 0.0));

        long soNgay = tinhSoNgayHieuLuc();
        double tienPhong = thuePhongService.quotedRoomTotal(thuePhong.getMaChiTiet(), LocalDateTime.now());

        // Tính ngày trả dự kiến ban đầu
        long bookedNights = 1;
        if (thuePhong.getNgayNhanDuKien() != null && thuePhong.getNgayTraDuKien() != null) {
            bookedNights = Math.max(1, thuePhongService.tinhSoNgay(
                Date.from(thuePhong.getNgayNhanDuKien().atZone(java.time.ZoneId.systemDefault()).toInstant()),
                Date.from(thuePhong.getNgayTraDuKien().atZone(java.time.ZoneId.systemDefault()).toInstant())
            ));
        } else if (thuePhong.getDatPhong() != null && thuePhong.getDatPhong().getNgayNhanDuKien() != null && thuePhong.getDatPhong().getNgayTraDuKien() != null) {
            bookedNights = Math.max(1, thuePhongService.tinhSoNgay(
                Date.from(thuePhong.getDatPhong().getNgayNhanDuKien().atZone(java.time.ZoneId.systemDefault()).toInstant()),
                Date.from(thuePhong.getDatPhong().getNgayTraDuKien().atZone(java.time.ZoneId.systemDefault()).toInstant())
            ));
        }

        long actualNights = soNgay;
        if (actualNights < bookedNights) {
            if (rEarly != null) rEarly.setVisible(true);
            if (lEarly != null) lEarly.setText("Phí phạt trả phòng sớm (Lệch " + (bookedNights - actualNights) + " đêm)");
            if (lblPhiPhatEarly != null) lblPhiPhatEarly.setText(earlyCheckoutPenalty > 0 ? String.format("%,.0f đ", earlyCheckoutPenalty) : "0 đ");
        } else {
            if (rEarly != null) rEarly.setVisible(false);
            earlyCheckoutPenalty = 0;
        }

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
                String.format("%,.0f đ", tienPhong),
                ""
        });

        if (phiCheckinOut > 0) {
            tableModel.addRow(new Object[] { "Phụ thu (Nhận sớm / Trả trễ)", "—", "—", String.format("%,.0f đ", phiCheckinOut), "" });
        }
        if (phiKhac > 0) {
            tableModel.addRow(new Object[] { "Phí phát sinh khác", "—", "—", String.format("%,.0f đ", phiKhac), "" });
        }

        if (actualNights < bookedNights && earlyCheckoutPenalty > 0) {
            tableModel.addRow(new Object[] {
                "Phí phạt trả phòng sớm (" + (bookedNights - actualNights) + " đêm)",
                "—",
                "—",
                String.format("%,.0f đ", earlyCheckoutPenalty),
                ""
            });
        }

        // Lưu vị trí bắt đầu dòng dịch vụ + danh sách dịch vụ hiện tại
        dvStartRow = tableModel.getRowCount();
        dsDichVuHienTai = new java.util.ArrayList<>(dsDichVu);

        long totalDV = 0;
        for (SuDungDichVu sddv : dsDichVu) {
            tableModel.addRow(new Object[] {
                    sddv.getTenDichVu() != null ? sddv.getTenDichVu() : "Dịch vụ",
                    String.format("%,.0f đ", (double) sddv.getDonGia()),
                    sddv.getSoLuong() + "x",
                    String.format("%,.0f đ", (double) sddv.tinhThanhTien()),
                    ""
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
                // Tiêu chuẩn PMS: Chỉ áp dụng giảm giá trên tổng tiền phòng (tienPhong)
                tienGiam = km.tinhSoTienGiam(tienPhong);
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

        // ── Tiền cọc ──────────────────────────────────────────────────────
        double tienCoc = 0;
        if (thuePhong.getDatPhong() != null) {
            java.util.List<ChiTietDatPhong> realDs = thuePhongService.getChiTietByDatPhong(thuePhong.getDatPhong().getMaDatPhong());
            boolean isGroup = realDs != null && realDs.size() > 1;
            long unpaidCount = realDs != null ? realDs.stream().filter(r -> !r.isDaThanhToan()).count() : 0;
            double maxCoc = thuePhong.getDatPhong().getTienDatCoc();

            if (customDeductedDeposit >= 0) {
                tienCoc = Math.min(customDeductedDeposit, maxCoc);
            } else if (!isGroup || unpaidCount <= 1) {
                tienCoc = maxCoc;
            }
        }
        if (tienCoc > 0) {
            tableModel.addRow(new Object[] {
                    "─────────────────────────",
                    String.format("-%,.0f đ", tienCoc),
                    "1",
                    String.format("-%,.0f đ", tienCoc),
                    ""
            });
            // Override nội dung ô đầu tiên cho rõ nghĩa hơn bằng cách xoá & thêm lại
            int lastRow = tableModel.getRowCount() - 1;
            tableModel.setValueAt("Khấu trừ tiền cọc đã đặt", lastRow, 0);
            tableModel.setValueAt("-" + String.format("%,.0f đ", tienCoc), lastRow, 3);
            tableModel.setValueAt("—", lastRow, 1);
            tableModel.setValueAt("—", lastRow, 2);
        }
        if (lblTienCoc != null)
            lblTienCoc.setText(tienCoc > 0 ? String.format("-%,.0f đ", tienCoc) : "Không cọc");
        // ──────────────────────────────────────────────────────────────────

        if (lblTongCong != null) {
            double finalTotal = bdTienPhong.add(bdPhuPhi).add(bdTienDV).subtract(bdGiamGia)
                    .subtract(java.math.BigDecimal.valueOf(tienCoc)).add(java.math.BigDecimal.valueOf(earlyCheckoutPenalty)).doubleValue();
            if (finalTotal < 0) finalTotal = 0; // Không cho âm
            lblTongCong.setText(String.format("%,.0f đ", finalTotal));
            updateTienThua();
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
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));

        panel.add(Box.createVerticalStrut(10));
        panel.add(sumRow("Tiền phòng", lblTongPhong, false));
        panel.add(Box.createVerticalStrut(4));
        panel.add(sumRow("Tiền dịch vụ", lblTongDV, false));
        panel.add(Box.createVerticalStrut(4));
        panel.add(sumRow("Giảm giá", lblGiamGia, false));
        panel.add(Box.createVerticalStrut(4));

        // ── Phí phạt trả phòng sớm ──
        rEarly = new JPanel(new BorderLayout());
        rEarly.setOpaque(false);
        rEarly.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));

        lEarly = new JLabel("Phí phạt trả phòng sớm");
        lEarly.setFont(UIConstants.FONT_BODY);
        lEarly.setForeground(UIConstants.TEXT_SECONDARY);
        rEarly.add(lEarly, BorderLayout.WEST);

        JPanel rEarlyRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rEarlyRight.setOpaque(false);
        lblPhiPhatEarly = new JLabel("0 đ");
        lblPhiPhatEarly.setFont(UIConstants.FONT_BODY);
        lblPhiPhatEarly.setForeground(UIConstants.DANGER);
        rEarlyRight.add(lblPhiPhatEarly);

        RoundedButton btnEditEarly = new RoundedButton("Sửa", UIConstants.PRIMARY_LIGHT, UIConstants.PRIMARY);
        btnEditEarly.setFont(UIConstants.FONT_SMALL_BOLD);
        btnEditEarly.setPreferredSize(new Dimension(50, 24));
        btnEditEarly.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnEditEarly.addActionListener(e -> {
            long bookedN = 1;
            if (thuePhong.getNgayNhanDuKien() != null && thuePhong.getNgayTraDuKien() != null) {
                bookedN = Math.max(1, thuePhongService.tinhSoNgay(
                    Date.from(thuePhong.getNgayNhanDuKien().atZone(java.time.ZoneId.systemDefault()).toInstant()),
                    Date.from(thuePhong.getNgayTraDuKien().atZone(java.time.ZoneId.systemDefault()).toInstant())
                ));
            } else if (thuePhong.getDatPhong() != null && thuePhong.getDatPhong().getNgayNhanDuKien() != null && thuePhong.getDatPhong().getNgayTraDuKien() != null) {
                bookedN = Math.max(1, thuePhongService.tinhSoNgay(
                    Date.from(thuePhong.getDatPhong().getNgayNhanDuKien().atZone(java.time.ZoneId.systemDefault()).toInstant()),
                    Date.from(thuePhong.getDatPhong().getNgayTraDuKien().atZone(java.time.ZoneId.systemDefault()).toInstant())
                ));
            }
            long actualN = tinhSoNgayHieuLuc();

            service.BangGiaService bangGiaService = new service.BangGiaService();
            Phong phong = phongService.getPhongById(thuePhong.getSoPhong());
            double policyPrice = bangGiaService.layGiaHienHanh(phong != null && phong.getLoaiPhong() != null ? phong.getLoaiPhong().getMaLoaiPhong() : "");
            double donGiaRoom = thuePhong.getGiaThucTeChot() > 0 ? thuePhong.getGiaThucTeChot() : (policyPrice > 0 ? policyPrice : (phong != null ? phong.getGiaTheoNgay() : 0.0));

            Double val = promptEarlyPenaltyDeduction(bookedN - actualN, donGiaRoom);
            if (val != null) {
                earlyCheckoutPenalty = val;
                loadBillRows();
            }
        });
        rEarlyRight.add(btnEditEarly);
        rEarly.add(rEarlyRight, BorderLayout.EAST);
        rEarly.setVisible(false); // Ẩn mặc định
        panel.add(rEarly);
        panel.add(Box.createVerticalStrut(4));

        // ── Deposit Row with optional manual edit ──
        JPanel rCoc = new JPanel(new BorderLayout());
        rCoc.setOpaque(false);
        rCoc.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        JLabel lCoc = new JLabel("Tiền cọc đã đặt (khấu trừ)");
        lCoc.setFont(UIConstants.FONT_BODY);
        lCoc.setForeground(UIConstants.TEXT_SECONDARY);
        rCoc.add(lCoc, BorderLayout.WEST);

        JPanel rCocRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rCocRight.setOpaque(false);
        rCocRight.add(lblTienCoc);

        if (thuePhong.getDatPhong() != null) {
            double maxCoc = thuePhong.getDatPhong().getTienDatCoc();
            if (maxCoc > 0) {
                RoundedButton btnEditCoc = new RoundedButton("Sửa", UIConstants.PRIMARY_LIGHT, UIConstants.PRIMARY);
                btnEditCoc.setFont(UIConstants.FONT_SMALL_BOLD);
                btnEditCoc.setPreferredSize(new Dimension(50, 24));
                btnEditCoc.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                btnEditCoc.addActionListener(e -> {
                    Double val = promptDepositDeduction(maxCoc);
                    if (val != null) {
                        customDeductedDeposit = val;
                        loadBillRows();
                    }
                });
                rCocRight.add(btnEditCoc);
            }
        }
        rCoc.add(rCocRight, BorderLayout.EAST);
        panel.add(rCoc);
        // ──────────────────────────────────────────

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

        String[] options = { "Tiền mặt", "Chuyển khoản" };
        cboHinhThuc = new ModernComboBox<>(options);
        // ... (khúc combo box giữ nguyên)
        cboHinhThuc.setPreferredSize(new Dimension(140, 28));

        JPanel comboPan = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        comboPan.setOpaque(false);
        comboPan.add(cboHinhThuc);
        rowHT.add(comboPan, BorderLayout.EAST);

        panel.add(rowHT);

        // Thêm text field Tiền khách đưa và Tiền thừa
        JPanel rowTienKhachDua = new JPanel(new BorderLayout());
        rowTienKhachDua.setOpaque(false);
        rowTienKhachDua.setMaximumSize(new Dimension(Integer.MAX_VALUE, 45));
        JLabel lblTienKhachDua = new JLabel("Tiền khách đưa");
        lblTienKhachDua.setFont(UIConstants.FONT_BODY);
        lblTienKhachDua.setForeground(UIConstants.TEXT_SECONDARY);
        rowTienKhachDua.add(lblTienKhachDua, BorderLayout.WEST);

        txtTienKhachDua = new ModernTextField("");
        txtTienKhachDua.setPreferredSize(new Dimension(180, 38));
        txtTienKhachDua.setFont(new Font("Segoe UI", Font.BOLD, 15));
        txtTienKhachDua.setHorizontalAlignment(SwingConstants.RIGHT);
        txtTienKhachDua.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { updateTienThua(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { updateTienThua(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { updateTienThua(); }
        });

        JPanel rowKhachDuaRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rowKhachDuaRight.setOpaque(false);
        rowKhachDuaRight.add(txtTienKhachDua);
        rowTienKhachDua.add(rowKhachDuaRight, BorderLayout.EAST);

        panel.add(Box.createVerticalStrut(4));
        panel.add(rowTienKhachDua);

        JPanel rowTienThua = new JPanel(new BorderLayout());
        rowTienThua.setOpaque(false);
        rowTienThua.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
        JLabel lblTienThuaTitle = new JLabel("Tiền thối lại");
        lblTienThuaTitle.setFont(UIConstants.FONT_BODY);
        lblTienThuaTitle.setForeground(UIConstants.TEXT_SECONDARY);
        rowTienThua.add(lblTienThuaTitle, BorderLayout.WEST);

        lblTienThua = new JLabel("0 đ");
        lblTienThua.setFont(new Font("Segoe UI", Font.BOLD, 15));
        lblTienThua.setForeground(UIConstants.SUCCESS);

        JPanel rowTienThuaRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        rowTienThuaRight.setOpaque(false);
        rowTienThuaRight.add(lblTienThua);
        rowTienThua.add(rowTienThuaRight, BorderLayout.EAST);

        panel.add(Box.createVerticalStrut(4));
        panel.add(rowTienThua);

        cboHinhThuc.addActionListener(e -> {
            boolean isCash = "Tiền mặt".equals(cboHinhThuc.getSelectedItem());
            rowTienKhachDua.setVisible(isCash);
            rowTienThua.setVisible(isCash);
            if (!isCash) txtTienKhachDua.setText("");
            updateTienThua();
        });

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

    private void updateTienThua() {
        if (lblTongCong == null || txtTienKhachDua == null || lblTienThua == null) return;

        String tongText = lblTongCong.getText().replaceAll("[^\\d]", "");
        long tongCong = 0;
        try { tongCong = Long.parseLong(tongText); } catch (Exception ignored) {}

        String khachText = txtTienKhachDua.getText().replaceAll("[^\\d]", "");
        long khachDua = 0;
        try { khachDua = Long.parseLong(khachText); } catch (Exception ignored) {}

        long tienThua = khachDua - tongCong;
        if (khachDua == 0 || khachText.isEmpty()) {
            lblTienThua.setText("0 đ");
            lblTienThua.setForeground(UIConstants.TEXT_SECONDARY);
        } else if (tienThua < 0) {
            lblTienThua.setText("Thiếu " + String.format("%,d đ", -tienThua));
            lblTienThua.setForeground(UIConstants.DANGER);
        } else {
            lblTienThua.setText(String.format("%,d đ", tienThua));
            lblTienThua.setForeground(UIConstants.SUCCESS);
        }
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
        btnPrint.addActionListener(e -> exportToPDF());

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
    private Double promptDepositDeduction(double maxCoc) {
        JPanel pnl = new JPanel();
        pnl.setLayout(new BoxLayout(pnl, BoxLayout.Y_AXIS));
        pnl.setBackground(Color.WHITE);
        pnl.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel lblInfo = new JLabel("Tổng cọc của Đơn đặt là: " + String.format("%,.0f đ", maxCoc));
        lblInfo.setFont(UIConstants.FONT_BODY);
        lblInfo.setForeground(UIConstants.TEXT_MUTED);

        JLabel lblTitle = new JLabel("NHẬP SỐ TIỀN CẦN CẤN TRỪ VÀO PHÒNG NÀY:");
        lblTitle.setFont(UIConstants.FONT_SMALL_BOLD);
        lblTitle.setForeground(UIConstants.PRIMARY);

        ModernTextField txtInput = new ModernTextField("VD: " + String.format("%.0f", maxCoc));
        txtInput.setText(customDeductedDeposit >= 0 ? String.format("%.0f", customDeductedDeposit) : "0");
        txtInput.setFont(new Font("Segoe UI", Font.BOLD, 16));
        txtInput.setPreferredSize(new Dimension(280, 42));

        pnl.add(lblInfo);
        pnl.add(Box.createVerticalStrut(15));
        pnl.add(lblTitle);
        pnl.add(Box.createVerticalStrut(8));
        pnl.add(txtInput);

        // Try styling OptionPane background temporarily via UIManager but better just use plain JOptionPane for now, styled from inside.
        Object[] options = {"Đồng ý", "Huỷ"};
        int res = JOptionPane.showOptionDialog(this, pnl, "Khấu trừ Tiền Cọc",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

        if (res == JOptionPane.OK_OPTION) {
            try {
                double val = Double.parseDouble(txtInput.getText().replace(",", "").trim());
                if (val >= 0 && val <= maxCoc) {
                    return val;
                } else {
                    ui.components.NotificationManager.showError("Lỗi nhập liệu", "Số tiền phải từ 0 đến " + String.format("%,.0f", maxCoc));
                }
            } catch (Exception ex) {
                ui.components.NotificationManager.showError("Lỗi", "Vui lòng nhập định dạng số hợp lệ (VD: 300000).");
            }
        }
        return null;
    }

    private Double promptEarlyPenaltyDeduction(long diffNights, double donGiaRoom) {
        JPanel pnl = new JPanel();
        pnl.setLayout(new BoxLayout(pnl, BoxLayout.Y_AXIS));
        pnl.setBackground(Color.WHITE);
        pnl.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel lblInfo = new JLabel("Khách trả phòng sớm " + diffNights + " đêm so với đặt phòng ban đầu.");
        lblInfo.setFont(UIConstants.FONT_BODY);
        lblInfo.setForeground(UIConstants.TEXT_MUTED);

        double costConLai = donGiaRoom * diffNights;
        JLabel lblCostInfo = new JLabel(String.format("Đơn giá: %,.0f đ/đêm. Tiền phòng chưa ở: %,.0f đ", donGiaRoom, costConLai));
        lblCostInfo.setFont(UIConstants.FONT_SMALL_BOLD);
        lblCostInfo.setForeground(UIConstants.TEXT_SECONDARY);

        JLabel lblQuickTitle = new JLabel("GỢI Ý PHẠT NHANH:");
        lblQuickTitle.setFont(UIConstants.FONT_SMALL_BOLD);
        lblQuickTitle.setForeground(UIConstants.TEXT_MUTED);

        JPanel pnlQuick = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        pnlQuick.setOpaque(false);

        JButton btn30 = new JButton("30% (" + String.format("%,.0f", costConLai * 0.3) + "đ)");
        JButton btn50 = new JButton("50% (" + String.format("%,.0f", costConLai * 0.5) + "đ)");
        JButton btn70 = new JButton("70% (" + String.format("%,.0f", costConLai * 0.7) + "đ)");
        JButton btn100 = new JButton("100% (" + String.format("%,.0f", costConLai * 1.0) + "đ)");

        for (JButton btn : new JButton[]{btn30, btn50, btn70, btn100}) {
            btn.setFont(new Font("Segoe UI", Font.PLAIN, 12));
            btn.setBackground(Color.WHITE);
            btn.setForeground(UIConstants.PRIMARY);
            btn.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(226, 232, 240)),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)
            ));
            btn.setFocusPainted(false);
            btn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            pnlQuick.add(btn);
        }

        JLabel lblTitle = new JLabel("NHẬP SỐ TIỀN PHẠT TRẢ SỚM (VNĐ):");
        lblTitle.setFont(UIConstants.FONT_SMALL_BOLD);
        lblTitle.setForeground(UIConstants.PRIMARY);

        ModernTextField txtInput = new ModernTextField("0");
        txtInput.setText(earlyCheckoutPenalty > 0 ? String.format("%.0f", earlyCheckoutPenalty) : "0");
        txtInput.setFont(new Font("Segoe UI", Font.BOLD, 16));
        txtInput.setPreferredSize(new Dimension(280, 42));

        btn30.addActionListener(e -> txtInput.setText(String.format("%.0f", costConLai * 0.3)));
        btn50.addActionListener(e -> txtInput.setText(String.format("%.0f", costConLai * 0.5)));
        btn70.addActionListener(e -> txtInput.setText(String.format("%.0f", costConLai * 0.7)));
        btn100.addActionListener(e -> txtInput.setText(String.format("%.0f", costConLai * 1.0)));

        pnl.add(lblInfo);
        pnl.add(Box.createVerticalStrut(4));
        pnl.add(lblCostInfo);
        pnl.add(Box.createVerticalStrut(15));
        pnl.add(lblQuickTitle);
        pnl.add(Box.createVerticalStrut(6));
        pnl.add(pnlQuick);
        pnl.add(Box.createVerticalStrut(15));
        pnl.add(lblTitle);
        pnl.add(Box.createVerticalStrut(8));
        pnl.add(txtInput);

        Object[] options = {"Đồng ý", "Huỷ"};
        int res = JOptionPane.showOptionDialog(this, pnl, "Phí Phạt Trả Phòng Sớm",
            JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE, null, options, options[0]);

        if (res == JOptionPane.OK_OPTION) {
            try {
                double val = Double.parseDouble(txtInput.getText().replace(",", "").trim());
                if (val >= 0) {
                    return val;
                } else {
                    ui.components.NotificationManager.showError("Lỗi nhập liệu", "Số tiền phạt phải từ 0 đ trở lên.");
                }
            } catch (Exception ex) {
                ui.components.NotificationManager.showError("Lỗi", "Vui lòng nhập định dạng số hợp lệ (VD: 300000).");
            }
        }
        return null;
    }

    private void doCheckout() {
        // Lấy hình thức thanh toán từ ComboBox trên giao diện
        String hinhThuc = (String) cboHinhThuc.getSelectedItem();
        if (hinhThuc == null)
            return;

        if ("Tiền mặt".equals(hinhThuc) && !chkDebt.isSelected()) {
            String tongText = lblTongCong.getText().replaceAll("[^\\d]", "");
            long tongCong = 0;
            try { tongCong = Long.parseLong(tongText); } catch (Exception ignored) {}

            String khachText = txtTienKhachDua.getText().replaceAll("[^\\d]", "");
            long khachDua = 0;
            try { khachDua = Long.parseLong(khachText); } catch (Exception ignored) {}

            if (khachDua < tongCong) {
                ui.components.NotificationManager.showError("Lỗi thanh toán", "Tiền khách đưa chưa đủ để thanh toán!");
                return;
            }
        }

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
                            sharedRooms.add((c.getPhong() != null ? c.getPhong().getSoPhongDisplay() : "Unknown"));
                        }
                    }
                }
            }

            if (!sharedRooms.isEmpty()) {
                int warn = JOptionPane.showConfirmDialog(this,
                    "CẢNH BÁO NGHIỆP VỤ LƯU TRÚ:\n"
                    + "Phòng " + thuePhong.getSoPhongDisplay() + " đang trả phòng.\n"
                    + "Các phòng phụ thuộc: " + String.join(", ", sharedRooms) + " đang dùng CHUNG thông tin người đại diện này.\n\n"
                    + "Khi người đại diện rời đi, bạn NÊN bổ sung giấy tờ/CCCD cho người quản lý các phòng còn lại.\n"
                    + "Bạn có muốn tiếp tục thanh toán phòng " + thuePhong.getSoPhongDisplay() + " ngay lúc này?",
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
            double tienPhong = thuePhongService.quotedRoomTotal(thuePhong.getMaChiTiet(), LocalDateTime.now());

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
                if (km != null && km.kiemTraHopLe(subTotal, thoiDiemDat)) {
                    // Tiêu chuẩn PMS: Chỉ áp dụng giảm giá trên tổng tiền phòng (tienPhong)
                    tienGiam = km.tinhSoTienGiam(tienPhong);
                }
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
                "Xác nhận trả phòng " + thuePhong.getSoPhongDisplay() + "?\nTổng tiền: " + lblTongCong.getText(),
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
                currentVoucher,
                customDeductedDeposit,
                earlyCheckoutPenalty);

        if (err == null) {
            confirmed = true;
            ui.components.NotificationManager.showSuccess("Trả phòng thành công",
                "Phòng " + thuePhong.getSoPhongDisplay() + " đã được thanh toán.");

            // Thông báo làm mới toàn bộ hệ thống (Sơ đồ phòng, Thống kê, Lịch sử)
            if (getOwner() instanceof MainFrame) {
                ((MainFrame) getOwner()).notifyDataChanged();
            }

            dispose();
        } else {
            ui.components.NotificationManager.showError("Lỗi", err);
        }
    }

    // ---- PDF EXPORT (MISA STYLE) ----
    private void exportToPDF() {
        try {
            // 1. Prepare temporary HoaDon object
            HoaDon hd = new HoaDon();
            String maHD = "HD-" + new java.text.SimpleDateFormat("yyyyMMdd").format(new java.util.Date()) + "-" + thuePhong.getSoPhong();
            hd.setMaHoaDon(maHD);
            hd.setNgayLap(java.time.LocalDateTime.now());

            // Calculate totals
            long soNgay = tinhSoNgayHieuLuc();
            service.PhongService ps = new service.PhongService();
            Phong phong = ps.getPhongById(thuePhong.getSoPhong());
            service.BangGiaService bgs = new service.BangGiaService();
            double policyPrice = bgs.layGiaHienHanh(phong != null && phong.getLoaiPhong() != null ? phong.getLoaiPhong().getMaLoaiPhong() : "");
            double donGiaRoom = thuePhong.getGiaThucTeChot() > 0 ? thuePhong.getGiaThucTeChot() : (policyPrice > 0 ? policyPrice : (phong != null ? phong.getGiaTheoNgay() : 0.0));

            double tienPhong = donGiaRoom * soNgay;

            service.StandardRoomCalculator calc = new service.StandardRoomCalculator();
            java.time.LocalDateTime ngayNhanR = resolveNgayNhan();
            double phiCheckinOut = (ngayNhanR != null) ? calc.tinhPhuPhi(thuePhong, ngayNhanR, java.time.LocalDateTime.now()) : 0;

            java.util.List<SuDungDichVu> dsDichVu = thuePhongService.getDichVuByChiTiet(thuePhong.getMaChiTiet());
            double tienDV = dsDichVu.stream().mapToDouble(SuDungDichVu::tinhThanhTien).sum();

            double subTotal = tienPhong + phiCheckinOut + thuePhong.getPhuPhiPhatSinh() + tienDV;

            double tienGiam = 0;
            String vc = thuePhong.getDatPhong() != null ? thuePhong.getDatPhong().getMaKhuyenMai() : null;
            if (vc != null && !vc.isBlank()) {
                dao.KhuyenMaiDAO kmDAO = new dao.KhuyenMaiDAO();
                KhuyenMai km = kmDAO.getByVoucherCode(vc.trim());
                if (km != null && km.kiemTraHopLe(subTotal)) {
                    // Tiêu chuẩn PMS: Chỉ áp dụng giảm giá trên tổng tiền phòng (tienPhong)
                    tienGiam = km.tinhSoTienGiam(tienPhong);
                }
            }

            double dpCoc = 0;
            if (thuePhong.getDatPhong() != null) {
                java.util.List<ChiTietDatPhong> realDs = thuePhongService.getChiTietByDatPhong(thuePhong.getDatPhong().getMaDatPhong());
                boolean isGroup = realDs != null && realDs.size() > 1;
                long unpaidCount = realDs != null ? realDs.stream().filter(r -> !r.isDaThanhToan()).count() : 0;
                double maxCoc = thuePhong.getDatPhong().getTienDatCoc();
                if (customDeductedDeposit >= 0) dpCoc = Math.min(customDeductedDeposit, maxCoc);
                else if (!isGroup || unpaidCount <= 1) dpCoc = maxCoc;
            }

            hd.setTongTienPhong(tienPhong + phiCheckinOut + thuePhong.getPhuPhiPhatSinh());
            hd.setTongTienDichVu(tienDV);
            hd.setTienGiamKhuyenMai(tienGiam);
            hd.setTongThanhToan(Math.max(0, subTotal - tienGiam - dpCoc));
            hd.setPhuongThucThanhToan((String) cboHinhThuc.getSelectedItem());
            hd.setDatPhong(thuePhong.getDatPhong());
            hd.setKhachHang(thuePhong.resolveKhachHang());

            // 2. Prepare detail list
            java.util.List<ChiTietHoaDon> details = new java.util.ArrayList<>();

            // Room
            ChiTietHoaDon ctRoom = new ChiTietHoaDon();
            ctRoom.setNoiDung("Tiền phòng (" + soNgay + " đêm) - " + thuePhong.getSoPhong());
            ctRoom.setDonViTinh("Đêm");
            ctRoom.setSoLuong((int)soNgay);
            ctRoom.setDonGia(donGiaRoom);
            ctRoom.setThanhTien(tienPhong);
            details.add(ctRoom);

            // Surcharges
            if (phiCheckinOut > 0) {
                ChiTietHoaDon ctSur = new ChiTietHoaDon();
                ctSur.setNoiDung("Phụ thu (Nhận sớm / Trả trễ)");
                ctSur.setDonViTinh("Lần");
                ctSur.setSoLuong(1);
                ctSur.setDonGia(phiCheckinOut);
                ctSur.setThanhTien(phiCheckinOut);
                details.add(ctSur);
            }
            if (thuePhong.getPhuPhiPhatSinh() > 0) {
                ChiTietHoaDon ctOther = new ChiTietHoaDon();
                ctOther.setNoiDung("Phí phát sinh khác");
                ctOther.setDonViTinh("Lần");
                ctOther.setSoLuong(1);
                ctOther.setDonGia(thuePhong.getPhuPhiPhatSinh());
                ctOther.setThanhTien(thuePhong.getPhuPhiPhatSinh());
                details.add(ctOther);
            }

            // Services
            for (SuDungDichVu sddv : dsDichVu) {
                ChiTietHoaDon ct = new ChiTietHoaDon();
                ct.setNoiDung(sddv.getTenDichVu());
                ct.setDonViTinh("Lần");
                ct.setSoLuong(sddv.getSoLuong());
                ct.setDonGia(sddv.getDonGia());
                ct.setThanhTien((double)sddv.tinhThanhTien());
                details.add(ct);
            }

            // Deposit (as a negative row if needed, but MISA layout doesn't explicitly have it in table,
            // we'll add it as a "Service" with negative value to show in the list)
            if (dpCoc > 0) {
                ChiTietHoaDon ctCoc = new ChiTietHoaDon();
                ctCoc.setNoiDung("Khấu trừ tiền cọc");
                ctCoc.setDonViTinh("Lần");
                ctCoc.setSoLuong(1);
                ctCoc.setDonGia(-dpCoc);
                ctCoc.setThanhTien(-dpCoc);
                details.add(ctCoc);
            }

            // 3. Export
            util.PDFExporter.exportHoaDon(this, hd, details);

        } catch (Exception ex) {
            ex.printStackTrace();
            ui.components.NotificationManager.showError("Lỗi xuất PDF", ex.getMessage());
        }
    }


    public boolean isConfirmed() {
        return confirmed;
    }
}
