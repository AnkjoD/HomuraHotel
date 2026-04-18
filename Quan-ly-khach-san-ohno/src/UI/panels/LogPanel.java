package ui.panels;

import service.LogService;
import entity.NhatKyHeThong;
import ui.components.UIConstants;
import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import javax.swing.table.DefaultTableCellRenderer;
import ui.components.RoundedComponents.*;
import ui.components.PaginationPanel;
import java.util.stream.Collectors;

public class LogPanel extends JPanel {
    private final LogService service = new LogService();
    private JTable table;
    private DefaultTableModel model;
    private ModernTextField txtSearch;
    private ModernComboBox<String> cbActionType;

    private PaginationPanel pagination;
    private List<NhatKyHeThong> allLogs;

    public LogPanel() {
        setOpaque(false);
        setLayout(new BorderLayout());
        buildUI();
        refresh();
    }

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        main.add(buildHeader(), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);

        String[] cols = {"THỜI GIAN", "TÀI KHOẢN", "HÀNH ĐỘNG", "ĐỐI TƯỢNG", "CHI TIẾT"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        styleTable();

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(160);
        table.getColumnModel().getColumn(1).setPreferredWidth(120);
        table.getColumnModel().getColumn(2).setPreferredWidth(150);
        table.getColumnModel().getColumn(3).setPreferredWidth(150);
        table.getColumnModel().getColumn(4).setPreferredWidth(450);

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER));
        scroll.getViewport().setBackground(Color.WHITE);
        
        // Pagination
        pagination = new PaginationPanel();
        pagination.setPageChangeListener(page -> displayCurrentPage());
        
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER));
        footer.add(pagination, BorderLayout.CENTER);

        JPanel tableWrap = new JPanel(new BorderLayout());
        tableWrap.setOpaque(false);
        tableWrap.add(scroll, BorderLayout.CENTER);
        tableWrap.add(footer, BorderLayout.SOUTH);

        center.add(tableWrap, BorderLayout.CENTER);
        main.add(center, BorderLayout.CENTER);

        add(main, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 5));

        // Title section
        JPanel titlePanel = new JPanel();
        titlePanel.setOpaque(false);
        titlePanel.setLayout(new BoxLayout(titlePanel, BoxLayout.Y_AXIS));

        JLabel lblTitle = new JLabel("Nhật ký hệ thống");
        lblTitle.setFont(UIConstants.FONT_TITLE);
        lblTitle.setForeground(UIConstants.TEXT_PRIMARY);

        JLabel lblSubTitle = new JLabel("Theo dõi các hoạt động vận hành thời gian thực");
        lblSubTitle.setFont(UIConstants.FONT_SMALL);
        lblSubTitle.setForeground(UIConstants.TEXT_MUTED);

        titlePanel.add(lblTitle);
        titlePanel.add(Box.createVerticalStrut(2));
        titlePanel.add(lblSubTitle);
        p.add(titlePanel, BorderLayout.WEST);

        // Filters section
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        filterPanel.setOpaque(false);

        txtSearch = new ModernTextField("Tìm tài khoản, đối tượng...");
        txtSearch.setPreferredSize(new Dimension(250, 40));
        txtSearch.addActionListener(e -> refresh());

        cbActionType = new ModernComboBox<>();
        cbActionType.setPreferredSize(new Dimension(180, 40));
        updateActionTypes();
        cbActionType.addActionListener(e -> refresh());

        RoundedButton btnRefresh = new RoundedButton("", UIConstants.SUCCESS, Color.WHITE);
        btnRefresh.setIcon(new ImageIcon("d:/HK2-2026/PTUD/Hotel_FH/Quan-ly-khach-san-ohno/icon/refresh_white.png"));
        btnRefresh.setPreferredSize(new Dimension(45, 40));
        btnRefresh.addActionListener(e -> {
            txtSearch.setText("");
            cbActionType.setSelectedIndex(0);
            refresh();
        });

        filterPanel.add(txtSearch);
        filterPanel.add(cbActionType);
        filterPanel.add(btnRefresh);
        p.add(filterPanel, BorderLayout.EAST);

        return p;
    }

    private void styleTable() {
        table.setRowHeight(40);
        table.setFont(UIConstants.FONT_BODY);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setSelectionBackground(UIConstants.PRIMARY_LIGHT);
        table.setSelectionForeground(UIConstants.TEXT_PRIMARY);

        // Table Header
        table.getTableHeader().setFont(UIConstants.FONT_SMALL_BOLD);
        table.getTableHeader().setBackground(UIConstants.BG_TABLE_HEADER);
        table.getTableHeader().setForeground(UIConstants.TEXT_SECONDARY);
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.BORDER));

        // Custom Zebra Renderer
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean sel, boolean foc, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(tbl, val, sel, foc, row, col);
                lbl.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                lbl.setBackground(sel ? UIConstants.PRIMARY_LIGHT : (row % 2 == 0 ? Color.WHITE : new Color(0xFAFAFA)));
                lbl.setFont(UIConstants.FONT_BODY);
                lbl.setForeground(UIConstants.TEXT_PRIMARY);
                return lbl;
            }
        });

        table.getColumnModel().getColumn(2).setCellRenderer(new LogActionRenderer());
    }

    private void updateActionTypes() {
        List<String> types = service.getActionTypes();
        String current = (String) cbActionType.getSelectedItem();
        cbActionType.setModel(new DefaultComboBoxModel<>(new java.util.Vector<>(types)));
        if (current != null && types.contains(current)) cbActionType.setSelectedItem(current);
    }

    public void refresh() {
        String keyword = txtSearch.getText();
        String actionType = (String) cbActionType.getSelectedItem();
        allLogs = service.searchLogs(keyword, actionType);
        
        if (pagination != null) {
            pagination.setCurrentPage(1);
        }
        displayCurrentPage();
    }

    private void displayCurrentPage() {
        model.setRowCount(0);
        if (allLogs == null) return;

        int pageSize = 12;
        int currentPage = pagination.getCurrentPage();
        int start = (currentPage - 1) * pageSize;
        int end = Math.min(start + pageSize, allLogs.size());

        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss dd/MM/yyyy");
        for (int i = start; i < end; i++) {
            NhatKyHeThong log = allLogs.get(i);
            model.addRow(new Object[]{
                log.getThoiGian().format(fmt),
                log.getTenDangNhap(),
                log.getHanhDong(),
                log.getDoiTuong(),
                log.getChiTiet()
            });
        }
        
        pagination.update(allLogs.size(), pageSize, currentPage);
    }

    // --- Custom Renderer for Action Badge ---
    private static class LogActionRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            String text = (value != null) ? value.toString() : "";
            Color bg, fg;

            if (text.contains("Đăng nhập") || text.contains("Đăng xuất")) {
                bg = new Color(235, 243, 255); fg = new Color(30, 100, 230);
            } else if (text.contains("Check-") || text.contains("đặt") || text.contains("thanh toán")) {
                bg = new Color(230, 250, 240); fg = new Color(30, 150, 80);
            } else if (text.contains("Sửa") || text.contains("Cập nhật")) {
                bg = new Color(255, 245, 230); fg = new Color(200, 110, 30);
            } else if (text.contains("Xóa") || text.contains("Hủy")) {
                bg = new Color(255, 235, 235); fg = new Color(220, 50, 50);
            } else {
                bg = new Color(240, 242, 245); fg = UIConstants.TEXT_SECONDARY;
            }

            StatusBadge badge = new StatusBadge(text, bg, fg);
            badge.setHorizontalAlignment(CENTER);
            badge.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE); // Handle table selection background
            
            // Wrapper to hold the badge centrally
            JPanel wrapper = new JPanel(new GridBagLayout());
            wrapper.setOpaque(true);
            wrapper.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);
            wrapper.add(badge);
            
            return wrapper;
        }
    }
}
