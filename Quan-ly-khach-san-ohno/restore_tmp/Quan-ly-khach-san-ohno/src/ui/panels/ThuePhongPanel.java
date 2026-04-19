package ui.panels;

import ui.MainFrame;
import ui.components.*;
import ui.components.RoundedComponents.RoundedBorder;
import ui.components.RoundedComponents.RoundedButton;
import ui.components.RoundedComponents.RoundedPanel;
import ui.dialogs.*;
import dao.*;
import entity.*;
import entity.enums.TrangThaiPhong;
import java.awt.*;
import java.awt.event.*;
import java.time.LocalDateTime;
import java.util.*;
import javax.swing.*;

import service.*;

public class ThuePhongPanel extends JPanel {

    private static final java.util.logging.Logger LOGGER = java.util.logging.Logger
            .getLogger(ThuePhongPanel.class.getName());

    private final MainFrame mainFrame;
    private final PhongService phongService = new PhongService();
    private final ThuePhongService thuePhongService = new ThuePhongService();
    private final BangGiaService bangGiaService = new BangGiaService();

    private JPanel roomGrid;
    private String currentFilter = "ALL";
    private int currentFloor = 0; // 0 = tat ca
    private String currentType = "ALL"; // ALL = tat ca
    private String currentView = "ALL"; // ALL = tat ca
    private String searchRoom = ""; // tim so phong
    private JPanel filterTabsPanel;
    // ★ Map phòng đã đặt trước: maPhong -> [maDatPhong, tenKH, ngayNhan]
    private Map<String, String[]> reservedRooms = new HashMap<>();

    public ThuePhongPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        setOpaque(false);
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.setBorder(BorderFactory.createEmptyBorder(24, 28, 16, 28));

        main.add(buildHeader(), BorderLayout.NORTH);

        // Filter + Scroll container
        JPanel filterAndScroll = new JPanel(new BorderLayout(0, 12));
        filterAndScroll.setOpaque(false);
        filterAndScroll.add(buildFilterTabs(), BorderLayout.NORTH);

        // Room grid inside scroll pane.
        // IMPORTANT: implement Scrollable so the JScrollPane viewport constrains
        // the panel's width — without this, WrapLayout sees an unbounded width
        // and never wraps cards to the next row.
        roomGrid = new ScrollablePanel();
        roomGrid.setOpaque(false);
        refreshGrid();

        JScrollPane scroll = new JScrollPane(roomGrid);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(20);
        // Hide horizontal scrollbar — wrapping handles overflow
        scroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

        filterAndScroll.add(scroll, BorderLayout.CENTER);
        main.add(filterAndScroll, BorderLayout.CENTER);

        setLayout(new BorderLayout());
        add(main, BorderLayout.CENTER);
    }

    // ---- Header ----
    private JPanel buildHeader() {
        JPanel panel = new JPanel(new BorderLayout());
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        JLabel title = new JLabel("Sơ đồ phòng");
        title.setFont(UIConstants.FONT_TITLE);
        JLabel sub = new JLabel("Click vào phòng để nhận/trả phòng, chuột phải để xem thêm");
        sub.setFont(UIConstants.FONT_BODY);
        sub.setForeground(UIConstants.TEXT_SECONDARY);
        left.add(title);
        left.add(Box.createVerticalStrut(2));
        left.add(sub);

        RoundedButton btnCheckin = new RoundedButton("+ Nhận phòng", UIConstants.PRIMARY, Color.WHITE);
        btnCheckin.addActionListener(e -> showCheckinDialog(null));

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        right.setOpaque(false);
        right.add(btnCheckin);

        panel.add(left, BorderLayout.WEST);
        panel.add(right, BorderLayout.EAST);
        return panel;
    }

    // ---- Filter bar (Compact clean design) ----
    private JPanel buildFilterTabs() {
        filterTabsPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(Color.WHITE);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 16, 16);
                // Subtle bottom shadow
                g2.setColor(new Color(0, 0, 0, 12));
                g2.fillRoundRect(0, getHeight() - 2, getWidth(), 2, 2, 2);
                g2.dispose();
            }
        };
        filterTabsPanel.setOpaque(false);
        filterTabsPanel.setLayout(new BoxLayout(filterTabsPanel, BoxLayout.Y_AXIS));
        filterTabsPanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        java.util.List<Phong> allPhong = phongService.getAllPhong();

        // === Row 1: Status chips + Search ===
        JPanel row1 = new JPanel(new BorderLayout(12, 0));
        row1.setOpaque(false);
        row1.setAlignmentX(Component.LEFT_ALIGNMENT);
        row1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 40));

        // Status chips (left side)
        JPanel statusPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        statusPanel.setOpaque(false);

        int total = allPhong.size();
        int available = (int) allPhong.stream().filter(p -> p.getTrangThai() == TrangThaiPhong.AVAILABLE).count();
        int occupied = (int) allPhong.stream().filter(p -> p.getTrangThai() == TrangThaiPhong.OCCUPIED).count();
        int cleaning = (int) allPhong.stream().filter(p -> p.getTrangThai() == TrangThaiPhong.CLEANING).count();
        int maint = (int) allPhong.stream().filter(p -> p.getTrangThai() == TrangThaiPhong.MAINTENANCE).count();

        statusPanel.add(statusChip("Tất cả", "ALL", total, new Color(0x64748B)));
        statusPanel.add(statusChip("Có sẵn", "AVAILABLE", available, new Color(0x10B981)));
        statusPanel.add(statusChip("Đang thuê", "OCCUPIED", occupied, new Color(0x3B82F6)));
        statusPanel.add(statusChip("Vệ sinh", "CLEANING", cleaning, new Color(0xF59E0B)));
        statusPanel.add(statusChip("Bảo trì", "MAINTENANCE", maint, new Color(0xEF4444)));

        row1.add(statusPanel, BorderLayout.WEST);

        // Search field (right side) - rounded with icon
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        searchPanel.setOpaque(false);

        JPanel searchBox = new JPanel(new BorderLayout(6, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(new Color(0xF8FAFC));
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 20, 20);
                g2.setColor(new Color(0xE2E8F0));
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 20, 20);
                g2.dispose();
            }
        };
        searchBox.setOpaque(false);
        searchBox.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        searchBox.setPreferredSize(new Dimension(180, 32));

        JLabel searchIcon = new JLabel("🔍");
        searchIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 12));
        searchIcon.setForeground(UIConstants.TEXT_MUTED);

        JTextField txtSearch = new JTextField() {
            @Override
            public void setBorder(javax.swing.border.Border border) {
                // No border for embedded text field
            }
        };
        txtSearch.setFont(UIConstants.FONT_BODY);
        txtSearch.setOpaque(false);
        txtSearch.setForeground(UIConstants.TEXT_PRIMARY);
        txtSearch.putClientProperty("JTextField.placeholderText", "Tìm số phòng...");
        // Manual placeholder
        txtSearch.setText(searchRoom.isEmpty() ? "" : searchRoom);
        txtSearch.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                searchRoom = txtSearch.getText().trim();
                refreshGrid();
            }
        });
        txtSearch.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                searchBox.repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                searchBox.repaint();
            }
        });

        searchBox.add(searchIcon, BorderLayout.WEST);
        searchBox.add(txtSearch, BorderLayout.CENTER);
        searchPanel.add(searchBox);

        row1.add(searchPanel, BorderLayout.EAST);

        // === Thin separator ===
        JPanel sepWrapper = new JPanel(new BorderLayout());
        sepWrapper.setOpaque(false);
        sepWrapper.setBorder(BorderFactory.createEmptyBorder(8, 0, 6, 0));
        sepWrapper.setAlignmentX(Component.LEFT_ALIGNMENT);
        sepWrapper.setMaximumSize(new Dimension(Integer.MAX_VALUE, 20));
        JSeparator thinSep = new JSeparator(SwingConstants.HORIZONTAL);
        thinSep.setForeground(new Color(0xF1F5F9));
        thinSep.setBackground(new Color(0xF1F5F9));
        sepWrapper.add(thinSep, BorderLayout.CENTER);

        // === Row 2: Floor chips + Type dropdown + View dropdown + Reset ===
        JPanel row2 = new JPanel(new BorderLayout(0, 0));
        row2.setOpaque(false);
        row2.setAlignmentX(Component.LEFT_ALIGNMENT);
        row2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));

        JPanel leftFilters = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        leftFilters.setOpaque(false);

        // Collect data
        Set<Integer> floors = new TreeSet<>();
        Set<String> types = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        Set<String> views = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (Phong p : allPhong) {
            floors.add(p.getTang());
            if (p.getTenLoaiPhong() != null && !p.getTenLoaiPhong().isBlank())
                types.add(p.getTenLoaiPhong());
            if (p.getView() != null && !p.getView().isBlank())
                views.add(p.getView());
        }

        // Floor label + chips
        JLabel lblFloor = new JLabel("Tầng");
        lblFloor.setFont(UIConstants.FONT_SMALL_BOLD);
        lblFloor.setForeground(UIConstants.TEXT_SECONDARY);
        lblFloor.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));
        leftFilters.add(lblFloor);

        leftFilters.add(floorChip("Tất cả", 0, currentFloor == 0));
        for (int f : floors) {
            leftFilters.add(floorChip("T" + f, f, currentFloor == f));
        }

        // Vertical separator
        leftFilters.add(Box.createHorizontalStrut(8));
        JPanel vSep1 = new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(1, 20);
            }

            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(0xE2E8F0));
                g.fillRect(0, 2, 1, getHeight() - 4);
            }
        };
        vSep1.setOpaque(false);
        leftFilters.add(vSep1);
        leftFilters.add(Box.createHorizontalStrut(8));

        // Type dropdown
        JLabel lblType = new JLabel("Loại phòng");
        lblType.setFont(UIConstants.FONT_SMALL_BOLD);
        lblType.setForeground(UIConstants.TEXT_SECONDARY);
        lblType.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));
        leftFilters.add(lblType);

        java.util.List<String> typeItems = new ArrayList<>();
        typeItems.add("Tất cả loại phòng");
        typeItems.addAll(types);
        JComboBox<String> cboType = createStyledCombo(typeItems.toArray(new String[0]));
        cboType.setSelectedItem("ALL".equals(currentType) ? "Tất cả loại phòng" : currentType);
        cboType.addActionListener(e -> {
            String selected = (String) cboType.getSelectedItem();
            currentType = "Tất cả loại phòng".equals(selected) ? "ALL" : selected;
            refreshGrid();
        });
        leftFilters.add(cboType);

        // Another vertical separator
        leftFilters.add(Box.createHorizontalStrut(8));
        JPanel vSep2 = new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(1, 20);
            }

            @Override
            protected void paintComponent(Graphics g) {
                g.setColor(new Color(0xE2E8F0));
                g.fillRect(0, 2, 1, getHeight() - 4);
            }
        };
        vSep2.setOpaque(false);
        leftFilters.add(vSep2);
        leftFilters.add(Box.createHorizontalStrut(8));

        // View dropdown
        JLabel lblView = new JLabel("Hướng");
        lblView.setFont(UIConstants.FONT_SMALL_BOLD);
        lblView.setForeground(UIConstants.TEXT_SECONDARY);
        lblView.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 4));
        leftFilters.add(lblView);

        java.util.List<String> viewItems = new ArrayList<>();
        viewItems.add("Tất cả hướng");
        viewItems.addAll(views);
        JComboBox<String> cboView = createStyledCombo(viewItems.toArray(new String[0]));
        cboView.setSelectedItem("ALL".equals(currentView) ? "Tất cả hướng" : currentView);
        cboView.addActionListener(e -> {
            String selected = (String) cboView.getSelectedItem();
            currentView = "Tất cả hướng".equals(selected) ? "ALL" : selected;
            refreshGrid();
        });
        leftFilters.add(cboView);

        row2.add(leftFilters, BorderLayout.WEST);

        // Reset button (right side, only show when filter is active)
        boolean hasActiveFilter = !currentFilter.equals("ALL") || currentFloor != 0
                || !currentType.equals("ALL") || !currentView.equals("ALL") || !searchRoom.isEmpty();
        if (hasActiveFilter) {
            JPanel resetPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
            resetPanel.setOpaque(false);

            JLabel resetBtn = new JLabel("✕ Xóa bộ lọc") {
                @Override
                protected void paintComponent(Graphics g) {
                    Graphics2D g2 = (Graphics2D) g.create();
                    g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                    g2.setColor(new Color(0xFEE2E2));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                    g2.dispose();
                    super.paintComponent(g);
                }
            };
            resetBtn.setFont(new Font("Segoe UI", Font.BOLD, 11));
            resetBtn.setForeground(new Color(0xEF4444));
            resetBtn.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
            resetBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            resetBtn.addMouseListener(new MouseAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    currentFilter = "ALL";
                    currentFloor = 0;
                    currentType = "ALL";
                    currentView = "ALL";
                    searchRoom = "";
                    rebuildFilter();
                    refreshGrid();
                }

                @Override
                public void mouseEntered(MouseEvent e) {
                    resetBtn.setForeground(new Color(0xDC2626));
                    resetBtn.repaint();
                }

                @Override
                public void mouseExited(MouseEvent e) {
                    resetBtn.setForeground(new Color(0xEF4444));
                    resetBtn.repaint();
                }
            });
            resetPanel.add(resetBtn);
            row2.add(resetPanel, BorderLayout.EAST);
        }

        filterTabsPanel.add(row1);
        filterTabsPanel.add(sepWrapper);
        filterTabsPanel.add(row2);
        return filterTabsPanel;
    }

    // Styled ComboBox with rounded appearance
    private JComboBox<String> createStyledCombo(String[] items) {
        JComboBox<String> combo = new JComboBox<>(items);
        combo.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        combo.setPreferredSize(new Dimension(160, 28));
        combo.setMaximumSize(new Dimension(160, 28));
        combo.setBackground(new Color(0xF8FAFC));
        combo.setForeground(UIConstants.TEXT_PRIMARY);
        combo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE2E8F0), 1),
                BorderFactory.createEmptyBorder(2, 6, 2, 6)));
        combo.setFocusable(false);
        combo.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Custom renderer
        combo.setRenderer(new DefaultListCellRenderer() {
            @Override
            public Component getListCellRendererComponent(JList<?> list, Object value,
                    int index, boolean isSelected, boolean cellHasFocus) {
                JLabel lbl = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
                lbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
                lbl.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
                if (isSelected) {
                    lbl.setBackground(UIConstants.PRIMARY_LIGHT);
                    lbl.setForeground(UIConstants.PRIMARY);
                } else {
                    lbl.setBackground(Color.WHITE);
                    lbl.setForeground(UIConstants.TEXT_PRIMARY);
                }
                return lbl;
            }
        });
        return combo;
    }

    // Status chip with badge count
    private JPanel statusChip(String label, String filterKey, int count, Color accent) {
        boolean active = filterKey.equals(currentFilter);
        JPanel chip = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (active) {
                    g2.setColor(accent);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                } else {
                    g2.setColor(new Color(0xF8FAFC));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), getHeight(), getHeight());
                    g2.setColor(new Color(0xE2E8F0));
                    g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, getHeight(), getHeight());
                }
                g2.dispose();
            }
        };
        chip.setOpaque(false);
        chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JLabel lblText = new JLabel(label);
        lblText.setFont(new Font("Segoe UI", Font.BOLD, 11));
        lblText.setForeground(active ? Color.WHITE : new Color(0x64748B));

        JLabel lblCount = new JLabel(String.valueOf(count));
        lblCount.setFont(new Font("Segoe UI", Font.BOLD, 10));
        if (active) {
            lblCount.setForeground(new Color(255, 255, 255, 200));
        } else {
            lblCount.setForeground(accent);
            lblCount.setOpaque(true);
            lblCount.setBackground(new Color(accent.getRed(), accent.getGreen(), accent.getBlue(), 25));
            lblCount.setBorder(BorderFactory.createEmptyBorder(1, 5, 1, 5));
        }

        chip.add(lblText);
        chip.add(lblCount);
        chip.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                currentFilter = filterKey;
                rebuildFilter();
                refreshGrid();
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                chip.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                chip.repaint();
            }
        });
        return chip;
    }

    // Floor toggle chip (compact)
    private JLabel floorChip(String text, int floor, boolean active) {
        JLabel lbl = new JLabel(text) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                if (active) {
                    g2.setColor(UIConstants.PRIMARY);
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                } else {
                    g2.setColor(new Color(0xF1F5F9));
                    g2.fillRoundRect(0, 0, getWidth(), getHeight(), 14, 14);
                }
                g2.dispose();
                super.paintComponent(g);
            }
        };
        lbl.setFont(new Font("Segoe UI", active ? Font.BOLD : Font.PLAIN, 11));
        lbl.setForeground(active ? Color.WHITE : new Color(0x64748B));
        lbl.setBorder(BorderFactory.createEmptyBorder(4, 10, 4, 10));
        lbl.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        lbl.setHorizontalAlignment(SwingConstants.CENTER);
        lbl.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                currentFloor = floor;
                rebuildFilter();
                refreshGrid();
            }
        });
        return lbl;
    }

    private void rebuildFilter() {
        java.awt.Container parent = filterTabsPanel.getParent();
        if (parent != null) {
            parent.remove(filterTabsPanel);
            parent.add(buildFilterTabs(), BorderLayout.NORTH);
            parent.revalidate();
            parent.repaint();
        }
    }

    // ---- Room grid ----
    // Card width = 190px, hgap = 10px → 5 cards × (190+10) = 1000px ≈ fits typical
    // 1280px window
    private static final int CARD_W = 190;
    private static final int CARD_H = 128;
    private static final int CARD_HGAP = 10;
    private static final int CARD_VGAP = 10;

    private void refreshGrid() {
        roomGrid.removeAll();
        roomGrid.setLayout(new BoxLayout(roomGrid, BoxLayout.Y_AXIS));

        java.util.List<Phong> allPhong = phongService.getAllPhong();

        // ★ Load reservation data for today
        try {
            reservedRooms = new PhongDAO().getReservedRoomsForDate(LocalDateTime.now());
        } catch (Exception e) {
            reservedRooms = new HashMap<>();
        }

        // Group by floor, apply filter
        Map<Integer, java.util.List<Phong>> byFloor = new TreeMap<>();
        for (Phong p : allPhong) {
            boolean showStatus = false;
            TrangThaiPhong tt = p.getTrangThai();
            if ("ALL".equals(currentFilter))
                showStatus = true;
            else {
                try {
                    TrangThaiPhong filterEnum = TrangThaiPhong.valueOf(currentFilter);
                    showStatus = (tt == filterEnum);
                } catch (Exception e) {
                    showStatus = true;
                }
            }
            boolean showFloor = (currentFloor == 0) || (p.getTang() == currentFloor);
            boolean showType = "ALL".equals(currentType)
                    || (p.getTenLoaiPhong() != null && p.getTenLoaiPhong().equalsIgnoreCase(currentType));
            boolean showSearch = searchRoom.isEmpty()
                    || (p.getSoPhong() != null && p.getSoPhong().toLowerCase().contains(searchRoom.toLowerCase()));
            boolean showView = "ALL".equals(currentView)
                    || (p.getView() != null && p.getView().equalsIgnoreCase(currentView));

            if (showStatus && showFloor && showType && showSearch && showView)
                byFloor.computeIfAbsent(p.getTang(), k -> new ArrayList<>()).add(p);
        }

        if (byFloor.isEmpty()) {
            JLabel lbl = new JLabel("Không có phòng nào phù hợp", SwingConstants.CENTER);
            lbl.setFont(UIConstants.FONT_BODY);
            lbl.setForeground(UIConstants.TEXT_MUTED);
            lbl.setAlignmentX(Component.CENTER_ALIGNMENT);
            roomGrid.add(Box.createVerticalStrut(40));
            roomGrid.add(lbl);
        } else {
            for (Map.Entry<Integer, java.util.List<Phong>> entry : byFloor.entrySet()) {
                roomGrid.add(buildFloorSection(entry.getKey(), entry.getValue()));
                roomGrid.add(Box.createVerticalStrut(12));
            }
        }

        roomGrid.revalidate();
        roomGrid.repaint();
    }

    // ---- Floor section ----
    private JPanel buildFloorSection(int floor, java.util.List<Phong> phongs) {
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

        // WrapLayout panel — cards auto-wrap based on container width
        // Each card = CARD_W px, so at ~1000px width ≈ 5 cards per row
        JPanel cardsPanel = new JPanel(new WrapLayout(FlowLayout.LEFT, CARD_HGAP, CARD_VGAP));
        cardsPanel.setOpaque(false);
        cardsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        for (Phong p : phongs)
            cardsPanel.add(buildRoomCard(p));

        section.add(floorHeader);
        section.add(Box.createVerticalStrut(6));
        section.add(separator);
        section.add(Box.createVerticalStrut(8));
        section.add(cardsPanel);
        return section;
    }

    // ---- Room card (fixed CARD_W × CARD_H so WrapLayout wraps at 5/row) ----
    private RoundedPanel buildRoomCard(Phong p) {
        TrangThaiPhong tt = p.getTrangThai();
        Color borderClr = UIConstants.getTrangThaiPhongColor(tt);
        Color bgHover = UIConstants.getTrangThaiPhongBg(tt);
        String txtStatus = UIConstants.getTrangThaiPhongLabel(tt);

        // ★ Reservation Logic Tweak (Professional PMS Style)
        boolean hasReservation = reservedRooms.containsKey(p.getSoPhong());
        String reservedInfo = "";
        if (hasReservation && tt == TrangThaiPhong.AVAILABLE) {
            borderClr = new Color(0xF59E0B); // Amber / Warning
            bgHover = new Color(0xFEF3C7);
            txtStatus = "Sắp check-in";
            String[] info = reservedRooms.get(p.getSoPhong());
            if (info.length > 2)
                reservedInfo = info[2]; // Time
        }

        RoundedPanel card = new RoundedPanel(10);
        card.setBackground(Color.WHITE);
        card.setLayout(new BorderLayout(0, 4));
        // Fixed size → WrapLayout knows exactly when to move to next row
        card.setPreferredSize(new Dimension(CARD_W, CARD_H));
        card.setMinimumSize(new Dimension(CARD_W, CARD_H));
        card.setMaximumSize(new Dimension(CARD_W, CARD_H));
        card.setBorder(BorderFactory.createCompoundBorder(
                new RoundedBorder(10, borderClr),
                BorderFactory.createEmptyBorder(10, 12, 10, 12)));
        card.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        // Top: số phòng + trạng thái
        JPanel topRow = new JPanel(new BorderLayout());
        topRow.setOpaque(false);
        JLabel lblNum = new JLabel(p.getSoPhong());
        lblNum.setFont(new Font("Segoe UI", Font.BOLD, 15));

        JLabel lblTT = new JLabel(txtStatus);
        lblTT.setFont(UIConstants.FONT_TINY);
        lblTT.setForeground(borderClr);
        if (hasReservation && tt == TrangThaiPhong.AVAILABLE) {
            lblTT.setOpaque(true);
            lblTT.setBackground(bgHover);
            lblTT.setBorder(BorderFactory.createEmptyBorder(2, 6, 2, 6));
        }
        topRow.add(lblNum, BorderLayout.WEST);
        topRow.add(lblTT, BorderLayout.EAST);

        // Middle: loại + view + khách
        JLabel lblType = new JLabel(p.getTenLoaiPhong() != null ? p.getTenLoaiPhong() : "");
        lblType.setFont(UIConstants.FONT_SMALL);
        lblType.setForeground(UIConstants.TEXT_SECONDARY);

        JLabel lblView = new JLabel(p.getView() != null ? p.getView() : "");
        lblView.setFont(UIConstants.FONT_TINY);
        lblView.setForeground(UIConstants.TEXT_MUTED);

        JLabel lblGuest = new JLabel();
        lblGuest.setFont(UIConstants.FONT_SMALL);

        if (p.getTenKhachHienTai() != null) {
            lblGuest.setText(truncate(p.getTenKhachHienTai(), 14));
            lblGuest.setForeground(UIConstants.PRIMARY);

            try {
                // Sử dụng đường dẫn tương đối để khi mang source code sang máy khác vẫn chạy được
                ImageIcon originalIcon = new ImageIcon("icon/canhan.png");
                // Cố định kích thước icon (ví dụ 14x14) cho đẹp với text
                Image scaled = originalIcon.getImage().getScaledInstance(14, 14, Image.SCALE_SMOOTH);
                lblGuest.setIcon(new ImageIcon(scaled));
            } catch (Exception ex) {
                // Đề phòng lỗi thiếu file icon, có thể bỏ trống catch này
            }
        } else {
            lblGuest.setText(p.getSucChua() + " người tối đa");
            lblGuest.setForeground(UIConstants.TEXT_MUTED);
            lblGuest.setIcon(null);
        }

        JPanel center = new JPanel();
        center.setOpaque(false);
        center.setLayout(new BoxLayout(center, BoxLayout.Y_AXIS));
        center.add(lblType);
        center.add(Box.createVerticalStrut(1));
        center.add(lblView);
        center.add(Box.createVerticalStrut(3));
        center.add(lblGuest);

        // Bottom: giá
        double giaActive = bangGiaService.layGiaHienHanh(p.getLoaiPhong() != null ? p.getLoaiPhong().getMaLoaiPhong() : "");
        JLabel lblPrice = new JLabel(String.format("%,.0fđ/đêm", giaActive));
        lblPrice.setFont(UIConstants.FONT_SMALL_BOLD);
        lblPrice.setForeground(UIConstants.SUCCESS);

        JPanel bottom = new JPanel(new BorderLayout());
        bottom.setOpaque(false);
        bottom.add(lblPrice, BorderLayout.WEST);

        // Show reservation time subtly if it exists
        if (!reservedInfo.isEmpty()) {
            JLabel lblRsvTime = new JLabel("🕒 " + reservedInfo);
            lblRsvTime.setFont(new Font("Segoe UI", Font.PLAIN, 10));
            lblRsvTime.setForeground(new Color(0xD97706));
            bottom.add(lblRsvTime, BorderLayout.EAST);
        }

        card.add(topRow, BorderLayout.NORTH);
        card.add(center, BorderLayout.CENTER);
        card.add(bottom, BorderLayout.SOUTH);

        final Color finalBgHover = bgHover;
        card.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getButton() == MouseEvent.BUTTON1)
                    handleRoomClick(p);
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger())
                    showContextMenu(p, e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger())
                    showContextMenu(p, e);
            }

            @Override
            public void mouseEntered(MouseEvent e) {
                card.setBackground(finalBgHover);
                card.repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                card.setBackground(Color.WHITE);
                card.repaint();
            }
        });
        return card;
    }

    private void handleRoomClick(Phong p) {
        TrangThaiPhong tt = p.getTrangThai();
        if (tt == TrangThaiPhong.AVAILABLE) {
            showCheckinDialog(p);
        } else if (tt == TrangThaiPhong.OCCUPIED) {
            showCheckoutDialog(p);
        } else if (tt == TrangThaiPhong.CLEANING) {
            showCleaningToAvailableDialog(p);
        } else {
            JOptionPane.showMessageDialog(mainFrame,
                    "Phòng " + p.getSoPhong() + " - Trạng thái: " + p.getTrangThai(),
                    "Thông tin", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void showCleaningToAvailableDialog(Phong p) {
        JDialog dlg = new JDialog(mainFrame, "Cập nhật phòng " + p.getSoPhong(), true);
        dlg.setSize(480, 230);
        dlg.setLocationRelativeTo(mainFrame);
        dlg.getContentPane().setBackground(Color.WHITE);

        JPanel main = new JPanel(new BorderLayout(15, 15));
        main.setBackground(Color.WHITE);
        main.setBorder(BorderFactory.createEmptyBorder(25, 25, 20, 25));

        // Center content
        JPanel centerPanel = new JPanel(new BorderLayout(15, 0));
        centerPanel.setOpaque(false);

        // Icon
        JLabel lblIcon = new JLabel("✨");
        lblIcon.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 42));
        lblIcon.setForeground(UIConstants.SUCCESS);
        centerPanel.add(lblIcon, BorderLayout.WEST);

        // Message
        String msg = "<html><body style='width: 320px; color: #334155; font-family: Segoe UI, sans-serif;'>"
                + "<h3 style='margin: 0 0 8px 0; color: #0F172A;'>Phòng đã dọn dẹp xong?</h3>"
                + "Bạn muốn chuyển phòng <b>" + p.getSoPhong()
                + "</b> sang trạng thái phòng trống sẵn sàng đón khách?</body></html>";
        JLabel lblMessage = new JLabel(msg);
        centerPanel.add(lblMessage, BorderLayout.CENTER);

        main.add(centerPanel, BorderLayout.CENTER);

        // Buttons
        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        buttons.setBackground(Color.WHITE);
        RoundedButton btnCancel = new RoundedButton("Huỷ", new Color(0xE2E8F0), UIConstants.TEXT_PRIMARY);
        RoundedButton btnConfirm = new RoundedButton("Xác nhận phòng trống", UIConstants.SUCCESS, Color.WHITE);

        btnCancel.addActionListener(e -> dlg.dispose());
        btnConfirm.addActionListener(e -> {
            phongService.updateTrangThai(p.getSoPhong(), TrangThaiPhong.AVAILABLE);
            refresh();
            dlg.dispose();
        });

        buttons.add(btnCancel);
        buttons.add(btnConfirm);
        main.add(buttons, BorderLayout.SOUTH);

        dlg.setContentPane(main);
        dlg.setVisible(true);
    }

    private void showCheckinDialog(Phong p) {
        CheckinDialog dlg = new CheckinDialog(
                (java.awt.Frame) SwingUtilities.getWindowAncestor(this), p);
        dlg.setVisible(true);
        if (dlg.isConfirmed())
            refresh();
    }

    private void showCheckoutDialog(Phong p) {
        ChiTietDatPhong tp = thuePhongService.getActiveByPhong(p.getSoPhong());
        if (tp == null) {
            JOptionPane.showMessageDialog(mainFrame, "Không tìm thấy thông tin thuê phòng!");
            return;
        }

        // Kiểm tra xem phòng này có thuộc đoàn không (nhiều hơn 1 phòng)
        boolean isGroup = false;
        long activeRoomsInGroup = 0;
        java.util.List<ChiTietDatPhong> allGroupRooms = null;
        if (tp.getDatPhong() != null) {
            allGroupRooms = thuePhongService.getChiTietByDatPhong(tp.getDatPhong().getMaDatPhong());
            activeRoomsInGroup = allGroupRooms.stream().filter(r -> r.getNgayTraThucTe() == null).count();
            if (allGroupRooms.size() > 1 || "DOAN".equalsIgnoreCase(tp.getDatPhong().getLoaiKhach())) {
                isGroup = true;
            }
        }

        if (isGroup && activeRoomsInGroup > 1) {
            // Còn nhiều phòng chưa checkout → Thanh toán riêng hoặc Treo nợ
            Object[] options = {"Thanh toán & Đóng bill ngay", "Trả phòng & Treo nợ Đoàn", "Hủy"};
            int choice = JOptionPane.showOptionDialog(mainFrame,
                    "Phòng " + p.getSoPhong() + " thuộc về một ĐOÀN có " + activeRoomsInGroup + " phòng đang thuê.\n"
                    + "Bạn muốn thanh toán riêng lẻ cho phòng này hay treo nợ vào bill Đoàn tổng?",
                    "Hình thức Trả phòng Đoàn",
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    options,
                    options[0]);

            if (choice == 1) {
                // Treo nợ đoàn (Master Folio Routing)
                int confirm = JOptionPane.showConfirmDialog(mainFrame,
                    "Xác nhận ghi nợ toàn bộ tiền phòng và dịch vụ của phòng " + p.getSoPhong() + " vào Hóa Đơn Đoàn?",
                    "Xác nhận Treo Nợ", JOptionPane.YES_NO_OPTION);
                if (confirm == JOptionPane.YES_OPTION) {
                    String err = thuePhongService.checkOutWithoutBilling(tp.getMaChiTiet());
                    if (err == null) {
                        JOptionPane.showMessageDialog(mainFrame, "Đã trả phòng và chuyển nợ thành công vào bill Đoàn!");
                        refresh();
                        mainFrame.refreshThongKe();
                    } else {
                        JOptionPane.showMessageDialog(mainFrame, "Lỗi: " + err, "Lỗi", JOptionPane.ERROR_MESSAGE);
                    }
                }
                return;
            } else if (choice == 2 || choice == JOptionPane.CLOSED_OPTION) {
                return;
            }
            // If choice == 0 -> Continue below to normal HoaDonDialog
        } else if (isGroup && activeRoomsInGroup == 1) {
            // Phòng CUỐI CÙNG của đoàn → Hỏi Gom Bill hay Thanh toán riêng
            // Kiểm tra có phòng nào đã treo nợ chưa
            long unpaidCount = allGroupRooms.stream().filter(r -> r.getNgayTraThucTe() != null && !r.isDaThanhToan()).count();
            
            if (unpaidCount > 0) {
                // Có phòng đã treo nợ → BẮT BUỘC gom bill (không cho thanh toán riêng vì sẽ làm mất bill treo)
                Object[] options = {"📋 Gom Bill toàn đoàn", "Hủy"};
                int choice = JOptionPane.showOptionDialog(mainFrame,
                        "Đây là phòng CUỐI CÙNG của đoàn.\n"
                        + "Trực hệ có " + unpaidCount + " phòng đã trả nhưng đang Treo nợ (Chưa thanh toán).\n\n"
                        + "BẮT BUỘC tiến hành GOM BILL toàn đoàn vào 1 Hóa đơn duy nhất để tất toán cho các phòng đã treo nợ.",
                        "Tiến hành Gom Bill Đoàn",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.WARNING_MESSAGE,
                        null,
                        options,
                        options[0]);

                if (choice == 0) {
                    // Gom Bill → mở MasterBillDialog
                    showMasterBillDialog(tp.getDatPhong(), allGroupRooms);
                    return;
                } else {
                    return;
                }
            } else {
                // Không có phòng treo nợ, nhưng vẫn là đoàn → cho chọn
                Object[] options = {"Gom Bill toàn đoàn", "Thanh toán riêng phòng này", "Hủy"};
                int choice = JOptionPane.showOptionDialog(mainFrame,
                        "Đây là phòng CUỐI CÙNG của đoàn (" + allGroupRooms.size() + " phòng).\n"
                        + "Bạn muốn GOM BILL toàn bộ đoàn vào 1 hóa đơn duy nhất?",
                        "Gom Bill Đoàn",
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        options,
                        options[0]);

                if (choice == 0) {
                    showMasterBillDialog(tp.getDatPhong(), allGroupRooms);
                    return;
                } else if (choice == 2 || choice == JOptionPane.CLOSED_OPTION) {
                    return;
                }
            }
        }

        HoaDonDialog dlg = new HoaDonDialog(
                (java.awt.Frame) SwingUtilities.getWindowAncestor(this), tp);
        dlg.setVisible(true);
        if (dlg.isConfirmed()) {
            refresh();
            mainFrame.refreshThongKe(); // Also refresh statistics after checkout
        }
    }

    /**
     * Mở dialog Gom Bill Đoàn (Master Bill).
     */
    private void showMasterBillDialog(DatPhong dp, java.util.List<ChiTietDatPhong> rooms) {
        // Load full DatPhong data
        DatPhong fullDp = new service.DatPhongService().getById(dp.getMaDatPhong());
        if (fullDp == null) fullDp = dp;

        MasterBillDialog dlg = new MasterBillDialog(
                (java.awt.Frame) SwingUtilities.getWindowAncestor(this),
                fullDp, rooms);
        dlg.setVisible(true);
        if (dlg.isConfirmed()) {
            refresh();
            mainFrame.refreshThongKe();
        }
    }

    private String truncate(String s, int max) {
        return s != null && s.length() > max ? s.substring(0, max) + "…" : s;
    }

    // ---- Context Menu Handler ----
    private void showContextMenu(Phong p, MouseEvent e) {
        ModernPopupMenu menu = new ModernPopupMenu();

        // Menu items with modern styling
        ModernMenuItem itemDetails = new ModernMenuItem("Xem chi tiết phòng");
        itemDetails.addActionListener(e2 -> showRoomDetails(p));

        ModernMenuItem itemSwitch = new ModernMenuItem("Đổi phòng cho khách");
        itemSwitch.setEnabled(p.getTrangThai() == TrangThaiPhong.OCCUPIED);
        itemSwitch.addActionListener(e2 -> showSwitchRoomDialog(p));

        ModernMenuItem itemService = new ModernMenuItem("Thêm dịch vụ sử dụng");
        itemService.setEnabled(p.getTrangThai() == TrangThaiPhong.OCCUPIED);
        itemService.addActionListener(e2 -> showAddServiceDialog(p));

        ModernMenuItem itemPayment = new ModernMenuItem("Thanh toán & Trả phòng");
        itemPayment.setEnabled(p.getTrangThai() == TrangThaiPhong.OCCUPIED);
        itemPayment.addActionListener(e2 -> showCheckoutDialog(p));

        ModernMenuItem itemExtend = new ModernMenuItem("Gia hạn lưu trú");
        itemExtend.setEnabled(p.getTrangThai() == TrangThaiPhong.OCCUPIED);
        itemExtend.addActionListener(e2 -> showExtendStayDialog(p));

        // Gom Bill Đoàn — chỉ hiện nếu phòng thuộc đoàn
        ModernMenuItem itemMasterBill = new ModernMenuItem("📋 Gom Bill Đoàn");
        itemMasterBill.setEnabled(false);
        if (p.getTrangThai() == TrangThaiPhong.OCCUPIED) {
            ChiTietDatPhong tp = thuePhongService.getActiveByPhong(p.getSoPhong());
            if (tp != null && tp.getDatPhong() != null && "DOAN".equalsIgnoreCase(tp.getDatPhong().getLoaiKhach())) {
                itemMasterBill.setEnabled(true);
                itemMasterBill.addActionListener(e2 -> {
                    java.util.List<ChiTietDatPhong> allRooms = thuePhongService.getChiTietByDatPhong(tp.getDatPhong().getMaDatPhong());
                    showMasterBillDialog(tp.getDatPhong(), allRooms);
                });
            }
        }

        menu.add(itemDetails);
        menu.addSeparator();
        menu.add(itemSwitch);
        menu.add(itemService);
        menu.add(itemExtend);
        menu.addSeparator();
        menu.add(itemPayment);
        menu.add(itemMasterBill);

        menu.show(e.getComponent(), e.getX(), e.getY());
    }

    // ---- Action: Xem chi tiết ----
    private void showRoomDetails(Phong p) {
        JDialog dlg = new JDialog(mainFrame, "Thông tin phòng " + p.getSoPhong(), true);
        dlg.setSize(400, 480);
        dlg.setLocationRelativeTo(mainFrame);
        dlg.getContentPane().setBackground(Color.WHITE);
        dlg.setLayout(new BorderLayout());

        JPanel main = new JPanel(new BorderLayout(15, 15));
        main.setBackground(Color.WHITE);
        main.setBorder(BorderFactory.createEmptyBorder(25, 25, 20, 25));

        // Info Panel using BoxLayout for tight control
        JPanel infoPanel = new JPanel();
        infoPanel.setLayout(new BoxLayout(infoPanel, BoxLayout.Y_AXIS));
        infoPanel.setOpaque(false);

        // title
        JLabel lblTitle = new JLabel("Chi tiết phòng " + p.getSoPhong());
        lblTitle.setFont(UIConstants.FONT_HEADER);
        lblTitle.setForeground(UIConstants.TEXT_PRIMARY);
        lblTitle.setAlignmentX(Component.LEFT_ALIGNMENT);
        infoPanel.add(lblTitle);
        infoPanel.add(Box.createVerticalStrut(10));
        
        JSeparator sep1 = new JSeparator();
        sep1.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
        infoPanel.add(sep1);
        infoPanel.add(Box.createVerticalStrut(15));

        // helper to add rows
        java.util.function.BiConsumer<String, String> addRow = (label, value) -> {
            JPanel row = new JPanel(new BorderLayout());
            row.setOpaque(false);
            row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
            JLabel lblL = new JLabel(label);
            lblL.setFont(UIConstants.FONT_BODY);
            lblL.setForeground(UIConstants.TEXT_SECONDARY);
            JLabel lblV = new JLabel(value);
            lblV.setFont(new Font("Segoe UI", Font.BOLD, 13));
            lblV.setForeground(UIConstants.TEXT_PRIMARY);
            row.add(lblL, BorderLayout.WEST);
            row.add(lblV, BorderLayout.EAST);
            infoPanel.add(row);
            infoPanel.add(Box.createVerticalStrut(8));
        };

        addRow.accept("Tầng:", "Tầng " + p.getTang());
        addRow.accept("Loại phòng:", p.getTenLoaiPhong());
        addRow.accept("Hướng view:", p.getView());
        addRow.accept("Sức chứa:", p.getSucChua() + " người");
        double giaDetail = bangGiaService.layGiaHienHanh(p.getLoaiPhong() != null ? p.getLoaiPhong().getMaLoaiPhong() : "");
        addRow.accept("Giá/đêm:", String.format("%,.0fđ", giaDetail));
        
        JPanel ttRow = new JPanel(new BorderLayout());
        ttRow.setOpaque(false);
        ttRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 25));
        JLabel lblTTTitle = new JLabel("Trạng thái:");
        lblTTTitle.setFont(UIConstants.FONT_BODY);
        lblTTTitle.setForeground(UIConstants.TEXT_SECONDARY);
        JLabel lblTTVal = new JLabel(UIConstants.getTrangThaiPhongLabel(p.getTrangThai()));
        lblTTVal.setFont(new Font("Segoe UI", Font.BOLD, 13));
        lblTTVal.setForeground(UIConstants.getTrangThaiPhongColor(p.getTrangThai()));
        ttRow.add(lblTTTitle, BorderLayout.WEST);
        ttRow.add(lblTTVal, BorderLayout.EAST);
        infoPanel.add(ttRow);
        infoPanel.add(Box.createVerticalStrut(20));

        if (p.getTenKhachHienTai() != null) {
            JLabel lblKhach = new JLabel("Khách đang thuê");
            lblKhach.setFont(UIConstants.FONT_SMALL_BOLD);
            lblKhach.setForeground(UIConstants.PRIMARY);
            lblKhach.setAlignmentX(Component.LEFT_ALIGNMENT);
            infoPanel.add(lblKhach);
            infoPanel.add(Box.createVerticalStrut(5));
            
            JSeparator sep2 = new JSeparator();
            sep2.setMaximumSize(new Dimension(Integer.MAX_VALUE, 1));
            infoPanel.add(sep2);
            infoPanel.add(Box.createVerticalStrut(10));

            addRow.accept("Họ và tên:", p.getTenKhachHienTai());

            ChiTietDatPhong tp = thuePhongService.getActiveByPhong(p.getSoPhong());
            if (tp != null && tp.getNgayNhanThucTe() != null) {
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                addRow.accept("Ngày nhận:", tp.getNgayNhanThucTe().format(formatter));
                if (tp.getNgayTraThucTe() != null) {
                    addRow.accept("Ngày trả:", tp.getNgayTraThucTe().format(formatter));
                }
            }
        }

        main.add(infoPanel, BorderLayout.CENTER);

        JPanel btnPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        btnPanel.setOpaque(false);
        RoundedButton btnClose = new RoundedButton("Đóng", UIConstants.PRIMARY, Color.WHITE);
        btnClose.addActionListener(e -> dlg.dispose());
        btnPanel.add(btnClose);
        main.add(btnPanel, BorderLayout.SOUTH);

        dlg.add(main);
        dlg.setVisible(true);
    }

    // ---- Action: Đổi phòng (Room Move) ----
    private void showSwitchRoomDialog(Phong currentPhong) {
        ChiTietDatPhong tp = thuePhongService.getActiveByPhong(currentPhong.getSoPhong());
        if (tp == null) {
            JOptionPane.showMessageDialog(mainFrame, "Không tìm thấy thông tin thuê phòng!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        ui.dialogs.RoomTransferDialog dlg = new ui.dialogs.RoomTransferDialog(mainFrame, tp);
        dlg.setVisible(true);

        if (dlg.isConfirmed()) {
            Phong newRoom = dlg.getSelectedRoom();
            double newPrice = dlg.isUseNewPrice() ? newRoom.getGiaTheoNgay() : tp.getGiaThucTeChot();
            doSwitchRoom(tp, currentPhong, newRoom, newPrice);
        }
    }

    private void doSwitchRoom(ChiTietDatPhong tp, Phong oldRoom, Phong newRoom, double newPrice) {
        try {
            // 1. Cập nhật phòng cũ về Trống (Có thể cần quét dọn - CLEANING nếu khách đã ở lâu)
            oldRoom.setTrangThai(TrangThaiPhong.AVAILABLE);
            oldRoom.setTenKhachHienTai(null);
            phongService.suaPhong(oldRoom);

            // 2. Cập nhật phòng mới sang Đang ở
            newRoom.setTrangThai(TrangThaiPhong.OCCUPIED);
            String guestName = tp.getDatPhong() != null && tp.getDatPhong().getKhachHang() != null
                    ? tp.getDatPhong().getKhachHang().getHoTen()
                    : "Khách";
            newRoom.setTenKhachHienTai(guestName);
            phongService.suaPhong(newRoom);

            // 3. Cập nhật ChiTietDatPhong (Đổi mã phòng và Chốt giá mới nếu có)
            tp.setPhong(newRoom);
            tp.setGiaThucTeChot(newPrice);

            ChiTietDatPhongDAO ctdpDAO = new ChiTietDatPhongDAO();
            boolean saved = ctdpDAO.update(tp);

            if (saved) {
                JOptionPane.showMessageDialog(mainFrame,
                        "Đã đổi phòng thành công!\n" +
                        "Từ: " + oldRoom.getSoPhong() + " (" + String.format("%,.0fđ", oldRoom.getGiaTheoNgay()) + ")\n" +
                        "Sang: " + newRoom.getSoPhong() + " (" + String.format("%,.0fđ", newPrice) + ")",
                        "Thành công", JOptionPane.INFORMATION_MESSAGE);
                refresh();
            } else {
                throw new Exception("Không thể cập nhật thông tin thuê phòng vào cơ sở dữ liệu.");
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(mainFrame, "Lỗi khi đổi phòng: " + e.getMessage(), "Lỗi", JOptionPane.ERROR_MESSAGE);
        }
    }

    // ---- Action: Thêm dịch vụ (Phối hợp nhiều dịch vụ cùng lúc) ----
    private void showAddServiceDialog(Phong p) {
        ChiTietDatPhong tp = thuePhongService.getActiveByPhong(p.getSoPhong());
        if (tp == null) {
            JOptionPane.showMessageDialog(mainFrame, "Không tìm thấy thông tin thuê phòng!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        ui.dialogs.ServiceOrderDialog dlg = new ui.dialogs.ServiceOrderDialog(mainFrame, tp);
        dlg.setVisible(true);
        if (dlg.isConfirmed()) {
            refresh();
        }
    }

    // ---- Action: Gia hạn lưu trú (Stay Extension) ----
    private void showExtendStayDialog(Phong p) {
        ChiTietDatPhong tp = thuePhongService.getActiveByPhong(p.getSoPhong());
        if (tp == null || tp.getDatPhong() == null) {
            JOptionPane.showMessageDialog(mainFrame, "Không tìm thấy thông tin thuê phòng!", "Lỗi", JOptionPane.ERROR_MESSAGE);
            return;
        }

        ui.dialogs.ExtendStayDialog dlg = new ui.dialogs.ExtendStayDialog(mainFrame, tp);
        dlg.setVisible(true);

        if (dlg.isConfirmed()) {
            LocalDateTime newTime = dlg.getNewCheckoutTime();
            String err = thuePhongService.extendStay(tp.getMaChiTiet(), newTime);
            if (err == null) {
                JOptionPane.showMessageDialog(mainFrame, "Gia hạn thành công!", "Thành công", JOptionPane.INFORMATION_MESSAGE);
                refresh();
            } else {
                JOptionPane.showMessageDialog(mainFrame, "Lỗi: " + err, "Lỗi", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public void refresh() {
        removeAll();
        buildUI();
        revalidate();
        repaint();
    }

    /**
     * JPanel implement Scrollable để JScrollPane có thể constraint width của panel
     * theo chiều rộng viewport — từ đó WrapLayout mới tính đúng điểm xuống hàng.
     */
    private static class ScrollablePanel extends JPanel implements Scrollable {
        @Override
        public Dimension getPreferredScrollableViewportSize() {
            return getPreferredSize();
        }

        @Override
        public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 20;
        }

        @Override
        public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
            return 100;
        }

        @Override
        public boolean getScrollableTracksViewportWidth() {
            return true;
        } // KEY: width = viewport width

        @Override
        public boolean getScrollableTracksViewportHeight() {
            return false;
        } // scroll dọc bình thường
    }
}
