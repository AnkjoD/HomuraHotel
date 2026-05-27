package ui.dialogs;

import service.AuthService;
import service.ThuePhongService;
import service.PhongService;
import service.KhachHangService;
import entity.*;
import ui.components.UIConstants;
import ui.components.RoundedComponents.*;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.awt.print.*;
import java.text.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;

/**
 * Dialog thanh toán tổng hợp đoàn (Master Bill / Gom Bill).
 * Hiển thị chi tiết tất cả phòng + dịch vụ + phụ phí trong 1 hóa đơn duy nhất.
 */
public class MasterBillDialog extends JDialog {

    private final ThuePhongService thuePhongService = new ThuePhongService();
    private final PhongService phongService = new PhongService();
    private final KhachHangService khService = new KhachHangService();

    private final DatPhong datPhong;
    private final List<ChiTietDatPhong> allRooms;
    private boolean confirmed = false;
    private final Map<String, Double> roomPenalties = new HashMap<>();
    private final Map<Integer, SuDungDichVu> rowServices = new HashMap<>();
    private int hoveredRow = -1;

    private DefaultTableModel tableModel;
    private JLabel lblTongPhong, lblTongDV, lblTongPhuPhi, lblGiamGia, lblTienCoc, lblTongCong;
    private JTextField txtTienKhachDua;
    private JLabel lblTienThua;
    private JComboBox<String> cboHinhThuc;
    private JComboBox<String> cboPhong;
    private JComboBox<String> cboDV;
    private JSpinner spnSL;
    private java.util.List<DichVu> dvList = new ArrayList<>();

    public MasterBillDialog(Frame parent, DatPhong dp, List<ChiTietDatPhong> rooms) {
        super(parent, "Gom Bill Đoàn — " + (dp.getTenDoan() != null ? dp.getTenDoan() : dp.getMaDatPhong()), true);
        this.datPhong = dp;
        
        // Lọc bỏ những phòng đã thanh toán riêng lẻ trước đó
        this.allRooms = new ArrayList<>();
        if (rooms != null) {
            for (ChiTietDatPhong ct : rooms) {
                if (!ct.isDaThanhToan()) {
                    this.allRooms.add(ct);
                }
            }
        }
        
        setSize(780, 780);
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
        banner.setBackground(UIConstants.PRIMARY); // Purple for group
        banner.setBorder(BorderFactory.createEmptyBorder(18, 28, 18, 28));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        JLabel t1 = new JLabel("HÓA ĐƠN ĐOÀN (MASTER BILL)");
        t1.setFont(new Font("Segoe UI", Font.BOLD, 20));
        t1.setForeground(Color.WHITE);

        String tenDoan = datPhong.getTenDoan() != null ? datPhong.getTenDoan() : "Khách đoàn";
        JLabel t2 = new JLabel(tenDoan + "  " + allRooms.size() + " phòng");
        t2.setFont(UIConstants.FONT_BODY);
        t2.setForeground(new Color(255, 255, 255, 200));
        left.add(t1);
        left.add(Box.createVerticalStrut(2));
        left.add(t2);

        JPanel right = new JPanel();
        right.setOpaque(false);
        right.setLayout(new BoxLayout(right, BoxLayout.Y_AXIS));
        JLabel lma = new JLabel("Mã ĐP: " + datPhong.getMaDatPhong());
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

        body.add(buildGroupInfoSection());
        body.add(Box.createVerticalStrut(18));
        body.add(buildBillSection());
        body.add(Box.createVerticalStrut(14));
        body.add(buildAddDVSection());
        body.add(Box.createVerticalStrut(16));
        body.add(buildSummarySection());
        return body;
    }

    private JPanel buildAddDVSection() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));

        JLabel hdr = new JLabel("THÊM DỊCH VỤ VÀO PHÒNG TRONG ĐOÀN");
        hdr.setFont(UIConstants.FONT_SMALL_BOLD);
        hdr.setForeground(UIConstants.TEXT_MUTED);
        hdr.setBorder(BorderFactory.createEmptyBorder(0, 0, 6, 0));

        // Combo Box chọn phòng
        cboPhong = new JComboBox<>();
        cboPhong.setFont(UIConstants.FONT_BODY);
        cboPhong.setBackground(Color.WHITE);
        for (ChiTietDatPhong ct : allRooms) {
            String roomId = ct.getPhong() != null ? ct.getPhong().getMaPhong() : "?";
            cboPhong.addItem("Phòng " + roomId);
        }
        cboPhong.setPreferredSize(new Dimension(150, 32));

        // Combo Box chọn dịch vụ
        dvList = new service.DichVuService().getActive();
        String[] names = dvList.stream().map(DichVu::getTenDV).toArray(String[]::new);
        cboDV = new JComboBox<>(names.length > 0 ? names : new String[] { "Không có dịch vụ" });
        cboDV.setFont(UIConstants.FONT_BODY);
        cboDV.setBackground(Color.WHITE);

        spnSL = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
        spnSL.setFont(UIConstants.FONT_BODY);
        spnSL.setPreferredSize(new Dimension(65, 32));

        JButton btnAdd = new JButton("+ Thêm dịch vụ");
        btnAdd.setFont(UIConstants.FONT_SMALL_BOLD);
        btnAdd.setBackground(UIConstants.SUCCESS);
        btnAdd.setForeground(Color.WHITE);
        btnAdd.setBorderPainted(false);
        btnAdd.setFocusPainted(false);
        btnAdd.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnAdd.addActionListener(e -> {
            int roomIdx = cboPhong.getSelectedIndex();
            int dvIdx = cboDV.getSelectedIndex();
            if (roomIdx < 0 || dvIdx < 0 || dvList.isEmpty() || allRooms.isEmpty())
                return;
            ChiTietDatPhong ct = allRooms.get(roomIdx);
            DichVu dv = dvList.get(dvIdx);
            int sl = (int) spnSL.getValue();
            String err = thuePhongService.themDichVu(ct.getMaChiTiet(), dv.getMaDV(), sl, dv.getGia());
            if (err != null) {
                ui.components.NotificationManager.showError("Lỗi", "Không thể thêm dịch vụ: " + err);
                return;
            }
            loadBillRows();
        });

        JPanel row = new JPanel(new GridBagLayout());
        row.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(0, 0, 0, 8);

        gbc.weightx = 0.3;
        gbc.gridx = 0;
        row.add(cboPhong, gbc);

        gbc.weightx = 0.5;
        gbc.gridx = 1;
        row.add(cboDV, gbc);

        gbc.weightx = 0.0;
        gbc.gridx = 2;
        JLabel slLbl = new JLabel("SL:");
        slLbl.setFont(UIConstants.FONT_BODY);
        row.add(slLbl, gbc);

        gbc.gridx = 3;
        row.add(spnSL, gbc);

        gbc.insets = new Insets(0, 0, 0, 0);
        gbc.gridx = 4;
        row.add(btnAdd, gbc);

        panel.add(hdr, BorderLayout.NORTH);
        panel.add(row, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildGroupInfoSection() {
        JPanel panel = new JPanel(new GridLayout(1, 2, 24, 0));
        panel.setOpaque(false);

        KhachHang kh = datPhong.getKhachHang();
        if (kh != null && (kh.getHoTen() == null || kh.getHoTen().isBlank()) && kh.getMaKhachHang() != null) {
            kh = khService.getById(kh.getMaKhachHang());
        }
        String ten = kh != null && kh.getHoTen() != null ? kh.getHoTen() : "—";
        String sdt = kh != null && kh.getSoDienThoai() != null ? kh.getSoDienThoai() : "—";
        String cccd = kh != null && kh.getCccd() != null ? kh.getCccd() : "—";

        panel.add(infoBlock("THÔNG TIN ĐOÀN", new String[][]{
                {"Người đại diện:", ten},
                {"SĐT:", sdt},
                {"CCCD:", cccd},
                {"Tên đoàn:", datPhong.getTenDoan() != null ? datPhong.getTenDoan() : "—"},
                {"Loại khách:", datPhong.getLoaiKhachLabel()}
        }));

        // Liệt kê danh sách phòng
        StringBuilder roomList = new StringBuilder();
        for (ChiTietDatPhong ct : allRooms) {
            if (roomList.length() > 0) roomList.append(", ");
            roomList.append(ct.getPhong() != null ? ct.getPhong().getMaPhong() : "?");
        }

        DateTimeFormatter sdf = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        String ngayNhan = datPhong.getNgayNhanDuKien() != null ? datPhong.getNgayNhanDuKien().format(sdf) : "—";
        String ngayTra = new SimpleDateFormat("dd/MM/yyyy HH:mm").format(new Date());

        panel.add(infoBlock("CHI TIẾT LƯU TRÚ", new String[][]{
                {"Danh sách phòng:", roomList.toString()},
                {"Số phòng:", allRooms.size() + " phòng"},
                {"Nhận phòng:", ngayNhan},
                {"Thanh toán:", ngayTra}
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
            k.setPreferredSize(new Dimension(100, 18));
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
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 300));

        JLabel hdr = new JLabel("BẢNG CHI TIẾT THANH TOÁN ĐOÀN");
        hdr.setFont(UIConstants.FONT_SMALL_BOLD);
        hdr.setForeground(UIConstants.TEXT_MUTED);
        hdr.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));

        String[] cols = {"Phòng", "Khoản mục", "Đơn giá", "Số lượng", "Thành tiền", "Hành động"};
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        JTable table = new JTable(tableModel);
        table.setFont(UIConstants.FONT_BODY);
        table.setRowHeight(30);
        table.setShowGrid(false);
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(UIConstants.PRIMARY_LIGHT);
        table.getTableHeader().setFont(UIConstants.FONT_SMALL_BOLD);
        table.getTableHeader().setBackground(UIConstants.BG_TABLE_HEADER);
        table.getTableHeader().setForeground(UIConstants.TEXT_SECONDARY);
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.BORDER));

        // Custom renderer
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                lbl.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                
                boolean dvRow = isDichVuRow(row);
                boolean hovered = (row == hoveredRow);
                if (dvRow && hovered) {
                    lbl.setBackground(new Color(0xEFF6FF));
                } else if (sel) {
                    lbl.setBackground(UIConstants.PRIMARY_LIGHT);
                } else {
                    lbl.setBackground(Color.WHITE);
                }

                lbl.setFont(UIConstants.FONT_BODY);
                if (col == 4) {
                    lbl.setFont(UIConstants.FONT_BODY_BOLD);
                    lbl.setForeground(UIConstants.SUCCESS);
                } else if (col == 0) {
                    lbl.setFont(UIConstants.FONT_SMALL_BOLD);
                    lbl.setForeground(UIConstants.PRIMARY);
                } else {
                    lbl.setForeground(UIConstants.TEXT_PRIMARY);
                }
                return lbl;
            }
        });

        // Renderer riêng cho cột action — JLabel opaque để tránh bị "..."
        table.getColumnModel().getColumn(5).setCellRenderer(new javax.swing.table.TableCellRenderer() {
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
        table.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            @Override
            public void mouseMoved(java.awt.event.MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                if (row != hoveredRow) {
                    hoveredRow = row;
                    table.repaint();
                    // Bật/tắt cột action
                    boolean show = isDichVuRow(row);
                    int w = show ? 95 : 0;
                    table.getColumnModel().getColumn(5).setMaxWidth(w);
                    table.getColumnModel().getColumn(5).setMinWidth(show ? 95 : 0);
                    table.getColumnModel().getColumn(5).setPreferredWidth(w);
                }
            }
        });
        table.addMouseListener(new java.awt.event.MouseAdapter() {
            @Override
            public void mouseExited(java.awt.event.MouseEvent e) {
                hoveredRow = -1;
                table.repaint();
                table.getColumnModel().getColumn(5).setMaxWidth(0);
                table.getColumnModel().getColumn(5).setMinWidth(0);
                table.getColumnModel().getColumn(5).setPreferredWidth(0);
            }
            @Override
            public void mouseClicked(java.awt.event.MouseEvent e) {
                int row = table.rowAtPoint(e.getPoint());
                int col = table.columnAtPoint(e.getPoint());
                if (col == 5 && isDichVuRow(row)) {
                    SuDungDichVu sddv = rowServices.get(row);
                    if (sddv == null) return;
                    java.awt.Rectangle cellRect = table.getCellRect(row, col, false);
                    int relX = e.getX() - cellRect.x;
                    if (relX > cellRect.width / 2) {
                        onXoaDichVu(sddv);
                    } else {
                        onSuaDichVu(sddv);
                    }
                }
            }
        });

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(60);
        table.getColumnModel().getColumn(1).setPreferredWidth(210);
        table.getColumnModel().getColumn(2).setPreferredWidth(90);
        table.getColumnModel().getColumn(3).setPreferredWidth(60);
        table.getColumnModel().getColumn(4).setPreferredWidth(110);
        table.getColumnModel().getColumn(5).setPreferredWidth(0);
        table.getColumnModel().getColumn(5).setMinWidth(0);
        table.getColumnModel().getColumn(5).setMaxWidth(0);

        // Init summary labels BEFORE loadBillRows
        lblTongPhong = new JLabel("0 đ");
        lblTongPhong.setFont(UIConstants.FONT_BODY);
        lblTongDV = new JLabel("0 đ");
        lblTongDV.setFont(UIConstants.FONT_BODY);
        lblTongPhuPhi = new JLabel("0 đ");
        lblTongPhuPhi.setFont(UIConstants.FONT_BODY);
        lblGiamGia = new JLabel("Không có");
        lblGiamGia.setFont(UIConstants.FONT_BODY);
        lblGiamGia.setForeground(new java.awt.Color(0x16A34A)); // xanh lá — giảm giá
        lblTienCoc = new JLabel("Không cọc");
        lblTienCoc.setFont(UIConstants.FONT_BODY);
        lblTienCoc.setForeground(new java.awt.Color(0xF97316)); // cam
        lblTongCong = new JLabel("0 đ");
        lblTongCong.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTongCong.setForeground(UIConstants.PRIMARY);

        loadBillRows();

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER));
        sp.setPreferredSize(new Dimension(0, 220));
        sp.setMaximumSize(new Dimension(Integer.MAX_VALUE, 220));

        JLabel hintLbl = new JLabel("(*) Rê chuột vào dòng dịch vụ → hiện nút [Sửa] [Xóa]");
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

    private void loadBillRows() {
        tableModel.setRowCount(0);
        if (rowServices != null) {
            rowServices.clear();
        }
        double totalPhong = 0, totalDV = 0, totalPhuPhi = 0;

        for (ChiTietDatPhong ct : allRooms) {
            String roomId = ct.getPhong() != null ? ct.getPhong().getMaPhong() : "?";
            String loaiPhong = ct.getTenLoaiPhong() != null ? ct.getTenLoaiPhong() : "";

            // Tính tiền phòng
            LocalDateTime ngayNhanResolved = ct.getNgayNhanThucTe();
            if (ngayNhanResolved == null) {
                ngayNhanResolved = ct.getNgayNhanDuKien() != null ? ct.getNgayNhanDuKien() : datPhong.getNgayNhanDuKien();
            }
            LocalDateTime ngayTraResolved = ct.getNgayTraThucTe() != null ? ct.getNgayTraThucTe() : LocalDateTime.now();
            long soNgay = Math.max(1, thuePhongService.tinhSoNgay(
                    ngayNhanResolved != null ? java.util.Date.from(ngayNhanResolved.atZone(java.time.ZoneId.systemDefault()).toInstant()) : new Date(),
                    java.util.Date.from(ngayTraResolved.atZone(java.time.ZoneId.systemDefault()).toInstant())
            ));

            // Tính số đêm dự kiến ban đầu để check trả sớm
            long bookedNights = 1;
            if (ct.getNgayNhanDuKien() != null && ct.getNgayTraDuKien() != null) {
                bookedNights = Math.max(1, thuePhongService.tinhSoNgay(
                    Date.from(ct.getNgayNhanDuKien().atZone(java.time.ZoneId.systemDefault()).toInstant()),
                    Date.from(ct.getNgayTraDuKien().atZone(java.time.ZoneId.systemDefault()).toInstant())
                ));
            } else if (datPhong.getNgayNhanDuKien() != null && datPhong.getNgayTraDuKien() != null) {
                bookedNights = Math.max(1, thuePhongService.tinhSoNgay(
                    Date.from(datPhong.getNgayNhanDuKien().atZone(java.time.ZoneId.systemDefault()).toInstant()),
                    Date.from(datPhong.getNgayTraDuKien().atZone(java.time.ZoneId.systemDefault()).toInstant())
                ));
            }
            long actualNights = soNgay;
            service.BangGiaService bangGiaService = new service.BangGiaService();
            double policyPrice = bangGiaService.layGiaHienHanh(ct.getPhong() != null && ct.getPhong().getLoaiPhong() != null ? ct.getPhong().getLoaiPhong().getMaLoaiPhong() : "");
            
            double donGia = policyPrice > 0 ? policyPrice : (ct.getGiaThucTeChot() > 0 ? ct.getGiaThucTeChot() : 400000);
            double tienPhong = donGia * soNgay;
            totalPhong += tienPhong;

            tableModel.addRow(new Object[]{
                    roomId,
                    "Tiền phòng – " + loaiPhong,
                    String.format("%,.0f đ/đêm", donGia),
                    soNgay + " đêm",
                    String.format("%,.0f đ", tienPhong),
                    ""
            });

            // Tính phụ phí bằng StandardRoomCalculator
            service.StandardRoomCalculator calc = new service.StandardRoomCalculator();
            double phiCheckinOut = (ngayNhanResolved != null) ? calc.tinhPhuPhi(ct, ngayNhanResolved, ngayTraResolved) : 0;
            double phuPhi = phiCheckinOut + ct.getPhuPhiPhatSinh();
            if (phuPhi > 0) {
                totalPhuPhi += phuPhi;
                tableModel.addRow(new Object[]{
                        roomId, "Phụ phí", "—", "—", String.format("%,.0f đ", phuPhi), ""
                });
            }

            double earlyPenalty = roomPenalties.getOrDefault(ct.getMaChiTiet(), 0.0);
            if (actualNights < bookedNights && earlyPenalty > 0) {
                totalPhuPhi += earlyPenalty;
                tableModel.addRow(new Object[]{
                        roomId,
                        "Phạt trả phòng sớm (" + (bookedNights - actualNights) + " đêm)",
                        "—",
                        "—",
                        String.format("%,.0f đ", earlyPenalty),
                        ""
                });
            }

            // Dịch vụ phòng
            List<SuDungDichVu> dsDichVu = thuePhongService.getDichVuByChiTiet(ct.getMaChiTiet());
            for (SuDungDichVu sddv : dsDichVu) {
                double tienDV = sddv.tinhThanhTien();
                totalDV += tienDV;
                tableModel.addRow(new Object[]{
                        roomId,
                        sddv.getTenDichVu() != null ? sddv.getTenDichVu() : "Dịch vụ",
                        String.format("%,.0f đ", sddv.getDonGia()),
                        sddv.getSoLuong() + "x",
                        String.format("%,.0f đ", tienDV),
                        ""
                });
                rowServices.put(tableModel.getRowCount() - 1, sddv);
            }
        }

        java.math.BigDecimal bdTotalPhong = java.math.BigDecimal.valueOf(totalPhong);
        java.math.BigDecimal bdTotalDV = java.math.BigDecimal.valueOf(totalDV);
        java.math.BigDecimal bdTotalPhuPhi = java.math.BigDecimal.valueOf(totalPhuPhi);

        // ── Tính giảm giá voucher/khuyến mãi ──
        double tienGiam = calculateDiscount(totalPhong);
        java.math.BigDecimal bdGiamGia = java.math.BigDecimal.valueOf(tienGiam);

        // Tiền cọc — hiển thị và trừ khỏi tổng
        double tienCoc = datPhong.getTienDatCoc();

        if (lblTongPhong  != null) lblTongPhong.setText(String.format("%,.0f đ", bdTotalPhong.doubleValue()));
        if (lblTongDV     != null) lblTongDV.setText(String.format("%,.0f đ", bdTotalDV.doubleValue()));
        if (lblTongPhuPhi != null) lblTongPhuPhi.setText(String.format("%,.0f đ", bdTotalPhuPhi.doubleValue()));
        if (lblGiamGia    != null) lblGiamGia.setText(tienGiam > 0
                ? String.format("-%,.0f đ  (Voucher: %s)", tienGiam, datPhong.getMaKhuyenMai())
                : "Không có");
        if (lblTienCoc    != null) lblTienCoc.setText(tienCoc > 0 ? String.format("-%,.0f đ", tienCoc) : "Không cọc");
        if (lblTongCong   != null) {
            double tongSauCoc = Math.max(0,
                    bdTotalPhong.add(bdTotalDV).add(bdTotalPhuPhi)
                    .subtract(bdGiamGia)
                    .subtract(java.math.BigDecimal.valueOf(tienCoc)).doubleValue());
            lblTongCong.setText(String.format("%,.0f đ", tongSauCoc));
            updateTienThua();
        }
    }

    private JPanel buildSummarySection() {
        JPanel panel = new JPanel();
        panel.setOpaque(false);
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));
        panel.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER));
        panel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 350));

        panel.add(Box.createVerticalStrut(10));
        panel.add(sumRow("Tổng tiền phòng (" + allRooms.size() + " phòng)", lblTongPhong, false));
        panel.add(Box.createVerticalStrut(4));
        panel.add(sumRow("Tổng phụ phí", lblTongPhuPhi, false));
        panel.add(Box.createVerticalStrut(4));
        panel.add(sumRow("Tổng dịch vụ", lblTongDV, false));
        panel.add(Box.createVerticalStrut(4));
        panel.add(sumRow("Giảm giá (voucher)", lblGiamGia, false));
        panel.add(Box.createVerticalStrut(4));

        // ── Thêm mục Phạt trả phòng sớm cho từng phòng (nếu có) ──
        for (ChiTietDatPhong ct : allRooms) {
            LocalDateTime ngayNhan = ct.getNgayNhanThucTe() != null ? ct.getNgayNhanThucTe() : (ct.getNgayNhanDuKien() != null ? ct.getNgayNhanDuKien() : datPhong.getNgayNhanDuKien());
            LocalDateTime ngayTra = ct.getNgayTraThucTe() != null ? ct.getNgayTraThucTe() : LocalDateTime.now();
            long actN = Math.max(1, thuePhongService.tinhSoNgay(
                ngayNhan != null ? java.util.Date.from(ngayNhan.atZone(java.time.ZoneId.systemDefault()).toInstant()) : new Date(),
                java.util.Date.from(ngayTra.atZone(java.time.ZoneId.systemDefault()).toInstant())
            ));
            
            long bookedN = 1;
            if (ct.getNgayNhanDuKien() != null && ct.getNgayTraDuKien() != null) {
                bookedN = Math.max(1, thuePhongService.tinhSoNgay(
                    Date.from(ct.getNgayNhanDuKien().atZone(java.time.ZoneId.systemDefault()).toInstant()),
                    Date.from(ct.getNgayTraDuKien().atZone(java.time.ZoneId.systemDefault()).toInstant())
                ));
            } else if (datPhong.getNgayNhanDuKien() != null && datPhong.getNgayTraDuKien() != null) {
                bookedN = Math.max(1, thuePhongService.tinhSoNgay(
                    Date.from(datPhong.getNgayNhanDuKien().atZone(java.time.ZoneId.systemDefault()).toInstant()),
                    Date.from(datPhong.getNgayTraDuKien().atZone(java.time.ZoneId.systemDefault()).toInstant())
                ));
            }
            
            if (actN < bookedN) {
                String rId = ct.getPhong() != null ? ct.getPhong().getSoPhongDisplay() : "?";
                long diff = bookedN - actN;
                
                service.BangGiaService bangGiaService = new service.BangGiaService();
                double policyPrice = bangGiaService.layGiaHienHanh(ct.getPhong() != null && ct.getPhong().getLoaiPhong() != null ? ct.getPhong().getLoaiPhong().getMaLoaiPhong() : "");
                double donGia = policyPrice > 0 ? policyPrice : (ct.getGiaThucTeChot() > 0 ? ct.getGiaThucTeChot() : 400000);
                
                JPanel rEarly = new JPanel(new BorderLayout());
                rEarly.setOpaque(false);
                rEarly.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
                
                JLabel lEarly = new JLabel("Phạt trả sớm P." + rId + " (Lệch " + diff + " đêm)");
                lEarly.setFont(UIConstants.FONT_BODY);
                lEarly.setForeground(UIConstants.TEXT_SECONDARY);
                rEarly.add(lEarly, BorderLayout.WEST);
                
                JPanel rEarlyRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
                rEarlyRight.setOpaque(false);
                
                JLabel lblPhat = new JLabel("0 đ");
                lblPhat.setFont(UIConstants.FONT_BODY);
                lblPhat.setForeground(UIConstants.DANGER);
                double initialPhat = roomPenalties.getOrDefault(ct.getMaChiTiet(), 0.0);
                lblPhat.setText(initialPhat > 0 ? String.format("%,.0f đ", initialPhat) : "0 đ");
                rEarlyRight.add(lblPhat);
                
                RoundedButton btnEdit = new RoundedButton("Sửa", UIConstants.PRIMARY_LIGHT, UIConstants.PRIMARY);
                btnEdit.setFont(UIConstants.FONT_SMALL_BOLD);
                btnEdit.setPreferredSize(new Dimension(50, 24));
                btnEdit.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                btnEdit.addActionListener(e -> {
                    Double val = promptEarlyPenaltyDeduction(rId, diff, roomPenalties.getOrDefault(ct.getMaChiTiet(), 0.0), donGia);
                    if (val != null) {
                        roomPenalties.put(ct.getMaChiTiet(), val);
                        lblPhat.setText(val > 0 ? String.format("%,.0f đ", val) : "0 đ");
                        loadBillRows();
                    }
                });
                rEarlyRight.add(btnEdit);
                rEarly.add(rEarlyRight, BorderLayout.EAST);
                
                panel.add(rEarly);
                panel.add(Box.createVerticalStrut(4));
            }
        }

        panel.add(sumRow("Tiền cọc đã đặt (khấu trừ)", lblTienCoc, false));
        panel.add(Box.createVerticalStrut(6));
        JSeparator sep = new JSeparator();
        sep.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        panel.add(sep);
        panel.add(Box.createVerticalStrut(6));
        panel.add(sumRow("TỔNG CỘNG ĐOÀN", lblTongCong, true));

        panel.add(Box.createVerticalStrut(12));
        JPanel rowHT = new JPanel(new BorderLayout());
        rowHT.setOpaque(false);
        rowHT.setMaximumSize(new Dimension(Integer.MAX_VALUE, 32));
        JLabel lblHT = new JLabel("Hình thức thanh toán");
        lblHT.setFont(UIConstants.FONT_BODY);
        lblHT.setForeground(UIConstants.TEXT_SECONDARY);
        rowHT.add(lblHT, BorderLayout.WEST);

        String[] options = {"Tiền mặt", "Chuyển khoản"};
        cboHinhThuc = new JComboBox<>(options);
        cboHinhThuc.setFont(UIConstants.FONT_BODY);
        cboHinhThuc.setBackground(Color.WHITE);
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

        txtTienKhachDua = new ui.components.RoundedComponents.ModernTextField("");
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

        JButton btnPrint = new JButton("In hóa đơn đoàn");
        btnPrint.setFont(UIConstants.FONT_BODY);
        btnPrint.setForeground(UIConstants.TEXT_SECONDARY);
        btnPrint.setBorderPainted(false);
        btnPrint.setContentAreaFilled(false);
        btnPrint.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        btnPrint.addActionListener(e -> printMasterBill());

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);
        RoundedButton btnCancel = new RoundedButton("Huỷ", new Color(0xE2E8F0), UIConstants.TEXT_PRIMARY);
        RoundedButton btnConfirm = new RoundedButton("Xác nhận thanh toán đoàn", UIConstants.PRIMARY, Color.WHITE);
        btnConfirm.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnCancel.addActionListener(e -> dispose());
        btnConfirm.addActionListener(e -> doMasterCheckout());
        btns.add(btnCancel);
        btns.add(btnConfirm);

        footer.add(btnPrint, BorderLayout.WEST);
        footer.add(btns, BorderLayout.EAST);
        return footer;
    }

    // ---- MASTER CHECKOUT ----
    private void doMasterCheckout() {
        String hinhThuc = (String) cboHinhThuc.getSelectedItem();
        if (hinhThuc == null) return;

        if ("Tiền mặt".equals(hinhThuc)) {
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

        // Nếu chuyển khoản → hiện QR
        if ("Chuyển khoản".equals(hinhThuc)) {
            String tongText = lblTongCong.getText().replaceAll("[^\\d]", "");
            long tongCong = 0;
            try {
                tongCong = Long.parseLong(tongText);
            } catch (NumberFormatException ignored) {}

            String maHD = "HD-GRP-" + datPhong.getMaDatPhong();
            QRPaymentDialog qrDialog = new QRPaymentDialog((Frame) getOwner(), tongCong, maHD);
            qrDialog.setVisible(true);
            if (!qrDialog.isConfirmed()) return;
        }

        int ok = JOptionPane.showConfirmDialog(this,
                "Xác nhận thanh toán gom bill TOÀN BỘ ĐOÀN?\n"
                        + "Đoàn: " + (datPhong.getTenDoan() != null ? datPhong.getTenDoan() : datPhong.getMaDatPhong()) + "\n"
                        + "Số phòng: " + allRooms.size() + "\n"
                        + "Tổng tiền: " + lblTongCong.getText(),
                "Xác nhận Gom Bill Đoàn", JOptionPane.YES_NO_OPTION);
        if (ok != JOptionPane.YES_OPTION) return;

        String err = thuePhongService.checkOutMasterBill(
                datPhong.getMaDatPhong(),
                AuthService.getInstance().getCurrentMaNV(),
                datPhong.getMaKhuyenMai(),
                roomPenalties);

        if (err == null) {
            confirmed = true;
            ui.components.NotificationManager.showSuccess("Thanh toán đoàn thành công", 
                allRooms.size() + " phòng đã được thanh toán.");
            
            // ── Custom invoice choice dialog ──────────────────────────────────
            final int[] invoiceChoice = {-1};
            javax.swing.JDialog invoiceDialog = new javax.swing.JDialog(this, "Xuất hóa đơn", true);
            invoiceDialog.setUndecorated(true);
            invoiceDialog.setSize(560, 310);
            invoiceDialog.setLocationRelativeTo(this);
            invoiceDialog.setBackground(new java.awt.Color(0, 0, 0, 0));
            invoiceDialog.getRootPane().putClientProperty("apple.awt.draggableWindowBackground", Boolean.FALSE);

            JPanel dialogRoot = new JPanel(new BorderLayout()) {
                @Override protected void paintComponent(java.awt.Graphics g) {
                    java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                    g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(java.awt.Color.WHITE);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                    
                    // Subtle light mode border
                    g2.setColor(new java.awt.Color(0xE2E8F0));
                    g2.setStroke(new java.awt.BasicStroke(1.5f));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                    
                    // Elegant top gradient bar matching Royal Blue to Indigo
                    java.awt.GradientPaint gp = new java.awt.GradientPaint(0, 0, new java.awt.Color(0x3B82F6), getWidth(), 0, new java.awt.Color(0x4F46E5));
                    g2.setPaint(gp);
                    g2.fillRoundRect(0, 0, getWidth(), 6, 6, 6);
                    g2.dispose();
                }
            };
            dialogRoot.setOpaque(false);
            dialogRoot.setBorder(BorderFactory.createEmptyBorder(26, 28, 26, 28));

            // Header - Light Mode
            JLabel headerIcon = new JLabel("✅");
            headerIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));
            JLabel headerTitle = new JLabel("Thanh toán đoàn thành công!");
            headerTitle.setFont(new Font("Segoe UI", Font.BOLD, 19));
            headerTitle.setForeground(new java.awt.Color(0x0F172A)); // Dark Slate
            JLabel headerSub = new JLabel(allRooms.size() + " phòng đã được thanh toán. Bạn muốn xuất hóa đơn theo hình thức nào?");
            headerSub.setFont(new Font("Segoe UI", Font.PLAIN, 13));
            headerSub.setForeground(new java.awt.Color(0x64748B)); // Slate Gray

            JPanel headerPanel = new JPanel();
            headerPanel.setOpaque(false);
            headerPanel.setLayout(new BoxLayout(headerPanel, BoxLayout.Y_AXIS));
            JPanel titleRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
            titleRow.setOpaque(false);
            titleRow.add(headerIcon);
            titleRow.add(headerTitle);
            headerPanel.add(titleRow);
            headerPanel.add(Box.createVerticalStrut(6));
            headerPanel.add(headerSub);
            headerSub.setAlignmentX(java.awt.Component.LEFT_ALIGNMENT);

            // Card button factory — Light SaaS Mode
            java.util.function.BiFunction<String, String, JPanel> makeCard = (emoji, label) -> {
                boolean[] hovered = {false};
                JLabel textLbl = new JLabel("<html><center>" + label + "</center></html>", SwingConstants.CENTER);
                textLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
                textLbl.setForeground(new java.awt.Color(0x334155)); // slate-700
                
                JPanel card = new JPanel(new BorderLayout(0, 10)) {
                    @Override protected void paintComponent(java.awt.Graphics g) {
                        java.awt.Graphics2D g2 = (java.awt.Graphics2D) g.create();
                        g2.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING, java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
                        
                        // Background color
                        if (hovered[0]) {
                            g2.setColor(new java.awt.Color(0xEFF6FF)); // blue-50
                        } else {
                            g2.setColor(new java.awt.Color(0xF8FAFC)); // slate-50
                        }
                        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                        
                        // Border stroke
                        if (hovered[0]) {
                            g2.setColor(new java.awt.Color(0x3B82F6)); // blue-500
                            g2.setStroke(new java.awt.BasicStroke(2.0f));
                        } else {
                            g2.setColor(new java.awt.Color(0xE2E8F0)); // slate-200
                            g2.setStroke(new java.awt.BasicStroke(1.2f));
                        }
                        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 16, 16);
                        g2.dispose();
                        super.paintComponent(g);
                    }
                };
                card.setOpaque(false);
                card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
                
                JLabel emojiLbl = new JLabel(emoji, SwingConstants.CENTER);
                emojiLbl.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 30));
                
                card.add(emojiLbl, BorderLayout.CENTER);
                card.add(textLbl, BorderLayout.SOUTH);
                card.setBorder(BorderFactory.createEmptyBorder(16, 10, 16, 10));
                
                card.addMouseListener(new java.awt.event.MouseAdapter() {
                    @Override public void mouseEntered(java.awt.event.MouseEvent e) {
                        hovered[0] = true;
                        textLbl.setForeground(new java.awt.Color(0x1D4ED8)); // blue-700
                        card.repaint();
                    }
                    @Override public void mouseExited(java.awt.event.MouseEvent e) {
                        hovered[0] = false;
                        textLbl.setForeground(new java.awt.Color(0x334155)); // slate-700
                        card.repaint();
                    }
                });
                return card;
            };

            // Build three cards
            JPanel cardRow = new JPanel(new java.awt.GridLayout(1, 3, 14, 0));
            cardRow.setOpaque(false);

            JPanel cardSep = makeCard.apply("📄", "Tách hóa đơn<br>từng phòng");
            JPanel cardGom = makeCard.apply("📋", "Gộp 1 hóa đơn<br>đoàn");
            JPanel cardSkip = makeCard.apply("⏭", "Để sau / Bỏ qua");

            cardSep.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                    invoiceChoice[0] = 0; invoiceDialog.dispose();
                }
            });
            cardGom.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                    invoiceChoice[0] = 1; invoiceDialog.dispose();
                }
            });
            cardSkip.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override public void mouseClicked(java.awt.event.MouseEvent e) {
                    invoiceChoice[0] = 2; invoiceDialog.dispose();
                }
            });

            // Esc to close
            invoiceDialog.getRootPane().registerKeyboardAction(
                ev -> { invoiceChoice[0] = 2; invoiceDialog.dispose(); },
                javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_ESCAPE, 0),
                javax.swing.JComponent.WHEN_IN_FOCUSED_WINDOW
            );

            cardRow.add(cardSep);
            cardRow.add(cardGom);
            cardRow.add(cardSkip);

            dialogRoot.add(headerPanel, BorderLayout.NORTH);
            dialogRoot.add(Box.createVerticalStrut(20), BorderLayout.CENTER);
            dialogRoot.add(cardRow, BorderLayout.SOUTH);

            invoiceDialog.setContentPane(dialogRoot);
            invoiceDialog.setVisible(true);

            int selection = invoiceChoice[0];
            // ─────────────────────────────────────────────────────────────────

            if (selection == 0) {
                exportSeparateBills();
            } else if (selection == 1) {
                printMasterBill();
            }
            dispose();
        } else {
            ui.components.NotificationManager.showError("Lỗi", err);
        }
    }

    private void exportSeparateBills() {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Chọn thư mục lưu hóa đơn các phòng");
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) return;

        java.io.File folder = chooser.getSelectedFile();
        
        KhachHang kh = datPhong.getKhachHang();
        if (kh != null && kh.getMaKhachHang() != null) {
            KhachHang full = khService.getById(kh.getMaKhachHang());
            if (full != null) kh = full;
        }
        String tenDaiDien = kh != null ? kh.getHoTen() : "—";
        String sdt = kh != null && kh.getSoDienThoai() != null ? kh.getSoDienThoai() : "—";

        int successCount = 0;
        double totalTongPhong = parsePriceLabel(lblTongPhong);
        double totalGiamGia = calculateDiscount(totalTongPhong);
        double tienCocPerRoom = datPhong.getTienDatCoc() / allRooms.size();
        double giamGiaPerRoom = totalGiamGia / allRooms.size();

        for (ChiTietDatPhong ct : allRooms) {
            String roomId = ct.getPhong() != null ? ct.getPhong().getMaPhong() : "?";
            
            // Lọc các dòng liên quan tới phòng này
            java.util.List<Object[]> roomRows = new java.util.ArrayList<>();
            double tongPhong = 0;
            double tongPhuPhi = 0;
            double tongDV = 0;

            for (int i = 0; i < tableModel.getRowCount(); i++) {
                String tenPhong = String.valueOf(tableModel.getValueAt(i, 0));
                if (tenPhong.equals(roomId)) {
                    String noiDung = String.valueOf(tableModel.getValueAt(i, 1));
                    String donGia = String.valueOf(tableModel.getValueAt(i, 2));
                    String soLuong = String.valueOf(tableModel.getValueAt(i, 3));
                    String thanhTienStr = String.valueOf(tableModel.getValueAt(i, 4));
                    
                    roomRows.add(new Object[]{tenPhong, noiDung, donGia, soLuong, thanhTienStr});

                    double thanhTienVal = 0;
                    try {
                        thanhTienVal = Double.parseDouble(thanhTienStr.replaceAll("[^\\d]", ""));
                    } catch (Exception ignored) {}

                    if (noiDung.startsWith("Tiền phòng")) {
                        tongPhong += thanhTienVal;
                    } else if (noiDung.equals("Phụ phí") || noiDung.startsWith("Phạt trả phòng sớm")) {
                        tongPhuPhi += thanhTienVal;
                    } else {
                        tongDV += thanhTienVal;
                    }
                }
            }

            double tongCong = tongPhong + tongPhuPhi + tongDV - tienCocPerRoom - giamGiaPerRoom;
            if (tongCong < 0) tongCong = 0;

            java.io.File file = new java.io.File(folder, "HoaDon_Doan_" + datPhong.getMaDatPhong() + "_Phong_" + roomId + ".pdf");
            try {
                util.PDFExporter.exportSingleRoomInvoiceFromGroupPDF(
                        datPhong.getMaDatPhong(),
                        datPhong.getTenDoan(),
                        tenDaiDien,
                        sdt,
                        roomId,
                        roomRows,
                        tongPhong,
                        tongPhuPhi,
                        tongDV,
                        tienCocPerRoom,
                        giamGiaPerRoom,
                        tongCong,
                        file
                );
                successCount++;
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        if (successCount > 0) {
            JOptionPane.showMessageDialog(this,
                    "Xuất thành công " + successCount + " hóa đơn phòng vào thư mục:\n" + folder.getAbsolutePath(),
                    "Thành công", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this,
                    "Không có hóa đơn nào được xuất!",
                    "Thông báo", JOptionPane.WARNING_MESSAGE);
        }
    }

    // ---- PRINT ----
    private void printMasterBill() {
        KhachHang kh = datPhong.getKhachHang();
        if (kh != null && kh.getMaKhachHang() != null) {
            KhachHang full = khService.getById(kh.getMaKhachHang());
            if (full != null) kh = full;
        }
        String tenDaiDien = kh != null ? kh.getHoTen() : "—";
        String sdt = kh != null && kh.getSoDienThoai() != null ? kh.getSoDienThoai() : "—";

        java.util.List<Object[]> rows = new java.util.ArrayList<>();
        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String tenPhong = String.valueOf(tableModel.getValueAt(i, 0));
            String noiDung = String.valueOf(tableModel.getValueAt(i, 1));
            String donGia = String.valueOf(tableModel.getValueAt(i, 2));
            String soLuong = String.valueOf(tableModel.getValueAt(i, 3));
            String thanhTien = String.valueOf(tableModel.getValueAt(i, 4));
            rows.add(new Object[]{tenPhong, noiDung, donGia, soLuong, thanhTien});
        }

        double tongPhong = parsePriceLabel(lblTongPhong);
        double tongPhuPhi = parsePriceLabel(lblTongPhuPhi);
        double tongDV = parsePriceLabel(lblTongDV);
        double tienCoc = datPhong.getTienDatCoc();
        double giamGia = calculateDiscount(tongPhong);
        double tongCong = parsePriceLabel(lblTongCong);

        util.PDFExporter.exportMasterBillPDF(
                this,
                datPhong.getMaDatPhong(),
                datPhong.getTenDoan(),
                tenDaiDien,
                sdt,
                allRooms.size(),
                rows,
                tongPhong,
                tongPhuPhi,
                tongDV,
                tienCoc,
                giamGia,
                tongCong
        );
    }

    private double parsePriceLabel(JLabel label) {
        if (label == null || label.getText() == null) return 0.0;
        String text = label.getText().replaceAll("[^\\d]", "");
        if (text.isEmpty()) return 0.0;
        try {
            return Double.parseDouble(text);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private double calculateDiscount(double totalPhong) {
        double tienGiam = 0;
        String voucherCode = datPhong.getMaKhuyenMai();
        if (voucherCode != null && !voucherCode.isBlank()) {
            try {
                entity.KhuyenMai km = new dao.KhuyenMaiDAO().getByVoucherCode(voucherCode.trim());
                java.time.LocalDateTime thoiDiemDat = datPhong.getNgayDat() != null
                        ? datPhong.getNgayDat() : java.time.LocalDateTime.now();
                if (km != null && km.kiemTraHopLe(totalPhong, thoiDiemDat)) {
                    tienGiam = km.tinhSoTienGiam(totalPhong);
                }
            } catch (Exception ex) {
                // Bỏ qua nếu voucher lỗi hoặc hết hạn
            }
        }
        return tienGiam;
    }

    public boolean isConfirmed() {
        return confirmed;
    }

    private Double promptEarlyPenaltyDeduction(String roomId, long diffNights, double currentVal, double donGiaRoom) {
        JPanel pnl = new JPanel();
        pnl.setLayout(new BoxLayout(pnl, BoxLayout.Y_AXIS));
        pnl.setBackground(Color.WHITE);
        pnl.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        JLabel lblInfo = new JLabel("Phòng " + roomId + " trả phòng sớm " + diffNights + " đêm so với đặt phòng.");
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
        txtInput.setText(currentVal > 0 ? String.format("%.0f", currentVal) : "0");
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

    private boolean isDichVuRow(int row) {
        return rowServices.containsKey(row);
    }

    /** Hiện dialog sửa số lượng dịch vụ */
    private void onSuaDichVu(SuDungDichVu sddv) {
        String tenDV = sddv.getTenDichVu();
        JPanel pnl = new JPanel(new BorderLayout(0, 10));
        pnl.setBackground(Color.WHITE);
        pnl.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JLabel lTitle = new JLabel("Sửa dịch vụ: " + tenDV);
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
}
