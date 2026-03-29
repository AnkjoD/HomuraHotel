package UI.panels;

import service.*;
import entity.*;
import UI.MainFrame;
import UI.components.UIConstants;
import UI.components.WrapLayout;
import UI.dialogs.CheckinDialog;
import UI.components.DateTimePicker;
import UI.components.RoundedComponents.*;
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

    private static final String CARD_LIST = "LIST";
    private static final String CARD_SEARCH = "SEARCH";

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

        txtSearchList = new ModernTextField("🔍  Tìm khách hàng, số phòng...");
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

        String[] statuses = { "Tất cả trạng thái", "Chờ xác nhận", "Đã xác nhận", "Đã check-in", "Đã trả phòng",
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

        RoundedButton btnRefresh = new RoundedButton("Làm mới", new Color(0xE2E8F0), UIConstants.TEXT_PRIMARY);
        btnRefresh.setPreferredSize(new Dimension(115, 36));
        btnRefresh.addActionListener(e -> {
            loadDatPhongTable();
            txtSearchList.setText("");
        });

        RoundedButton btnHuy = new RoundedButton("Hủy đặt phòng", UIConstants.DANGER, Color.WHITE);
        btnHuy.setEnabled(false);
        btnHuy.setPreferredSize(new Dimension(150, 36));

        rightTools.add(btnRefresh);
        rightTools.add(btnHuy);

        toolbar.add(leftTools, BorderLayout.WEST);
        toolbar.add(rightTools, BorderLayout.EAST);

        // ---- Table ----
        String[] cols = { "_ma", "Mã đặt", "Khách hàng", "Phòng", "Loại phòng", "Check-in", "Check-out", "Số khách",
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
        tableDatPhong.getColumn("Đặt cọc").setPreferredWidth(110);
        tableDatPhong.getSelectionModel().addListSelectionListener(
                e -> btnHuy.setEnabled(tableDatPhong.getSelectedRow() >= 0));

        tableDatPhong.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2 && tableDatPhong.getSelectedRow() >= 0) {
                    int row = tableDatPhong.convertRowIndexToModel(tableDatPhong.getSelectedRow());
                    String maDat = (String) modelDatPhong.getValueAt(row, 0);
                    DatPhong dp = datPhongService.getById(maDat);
                    if (dp != null) {
                        if (dp.getTrangThai() != TrangThaiDatPhong.CONFIRMED) {
                            JOptionPane.showMessageDialog(DatPhongPanel.this,
                                    "Chỉ đơn ở trạng thái 'Đã xác nhận' mới có thể Check-in.",
                                    "Thông báo", JOptionPane.INFORMATION_MESSAGE);
                            return;
                        }
                        try {
                            showQuickCheckinDialog(dp);
                        } catch (Exception ex) {
                            JOptionPane.showMessageDialog(DatPhongPanel.this,
                                    "Không thể mở hộp thoại: " + ex.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
            }
        });

        btnHuy.addActionListener(e -> {
            int row = tableDatPhong.getSelectedRow();
            if (row < 0)
                return;
            String ma = (String) modelDatPhong.getValueAt(row, 0);
            String ten = (String) modelDatPhong.getValueAt(row, 2);
            int ok = JOptionPane.showConfirmDialog(this,
                    "Hủy đặt phòng của \"" + ten + "\"?", "Xác nhận",
                    JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (ok == JOptionPane.YES_OPTION) {
                String err = datPhongService.huyDatPhong(ma);
                if (err == null) {
                    loadDatPhongTable();
                    JOptionPane.showMessageDialog(this, "Đã hủy thành công!");
                } else
                    JOptionPane.showMessageDialog(this, "Lỗi: " + err, "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        });

        JScrollPane sp = new JScrollPane(tableDatPhong);
        sp.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER));

        JLabel footer = new JLabel("  Double-click để Check-in  |  Chọn dòng và nhấn 'Hủy đặt phòng' để hủy");
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
                    Object v = entry.getValue(9);
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

        RoundedButton btnSearch = new RoundedButton("🔍  Tìm phòng", UIConstants.PRIMARY, Color.WHITE);
        btnSearch.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btnSearch.setPreferredSize(new Dimension(150, 38));
        btnSearch.addActionListener(e -> doSearch());

        // Layout: 2 rows in the search card
        JPanel row1 = new JPanel(new GridLayout(1, 3, 14, 0));
        row1.setOpaque(false);
        row1.add(labeledComp("📅  Ngày nhận phòng *", txtNgayNhan));
        row1.add(labeledComp("📅  Ngày trả phòng *", txtNgayTra));
        row1.add(labeledComp("👥  Số khách", spnSoKhach));

        JPanel row2 = new JPanel(new GridLayout(1, 4, 14, 0));
        row2.setOpaque(false);
        row2.add(labeledComp("🏢  Tầng", cboTang));
        row2.add(labeledComp("🌅  Hướng view", cboView));
        row2.add(labeledComp("🛏  Loại phòng", cboLoaiPhong));

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
            boolean matchCap = ph.getSucChua() >= soKhach;
            if (matchTang && matchView && matchType && matchCap)
                rooms.add(ph);
        }
        showResults(rooms, checkIn, checkOut);
    }

    private void showResults(java.util.List<Phong> rooms, Date checkIn, Date checkOut) {
        resultPanel.removeAll();
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

            JLabel cnt = new JLabel("✅  Tìm thấy " + rooms.size() + " phòng trống phù hợp");
            cnt.setFont(UIConstants.FONT_BODY_BOLD);
            cnt.setForeground(UIConstants.SUCCESS);

            long soNgay = new ThuePhongService().tinhSoNgay(checkIn, checkOut);
            JLabel nights = new JLabel(soNgay + " đêm");
            nights.setFont(UIConstants.FONT_SMALL);
            nights.setForeground(UIConstants.TEXT_MUTED);

            summary.add(cnt, BorderLayout.WEST);
            summary.add(nights, BorderLayout.EAST);

            JPanel grid = new JPanel(new WrapLayout(FlowLayout.LEFT, 12, 10));
            grid.setOpaque(false);
            for (Phong ph : rooms)
                grid.add(buildRoomCard(ph, soNgay, checkIn, checkOut));

            JScrollPane sp = new JScrollPane(grid);
            sp.setOpaque(false);
            sp.getViewport().setOpaque(false);
            sp.setBorder(null);

            resultPanel.add(summary, BorderLayout.NORTH);
            resultPanel.add(sp, BorderLayout.CENTER);
        }
        resultPanel.revalidate();
        resultPanel.repaint();
    }

    private RoundedPanel buildRoomCard(Phong p, long soNgay, Date checkIn, Date checkOut) {
        RoundedPanel card = new RoundedPanel(UIConstants.CARD_RADIUS);
        card.setBackground(Color.WHITE);
        card.setShadow(true);
        card.setLayout(new BorderLayout(0, 6));
        card.setPreferredSize(new Dimension(230, 188));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UIConstants.BORDER, 1),
                BorderFactory.createEmptyBorder(14, 16, 14, 16)));

        // Top: room number + view badge
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        JLabel lRoom = new JLabel("P." + p.getSoPhong());
        lRoom.setFont(new Font("Segoe UI", Font.BOLD, 18));

        // View badge
        JLabel lView = new JLabel(p.getView() != null ? p.getView() : "");
        lView.setFont(UIConstants.FONT_TINY);
        lView.setForeground(UIConstants.PRIMARY);
        lView.setBackground(UIConstants.PRIMARY_LIGHT);
        lView.setOpaque(true);
        lView.setBorder(BorderFactory.createEmptyBorder(2, 7, 2, 7));
        topRow.add(lRoom, BorderLayout.WEST);
        topRow.add(lView, BorderLayout.EAST);

        JLabel lType = new JLabel(p.getTenLoaiPhong() != null ? p.getTenLoaiPhong() : "");
        lType.setFont(UIConstants.FONT_SMALL);
        lType.setForeground(UIConstants.TEXT_SECONDARY);

        JLabel lFloor = new JLabel("🏢 Tầng " + p.getTang() + "   👥 " + p.getSucChua() + " người tối đa");
        lFloor.setFont(UIConstants.FONT_SMALL);
        lFloor.setForeground(UIConstants.TEXT_MUTED);

        // Price
        JLabel lPrice = new JLabel(String.format("%,.0fđ / đêm", p.getGiaTheoNgay()));
        lPrice.setFont(UIConstants.FONT_BODY_BOLD);
        lPrice.setForeground(UIConstants.SUCCESS);

        JLabel lTotal = new JLabel(String.format("→ Tổng %,.0fđ (%d đêm)", p.getGiaTheoNgay() * soNgay, soNgay));
        lTotal.setFont(UIConstants.FONT_SMALL);
        lTotal.setForeground(UIConstants.TEXT_MUTED);

        RoundedButton btnDat = new RoundedButton("Đặt phòng này", UIConstants.PRIMARY, Color.WHITE);
        btnDat.setFont(UIConstants.FONT_SMALL_BOLD);
        btnDat.addActionListener(e -> showDatPhongDialog(p, checkIn, checkOut));

        // Hover effect
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBackground(UIConstants.PRIMARY_LIGHT);
                card.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBackground(Color.WHITE);
                card.repaint();
            }
        });

        JPanel info = new JPanel();
        info.setOpaque(false);
        info.setLayout(new BoxLayout(info, BoxLayout.Y_AXIS));
        info.add(topRow);
        info.add(Box.createVerticalStrut(2));
        info.add(lType);
        info.add(lFloor);
        info.add(Box.createVerticalStrut(6));
        info.add(lPrice);
        info.add(lTotal);

        card.add(info, BorderLayout.CENTER);
        card.add(btnDat, BorderLayout.SOUTH);
        return card;
    }

    // ================================================================
    // DIALOG ĐẶT PHÒNG — Cải tiến: thêm số khách, cọc, ghi chú, CCCD
    // ================================================================
    private void showDatPhongDialog(Phong p, Date checkIn, Date checkOut) {
        JDialog dlg = new JDialog(
                (Frame) SwingUtilities.getWindowAncestor(this),
                "Đặt phòng — P." + p.getSoPhong(), true);
        dlg.setSize(540, 620);
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

        JLabel bannerTitle = new JLabel("Xác nhận đặt phòng P." + p.getSoPhong());
        bannerTitle.setFont(new Font("Segoe UI", Font.BOLD, 17));
        bannerTitle.setForeground(Color.WHITE);

        JPanel bannerDetails = new JPanel(new FlowLayout(FlowLayout.LEFT, 16, 0));
        bannerDetails.setOpaque(false);

        for (String info : new String[] {
                "🛏 " + (p.getTenLoaiPhong() != null ? p.getTenLoaiPhong() : ""),
                "📅 " + sdf.format(checkIn) + " → " + sdf.format(checkOut),
                "🌙 " + soNgay + " đêm",
                "💰 " + String.format("%,.0fđ/đêm", p.getGiaTheoNgay())
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

        // Section: Thông tin khách hàng
        form.add(sectionLabel("👤  Thông tin khách hàng"));
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
        JSpinner spnKhach = new JSpinner(new SpinnerNumberModel(1, 1, p.getSucChua() > 0 ? p.getSucChua() : 10, 1));
        spnKhach.setFont(UIConstants.FONT_BODY);
        row2.add(fieldGroup("CCCD / Hộ chiếu", txtCCCD));
        row2.add(fieldGroup("Số lượng khách (tối đa " + p.getSucChua() + ")", spnKhach));
        form.add(row2);
        form.add(Box.createVerticalStrut(18));

        // Section: Thanh toán
        form.add(sectionLabel("💳  Thông tin thanh toán"));
        form.add(Box.createVerticalStrut(10));

        double tongTien = p.getGiaTheoNgay() * soNgay;
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
            if (soKhach > p.getSucChua() && p.getSucChua() > 0) {
                showError(dlg, "Số khách vượt quá sức chứa phòng (" + p.getSucChua() + " người)!");
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

            NhanVien nv = new NhanVien();
            nv.setMaNhanVien(AuthService.getInstance().getCurrentMaNV());
            dp.setNhanVien(nv);

            ChiTietDatPhong ct = new ChiTietDatPhong();
            ct.setPhong(p);
            ct.setDatPhong(dp);
            ct.setGiaThucTeChot(p.getGiaTheoNgay());
            dp.getDsChiTiet().add(ct);

            String err = datPhongService.datPhong(dp);
            if (err == null) {
                showSuccessDialog(dlg, dp, p, soNgay, tienCoc);
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
    private void showSuccessDialog(JDialog parent, DatPhong dp, Phong p, long soNgay, double tienCoc) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        String msg = "<html><div style='font-family:Segoe UI; padding:4px'>"
                + "<b style='font-size:14px; color:#10B981'>✅ Đặt phòng thành công!</b><br><br>"
                + "<table cellpadding='3'>"
                + "<tr><td><b>Mã đặt:</b></td><td><b style='color:#2563EB'>" + dp.getMaDatPhong() + "</b></td></tr>"
                + "<tr><td><b>Phòng:</b></td><td>P." + p.getSoPhong() + " (" + p.getTenLoaiPhong() + ")</td></tr>"
                + "<tr><td><b>Khách:</b></td><td>" + dp.getTenKhachHang() + "</td></tr>"
                + "<tr><td><b>Số khách:</b></td><td>" + dp.getSoLuongKhach() + " người</td></tr>"
                + "<tr><td><b>Check-in:</b></td><td>"
                + (dp.getNgayNhanDK_Date() != null ? sdf.format(dp.getNgayNhanDK_Date()) : "") + "</td></tr>"
                + "<tr><td><b>Check-out:</b></td><td>"
                + (dp.getNgayTraDK_Date() != null ? sdf.format(dp.getNgayTraDK_Date()) : "") + "</td></tr>"
                + "<tr><td><b>Số đêm:</b></td><td>" + soNgay + " đêm</td></tr>"
                + "<tr><td><b>Giá/đêm:</b></td><td>" + String.format("%,.0fđ", p.getGiaTheoNgay()) + "</td></tr>"
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
            modelDatPhong.addRow(new Object[] {
                    dp.getMaDatPhong(),
                    dp.getMaDatPhong(),
                    dp.getTenKhachHang() != null ? dp.getTenKhachHang() : "—",
                    dp.getSoPhong(),
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
        if (dp.getTrangThai() != TrangThaiDatPhong.CONFIRMED) {
            JOptionPane.showMessageDialog(this, "Chỉ đơn 'Đã xác nhận' mới có thể Check-in.",
                    "Thông báo", JOptionPane.INFORMATION_MESSAGE);
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
        addInfoRow(info, "🏠 Phòng:", dp.getSoPhong() + " — " + dp.getTenLoaiPhong(), UIConstants.TEXT_PRIMARY);
        addInfoRow(info, "👤 Khách:", dp.getTenKhachHang() != null ? dp.getTenKhachHang() : "—",
                UIConstants.TEXT_PRIMARY);
        addInfoRow(info, "👥 Số khách:", dp.getSoLuongKhach() + " người", UIConstants.TEXT_PRIMARY);
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
            Phong phong = phongService.getPhongById(dp.getSoPhong());
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
            CheckinDialog ck = new CheckinDialog((Frame) SwingUtilities.getWindowAncestor(this), phong, kh, dp);
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