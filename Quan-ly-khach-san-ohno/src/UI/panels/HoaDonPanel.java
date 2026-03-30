package ui.panels;

import dao.HoaDonDAO;
import entity.HoaDon;
import service.DatPhongService;
import service.KhachHangService;
import entity.DatPhong;
import entity.KhachHang;
import ui.MainFrame;
import ui.components.UIConstants;
import ui.components.RoundedComponents.*;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HoaDonPanel extends JPanel {

    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger.getLogger(HoaDonPanel.class.getName());

    private final HoaDonDAO dao = new HoaDonDAO();
    private final DatPhongService dpSvc = new DatPhongService();
    private final KhachHangService khSvc = new KhachHangService();
    private final Map<String, KhachHang> khachHangCache = new HashMap<>();

    private List<HoaDon> tatCaHoaDon;
    
    private ModernTextField txtSearch;
    private ModernTextField txtTuNgay;
    private ModernTextField txtDenNgay;
    private ModernComboBox<String> cboThoiGian;
    private ModernComboBox<String> cboTrangThai;
    private JTable table;
    private DefaultTableModel tableModel;
    private JLabel lblFooter;

    public HoaDonPanel(MainFrame mainFrame) {
        setOpaque(false);
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        
        main.add(buildHeader(), BorderLayout.NORTH);
        
        JPanel bot = new JPanel(new BorderLayout());
        bot.setOpaque(false);
        bot.add(Box.createVerticalStrut(14), BorderLayout.NORTH);
        bot.add(buildFilterBar(), BorderLayout.CENTER);
        
        JPanel tableContainer = new JPanel(new BorderLayout());
        tableContainer.setOpaque(false);
        tableContainer.add(bot, BorderLayout.NORTH);
        tableContainer.add(buildTableArea(), BorderLayout.CENTER);
        
        main.add(tableContainer, BorderLayout.CENTER);
        
        add(main, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(0,0,16,0));

        JPanel left = new JPanel(); left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        JLabel t = new JLabel("Lịch sử Hóa đơn");
        t.setFont(UIConstants.FONT_TITLE);
        JLabel s = new JLabel("Tra cứu toàn bộ hóa đơn đã xuất với công cụ lọc thông minh");
        s.setFont(UIConstants.FONT_BODY); s.setForeground(UIConstants.TEXT_SECONDARY);
        left.add(t); left.add(Box.createVerticalStrut(2)); left.add(s);

        RoundedButton btnExport = new RoundedButton("Xuất Excel", UIConstants.PRIMARY, Color.WHITE);
        btnExport.addActionListener(e -> JOptionPane.showMessageDialog(this, "Chức năng xuất Excel đang được xây dựng!"));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);
        btns.add(btnExport);
        
        p.add(left, BorderLayout.WEST);
        p.add(btns, BorderLayout.EAST);
        return p;
    }

    private JPanel buildFilterBar() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(0,0,10,0));
        
        txtSearch = new ModernTextField("Tìm Mã HĐ, Tên KH, SĐT...");
        txtSearch.setPreferredSize(new Dimension(220, 36));
        
        cboThoiGian = new ModernComboBox<>();
        cboThoiGian.setPreferredSize(new Dimension(140, 36));
        cboThoiGian.addItem("Tất cả thời gian");
        cboThoiGian.addItem("Hôm nay");
        cboThoiGian.addItem("Hôm qua");
        cboThoiGian.addItem("7 ngày qua");
        cboThoiGian.addItem("30 ngày qua");
        cboThoiGian.addItem("Tháng này");
        cboThoiGian.addItem("Tháng trước");
        cboThoiGian.addItem("Tùy chỉnh...");
        
        txtTuNgay = new ModernTextField("Từ (dd/MM/yyyy)");
        txtTuNgay.setPreferredSize(new Dimension(140, 36));
        txtDenNgay = new ModernTextField("Đến (dd/MM/yyyy)");
        txtDenNgay.setPreferredSize(new Dimension(140, 36));
        
        cboTrangThai = new ModernComboBox<>();
        cboTrangThai.setPreferredSize(new Dimension(140, 36));
        cboTrangThai.addItem("Tất cả trạng thái");
        cboTrangThai.addItem("Đã thanh toán");
        cboTrangThai.addItem("Chưa thanh toán");

        // Logic Auto-Fill từ ComboBox sang Text
        cboThoiGian.addActionListener(e -> {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            LocalDate today = LocalDate.now();
            int idx = cboThoiGian.getSelectedIndex();
            
            if (idx == 0 || idx == 7) { // Tất cả / Tùy chỉnh
                txtTuNgay.setText(idx == 0 ? "Từ (dd/MM/yyyy)" : "");
                txtDenNgay.setText(idx == 0 ? "Đến (dd/MM/yyyy)" : "");
            } else if (idx == 1) { // Hôm nay
                txtTuNgay.setText(today.format(fmt)); txtDenNgay.setText(today.format(fmt));
            } else if (idx == 2) { // Hôm qua
                txtTuNgay.setText(today.minusDays(1).format(fmt)); txtDenNgay.setText(today.minusDays(1).format(fmt));
            } else if (idx == 3) { // 7 ngày qua
                txtTuNgay.setText(today.minusDays(7).format(fmt)); txtDenNgay.setText(today.format(fmt));
            } else if (idx == 4) { // 30 ngày qua
                txtTuNgay.setText(today.minusDays(30).format(fmt)); txtDenNgay.setText(today.format(fmt));
            } else if (idx == 5) { // Tháng này
                txtTuNgay.setText(today.withDayOfMonth(1).format(fmt)); txtDenNgay.setText(today.withDayOfMonth(today.lengthOfMonth()).format(fmt));
            } else if (idx == 6) { // Tháng trước
                LocalDate prev = today.minusMonths(1);
                txtTuNgay.setText(prev.withDayOfMonth(1).format(fmt)); txtDenNgay.setText(prev.withDayOfMonth(prev.lengthOfMonth()).format(fmt));
            }
            if(idx != 7) runFilter();
        });

        javax.swing.event.DocumentListener dl = new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) { runFilter(); }
            public void removeUpdate(javax.swing.event.DocumentEvent e) { runFilter(); }
            public void changedUpdate(javax.swing.event.DocumentEvent e) { runFilter(); }
        };
        txtSearch.getDocument().addDocumentListener(dl);
        txtTuNgay.getDocument().addDocumentListener(dl);
        txtDenNgay.getDocument().addDocumentListener(dl);
        
        cboTrangThai.addActionListener(e -> runFilter());
        
        p.add(txtSearch);
        p.add(cboThoiGian);
        p.add(txtTuNgay);
        p.add(txtDenNgay);
        p.add(cboTrangThai);
        return p;
    }

    private JPanel buildTableArea() {
        String[] cols = {"Mã HĐ", "Khách hàng", "Số ĐT", "Ngày xuất", "Tiền phòng", "Cộng thêm", "Tổng tiền", "Hình thức TT", "Trạng thái"};
        tableModel = new DefaultTableModel(cols,0) { @Override public boolean isCellEditable(int r,int c){return false;} };
        table = new JTable(tableModel);
        styleTable(); 
        
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    int row = table.getSelectedRow();
                    if (row >= 0 && tatCaHoaDon != null) {
                        showDetails((String) tableModel.getValueAt(row, 0));
                    }
                }
            }
            @Override
            public void mousePressed(MouseEvent e) {
                int r = table.rowAtPoint(e.getPoint());
                if (r >= 0) table.setRowSelectionInterval(r, r);
            }
        });
        
        table.setComponentPopupMenu(ctxMenu(
            () -> {
                int row = table.getSelectedRow();
                if (row >= 0) showDetails((String) tableModel.getValueAt(row, 0));
            },
            () -> {
                int row = table.getSelectedRow();
                if (row >= 0) JOptionPane.showMessageDialog(HoaDonPanel.this, "Hệ thống đang in lại hóa đơn " + tableModel.getValueAt(row, 0) + "...");
            }
        ));

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER));
        sp.setPreferredSize(new Dimension(1000, 500));
        
        lblFooter = new JLabel("Đang tải dữ liệu...");
        lblFooter.setFont(UIConstants.FONT_SMALL);
        lblFooter.setForeground(UIConstants.TEXT_MUTED);
        
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.LEFT));
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1,0,0,0,UIConstants.BORDER));
        footer.add(lblFooter);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(sp, BorderLayout.CENTER);
        wrap.add(footer, BorderLayout.SOUTH);
        
        loadData();
        return wrap;
    }

    private JPopupMenu ctxMenu(Runnable onView, Runnable onPrint) {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem miView = new JMenuItem("◎ Xem chi tiết");
        JMenuItem miPrint = new JMenuItem("🖨 In lại hóa đơn");
        miView.setFont(UIConstants.FONT_BODY);
        miPrint.setFont(UIConstants.FONT_BODY);
        miView.addActionListener(e -> onView.run());
        miPrint.addActionListener(e -> onPrint.run());
        menu.add(miView);
        menu.addSeparator();
        menu.add(miPrint);
        return menu;
    }

    private void styleTable() {
        table.setFont(UIConstants.FONT_BODY); table.setRowHeight(48);
        table.setShowGrid(false); table.setIntercellSpacing(new Dimension(0,0));
        table.setBackground(Color.WHITE); 
        table.setSelectionBackground(UIConstants.PRIMARY_LIGHT);
        table.getTableHeader().setFont(UIConstants.FONT_SMALL_BOLD);
        table.getTableHeader().setBackground(UIConstants.BG_TABLE_HEADER);
        table.getTableHeader().setForeground(UIConstants.TEXT_SECONDARY);
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0,0,1,0,UIConstants.BORDER));
        
        DefaultTableCellRenderer rightRender = new DefaultTableCellRenderer();
        rightRender.setHorizontalAlignment(JLabel.RIGHT);
        for (int i : new int[]{4,5,6}) table.getColumnModel().getColumn(i).setCellRenderer(rightRender);

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean sel, boolean foc, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(tbl, val, sel, foc, row, col);
                lbl.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                lbl.setBackground(sel ? UIConstants.PRIMARY_LIGHT : (row % 2 == 0 ? Color.WHITE : new Color(0xFAFAFA)));
                lbl.setForeground(UIConstants.TEXT_PRIMARY);
                
                if (col == 8) {
                    lbl.setFont(UIConstants.FONT_SMALL_BOLD);
                    String v = val != null ? val.toString() : "";
                    if (v.equals("Đã thanh toán")) {
                        lbl.setForeground(UIConstants.SUCCESS);
                    } else if (!v.isEmpty()) {
                        lbl.setForeground(UIConstants.DANGER);
                    }
                }
                return lbl;
            }
        });
    }

    private void loadData() {
        SwingWorker<List<HoaDon>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<HoaDon> doInBackground() throws Exception {
                List<HoaDon> list = dao.getAll();
                // Map DatPhong and KhachHang into HoaDon object to avoid UI freeze during filter
                for (HoaDon hd : list) {
                    if (hd.getDatPhong() != null && hd.getDatPhong().getMaDatPhong() != null) {
                        DatPhong dp = dpSvc.getById(hd.getDatPhong().getMaDatPhong());
                        if (dp != null && dp.getKhachHang() != null) {
                            String maKh = dp.getKhachHang().getMaKhachHang();
                            if (!khachHangCache.containsKey(maKh)) {
                                khachHangCache.put(maKh, khSvc.getById(maKh));
                            }
                            hd.setKhachHang(khachHangCache.get(maKh));
                        }
                    }
                }
                return list;
            }

            @Override
            protected void done() {
                try {
                    tatCaHoaDon = get();
                    runFilter();
                } catch (Exception e) {
                    LOGGER.log(java.util.logging.Level.WARNING, "UI error in HoaDonPanel", e);
                    lblFooter.setText("Lỗi tải dữ liệu hóa đơn.");
                }
            }
        };
        worker.execute();
    }

    private void runFilter() {
        if (tatCaHoaDon == null) return;
        tableModel.setRowCount(0);
        
        String kw = txtSearch.getText().trim().toLowerCase();
        if (kw.equals("tìm mã hđ, tên kh, sđt...")) kw = "";
        
        String tuNgayStr = txtTuNgay.getText().trim();
        String denNgayStr = txtDenNgay.getText().trim();

        DateTimeFormatter outputFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        DateTimeFormatter inputFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        LocalDateTime fromDate = null;
        LocalDateTime toDate = null;
        try {
            if (tuNgayStr.matches("\\d{1,2}/\\d{1,2}/\\d{4}")) fromDate = LocalDate.parse(tuNgayStr, inputFmt).atStartOfDay();
        } catch (Exception ignored) {}
        try {
            if (denNgayStr.matches("\\d{1,2}/\\d{1,2}/\\d{4}")) toDate = LocalDate.parse(denNgayStr, inputFmt).atTime(LocalTime.MAX);
        } catch (Exception ignored) {}
        
        int idxStatus = cboTrangThai.getSelectedIndex();
        boolean filterPaid = (idxStatus == 1);
        boolean filterUnpaid = (idxStatus == 2);
        
        final LocalDateTime finalFrom = fromDate;
        final LocalDateTime finalTo = toDate;
        final String finalKw = kw;

        List<HoaDon> filtered = tatCaHoaDon.stream()
            .filter(hd -> {
                boolean matchKw = finalKw.isEmpty() || 
                    (hd.getMaHoaDon() != null && hd.getMaHoaDon().toLowerCase().contains(finalKw)) || 
                    (hd.getKhachHang() != null && hd.getKhachHang().getHoTen() != null && hd.getKhachHang().getHoTen().toLowerCase().contains(finalKw)) ||
                    (hd.getKhachHang() != null && hd.getKhachHang().getSoDienThoai() != null && hd.getKhachHang().getSoDienThoai().contains(finalKw));
                
                boolean matchDate = true;
                if (finalFrom != null && hd.getNgayLap() != null && hd.getNgayLap().isBefore(finalFrom)) matchDate = false;
                if (finalTo != null && hd.getNgayLap() != null && hd.getNgayLap().isAfter(finalTo)) matchDate = false;
                
                boolean matchStatus = true;
                boolean isPaid = hd.getTrangThai() != null && "PAID".equalsIgnoreCase(hd.getTrangThai().name());
                if (filterPaid && !isPaid) matchStatus = false;
                if (filterUnpaid && isPaid) matchStatus = false;
                
                return matchKw && matchDate && matchStatus;
            })
            .collect(Collectors.toList());

        for (HoaDon hd : filtered) {
            String tenKh = (hd.getKhachHang() != null && hd.getKhachHang().getHoTen() != null) ? hd.getKhachHang().getHoTen() : "Khách vãng lai";
            String sdt = (hd.getKhachHang() != null && hd.getKhachHang().getSoDienThoai() != null) ? hd.getKhachHang().getSoDienThoai() : "";
            double phuPhiDichVu = hd.getTongTienDichVu(); // Bao gộp cả dịch vụ và phụ phí khác
            
            String statusGui = "Chưa thanh toán";
            if (hd.getTrangThai() != null && "PAID".equalsIgnoreCase(hd.getTrangThai().name())) {
                statusGui = "Đã thanh toán";
            }
            
            tableModel.addRow(new Object[]{
                hd.getMaHoaDon(),
                tenKh,
                sdt,
                hd.getNgayLap() != null ? hd.getNgayLap().format(outputFmt) : "",
                String.format("%,.0f đ", hd.getTongTienPhong()),
                String.format("%,.0f đ", phuPhiDichVu),
                String.format("%,.0f đ", hd.getTongThanhToan()),
                "Tiền mặt/CK", // Placeholder pending proper DB schema
                statusGui
            });
        }
        lblFooter.setText("  Hiển thị " + filtered.size() + " hóa đơn  |  Right-click: Xem, In lại • Double-click: Xem chi tiết");
    }

    private void showDetails(String maHD) {
        HoaDon hd = tatCaHoaDon.stream()
                .filter(h -> h.getMaHoaDon().equals(maHD))
                .findFirst().orElse(null);
        if (hd != null) {
            Frame owner = (Frame) SwingUtilities.getWindowAncestor(HoaDonPanel.this);
            ui.dialogs.XemHoaDonDialog dlg = new ui.dialogs.XemHoaDonDialog(owner, hd);
            dlg.setVisible(true);
        }
    }

    public void refresh() {
        loadData();
    }
}

