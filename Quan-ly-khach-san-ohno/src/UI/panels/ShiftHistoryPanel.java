package ui.panels;

import service.GiaoCaService;
import entity.GiaoCa;
import ui.components.UIConstants;
import java.awt.*;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;

import ui.components.RoundedComponents.*;
import ui.components.PaginationPanel;
import javax.swing.table.DefaultTableCellRenderer;

public class ShiftHistoryPanel extends JPanel {
    private final GiaoCaService service = new GiaoCaService();
    private JTable table;
    private DefaultTableModel model;
    private ModernTextField txtSearch;
    private ModernComboBox<String> cbStatus;

    private PaginationPanel pagination;
    private List<GiaoCa> allHistory;

    public ShiftHistoryPanel() {
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

        String[] cols = {"MÃ CA", "NHÂN VIÊN", "BẮT ĐẦU", "KẾT THÚC", "VỐN ĐẦU CA", "THU TIỀN MẶT", "BÀN GIAO", "CHÊNH LỆCH", "TRẠNG THÁI"};
        model = new DefaultTableModel(cols, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(model);
        styleTable();

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(100);
        table.getColumnModel().getColumn(1).setPreferredWidth(150);
        table.getColumnModel().getColumn(2).setPreferredWidth(120);
        table.getColumnModel().getColumn(3).setPreferredWidth(120);
        table.getColumnModel().getColumn(8).setPreferredWidth(120);

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

        JLabel lblTitle = new JLabel("Lịch sử giao ca");
        lblTitle.setFont(UIConstants.FONT_TITLE);
        lblTitle.setForeground(UIConstants.TEXT_PRIMARY);

        JLabel lblSubTitle = new JLabel("Theo dõi và đối soát dữ liệu bàn giao ca trực");
        lblSubTitle.setFont(UIConstants.FONT_SMALL);
        lblSubTitle.setForeground(UIConstants.TEXT_MUTED);

        titlePanel.add(lblTitle);
        titlePanel.add(Box.createVerticalStrut(2));
        titlePanel.add(lblSubTitle);
        p.add(titlePanel, BorderLayout.WEST);

        // Filters section
        JPanel filterPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        filterPanel.setOpaque(false);

        txtSearch = new ModernTextField("Tìm nhân viên, mã ca...");
        txtSearch.setPreferredSize(new Dimension(250, 40));
        txtSearch.addActionListener(e -> refresh());

        cbStatus = new ModernComboBox<>(new String[]{"Tất cả", "OPEN", "CLOSED"});
        cbStatus.setPreferredSize(new Dimension(150, 40));
        cbStatus.addActionListener(e -> refresh());

        RoundedButton btnRefresh = new RoundedButton("", UIConstants.PRIMARY, Color.WHITE);
        btnRefresh.setIcon(new ImageIcon("d:/HK2-2026/PTUD/Hotel_FH/Quan-ly-khach-san-ohno/icon/refresh_white.png"));
        btnRefresh.setPreferredSize(new Dimension(45, 40));
        btnRefresh.addActionListener(e -> {
            txtSearch.setText("");
            cbStatus.setSelectedIndex(0);
            refresh();
        });

        filterPanel.add(txtSearch);
        filterPanel.add(cbStatus);
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

        table.getColumnModel().getColumn(8).setCellRenderer(new ShiftStatusRenderer());
        
        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(JLabel.RIGHT);
        for(int i=4; i<=7; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(rightRenderer);
        }
    }

    public void refresh() {
        String keyword = txtSearch.getText();
        String status = (String) cbStatus.getSelectedItem();
        allHistory = service.searchHistory(keyword, status);
        
        if (pagination != null) {
            pagination.setCurrentPage(1);
        }
        displayCurrentPage();
    }

    private void displayCurrentPage() {
        model.setRowCount(0);
        if (allHistory == null) return;

        int pageSize = 12;
        int currentPage = pagination.getCurrentPage();
        int start = (currentPage - 1) * pageSize;
        int end = Math.min(start + pageSize, allHistory.size());

        java.time.format.DateTimeFormatter fmt = java.time.format.DateTimeFormatter.ofPattern("HH:mm dd/MM");
        for (int i = start; i < end; i++) {
            GiaoCa gc = allHistory.get(i);
            model.addRow(new Object[]{
                gc.getMaGiaoCa(),
                gc.getNhanVien().getHoTen(),
                gc.getThoiGianBatDau().format(fmt),
                gc.getThoiGianKetThuc() != null ? gc.getThoiGianKetThuc().format(fmt) : "--",
                String.format("%,.0f", gc.getTienMatDauCa()),
                String.format("%,.0f", gc.getTienMatThuTrongCa()),
                String.format("%,.0f", gc.getTienMatBanGiao()),
                String.format("%,.0f", gc.getTienMatChenhLech()),
                gc.getTrangThai()
            });
        }
        
        pagination.update(allHistory.size(), pageSize, currentPage);
    }

    // --- Custom Renderer for Shift Status ---
    private static class ShiftStatusRenderer extends DefaultTableCellRenderer {
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            String text = (value != null) ? value.toString() : "";
            Color bg, fg;

            if ("OPEN".equals(text)) {
                bg = new Color(230, 250, 240); fg = new Color(30, 150, 80);
            } else {
                bg = new Color(235, 243, 255); fg = new Color(30, 100, 230);
            }

            StatusBadge badge = new StatusBadge(text, bg, fg);
            badge.setHorizontalAlignment(CENTER);
            badge.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);
            
            JPanel wrapper = new JPanel(new GridBagLayout());
            wrapper.setOpaque(true);
            wrapper.setBackground(isSelected ? table.getSelectionBackground() : Color.WHITE);
            wrapper.add(badge);
            
            return wrapper;
        }
    }
}
