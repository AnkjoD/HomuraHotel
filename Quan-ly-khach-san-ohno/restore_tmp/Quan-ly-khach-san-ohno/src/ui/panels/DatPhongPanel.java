package ui.panels;

import service.*;
import entity.*;
import ui.MainFrame;
import ui.components.UIConstants;
import ui.components.WrapLayout;
import ui.dialogs.CheckinDialog;
import ui.components.DateTimePicker;
import ui.components.RoundedComponents.*;
import entity.enums.TrangThaiDatPhong;

import javax.swing.*;
import javax.swing.table.*;
import java.awt.*;
import java.text.*;
import java.util.*;
import java.awt.event.*;

public class DatPhongPanel extends JPanel {

    private final DatPhongService datPhongService = new DatPhongService();
    private final KhachHangService khService = new KhachHangService();
    private final PhongService phongService = new PhongService();
    private final BangGiaService bangGiaService = new BangGiaService();

    private CardLayout cardLayout;
    private JPanel cardContainer;

    // ---- Card 1: Danh sách đặt phòng ----
    private DefaultTableModel modelDatPhong;
    private JTable tableDatPhong;
    private ModernTextField txtSearchList;
    private JComboBox<String> cboFilterStatus;

    // ---- Card 2: Tìm & Đặt phòng ----
    private DateTimePicker txtNgayNhan, txtNgayTra;
    private JComboBox<String> cboTang, cboView, cboLoaiPhong;
    private JSpinner spnSoKhach;
    private JPanel resultPanel;
    
    private Set<Phong> selectedRooms = new LinkedHashSet<>();
    private RoundedButton btnDatNhieuPhong;
    private JLabel lblSelectedCount;

    private static final String CARD_LIST = "LIST";
    private static final String CARD_SEARCH = "SEARCH";

    private KhachHang prefilledKhachHang;

    public void prefillKhachHang(KhachHang kh) {
        this.prefilledKhachHang = kh;
        showSearchCard();
    }

    public DatPhongPanel(MainFrame mainFrame) {
        setOpaque(false);
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        cardLayout = new CardLayout();
        cardContainer = new JPanel(cardLayout);
        cardContainer.setOpaque(false);

        cardContainer.add(buildListCard(), CARD_LIST);
        cardContainer.add(buildSearchCard(), CARD_SEARCH);

        add(cardContainer, BorderLayout.CENTER);
        cardLayout.show(cardContainer, CARD_LIST);
    }

    // ================================================================
    // CARD 1 — DANH SÁCH ĐẶT PHÒNG
    // ================================================================
    private JPanel buildListCard() {
        JPanel root = new JPanel(new BorderLayout());
        root.setOpaque(false);
        root.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        // ---- Header ----
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setOpaque(false);
        hdr.setBorder(BorderFactory.createEmptyBorder(0, 0, 18, 0));

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Đặt phòng trước");
        title.setFont(UIConstants.FONT_TITLE);
        JLabel sub = new JLabel("Quản lý danh sách phòng đã đặt trước");
        sub.setFont(UIConstants.FONT_BODY);
        sub.setForeground(UIConstants.TEXT_SECONDARY);
        titleBox.add(title);
        titleBox.add(Box.createVerticalStrut(2));
        titleBox.add(sub);

        RoundedButton btnDatPhong = new RoundedButton("+ Đặt phòng", UIConstants.PRIMARY, Color.WHITE);
        btnDatPhong.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnDatPhong.setPreferredSize(new Dimension(150, 38));
        btnDatPhong.addActionListener(e -> showSearchCard());

        hdr.add(titleBox, BorderLayout.WEST);
        hdr.add(btnDatPhong, BorderLayout.EAST);

        // ---- Card trắng ----
        RoundedPanel card = new RoundedPanel(UIConstants.CARD_RADIUS);
        card.setBackground(Color.WHITE);
        card.setShadow(true);
        card.setLayout(new BorderLayout(0, 10));
        card.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        // ---- Toolbar ----
        JPanel toolbar = new JPanel(new BorderLayout(8, 0));
        toolbar.setOpaque(false);
        toolbar.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        // Left: search + status filter
        JPanel leftTools = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        leftTools.setOpaque(false);

        txtSearchList = new ModernTextField("Tìm khách hàng, số phòng...");
        txtSearchList.setPreferredSize(new Dimension(260, 36));
        txtSearchList.getDocument().addDocumentListener(new javax.swing.event.DocumentListener() {
            public void insertUpdate(javax.swing.event.DocumentEvent e) {
                filterListTable();
            }

            public void removeUpdate(javax.swing.event.DocumentEvent e) {
                filterListTable();
            }

            public void changedUpdate(javax.swing.event.DocumentEvent e) {
                filterListTable();
            }
        });

        String[] statuses = { "Tất cả trạng thái", "Chờ xác nhận", "Đã xác nhận", "Đang check-in", "Đã check-in", "Đã trả phòng",
                "Đã hủy" };
        cboFilterStatus = new JComboBox<>(statuses);
        cboFilterStatus.setFont(UIConstants.FONT_BODY);
        cboFilterStatus.setPreferredSize(new Dimension(170, 36));
        cboFilterStatus.addActionListener(e -> filterListTable());

        leftTools.add(txtSearchList);
        leftTools.add(cboFilterStatus);

        // Right: action buttons
        JPanel rightTools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        rightTools.setOpaque(false);

        toolbar.add(leftTools, BorderLayout.WEST);
        toolbar.add(rightTools, BorderLayout.EAST);

        // ---- Table ----
        String[] cols = { "_ma", "Mã đặt", "Khách hàng", "Loại", "Phòng", "Loại phòng", "Check-in", "Check-out", "Số khách",
                "Đặt cọc", "Trạng thái" };
        modelDatPhong = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        tableDatPhong = new JTable(modelDatPhong);
        styleListTable();
        loadDatPhongTable();

        tableDatPhong.getColumnModel().removeColumn(tableDatPhong.getColumnModel().getColumn(0));
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem mnuCheckin = new JMenuItem("▶ Nhận phòng");
        mnuCheckin.setFont(new Font("Segoe UI", Font.BOLD, 12));
        mnuCheckin.setForeground(UIConstants.SUCCESS);
        JMenuItem mnuHuy = new JMenuItem("Hủy đặt phòng");
        JMenuItem mnuXoa = new JMenuItem("Xóa đơn đặt");
        
        popupMenu.add(mnuCheckin);
        popupMenu.addSeparator();
        popupMenu.add(mnuHuy);
        popupMenu.add(mnuXoa);

        tableDatPhong.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) { showPopup(e); }
            @Override
            public void mousePressed(MouseEvent e) { showPopup(e); }

            private void showPopup(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    int r = tableDatPhong.rowAtPoint(e.getPoint());
                    if (r >= 0 && r < tableDatPhong.getRowCount()) {
                        tableDatPhong.setRowSelectionInterval(r, r);
                    } else {
                        tableDatPhong.clearSelection();
                    }
                    if (tableDatPhong.getSelectedRow() >= 0) {
                        popupMenu.show(e.getComponent(), e.getX(), e.getY());
                    }
                }
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (SwingUtilities.isLeftMouseButton(e) && e.getClickCount() == 2 && tableDatPhong.getSelectedRow() >= 0) {
                    int row = tableDatPhong.convertRowIndexToModel(tableDatPhong.getSelectedRow());
                    String maDat = (String) modelDatPhong.getValueAt(row, 0);
                    DatPhong dp = datPhongService.getById(maDat);
                    if (dp != null) {
                        try {
                            new ui.dialogs.DatPhongDetailDialog((Frame) SwingUtilities.getWindowAncestor(DatPhongPanel.this), dp, DatPhongPanel.this).setVisible(true);
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(DatPhongPanel.this,
                                    "Không thể mở hộp thoại: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }
        });

        mnuCheckin.addActionListener(e -> {
            int row = tableDatPhong.getSelectedRow();
            if (row < 0) return;
            String ma = (String) modelDatPhong.getValueAt(tableDatPhong.convertRowIndexToModel(row), 0);
            DatPhong dp = datPhongService.getById(ma);
            if (dp != null) {
                showQuickCheckinDialog(dp);
            }
        });

        mnuHuy.addActionListener(e -> {
            int row = tableDatPhong.getSelectedRow();
            if (row < 0) return;
            String ma = (String) modelDatPhong.getValueAt(tableDatPhong.convertRowIndexToModel(row), 0);
            String ten = (String) modelDatPhong.getValueAt(tableDatPhong.convertRowIndexToModel(row), 2);
            int ok = JOptionPane.showConfirmDialog(DatPhongPanel.this,
                    "Hủy đặt phòng của \"" + ten + "\"?", "Xác nhận",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ok == JOptionPane.YES_OPTION) {
                String err = datPhongService.huyDatPhong(ma);
                if (err == null) {
                    loadDatPhongTable();
                    JOptionPane.showMessageDialog(DatPhongPanel.this, "Đã hủy thành công!");
                } else JOptionPane.showMessageDialog(DatPhongPanel.this, "Lỗi: " + err, "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        mnuXoa.addActionListener(e -> {
            int row = tableDatPhong.getSelectedRow();
            if (row < 0) return;
            String ma = (String) modelDatPhong.getValueAt(tableDatPhong.convertRowIndexToModel(row), 0);
            String ten = (String) modelDatPhong.getValueAt(tableDatPhong.convertRowIndexToModel(row), 2);
            int ok = JOptionPane.showConfirmDialog(DatPhongPanel.this,
                    "Bạn có chắc chắn muốn xóa đơn đặt phòng của \"" + ten + "\" không?\nHành động này không thể hoàn tác!", "Xác nhận xóa",
                    JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE);
            if (ok == JOptionPane.YES_OPTION) {
                String err = datPhongService.xoaDatPhong(ma);
                if (err == null) {
                    loadDatPhongTable();
                    JOptionPane.showMessageDialog(DatPhongPanel.this, "Đã xóa đơn đặt phòng thành công!");
                } else {
                    JOptionPane.showMessageDialog(DatPhongPanel.this, err, "Lỗi", JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        JScrollPane sp = new JScrollPane(tableDatPhong);
        sp.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER));

        JLabel footer = new JLabel("  Double-click để xem chi tiết  |  Click chuột phải để Hủy/Xóa đơn đặt");
        footer.setFont(UIConstants.FONT_SMALL);
        footer.setForeground(UIConstants.TEXT_MUTED);
        footer.setBackground(Color.WHITE);
        footer.setOpaque(true);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER),
                BorderFactory.createEmptyBorder(6, 4, 6, 0)));

        card.add(toolbar, BorderLayout.NORTH);
        card.add(sp, BorderLayout.CENTER);
        card.add(footer, BorderLayout.SOUTH);

        // ★ No-show warning banner
        JPanel noShowBanner = buildNoShowBanner();

        JPanel mainContent = new JPanel(new BorderLayout(0, 10));
        mainContent.setOpaque(false);
        if (noShowBanner != null)
            mainContent.add(noShowBanner, BorderLayout.NORTH);
        mainContent.add(card, BorderLayout.CENTER);

        root.add(hdr, BorderLayout.NORTH);
        root.add(mainContent, BorderLayout.CENTER);
        return root;
    }

    private void filterListTable() {
        String kw = txtSearchList.getText().trim().toLowerCase();
        String selectedStatus = (String) cboFilterStatus.getSelectedItem();

        TableRowSorter<DefaultTableModel> sorter = new TableRowSorter<>(modelDatPhong);
        tableDatPhong.setRowSorter(sorter);
        sorter.setRowFilter(new RowFilter<DefaultTableModel, Integer>() {
            @Override
            public boolean include(Entry<? extends DefaultTableModel, ? extends Integer> entry) {
                boolean matchSearch = kw.isEmpty()
                        || entry.getStringValue(2).toLowerCase().contains(kw)
                        || entry.getStringValue(3).toLowerCase().contains(kw)
                        || entry.getStringValue(1).toLowerCase().contains(kw);

                boolean matchStatus = "Tất cả trạng thái".equals(selectedStatus);
                if (!matchStatus) {
                    Object v = entry.getValue(10);
                    TrangThaiDatPhong st = null;
                    if (v instanceof TrangThaiDatPhong) {
                        st = (TrangThaiDatPhong) v;
                    } else {
                        try {
                            st = TrangThaiDatPhong.valueOf(v != null ? v.toString() : "");
                        } catch (Exception ignored) {
                        }
                    }
                    String label = UIConstants.getTrangThaiDatPhongLabel(st);
                    matchStatus = label != null && label.contains(selectedStatus);
                }

                return matchSearch && matchStatus;
            }
        });
    }

    // ================================================================
    // CARD 2 — TÌM & ĐẶT PHÒNG (redesigned)
    // ================================================================
    private JPanel buildSearchCard() {
        JPanel root = new JPanel(new BorderLayout());
        root.setOpaque(false);
        root.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        // ---- Header ----
        JPanel hdr = new JPanel(new BorderLayout());
        hdr.setOpaque(false);
        hdr.setBorder(BorderFactory.createEmptyBorder(0, 0, 18, 0));

        JPanel titleBox = new JPanel();
        titleBox.setOpaque(false);
        titleBox.setLayout(new BoxLayout(titleBox, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Tìm phòng trống");
        title.setFont(UIConstants.FONT_TITLE);
        JLabel sub = new JLabel("Chọn ngày, số khách và bộ lọc để tìm phòng phù hợp");
        sub.setFont(UIConstants.FONT_BODY);
        sub.setForeground(UIConstants.TEXT_SECONDARY);
        titleBox.add(title);
        titleBox.add(Box.createVerticalStrut(2));
        titleBox.add(sub);

        RoundedButton btnBack = new RoundedButton("← Quay lại", new Color(0xE2E8F0), UIConstants.TEXT_PRIMARY);
        btnBack.setFont(UIConstants.FONT_BODY_BOLD);
        btnBack.setPreferredSize(new Dimension(120, 38));
        btnBack.addActionListener(e -> cardLayout.show(cardContainer, CARD_LIST));

        hdr.add(titleBox, BorderLayout.WEST);
        hdr.add(btnBack, BorderLayout.EAST);

        // ---- Search filters card ----
        RoundedPanel searchCard = new RoundedPanel(UIConstants.CARD_RADIUS);
        searchCard.setBackground(Color.WHITE);
        searchCard.setShadow(true);
        searchCard.setLayout(new BorderLayout());
        searchCard.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Row 1: date pickers + search button
        txtNgayNhan = new DateTimePicker(new Date());
        txtNgayTra = new DateTimePicker(new Date(System.currentTimeMillis() + 86_400_000L));

        // Row 2: filters
        cboTang = new JComboBox<>(new String[] { "Tất cả tầng", "Tầng 1", "Tầng 2", "Tầng 3", "Tầng 4", "Tầng 5" });
        cboTang.setFont(UIConstants.FONT_BODY);

        // Views from DB
        java.util.Set<String> views = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        phongService.getAllPhong().stream()
                .map(Phong::getView)
                .filter(v -> v != null && !v.isBlank())
                .forEach(views::add);
        String[] viewArr = new String[views.size() + 1];
        viewArr[0] = "Tất cả view";
        int vi = 1;
        for (String v : views)
            viewArr[vi++] = v;
        cboView = new JComboBox<>(viewArr);
        cboView.setFont(UIConstants.FONT_BODY);

        // Room types from DB
        java.util.Set<String> types = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        phongService.getAllPhong().stream()
                .map(Phong::getTenLoaiPhong)
                .filter(t -> t != null && !t.isBlank())
                .forEach(types::add);
        String[] typeArr = new String[types.size() + 1];
        typeArr[0] = "Tất cả loại";
        int ti = 1;
        for (String t : types)
            typeArr[ti++] = t;
        cboLoaiPhong = new JComboBox<>(typeArr);
        cboLoaiPhong.setFont(UIConstants.FONT_BODY);

        // Guest count spinner
        spnSoKhach = new JSpinner(new SpinnerNumberModel(1, 1, 10, 1));
        spnSoKhach.setFont(UIConstants.FONT_BODY);

        RoundedButton btnSearch = new RoundedButton("Tìm phòng", UIConstants.PRIMARY, Color.WHITE);
        btnSearch.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnSearch.setPreferredSize(new Dimension(150, 38));
        btnSearch.addActionListener(e -> doSearch());

        // Layout: 2 rows in the search card
        JPanel row1 = new JPanel(new GridLayout(1, 3, 14, 0));
        row1.setOpaque(false);
        row1.add(labeledComp("Ngày nhận phòng *", txtNgayNhan));
        row1.add(labeledComp("Ngày trả phòng *", txtNgayTra));
        row1.add(labeledComp("Số khách", spnSoKhach));

        JPanel row2 = new JPanel(new GridLayout(1, 4, 14, 0));
        row2.setOpaque(false);
        row2.add(labeledComp("Tầng", cboTang));
        row2.add(labeledComp("Hướng view", cboView));
        row2.add(labeledComp("Loại phòng", cboLoaiPhong));

        // Button panel (right-aligned)
        JPanel btnWrap = new JPanel(new BorderLayout());
        btnWrap.setOpaque(false);
        JLabel lbl = new JLabel(" ");
        lbl.setFont(UIConstants.FONT_SMALL_BOLD);
        btnWrap.add(lbl, BorderLayout.NORTH);
        btnWrap.add(btnSearch, BorderLayout.CENTER);
        row2.add(btnWrap);

        JPanel filterRows = new JPanel();
        filterRows.setOpaque(false);
        filterRows.setLayout(new BoxLayout(filterRows, BoxLayout.Y_AXIS));
        filterRows.add(row1);
        filterRows.add(Box.createVerticalStrut(12));

        JSeparator divider = new JSeparator();
        divider.setForeground(UIConstants.BORDER);
        divider.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        filterRows.add(divider);
        filterRows.add(Box.createVerticalStrut(12));
        filterRows.add(row2);

        searchCard.add(filterRows, BorderLayout.CENTER);

        // ---- Result panel ----
        resultPanel = new JPanel(new BorderLayout());
        resultPanel.setOpaque(false);
        showResultPlaceholder();

        JPanel body = new JPanel(new BorderLayout(0, 16));
        body.setOpaque(false);
        body.add(searchCard, BorderLayout.NORTH);
        body.add(resultPanel, BorderLayout.CENTER);

        root.add(hdr, BorderLayout.NORTH);
        root.add(body, BorderLayout.CENTER);
        return root;
    }

    private JPanel labeledComp(String label, JComponent comp) {
        JPanel p = new JPanel(new BorderLayout(0, 4));
        p.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(UIConstants.FONT_SMALL_BOLD);
        lbl.setForeground(UIConstants.TEXT_SECONDARY);
        p.add(lbl, BorderLayout.NORTH);
        p.add(comp, BorderLayout.CENTER);
        return p;
    }

    private void showResultPlaceholder() {
        resultPanel.removeAll();
        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));

        JLabel icon = new JLabel("🔍", SwingConstants.CENTER);
        icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 40));
        icon.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lbl = new JLabel("Nhập ngày nhận & ngày trả rồi nhấn Tìm phòng", SwingConstants.CENTER);
        lbl.setFont(UIConstants.FONT_BODY);
        lbl.setForeground(UIConstants.TEXT_MUTED);
        lbl.setAlignmentX(Component.CENTER_ALIGNMENT);

        center.add(Box.createVerticalGlue());
        center.add(icon);
        center.add(Box.createVerticalStrut(10));
        center.add(lbl);
        center.add(Box.createVerticalGlue());

        resultPanel.add(center, BorderLayout.CENTER);
        resultPanel.revalidate();
        resultPanel.repaint();
    }

    private void showSearchCard() {
        showResultPlaceholder();
        if (prefilledKhachHang != null) {
            // Optional: show a banner or toast message
        }
        cardLayout.show(cardContainer, CARD_SEARCH);
    }

    // ================================================================
    // TÌM PHÒNG TRỐNG
    // ================================================================
    private void doSearch() {
        Date checkIn = txtNgayNhan.getDate();
        Date checkOut = txtNgayTra.getDate();
        if (!checkOut.after(checkIn)) {
            JOptionPane.showMessageDialog(this, "Ngày trả phải sau ngày nhận!", "Lỗi", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int soKhach = (int) spnSoKhach.getValue();
        String tangSel = (String) cboTang.getSelectedItem();
        String tangFilter = (tangSel != null && !tangSel.equals("Tất cả tầng"))
                ? tangSel.replace("Tầng ", "").trim()
                : null;
        String viewSel = (String) cboView.getSelectedItem();
        String viewFilter = (viewSel != null && !viewSel.equals("Tất cả view")) ? viewSel : null;
        String typeSel = (String) cboLoaiPhong.getSelectedItem();
        String typeFilter = (typeSel != null && !typeSel.equals("Tất cả loại")) ? typeSel : null;

        java.util.List<Phong> allRooms = datPhongService.timPhongTrong(checkIn, checkOut, 1);
        java.util.List<Phong> rooms = new ArrayList<>();
        for (Phong ph : allRooms) {
            boolean matchTang = tangFilter == null || String.valueOf(ph.getTang()).equals(tangFilter);
            boolean matchView = viewFilter == null
                    || (ph.getView() != null && ph.getView().equalsIgnoreCase(viewFilter));
            boolean matchType = typeFilter == null
                    || (ph.getTenLoaiPhong() != null && ph.getTenLoaiPhong().equalsIgnoreCase(typeFilter));
            if (matchTang && matchView && matchType)
                rooms.add(ph);
        }
        showResults(rooms, checkIn, checkOut);
    }

    private void showResults(java.util.List<Phong> rooms, Date checkIn, Date checkOut) {
        resultPanel.removeAll();
        selectedRooms.clear();
        if (rooms.isEmpty()) {
            JPanel empty = new JPanel();
            empty.setOpaque(false);
            empty.setLayout(new BoxLayout(empty, BoxLayout.Y_AXIS));
            JLabel icon = new JLabel("🏨", SwingConstants.CENTER);
            icon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 36));
            icon.setAlignmentX(Component.CENTER_ALIGNMENT);
            JLabel lbl = new JLabel("Không tìm thấy phòng trống phù hợp!", SwingConstants.CENTER);
            lbl.setFont(UIConstants.FONT_BODY_BOLD);
            lbl.setForeground(UIConstants.DANGER);
            lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
            empty.add(Box.createVerticalGlue());
            empty.add(icon);
            empty.add(Box.createVerticalStrut(8));
            empty.add(lbl);
            empty.add(Box.createVerticalGlue());
            resultPanel.add(empty, BorderLayout.CENTER);
        } else {
            // Summary bar
            JPanel summary = new JPanel(new BorderLayout());
            summary.setOpaque(false);
            summary.setBorder(BorderFactory.createEmptyBorder(10, 0, 8, 0));

            JLabel cnt = new JLabel("Tìm thấy " + rooms.size() + " phòng trống phù hợp");
            cnt.setFont(UIConstants.FONT_BODY_BOLD);
            cnt.setForeground(UIConstants.SUCCESS);

            long soNgay = new ThuePhongService().tinhSoNgay(checkIn, checkOut);
            JLabel nights = new JLabel(soNgay + " đêm");
            nights.setFont(UIConstants.FONT_SMALL);
            nights.setForeground(UIConstants.TEXT_MUTED);

            summary.add(cnt, BorderLayout.WEST);
            summary.add(nights, BorderLayout.EAST);

            // Group by Floor
            Map<Integer, java.util.List<Phong>> byFloor = new TreeMap<>();
            for (Phong p : rooms) {
                byFloor.computeIfAbsent(p.getTang(), k -> new ArrayList<>()).add(p);
            }

            JPanel roomGrid = new JPanel();
            roomGrid.setOpaque(false);
            roomGrid.setLayout(new BoxLayout(roomGrid, BoxLayout.Y_AXIS));

            for (Map.Entry<Integer, java.util.List<Phong>> entry : byFloor.entrySet()) {
                roomGrid.add(buildFloorSection(entry.getKey(), entry.getValue(), soNgay, checkIn, checkOut));
                roomGrid.add(Box.createVerticalStrut(12));
            }

            JScrollPane sp = new JScrollPane(roomGrid);
            sp.setOpaque(false);
            sp.getViewport().setOpaque(false);
            sp.setBorder(null);
            sp.getVerticalScrollBar().setUnitIncrement(20);
            sp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

            // Selection Bar
            JPanel selectionBar = new JPanel(new BorderLayout());
            selectionBar.setOpaque(false);
            selectionBar.setBorder(BorderFactory.createEmptyBorder(10, 0, 0, 0));
            lblSelectedCount = new JLabel("Chưa chọn phòng nào");
            lblSelectedCount.setFont(UIConstants.FONT_BODY_BOLD);
            btnDatNhieuPhong = new RoundedButton("Đặt phòng đã chọn", UIConstants.PRIMARY, Color.WHITE);
            btnDatNhieuPhong.setEnabled(false);
            btnDatNhieuPhong.setPreferredSize(new Dimension(180, 40));
            btnDatNhieuPhong.addActionListener(e -> {
                if (!selectedRooms.isEmpty()) {
                    showDatPhongDialog(new ArrayList<>(selectedRooms), checkIn, checkOut);
                }
            });
            selectionBar.add(lblSelectedCount, BorderLayout.WEST);
            selectionBar.add(btnDatNhieuPhong, BorderLayout.EAST);

            resultPanel.add(summary, BorderLayout.NORTH);
            resultPanel.add(sp, BorderLayout.CENTER);
            resultPanel.add(selectionBar, BorderLayout.SOUTH);
        }
        resultPanel.revalidate();
        resultPanel.repaint();
    }

    private JPanel buildFloorSection(int floor, java.util.List<Phong> phongs, long soNgay, Date checkIn,
            Date checkOut) {
        JPanel section = new JPanel();
        section.setOpaque(false);
        section.setLayout(new BoxLayout(section, BoxLayout.Y_AXIS));
        section.setMaximumSize(new Dimension(Integer.MAX_VALUE, 9999));
        section.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Floor header
        JPanel floorHeader = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        floorHeader.setOpaque(false);
        floorHeader.setAlignmentX(Component.LEFT_ALIGNMENT);
        floorHeader.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));

        JLabel lblFloor = new JLabel("Tầng " + floor);
        lblFloor.setFont(UIConstants.FONT_HEADER);
        lblFloor.setForeground(UIConstants.TEXT_PRIMARY);
        JLabel lblCount = new JLabel("(" + phongs.size() + " phòng)");
        lblCount.setFont(UIConstants.FONT_SMALL);
        lblCount.setForeground(UIConstants.TEXT_MUTED);
        floorHeader.add(lblFloor);
        floorHeader.add(lblCount);

        JSeparator separator = new JSeparator(SwingConstants.HORIZONTAL);
        separator.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        separator.setForeground(new Color(200, 210, 245));
        separator.setBackground(new Color(235, 240, 255));
        separator.setAlignmentX(Component.LEFT_ALIGNMENT);

        JPanel cardsPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, 10, 10));
        cardsPanel.setOpaque(false);
        cardsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (Phong p : phongs)
            cardsPanel.add(buildRoomCard(p, soNgay, checkIn, checkOut));

        section.add(floorHeader);
        section.add(Box.createVerticalStrut(6));
        section.add(separator);
        section.add(Box.createVerticalStrut(8));
        section.add(cardsPanel);
        return section;
    }

    private RoundedPanel buildRoomCard(Phong p, long soNgay, Date checkIn, Date checkOut) {
        Color borderClr = new Color(0x10B981); // AVAILABLE color
        Color bgHover = new Color(0xECFDF5);
        String txtStatus = "Có sẵn";

        RoundedPanel card = new RoundedPanel(10);
        card.setBackground(Color.WHITE);
        card.setLayout(new BorderLayout(0, 4));
        card.setPreferredSize(new Dimension(190, 128));
        card.setMinimumSize(new Dimension(190, 128));
        card.setMaximumSize(new Dimension(190, 128));
        card.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(10, borderClr),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Top: số phòng + trạng thái
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        JLabel lblNum = new JLabel("P." + p.getSoPhong());
        lblNum.setFont(new Font("Segoe UI", Font.BOLD, 15));

        JLabel lblTT = new JLabel(txtStatus);
        lblTT.setFont(UIConstants.FONT_TINY);
        lblTT.setForeground(borderClr);

        topRow.add(lblNum, BorderLayout.WEST);
        topRow.add(lblTT, BorderLayout.EAST);

        // Middle: loại + view + khách
        JLabel lblType = new JLabel(p.getTenLoaiPhong() != null ? p.getTenLoaiPhong() : "");
        lblType.setFont(UIConstants.FONT_SMALL);
        lblType.setForeground(UIConstants.TEXT_SECONDARY);

        JLabel lblView = new JLabel(p.getView() != null ? "Hướng: " + p.getView() : "");
        lblView.setFont(UIConstants.FONT_TINY);
        lblView.setForeground(UIConstants.TEXT_MUTED);

        JLabel lblGuest = new JLabel(p.getSucChua() + " người tối đa");
        lblGuest.setFont(UIConstants.FONT_SMALL);
        lblGuest.setForeground(UIConstants.TEXT_MUTED);

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.add(lblType);
        center.add(Box.createVerticalStrut(1));
        center.add(lblView);
        center.add(Box.createVerticalStrut(3));
        center.add(lblGuest);

        // Bottom: giá
        double giaActive = bangGiaService.layGiaVaoThoiDiem(p.getLoaiPhong() != null ? p.getLoaiPhong().getMaLoaiPhong() : "", 
                           checkIn.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
        JLabel lblPrice = new JLabel(String.format("%,.0fđ/đêm", giaActive));
        lblPrice.setFont(UIConstants.FONT_SMALL_BOLD);
        lblPrice.setForeground(UIConstants.SUCCESS);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(lblPrice, BorderLayout.WEST);

        card.add(topRow, BorderLayout.NORTH);
        card.add(center, BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);

        Color selectedBorderClr = UIConstants.PRIMARY;
        Color selectedBg = new Color(0xEFF6FF);

        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1) {
                    if (selectedRooms.contains(p)) {
                        selectedRooms.remove(p);
                    } else {
                        selectedRooms.add(p);
                    }
                    updateSelectionBar();
                    card.setBackground(selectedRooms.contains(p) ? selectedBg : Color.WHITE);
                    card.setBorder(BorderFactory.createCompoundBorder(
                            new RoundedBorder(10, selectedRooms.contains(p) ? selectedBorderClr : borderClr),
                            BorderFactory.createEmptyBorder(10, 12, 10, 12)));
                    card.repaint();
                }
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                if (!selectedRooms.contains(p)) {
                    card.setBackground(bgHover);
                    card.repaint();
                }
            }

            @Override
            public void mouseExited(MouseEvent e) {
                if (!selectedRooms.contains(p)) {
                    card.setBackground(Color.WHITE);
                    card.repaint();
                }
            }
        });
        
        card.setBackground(selectedRooms.contains(p) ? selectedBg : Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(10, selectedRooms.contains(p) ? selectedBorderClr : borderClr),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));

        return card;
    }

    private void updateSelectionBar() {
        if (lblSelectedCount != null && btnDatNhieuPhong != null) {
            if (selectedRooms.isEmpty()) {
                lblSelectedCount.setText("Chưa chọn phòng nào");
                lblSelectedCount.setForeground(UIConstants.TEXT_MUTED);
                btnDatNhieuPhong.setEnabled(false);
            } else {
                lblSelectedCount.setText("Đã chọn " + selectedRooms.size() + " phòng");
                lblSelectedCount.setForeground(UIConstants.PRIMARY);
                btnDatNhieuPhong.setEnabled(true);
            }
        }
    }

    // ================================================================
    // DIALOG ĐẶT PHÒNG — Cải tiến: thêm số khách, cọc, ghi chú, CCCD
    // ================================================================
    private void showDatPhongDialog(java.util.List<Phong> selectedRoomsList, Date checkIn, Date checkOut) {
        String titleStr = selectedRoomsList.size() == 1 
                ? "P." + selectedRoomsList.get(0).getSoPhong() 
                : selectedRoomsList.size() + " phòng";
        JDialog dlg = new JDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                "Đặt phòng — " + titleStr, true);
        dlg.setSize(540, 680);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(new Color(0xF8FAFC));

        // ---- Top banner (room info) ----
        JPanel banner = new JPanel(new BorderLayout());
        banner.setBackground(UIConstants.PRIMARY);
        banner.setBorder(BorderFactory.createEmptyBorder(18, 24, 18, 24));

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        long soNgay = new ThuePhongService().tinhSoNgay(checkIn, checkOut);

        int totalSucChua = 0;
        int totalAbsMax = 0;
        double pricePerNight = 0;
        StringBuilder roomNames = new StringBuilder();
        for (Phong p : selectedRoomsList) {
            totalSucChua += p.getSucChua();
            totalAbsMax += (p.getSucChua() + 2); // Mỗi phòng cho phép ghép thêm tối đa 2 người
            double giaBook = bangGiaService.layGiaVaoThoiDiem(p.getLoaiPhong() != null ? p.getLoaiPhong().getMaLoaiPhong() : "", 
                             checkIn.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
            pricePerNight += giaBook;
            roomNames.append("P.").append(p.getSoPhong()).append(", ");
        }
        if (roomNames.length() > 2) roomNames.setLength(roomNames.length() - 2);

        JLabel bannerTitle = new JLabel("Xác nhận đặt phòng " + titleStr);
        bannerTitle.setFont(new Font("Segoe UI", Font.BOLD, 17));
        bannerTitle.setForeground(Color.WHITE);

        JPanel bannerDetails = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        bannerDetails.setOpaque(false);

        for (String info : new String[] {
                "🛏 " + roomNames.toString(),
                "📅 " + sdf.format(checkIn) + " → " + sdf.format(checkOut),
                "🌙 " + soNgay + " đêm",
                "💰 " + String.format("%,.0fđ/đêm (Tất cả)", pricePerNight)
        }) {
            JLabel lbl = new JLabel(info);
            lbl.setFont(UIConstants.FONT_SMALL);
            lbl.setForeground(new Color(255, 255, 255, 200));
            bannerDetails.add(lbl);
        }

        banner.add(bannerTitle, BorderLayout.NORTH);
        banner.add(bannerDetails, BorderLayout.CENTER);

        // ---- Form content ----
        JPanel form = new JPanel();
        form.setBackground(Color.WHITE);
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Section: Loại khách
        form.add(sectionLabel("🏷  Loại khách"));
        form.add(Box.createVerticalStrut(10));

        JPanel rowLoaiKhach = new JPanel(new GridLayout(1, 2, 12, 0));
        rowLoaiKhach.setOpaque(false);
        rowLoaiKhach.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        rowLoaiKhach.setAlignmentX(Component.LEFT_ALIGNMENT);

        JComboBox<String> cboLoaiKhach = new JComboBox<>(new String[] { "Khách lẻ (Cá nhân)", "Khách đoàn" });
        cboLoaiKhach.setFont(UIConstants.FONT_BODY);
        ModernTextField txtTenDoan = new ModernTextField("Tên đoàn / công ty...");
        txtTenDoan.setEnabled(false);
        txtTenDoan.setBackground(new Color(0xF1F5F9));
        
        ModernTextField txtGiamGia = new ModernTextField("Giảm giá (%) - VD: 10");
        txtGiamGia.setEnabled(false);
        txtGiamGia.setBackground(new Color(0xF1F5F9));

        cboLoaiKhach.addActionListener(ev -> {
            boolean isDoan = cboLoaiKhach.getSelectedIndex() == 1;
            txtTenDoan.setEnabled(isDoan);
            txtTenDoan.setBackground(isDoan ? Color.WHITE : new Color(0xF1F5F9));
            txtGiamGia.setEnabled(isDoan);
            txtGiamGia.setBackground(isDoan ? Color.WHITE : new Color(0xF1F5F9));
            if (!isDoan) {
                txtTenDoan.setText("");
                txtGiamGia.setText("");
            }
        });

        rowLoaiKhach.add(fieldGroup("Phân loại *", cboLoaiKhach));
        rowLoaiKhach.add(fieldGroup("Tên đoàn / Công ty", txtTenDoan));
        form.add(rowLoaiKhach);
        form.add(Box.createVerticalStrut(10));
        
        JPanel rowGiamGia = new JPanel(new GridLayout(1, 2, 12, 0));
        rowGiamGia.setOpaque(false);
        rowGiamGia.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        rowGiamGia.setAlignmentX(Component.LEFT_ALIGNMENT);
        rowGiamGia.add(fieldGroup("Giảm giá đoàn (%)", txtGiamGia));
        rowGiamGia.add(new JLabel("")); // empty cell
        form.add(rowGiamGia);
        form.add(Box.createVerticalStrut(18));

        // Section: Thông tin khách hàng
        form.add(sectionLabel("👤  Thông tin khách hàng (Người đại diện)"));
        form.add(Box.createVerticalStrut(10));

        // Row: Họ tên + SĐT
        JPanel row1 = new JPanel(new GridLayout(1, 2, 12, 0));
        row1.setOpaque(false);
        row1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        row1.setAlignmentX(Component.LEFT_ALIGNMENT);
        ModernTextField txtHoTen = new ModernTextField("Nhập họ và tên...");
        ModernTextField txtSDT = new ModernTextField("Nhập số điện thoại...");
        row1.add(fieldGroup("Họ và tên *", txtHoTen));
        row1.add(fieldGroup("Số điện thoại *", txtSDT));
        form.add(row1);
        form.add(Box.createVerticalStrut(10));

        // Row: CCCD + Số khách
        JPanel row2 = new JPanel(new GridLayout(1, 2, 12, 0));
        row2.setOpaque(false);
        row2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        row2.setAlignmentX(Component.LEFT_ALIGNMENT);
        ModernTextField txtCCCD = new ModernTextField("CCCD/Hộ chiếu (tùy chọn)");
        JSpinner spnKhach = new JSpinner(new SpinnerNumberModel(1, 1, totalAbsMax > 0 ? totalAbsMax : 50, 1));
        spnKhach.setFont(UIConstants.FONT_BODY);
        spnKhach.setToolTipText("Lưu ý: Nếu số khách nhập vào lớn hơn tiêu chuẩn (" + totalSucChua + "), phụ thu sẽ áp dụng khi nhận phòng.");
        row2.add(fieldGroup("CCCD / Hộ chiếu", txtCCCD));
        row2.add(fieldGroup("Số lượng khách dự kiến (Tối đa " + totalAbsMax + ")", spnKhach));
        form.add(row2);
        form.add(Box.createVerticalStrut(18));

        // Auto-fill CRM Data if available
        if (prefilledKhachHang != null) {
            txtHoTen.setText(prefilledKhachHang.getHoTen());
            txtSDT.setText(prefilledKhachHang.getSoDienThoai());
            if (prefilledKhachHang.getCccd() != null) {
                txtCCCD.setText(prefilledKhachHang.getCccd());
            }
            // Khóa lại không cho tự ý đổi tên người đặt
            txtHoTen.setEditable(false);
            txtHoTen.setBackground(new Color(0xE2E8F0));
            txtSDT.setEditable(false);
            txtSDT.setBackground(new Color(0xE2E8F0));
        }

        // Section: Thanh toán
        form.add(sectionLabel("💳  Thông tin thanh toán"));
        form.add(Box.createVerticalStrut(10));

        double tongTien = pricePerNight * soNgay;
        JPanel row3 = new JPanel(new GridLayout(1, 2, 12, 0));
        row3.setOpaque(false);
        row3.setMaximumSize(new Dimension(Integer.MAX_VALUE, 60));
        row3.setAlignmentX(Component.LEFT_ALIGNMENT);

        // Tổng tiền (readonly display)
        JTextField txtTongTien = new JTextField(String.format("%,.0fđ", tongTien));
        txtTongTien.setFont(UIConstants.FONT_BODY_BOLD);
        txtTongTien.setForeground(UIConstants.SUCCESS);
        txtTongTien.setEditable(false);
        txtTongTien.setBackground(new Color(0xF0FDF4));
        txtTongTien.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER),
                BorderFactory.createEmptyBorder(6, 10, 6, 10)));

        // Đặt cọc spinner (% of total)
        String[] cocOptions = { "Không đặt cọc", "30% (" + String.format("%,.0fđ", tongTien * 0.3) + ")",
                "50% (" + String.format("%,.0fđ", tongTien * 0.5) + ")",
                "100% (" + String.format("%,.0fđ", tongTien) + ")" };
        JComboBox<String> cboCoc = new JComboBox<>(cocOptions);
        cboCoc.setFont(UIConstants.FONT_BODY);

        row3.add(fieldGroup("Tổng tiền phòng (ước tính)", txtTongTien));
        row3.add(fieldGroup("Đặt cọc", cboCoc));
        form.add(row3);
        form.add(Box.createVerticalStrut(18));

        // Section: Ghi chú
        form.add(sectionLabel("📝  Ghi chú / Yêu cầu đặc biệt"));
        form.add(Box.createVerticalStrut(8));
        JTextArea txtGhiChu = new JTextArea(3, 0);
        txtGhiChu.setFont(UIConstants.FONT_BODY);
        txtGhiChu.setLineWrap(true);
        txtGhiChu.setWrapStyleWord(true);
        txtGhiChu.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        txtGhiChu.setBackground(Color.WHITE);
        String[] hints = { "VD: Cần giường phụ, yêu cầu tầng cao, check-in muộn...", "" };
        txtGhiChu.setText(hints[0]);
        txtGhiChu.setForeground(UIConstants.TEXT_MUTED);
        txtGhiChu.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                if (txtGhiChu.getText().equals(hints[0])) {
                    txtGhiChu.setText("");
                    txtGhiChu.setForeground(UIConstants.TEXT_PRIMARY);
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (txtGhiChu.getText().isBlank()) {
                    txtGhiChu.setText(hints[0]);
                    txtGhiChu.setForeground(UIConstants.TEXT_MUTED);
                }
            }
        });
        JScrollPane spGhiChu = new JScrollPane(txtGhiChu);
        spGhiChu.setAlignmentX(Component.LEFT_ALIGNMENT);
        spGhiChu.setMaximumSize(new Dimension(Integer.MAX_VALUE, 80));
        spGhiChu.setBorder(null);
        form.add(spGhiChu);

        // ---- Buttons ----
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btns.setBackground(new Color(0xF8FAFC));
        btns.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));

        RoundedButton btnCancel = new RoundedButton("Hủy", new Color(0xE2E8F0), UIConstants.TEXT_PRIMARY);
        btnCancel.setPreferredSize(new Dimension(90, 38));
        RoundedButton btnOk = new RoundedButton("✓  Xác nhận đặt phòng", UIConstants.PRIMARY, Color.WHITE);
        btnOk.setFont(new Font("Segoe UI", Font.BOLD, 13));
        btnOk.setPreferredSize(new Dimension(200, 38));

        btnCancel.addActionListener(e -> dlg.dispose());

        final int finalTotalAbsMax = totalAbsMax;

        btnOk.addActionListener(e -> {
            String hoTen = txtHoTen.getText().trim();
            String sdt = txtSDT.getText().trim();
            String cccd = txtCCCD.getText().trim();
            int soKhach = (int) spnKhach.getValue();
            String ghiChuText = txtGhiChu.getText().trim();
            if (ghiChuText.equals(hints[0]))
                ghiChuText = "";

            // Validate
            if (hoTen.isEmpty()) {
                showError(dlg, "Vui lòng nhập họ và tên khách hàng!");
                return;
            }
            if (sdt.isEmpty()) {
                showError(dlg, "Vui lòng nhập số điện thoại!");
                return;
            }
            if (!sdt.matches("\\d{9,11}")) {
                showError(dlg, "Số điện thoại không hợp lệ (9–11 chữ số)!");
                return;
            }
            if (soKhach > finalTotalAbsMax && finalTotalAbsMax > 0) {
                showError(dlg, "Số lượng khách dự kiến vượt quá sức chứa tối đa tuyệt đối (" + finalTotalAbsMax + " người)!");
                return;
            }

            // Tìm hoặc tạo khách hàng
            KhachHang kh = khService.getByPhone(sdt);
            if (kh == null) {
                kh = new KhachHang();
                kh.setHoTen(hoTen);
                kh.setSoDienThoai(sdt);
                kh.setCccd(cccd);
                String errKH = khService.them(kh);
                if (errKH != null) {
                    showError(dlg, "Lỗi tạo khách hàng: " + errKH);
                    return;
                }
                kh = khService.getByPhone(sdt);
            } else {
                // Update CCCD nếu có
                if (!cccd.isEmpty() && (kh.getCccd() == null || kh.getCccd().isBlank())) {
                    kh.setCccd(cccd);
                }
            }

            // Tính tiền cọc
            int cocIdx = cboCoc.getSelectedIndex();
            double[] cocRatios = { 0.0, 0.3, 0.5, 1.0 };
            double tienCoc = tongTien * cocRatios[cocIdx];

            // Tạo đặt phòng
            DatPhong dp = new DatPhong();
            dp.setKhachHang(kh);
            dp.setNgayNhanDuKien(checkIn.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
            dp.setNgayTraDuKien(checkOut.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
            dp.setSoLuongKhach(soKhach);
            dp.setTienDatCoc(tienCoc);
            dp.setGhiChu(ghiChuText.isEmpty() ? null : ghiChuText);
            dp.setTrangThai(TrangThaiDatPhong.CONFIRMED);

            // Loại khách
            boolean isDoanSelected = cboLoaiKhach.getSelectedIndex() == 1;
            dp.setLoaiKhach(isDoanSelected ? "DOAN" : "CA_NHAN");
            if (isDoanSelected) {
                String tenDoanVal = txtTenDoan.getText().trim();
                if (tenDoanVal.isEmpty()) {
                    showError(dlg, "Vui lòng nhập tên đoàn / công ty!");
                    return;
                }
                dp.setTenDoan(tenDoanVal);
            }

            NhanVien nv = new NhanVien();
            nv.setMaNhanVien(AuthService.getInstance().getCurrentMaNV());
            dp.setNhanVien(nv);

            double discountDoan = 0;
            if (isDoanSelected) {
                try {
                    discountDoan = Double.parseDouble(txtGiamGia.getText().trim());
                } catch (NumberFormatException ignored) {}
            }

            for (Phong ph : selectedRoomsList) {
                ChiTietDatPhong ct = new ChiTietDatPhong();
                ct.setPhong(ph);
                ct.setDatPhong(dp);
                double basePrice = bangGiaService.layGiaVaoThoiDiem(ph.getLoaiPhong() != null ? ph.getLoaiPhong().getMaLoaiPhong() : "", 
                                   checkIn.toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
                if (discountDoan > 0 && discountDoan <= 100) {
                    ct.setGiaThucTeChot(basePrice * (1 - discountDoan / 100.0));
                } else {
                    ct.setGiaThucTeChot(basePrice);
                }
                dp.getDsChiTiet().add(ct);
            }

            String err = datPhongService.datPhong(dp);
            if (err == null) {
                showSuccessDialog(dlg, dp, selectedRoomsList, soNgay, tienCoc);
                dlg.dispose();
                refresh();
                cardLayout.show(cardContainer, CARD_LIST);
            } else {
                showError(dlg, "Lỗi: " + err);
            }
        });

        btns.add(btnCancel);
        btns.add(btnOk);

        JScrollPane formScroll = new JScrollPane(form);
        formScroll.setBorder(null);
        formScroll.getVerticalScrollBar().setUnitIncrement(10);

        root.add(banner, BorderLayout.NORTH);
        root.add(formScroll, BorderLayout.CENTER);
        root.add(btns, BorderLayout.SOUTH);

        dlg.setContentPane(root);
        dlg.setVisible(true);
    }

    // Show success notification with booking summary
    private void showSuccessDialog(JDialog parent, DatPhong dp, java.util.List<Phong> selectedRoomsList, long soNgay, double tienCoc) {
        StringBuilder roomNames = new StringBuilder();
        double pricePerNight = 0;
        for (Phong p : selectedRoomsList) {
            roomNames.append("P.").append(p.getSoPhong()).append(", ");
            pricePerNight += p.getGiaTheoNgay();
        }
        if (roomNames.length() > 2) roomNames.setLength(roomNames.length() - 2);

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        String msg = "<html><div style='font-family:Segoe UI; padding:4px'>"
                + "<b style='font-size:14px; color:#10B981'>✅ Đặt phòng thành công!</b><br><br>"
                + "<table cellpadding='3'>"
                + "<tr><td><b>Mã đặt:</b></td><td><b style='color:#2563EB'>" + dp.getMaDatPhong() + "</b></td></tr>"
                + "<tr><td><b>Phòng:</b></td><td>" + roomNames.toString() + "</td></tr>"
                + "<tr><td><b>Khách:</b></td><td>" + dp.getTenKhachHang() + "</td></tr>"
                + "<tr><td><b>Loại khách:</b></td><td style='color:" + (dp.isDoan() ? "#7C3AED" : "#0EA5E9") + "'>" + dp.getLoaiKhachLabel()
                + (dp.isDoan() && dp.getTenDoan() != null ? " — " + dp.getTenDoan() : "") + "</td></tr>"
                + "<tr><td><b>Số khách:</b></td><td>" + dp.getSoLuongKhach() + " người</td></tr>"
                + "<tr><td><b>Check-in:</b></td><td>"
                + (dp.getNgayNhanDK_Date() != null ? sdf.format(dp.getNgayNhanDK_Date()) : "") + "</td></tr>"
                + "<tr><td><b>Check-out:</b></td><td>"
                + (dp.getNgayTraDK_Date() != null ? sdf.format(dp.getNgayTraDK_Date()) : "") + "</td></tr>"
                + "<tr><td><b>Số đêm:</b></td><td>" + soNgay + " đêm</td></tr>"
                + "<tr><td><b>Giá/đêm:</b></td><td>" + String.format("%,.0fđ (Tất cả)", pricePerNight) + "</td></tr>"
                + (tienCoc > 0
                        ? "<tr><td><b>Tiền cọc:</b></td><td style='color:#F59E0B'>" + String.format("%,.0fđ", tienCoc)
                                + "</td></tr>"
                        : "")
                + (dp.getGhiChu() != null ? "<tr><td><b>Ghi chú:</b></td><td>" + dp.getGhiChu() + "</td></tr>" : "")
                + "</table></div></html>";
        JOptionPane.showMessageDialog(parent, msg, "Đặt phòng thành công", JOptionPane.INFORMATION_MESSAGE);
    }

    private JPanel fieldGroup(String label, JComponent comp) {
        JPanel g = new JPanel(new BorderLayout(0, 4));
        g.setOpaque(false);
        JLabel lbl = new JLabel(label);
        lbl.setFont(UIConstants.FONT_SMALL_BOLD);
        lbl.setForeground(UIConstants.TEXT_SECONDARY);
        g.add(lbl, BorderLayout.NORTH);
        g.add(comp, BorderLayout.CENTER);
        return g;
    }

    private JLabel sectionLabel(String text) {
        JLabel lbl = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UIConstants.PRIMARY_LIGHT);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 6, 6);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lbl.setFont(UIConstants.FONT_SMALL_BOLD);
        lbl.setForeground(UIConstants.PRIMARY);
        lbl.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        lbl.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        return lbl;
    }

    private void showError(JDialog parent, String msg) {
        JOptionPane.showMessageDialog(parent, msg, "Lỗi", JOptionPane.WARNING_MESSAGE);
    }

    // ================================================================
    // TABLE STYLE & LOAD
    // ================================================================
    private void styleListTable() {
        tableDatPhong.setFont(UIConstants.FONT_BODY);
        tableDatPhong.setRowHeight(40);
        tableDatPhong.setShowGrid(false);
        tableDatPhong.setIntercellSpacing(new Dimension(0, 0));
        tableDatPhong.setBackground(Color.WHITE);
        tableDatPhong.setSelectionBackground(UIConstants.PRIMARY_LIGHT);
        tableDatPhong.getTableHeader().setFont(UIConstants.FONT_SMALL_BOLD);
        tableDatPhong.getTableHeader().setBackground(UIConstants.BG_TABLE_HEADER);
        tableDatPhong.getTableHeader().setForeground(UIConstants.TEXT_SECONDARY);
        tableDatPhong.getTableHeader().setBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.BORDER));

        tableDatPhong.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable t, Object v, boolean sel, boolean foc, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, v, sel, foc, row, col);
                lbl.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                lbl.setBackground(sel ? UIConstants.PRIMARY_LIGHT
                        : (row % 2 == 0 ? Color.WHITE : new Color(0xFAFAFA)));
                lbl.setFont(UIConstants.FONT_BODY);
                lbl.setForeground(UIConstants.TEXT_PRIMARY);

                if (col == 2) { // Loại khách column
                    String val = v != null ? v.toString() : "Khách lẻ";
                    lbl.setText(val);
                    boolean isDoan = val.contains("đoàn");
                    lbl.setForeground(isDoan ? new Color(0x7C3AED) : new Color(0x0EA5E9));
                    lbl.setFont(UIConstants.FONT_SMALL_BOLD);
                }

                if (col == 8) { // Đặt cọc column
                    String val = v != null ? v.toString() : "0đ";
                    lbl.setText(val);
                    lbl.setForeground("0đ".equals(val) || "0,0đ".equals(val)
                            ? UIConstants.TEXT_MUTED
                            : UIConstants.WARNING);
                    lbl.setFont(UIConstants.FONT_SMALL_BOLD);
                }

                if (col == 9) { // Trạng thái column
                    TrangThaiDatPhong st = null;
                    if (v instanceof TrangThaiDatPhong) {
                        st = (TrangThaiDatPhong) v;
                    } else {
                        try {
                            st = TrangThaiDatPhong.valueOf(v != null ? v.toString() : "");
                        } catch (Exception ignored) {
                        }
                    }
                    lbl.setText(UIConstants.getTrangThaiDatPhongLabel(st));
                    lbl.setForeground(UIConstants.getTrangThaiDatPhongColor(st));
                    lbl.setFont(UIConstants.FONT_SMALL_BOLD);
                }
                return lbl;
            }
        });
    }

    private void loadDatPhongTable() {
        modelDatPhong.setRowCount(0);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        for (DatPhong dp : datPhongService.getAll()) {
            String loaiLabel = dp.getLoaiKhachLabel();
            if (dp.isDoan() && dp.getTenDoan() != null && !dp.getTenDoan().isEmpty()) {
                loaiLabel += " (" + dp.getTenDoan() + ")";
            }
            modelDatPhong.addRow(new Object[] {
                    dp.getMaDatPhong(),
                    dp.getMaDatPhong(),
                    dp.getTenKhachHang() != null ? dp.getTenKhachHang() : "—",
                    loaiLabel,
                    dp.getDanhSachTenPhong(),
                    dp.getTenLoaiPhong() != null ? dp.getTenLoaiPhong() : "",
                    dp.getNgayNhanDK() != null ? sdf.format(dp.getNgayNhanDK_Date()) : "",
                    dp.getNgayTraDK() != null ? sdf.format(dp.getNgayTraDK_Date()) : "",
                    dp.getSoLuongKhach(),
                    dp.getTienDatCoc() > 0
                            ? String.format("%,.0fđ", dp.getTienDatCoc())
                            : "Không cọc",
                    dp.getTrangThai()
            });
        }
    }

    // ================================================================
    // QUICK CHECK-IN DIALOG
    // ================================================================
    private void showQuickCheckinDialog(DatPhong dp) {
        if (dp == null)
            return;
        if (dp.getTrangThai() != TrangThaiDatPhong.CONFIRMED && 
            dp.getTrangThai() != TrangThaiDatPhong.PARTIALLY_CHECKED_IN &&
            dp.getTrangThai() != TrangThaiDatPhong.CHECKED_IN) {
            JOptionPane.showMessageDialog(this, "Chỉ đơn 'Đã xác nhận' hoặc 'Đang check-in' mới có thể tiếp tục Check-in.",
                    "Thông báo", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // --- GROUP CHECK-IN HANDLING ---
        if (dp.getDsChiTiet() != null && dp.getDsChiTiet().size() > 1) {
            JDialog mapDlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this), "Chọn phòng để Check-in", true);
            mapDlg.setSize(400, Math.min(600, 100 + dp.getDsChiTiet().size() * 50));
            mapDlg.setLocationRelativeTo(this);
            JPanel mapPanel = new JPanel();
            mapPanel.setLayout(new BoxLayout(mapPanel, BoxLayout.Y_AXIS));
            mapPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
            mapPanel.setBackground(Color.WHITE);
            
            JLabel lblGuide = new JLabel("Đơn đặt này có " + dp.getDsChiTiet().size() + " phòng. Chọn phòng để xử lý:");
            lblGuide.setFont(UIConstants.FONT_BODY_BOLD);
            lblGuide.setAlignmentX(Component.CENTER_ALIGNMENT);
            mapPanel.add(lblGuide);
            mapPanel.add(Box.createVerticalStrut(15));
            
            for (ChiTietDatPhong ct : dp.getDsChiTiet()) {
                Phong p = ct.getPhong();
                RoundedButton btnRoom = new RoundedButton("P." + p.getSoPhong() + " (" + p.getTenLoaiPhong() + ")", UIConstants.PRIMARY, Color.WHITE);
                btnRoom.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));
                btnRoom.setAlignmentX(Component.CENTER_ALIGNMENT);
                
                if (p.getTrangThai() == entity.enums.TrangThaiPhong.OCCUPIED) {
                     btnRoom.setText("P." + p.getSoPhong() + " - Đã nhận phòng");
                     btnRoom.setBackground(new Color(156, 163, 175)); // Khóa nút
                } else {
                     btnRoom.addActionListener(ev -> {
                         mapDlg.dispose();
                         KhachHang kh = (dp.getMaKH() != null) ? khService.getById(dp.getMaKH()) : null;
                         ui.dialogs.CheckinDialog ck = new ui.dialogs.CheckinDialog((Frame) SwingUtilities.getWindowAncestor(this), p, kh, dp);
                         ck.setVisible(true);
                         if (ck.isConfirmed()) refresh();
                     });
                }
                mapPanel.add(btnRoom);
                mapPanel.add(Box.createVerticalStrut(10));
            }
            mapDlg.add(new JScrollPane(mapPanel));
            mapDlg.setVisible(true);
            return;
        }

        JDialog dlg = new JDialog((Frame) SwingUtilities.getWindowAncestor(this),
                "Check-in — " + dp.getMaDatPhong(), true);
        dlg.setSize(440, 300);
        dlg.setLocationRelativeTo(this);
        dlg.setResizable(false);

        JPanel container = new JPanel(new BorderLayout(0, 12));
        container.setBackground(Color.WHITE);
        container.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));

        // Info section
        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));

        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        addInfoRow(info, "🕐 Thời gian hiện tại:", sdf.format(new Date()), UIConstants.TEXT_PRIMARY);
        // Fallback cho khách lẻ
        String phongDisplay = dp.getSoPhong();
        if (dp.getDsChiTiet() != null && !dp.getDsChiTiet().isEmpty() && dp.getDsChiTiet().get(0).getPhong() != null) {
            phongDisplay = dp.getDsChiTiet().get(0).getPhong().getMaPhong();
        }
        addInfoRow(info, "🏠 Phòng:", phongDisplay + " — " + dp.getTenLoaiPhong(), UIConstants.TEXT_PRIMARY);
        addInfoRow(info, "👤 Khách:", dp.getTenKhachHang() != null ? dp.getTenKhachHang() : "—",
                UIConstants.TEXT_PRIMARY);
        addInfoRow(info, "👥 Số lượng khách:", dp.getSoLuongKhach() + " người", UIConstants.TEXT_PRIMARY);
        addInfoRow(info, "📅 Check-in dự kiến:",
                dp.getNgayNhanDK_Date() != null
                        ? new SimpleDateFormat("dd/MM/yyyy HH:mm").format(dp.getNgayNhanDK_Date())
                        : "—",
                UIConstants.WARNING);
        if (dp.getGhiChu() != null && !dp.getGhiChu().isBlank()) {
            addInfoRow(info, "📝 Ghi chú:", dp.getGhiChu(), UIConstants.INFO);
        }

        container.add(info, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        btnPanel.setOpaque(false);
        RoundedButton btnCancel = new RoundedButton("Hủy đơn", UIConstants.DANGER, Color.WHITE);
        RoundedButton btnCheckin = new RoundedButton("▶ Check-in ngay", UIConstants.SUCCESS, Color.WHITE);
        btnCheckin.setFont(new Font("Segoe UI", Font.BOLD, 13));

        btnCancel.addActionListener(e -> {
            int ok = JOptionPane.showConfirmDialog(dlg,
                    "Hủy đơn đặt phòng " + dp.getMaDatPhong() + "?", "Xác nhận",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ok != JOptionPane.YES_OPTION)
                return;
            String err = datPhongService.huyDatPhong(dp.getMaDatPhong());
            if (err != null) {
                JOptionPane.showMessageDialog(dlg, "Lỗi: " + err, "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            JOptionPane.showMessageDialog(dlg, "Đã hủy đơn thành công.");
            dlg.dispose();
            refresh();
        });

        btnCheckin.addActionListener(e -> {
            Phong phong = null;
            if (dp.getDsChiTiet() != null && !dp.getDsChiTiet().isEmpty()) {
                phong = phongService.getPhongById(dp.getDsChiTiet().get(0).getPhong().getMaPhong());
            } else {
                phong = phongService.getPhongById(dp.getSoPhong());
            }
            if (phong == null) {
                JOptionPane.showMessageDialog(dlg, "Không tìm thấy thông tin phòng!", "Lỗi", JOptionPane.ERROR_MESSAGE);
                return;
            }
            String status = phong.getTrangThaiString();
            if (!"Có sẵn".equals(status) && !"Đã đặt".equals(status)) {
                JOptionPane.showMessageDialog(dlg, "Phòng hiện không trống. Trạng thái: " + status,
                        "Không thể Check-in", JOptionPane.WARNING_MESSAGE);
                return;
            }
            KhachHang kh = (dp.getMaKH() != null) ? khService.getById(dp.getMaKH()) : null;
            dlg.dispose();
            ui.dialogs.CheckinDialog ck = new ui.dialogs.CheckinDialog((Frame) SwingUtilities.getWindowAncestor(this), phong, kh, dp);
            ck.setVisible(true);
            if (ck.isConfirmed())
                refresh();
        });

        btnPanel.add(btnCancel);
        btnPanel.add(btnCheckin);
        container.add(btnPanel, BorderLayout.SOUTH);

        dlg.setContentPane(container);
        dlg.setVisible(true);
    }

    private void addInfoRow(JPanel panel, String key, String val, Color valColor) {
        JPanel row = new JPanel(new BorderLayout(8, 0));
        row.setOpaque(false);
        row.setAlignmentX(Component.LEFT_ALIGNMENT);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 26));
        JLabel k = new JLabel(key);
        k.setFont(UIConstants.FONT_SMALL_BOLD);
        k.setPreferredSize(new Dimension(160, 22));
        JLabel v = new JLabel(val);
        v.setFont(UIConstants.FONT_BODY);
        v.setForeground(valColor);
        row.add(k, BorderLayout.WEST);
        row.add(v, BorderLayout.CENTER);
        panel.add(row);
        panel.add(Box.createVerticalStrut(4));
    }

    // ★ No-show warning banner
    private JPanel buildNoShowBanner() {
        java.util.List<DatPhong> noShows = datPhongService.getNoShowBookings(2); // 2 giờ quá hạn
        if (noShows.isEmpty())
            return null;

        JPanel banner = new JPanel(new BorderLayout(10, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0xFEF3C7));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 10, 10);
                g2.setColor(new Color(0xF59E0B, true));
                g2.setStroke(new BasicStroke(1));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
            }
        };
        banner.setOpaque(false);
        banner.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        JLabel lblIcon = new JLabel("⚠️");
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 18));

        StringBuilder sb = new StringBuilder(
                "<html><b style='color:#92400E'>No-show: " + noShows.size() + " đơn quá hạn check-in</b><br>");
        sb.append("<span style='color:#78716C; font-size:10px'>");
        for (int i = 0; i < Math.min(noShows.size(), 3); i++) {
            DatPhong d = noShows.get(i);
            sb.append("• ").append(d.getMaDatPhong());
            sb.append(" — ").append(d.getTenKhachHang() != null ? d.getTenKhachHang() : "—");
            sb.append(" (P.").append(d.getSoPhong()).append(")");
            if (i < Math.min(noShows.size(), 3) - 1)
                sb.append("<br>");
        }
        if (noShows.size() > 3)
            sb.append("<br>... và ").append(noShows.size() - 3).append(" đơn khác");
        sb.append("</span></html>");

        JLabel lblText = new JLabel(sb.toString());
        lblText.setFont(UIConstants.FONT_SMALL);

        RoundedButton btnMarkAll = new RoundedButton("Đánh dấu No-show", UIConstants.WARNING, Color.WHITE);
        btnMarkAll.setFont(UIConstants.FONT_SMALL_BOLD);
        btnMarkAll.setPreferredSize(new Dimension(155, 30));
        btnMarkAll.addActionListener(e -> {
            int ok = JOptionPane.showConfirmDialog(this,
                    "Đánh dấu " + noShows.size() + " đơn quá hạn là No-show?\nCác đơn này sẽ bị hủy.",
                    "Xác nhận No-show", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ok != JOptionPane.YES_OPTION)
                return;
            int success = 0;
            for (DatPhong d : noShows) {
                if (datPhongService.markNoShow(d.getMaDatPhong()) == null)
                    success++;
            }
            JOptionPane.showMessageDialog(this,
                    "Đã xử lý " + success + "/" + noShows.size() + " đơn No-show.",
                    "Hoàn tất", JOptionPane.INFORMATION_MESSAGE);
            refresh();
        });

        banner.add(lblIcon, BorderLayout.WEST);
        banner.add(lblText, BorderLayout.CENTER);
        banner.add(btnMarkAll, BorderLayout.EAST);
        return banner;
    }

    public void refresh() {
        removeAll();
        buildUI();
        revalidate();
        repaint();
    }
}
