package ui.panels;

import entity.KhachHang;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.stream.Collectors;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import service.AuthService;
import service.KhachHangService;
import ui.MainFrame;
import ui.components.NotificationManager;
import ui.components.PaginationPanel;
import ui.components.ModernPopupMenu;
import ui.components.ModernMenuItem;
import ui.components.RoundedComponents.*;
import ui.components.RoundedComponents.ModernComboBox;
import ui.components.RoundedComponents.ModernTextField;
import ui.components.RoundedComponents.RoundedButton;
import ui.components.RoundedComponents.RoundedPanel;
import ui.components.UIConstants;
import ui.dialogs.KhachHangDialog;
import util.ExcelExporter;

public class KhachHangPanel extends JPanel implements ResettableFilter {

    @Override
    public void resetFilters() {
        if (txtSearch != null)
            txtSearch.setText("");
        if (cboQuocTich != null)
            cboQuocTich.setSelectedIndex(0);
        if (cboHang != null)
            cboHang.setSelectedIndex(0);
        if (cboTrangThai != null)
            cboTrangThai.setSelectedIndex(0);
        runFilter();
    }

    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger
            .getLogger(KhachHangPanel.class.getName());

    private final MainFrame mainFrame;
    private final KhachHangService service = new KhachHangService();

    private ModernTextField txtSearch;
    private ModernComboBox<String> cboQuocTich;
    private ModernComboBox<String> cboHang;
    private ModernComboBox<String> cboTrangThai;
    private JTable table;
    private DefaultTableModel tableModel;
    private PaginationPanel pagination;

    private List<KhachHang> tatCaKhachHang;
    private List<KhachHang> filteredList;

    private JLabel lblTongKH, lblKhachVIP, lblKhachQuen, lblQuocTe;

    public KhachHangPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setOpaque(false);
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        main.add(buildHeader(), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout());
        center.setOpaque(false);
        center.add(buildStats(), BorderLayout.NORTH);

        // filterBar ở NORTH (compact), tableArea ở CENTER (fill remaining space)
        JPanel filterWrapper = new JPanel(new BorderLayout());
        filterWrapper.setOpaque(false);
        filterWrapper.add(Box.createVerticalStrut(14), BorderLayout.NORTH);
        filterWrapper.add(buildFilterBar(), BorderLayout.CENTER);

        JPanel bot = new JPanel(new BorderLayout());
        bot.setOpaque(false);
        bot.add(filterWrapper, BorderLayout.NORTH);
        bot.add(buildTableArea(), BorderLayout.CENTER);

        center.add(bot, BorderLayout.CENTER);
        main.add(center, BorderLayout.CENTER);

        add(main, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        JLabel t = new JLabel("Hồ sơ Khách hàng");
        t.setFont(UIConstants.FONT_TITLE);
        JLabel s = new JLabel("Lưu trữ, tra cứu và quản lý thông tin lưu trú của khách hàng (CRM)");
        s.setFont(UIConstants.FONT_BODY);
        s.setForeground(UIConstants.TEXT_SECONDARY);
        left.add(t);
        left.add(Box.createVerticalStrut(2));
        left.add(s);

        RoundedButton btnThem = new RoundedButton("+ Thêm khách hàng", UIConstants.PRIMARY, Color.WHITE);
        RoundedButton btnExcel = new RoundedButton("↓ Xuất danh sách", new Color(0x16A34A), Color.WHITE);

        btnThem.addActionListener(e -> showDialog(null, false));
        btnExcel.addActionListener(e -> {
            if (filteredList != null && !filteredList.isEmpty()) {
                ExcelExporter.exportKhachHang(KhachHangPanel.this, filteredList);
            } else {
                JOptionPane.showMessageDialog(this, "Không có dữ liệu để xuất!");
            }
        });

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);
        btns.add(btnExcel);
        btns.add(btnThem);

        p.add(left, BorderLayout.WEST);
        p.add(btns, BorderLayout.EAST);
        return p;
    }

    private JPanel buildStats() {
        JPanel row = new JPanel(new GridLayout(1, 4, 16, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 95));

        lblTongKH = new JLabel("...");
        lblKhachVIP = new JLabel("...");
        lblKhachQuen = new JLabel("...");
        lblQuocTe = new JLabel("...");

        row.add(statCard("Tổng khách hàng", lblTongKH, UIConstants.CARD_INDIGO));
        row.add(statCard("Khách VIP", lblKhachVIP, UIConstants.CARD_AMBER));
        row.add(statCard("Khách quay lại", lblKhachQuen, UIConstants.CARD_BLUE));
        row.add(statCard("Khách quốc tế", lblQuocTe, UIConstants.CARD_GREEN));

        return row;
    }

    private RoundedPanel statCard(String label, JLabel lblValue, Color c) {
        RoundedPanel card = new RoundedPanel(12);
        card.setBackground(Color.WHITE);
        card.setShadow(true);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(18, 20, 18, 20));

        JLabel l1 = new JLabel(label);
        l1.setFont(UIConstants.FONT_SMALL_BOLD);
        l1.setForeground(UIConstants.TEXT_SECONDARY);

        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 24));
        lblValue.setForeground(UIConstants.TEXT_PRIMARY);

        JPanel indicator = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(c);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 4, 4);
            }
        };
        indicator.setPreferredSize(new Dimension(4, 24));

        JPanel left = new JPanel(new BorderLayout(12, 0));
        left.setOpaque(false);
        left.add(indicator, BorderLayout.WEST);

        JPanel text = new JPanel(new GridLayout(2, 1, 0, 2));
        text.setOpaque(false);
        text.add(l1);
        text.add(lblValue);
        left.add(text, BorderLayout.CENTER);

        card.add(left, BorderLayout.WEST);
        return card;
    }

    private JPanel buildFilterBar() {
        JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        txtSearch = new ModernTextField("Tìm Mã, Họ Tên, SĐT, CCCD...");
        txtSearch.setPreferredSize(new Dimension(280, 40));

        cboQuocTich = new ModernComboBox<>();
        cboQuocTich.setPreferredSize(new Dimension(220, 40));
        // Load từ CountryList
        for (String country : util.CountryList.getFilterList()) {
            cboQuocTich.addItem(country);
        }

        cboHang = new ModernComboBox<>();
        cboHang.setPreferredSize(new Dimension(200, 40));
        cboHang.addItem("Hạng khách (Tất cả)");
        cboHang.addItem("Hạng Thường (Bronze)");
        cboHang.addItem("Thân thiết (Silver)");
        cboHang.addItem("VIP (Gold)");

        cboTrangThai = new ModernComboBox<>();
        cboTrangThai.setPreferredSize(new Dimension(200, 40));
        cboTrangThai.addItem("Trạng thái: Hoạt động");
        cboTrangThai.addItem("Trạng thái: Vô hiệu hóa");
        cboTrangThai.addItem("Trạng thái: Tất cả");

        DocumentListener dl = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                runFilter();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                runFilter();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                runFilter();
            }
        };
        txtSearch.getDocument().addDocumentListener(dl);

        ActionListener al = e -> runFilter();
        cboQuocTich.addActionListener(al);
        cboHang.addActionListener(al);
        cboTrangThai.addActionListener(al);

        p.add(txtSearch);
        p.add(Box.createHorizontalStrut(12));
        p.add(cboQuocTich);
        p.add(Box.createHorizontalStrut(12));
        p.add(cboHang);
        p.add(Box.createHorizontalStrut(12));
        p.add(cboTrangThai);
        p.add(Box.createHorizontalStrut(12));

        RoundedButton btnClear = new RoundedButton("Xóa lọc", new Color(0xF1F5F9), UIConstants.TEXT_PRIMARY);
        btnClear.setPreferredSize(new Dimension(100, 40));
        btnClear.setFont(new Font("Segoe UI", Font.BOLD, 12));
        btnClear.addActionListener(e -> resetFilters());
        p.add(btnClear);

        return p;
    }

    private JPanel buildTableArea() {
        String[] cols = { "Mã KH", "Họ tên", "Số ĐT", "Hạng khách", "Lượt ở", "Tổng chi tiêu", "Quốc tịch", "Trạng thái" };
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        table = new JTable(tableModel);
        styleTable();

        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2)
                    editSelected();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                int r = table.rowAtPoint(e.getPoint());
                if (r >= 0)
                    table.setRowSelectionInterval(r, r);
                showPopup(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                showPopup(e);
            }

            private void showPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    JPopupMenu menu = buildContextMenu();
                    menu.show(e.getComponent(), e.getX(), e.getY());
                }
            }
        });

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER));

        pagination = new PaginationPanel();
        pagination.setPageChangeListener(page -> displayCurrentPage());

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)));
        footer.add(pagination, BorderLayout.CENTER);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(sp, BorderLayout.CENTER);
        wrap.add(footer, BorderLayout.SOUTH);

        loadData();
        return wrap;
    }

    private JPopupMenu buildContextMenu() {
        ModernPopupMenu menu = new ModernPopupMenu();
 
        // --- Group 1: Profile & Info ---
        ModernMenuItem miXem = new ModernMenuItem("Xem hồ sơ chi tiết");
        ModernMenuItem miSua = new ModernMenuItem("Chỉnh sửa hồ sơ");
 
        // --- Group 2: Business actions ---
        ModernMenuItem miDat = new ModernMenuItem("Đặt phòng mới");
        ModernMenuItem miLichSu = new ModernMenuItem("Xem hóa đơn khách này");
 
        // --- Group 3: System / Dangerous ---
        KhachHang khSelected = getSelected();
        boolean isNgung = khSelected != null && "Ngừng".equals(khSelected.getTrangThai());
        ModernMenuItem miXoa = new ModernMenuItem(isNgung ? "Kích hoạt lại hồ sơ" : "Vô hiệu hóa hồ sơ");
        miXoa.setEnabled(AuthService.getInstance().isManager());
 
        // Logic actions
        miXem.addActionListener(e -> viewSelected());
        miSua.addActionListener(e -> editSelected());
 
        miDat.addActionListener(e -> {
            KhachHang kh = getSelected();
            if (kh != null) {
                mainFrame.navigateTo("datphong", () -> {
                    if (mainFrame.getDatPhongPanel() != null)
                        mainFrame.getDatPhongPanel().prefillKhachHang(kh);
                });
            }
        });
 
        miLichSu.addActionListener(e -> {
            KhachHang kh = getSelected();
            if (kh != null) {
                mainFrame.navigateTo("hoadon", () -> {
                    if (mainFrame.getHoaDonPanel() != null)
                        mainFrame.getHoaDonPanel().prefillSearch(kh.getHoTen());
                });
            }
        });
 
        miXoa.addActionListener(e -> toggleActivationSelected());
 
        menu.add(miXem);
        menu.add(miSua);
        menu.addSeparator();
        menu.add(miDat);
        menu.add(miLichSu);
        menu.addSeparator();
        menu.add(miXoa);
        return menu;
    }

    private void styleTable() {
        table.setFont(UIConstants.FONT_BODY);
        table.setRowHeight(50);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(UIConstants.PRIMARY_LIGHT);
        // Căn lề cho tiêu đề
        DefaultTableCellRenderer headerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean s, boolean f, int r, int c) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, v, s, f, r, c);
                lbl.setFont(UIConstants.FONT_SMALL_BOLD);
                lbl.setBackground(UIConstants.BG_TABLE_HEADER);
                lbl.setForeground(UIConstants.TEXT_SECONDARY);
                lbl.setHorizontalAlignment(JLabel.CENTER);
                lbl.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.BORDER));
                return lbl;
            }
        };
        table.getTableHeader().setDefaultRenderer(headerRenderer);

        // Thiết lập chiều rộng cột
        int[] widths = { 90, 150, 100, 140, 70, 120, 100, 110 };
        for (int i = 0; i < widths.length; i++) {
            table.getColumnModel().getColumn(i).setPreferredWidth(widths[i]);
        }

        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean foc, int row,
                    int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                lbl.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                lbl.setBackground(sel ? UIConstants.PRIMARY_LIGHT : (row % 2 == 0 ? Color.WHITE : new Color(0xFAFAFA)));
                lbl.setForeground(UIConstants.TEXT_PRIMARY);

                // Căn chỉnh và màu sắc dựa trên từng cột
                if (col == 7) {
                    lbl.setHorizontalAlignment(SwingConstants.CENTER);
                    String status = val != null ? val.toString() : "Hoạt động";
                    if ("Ngừng".equals(status) || "Vô hiệu hóa".equals(status)) {
                        lbl.setForeground(new Color(0xEF4444)); // Đỏ
                        lbl.setText("Vô hiệu hóa");
                    } else {
                        lbl.setForeground(new Color(0x10B981)); // Xanh lá
                        lbl.setText("Hoạt động");
                    }
                } else if (col == 0 || col == 2 || col == 3 || col == 4 || col == 6) {
                    lbl.setHorizontalAlignment(SwingConstants.CENTER);
                } else if (col == 5) {
                    lbl.setHorizontalAlignment(SwingConstants.RIGHT);
                } else {
                    lbl.setHorizontalAlignment(SwingConstants.LEFT);
                }

                return lbl;
            }
        });
    }

    private void loadData() {
        SwingWorker<List<KhachHang>, Void> worker = new SwingWorker<>() {
            @Override
            protected List<KhachHang> doInBackground() throws Exception {
                return service.getAllWithInactive();
            }

            @Override
            protected void done() {
                try {
                    tatCaKhachHang = get();
                    updateStats();
                    runFilter();
                } catch (Exception e) {
                    LOGGER.log(java.util.logging.Level.WARNING, "UI error in KhachHangPanel", e);
                }
            }
        };
        worker.execute();
    }

    private void updateStats() {
        if (tatCaKhachHang == null)
            return;
        List<KhachHang> activeList = tatCaKhachHang.stream()
                .filter(k -> !"Ngừng".equals(k.getTrangThai())).collect(Collectors.toList());
        int total = activeList.size();
        long vip = activeList.stream()
                .filter(k -> k.getHangKhachHang() != null && k.getHangKhachHang().contains("VIP")).count();
        long quen = activeList.stream().filter(k -> k.getSoLanO() > 1).count();
        long qte = activeList.stream()
                .filter(k -> k.getQuocTich() != null && !k.getQuocTich().equalsIgnoreCase("Việt Nam")).count();

        lblTongKH.setText(String.format("%,d", total));
        lblKhachVIP.setText(String.format("%,d", vip));
        lblKhachQuen.setText(String.format("%,d", quen));
        lblQuocTe.setText(String.format("%,d", qte));
    }

    private void runFilter() {
        if (tatCaKhachHang == null)
            return;

        String kw = txtSearch.getText().trim().toLowerCase();
        if (kw.equals("tìm mã, họ tên, sđt, cccd..."))
            kw = "";

        String selectedCountry = (String) cboQuocTich.getSelectedItem();
        final String finalKw = kw;

        filteredList = tatCaKhachHang.stream()
                .filter(kh -> {
                    boolean matchKw = finalKw.isEmpty() ||
                            (kh.getMaKhachHang() != null && kh.getMaKhachHang().toLowerCase().contains(finalKw)) ||
                            (kh.getHoTen() != null && kh.getHoTen().toLowerCase().contains(finalKw)) ||
                            (kh.getSdt() != null && kh.getSdt().contains(finalKw)) ||
                            (kh.getCccd() != null && kh.getCccd().contains(finalKw));

                    // Filter by country
                    boolean matchQt = true;
                    if (selectedCountry != null && !selectedCountry.equals("Quốc tịch (Tất cả)")) {
                        if (selectedCountry.equals("Nội địa (Việt Nam)")) {
                            // Show only Vietnamese customers
                            matchQt = util.CountryList.isDomestic(kh.getQuocTich());
                        } else {
                            // Show specific country
                            String customerCountry = kh.getQuocTich() != null ? kh.getQuocTich()
                                    : util.CountryList.VIETNAM;
                            matchQt = selectedCountry.equalsIgnoreCase(customerCountry);
                        }
                    }

                    boolean matchHang = true;
                    String hang = kh.getHangKhachHang();
                    int idxHang = cboHang.getSelectedIndex();
                    if (idxHang == 1 && !hang.contains("Thường"))
                        matchHang = false;
                    if (idxHang == 2 && !hang.contains("Thân thiết"))
                        matchHang = false;
                    if (idxHang == 3 && !hang.contains("VIP"))
                        matchHang = false;

                    boolean matchTrangThai = true;
                    int idxTrangThai = cboTrangThai.getSelectedIndex();
                    if (idxTrangThai == 0 && !"Hoạt động".equals(kh.getTrangThai())) {
                        matchTrangThai = false;
                    } else if (idxTrangThai == 1 && !"Ngừng".equals(kh.getTrangThai())) {
                        matchTrangThai = false;
                    }

                    return matchKw && matchQt && matchHang && matchTrangThai;
                })
                .collect(Collectors.toList());

        displayCurrentPage();
    }

    private void displayCurrentPage() {
        tableModel.setRowCount(0);
        if (filteredList == null)
            return;

        int pageSize = 12;
        int currentPage = pagination != null ? pagination.getCurrentPage() : 1;

        int totalPages = (int) Math.ceil((double) filteredList.size() / pageSize);
        if (currentPage > totalPages && totalPages > 0) {
            currentPage = totalPages;
            if (pagination != null)
                pagination.setCurrentPage(currentPage);
        }

        int start = (currentPage - 1) * pageSize;
        int end = Math.min(start + pageSize, filteredList.size());

        for (int i = start; i < end; i++) {
            KhachHang kh = filteredList.get(i);
            String gt = kh.getGioiTinh() != null ? kh.getGioiTinh() : "Khác";
            String qte = (kh.getQuocTich() == null || kh.getQuocTich().isBlank()) ? "Việt Nam" : kh.getQuocTich();

            tableModel.addRow(new Object[] {
                    kh.getMaKhachHang(),
                    kh.getHoTen(),
                    kh.getSdt(),
                    kh.getHangKhachHang(),
                    kh.getSoLanO(),
                    String.format("%,.0f đ", kh.getTongChiTieu()),
                    qte,
                    kh.getTrangThai()
            });
        }

        if (pagination != null) {
            pagination.update(filteredList.size(), pageSize, currentPage);
        }
    }

    private KhachHang getSelected() {
        int row = table.getSelectedRow();
        if (row < 0)
            return null;
        return service.getById((String) tableModel.getValueAt(row, 0));
    }

    private void showDialog(KhachHang kh, boolean readOnly) {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        KhachHangDialog dlg = new KhachHangDialog(owner, kh, readOnly);
        dlg.setVisible(true);
        if (!readOnly && dlg.isConfirmed()) {
            NotificationManager.showSuccess("Thành công",
                    (kh == null ? "Đã thêm khách hàng mới" : "Đã cập nhật thông tin khách hàng"));
            refresh();
        }
    }

    private void editSelected() {
        KhachHang kh = getSelected();
        if (kh == null) {
            JOptionPane.showMessageDialog(this, "Chọn một hồ sơ khách hàng để mở!");
            return;
        }
        showDialog(kh, false);
    }

    private void viewSelected() {
        KhachHang kh = getSelected();
        if (kh == null) {
            JOptionPane.showMessageDialog(this, "Chọn một hồ sơ khách hàng để mở!");
            return;
        }
        showDialog(kh, true);
    }

    private void toggleActivationSelected() {
        if (!AuthService.getInstance().isManager()) {
            NotificationManager.showWarning("Truy cập bị từ chối", "Bạn không có quyền thay đổi trạng thái hồ sơ khách hàng!");
            return;
        }
        KhachHang kh = getSelected();
        if (kh == null) {
            JOptionPane.showMessageDialog(this, "Chọn một hồ sơ khách hàng!");
            return;
        }

        boolean isNgung = "Ngừng".equals(kh.getTrangThai());
        if (isNgung) {
            int ok = JOptionPane.showConfirmDialog(this,
                    "Bạn có chắc chắn muốn kích hoạt lại hồ sơ khách hàng: \"" + kh.getHoTen() + "\" (" + kh.getMaKhachHang() + ")?",
                    "Xác nhận kích hoạt lại hồ sơ", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

            if (ok == JOptionPane.YES_OPTION) {
                String err = service.activate(kh.getMaKhachHang());
                if (err == null) {
                    NotificationManager.showSuccess("Thành công", "Đã kích hoạt lại hồ sơ khách hàng " + kh.getHoTen());
                    refresh();
                } else {
                    JOptionPane.showMessageDialog(this, "Lỗi: " + err, "Không thể kích hoạt lại", JOptionPane.ERROR_MESSAGE);
                }
            }
        } else {
            int ok = JOptionPane.showConfirmDialog(this,
                    "Bạn có chắc chắn muốn vô hiệu hóa hồ sơ khách hàng: \"" + kh.getHoTen() + "\" (" + kh.getMaKhachHang() + ")?\n"
                            + "Dữ liệu hóa đơn và phòng liên kết cũng có thể bị ảnh hưởng.",
                    "Xác nhận vô hiệu hóa hồ sơ", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

            if (ok == JOptionPane.YES_OPTION) {
                String err = service.xoa(kh.getMaKhachHang());
                if (err == null) {
                    NotificationManager.showSuccess("Thành công", "Đã vô hiệu hóa hồ sơ khách hàng " + kh.getHoTen());
                    refresh();
                } else {
                    JOptionPane.showMessageDialog(this,
                            "Lỗi: " + err + "\nKhách hàng này có thể đang có Hóa đơn hoặc Phòng đang thuê.",
                            "Không thể vô hiệu hóa", JOptionPane.ERROR_MESSAGE);
                }
            }
        }
    }

    public void refresh() {
        if (pagination != null)
            pagination.setCurrentPage(1);
        loadData();
    }
}
