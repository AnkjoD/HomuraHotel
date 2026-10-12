package ui.dialogs;

import entity.Phong;
import ui.components.UIConstants;
import ui.components.RoundedComponents.*;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * SelectedRoomsDialog - Giao diện "Giỏ hàng phòng" hiện đại.
 * Giúp người dùng xem danh sách phòng đã chọn và bỏ chọn từng phòng dễ dàng.
 */
public class SelectedRoomsDialog extends JDialog {

    private final Set<Phong> selectedRooms;
    private final Runnable onUpdate;
    private DefaultTableModel model;
    private JTable table;
    private JLabel lblTotalSum;

    public SelectedRoomsDialog(Frame parent, Set<Phong> selectedRooms, Runnable onUpdate) {
        super(parent, "Giỏ hàng phòng đã chọn 🛒", true);
        this.selectedRooms = selectedRooms;
        this.onUpdate = onUpdate;

        setLayout(new BorderLayout());
        setSize(650, 450);
        setLocationRelativeTo(parent);
        setResizable(false);

        buildUI();
        loadData();
    }

    private void buildUI() {
        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(Color.WHITE);

        // --- Header ---
        JPanel headerPanel = new JPanel(new BorderLayout());
        headerPanel.setBackground(UIConstants.PRIMARY_LIGHT);
        headerPanel.setBorder(new EmptyBorder(18, 24, 18, 24));

        JLabel lblTitle = new JLabel("Danh sách phòng đang chọn (Giỏ hàng)");
        lblTitle.setFont(UIConstants.FONT_TITLE);
        lblTitle.setForeground(UIConstants.PRIMARY);

        JLabel lblSub = new JLabel("Lưu ý: Bạn có thể bỏ chọn từng phòng hoặc xóa toàn bộ giỏ hàng.");
        lblSub.setFont(UIConstants.FONT_SMALL);
        lblSub.setForeground(UIConstants.TEXT_SECONDARY);

        headerPanel.add(lblTitle, BorderLayout.NORTH);
        headerPanel.add(lblSub, BorderLayout.CENTER);
        root.add(headerPanel, BorderLayout.NORTH);

        // --- Table ---
        String[] cols = { "Số phòng", "Tầng", "Loại phòng", "Hướng nhìn", "Giá/Đêm", "Hành động" };
        model = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return c == 5; // Chỉ cột hành động (nút xóa) mới tương tác được
            }
        };

        table = new JTable(model);
        styleTable();

        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER));
        scroll.setBackground(Color.WHITE);
        scroll.getViewport().setBackground(Color.WHITE);

        JPanel centerPanel = new JPanel(new BorderLayout(0, 10));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(15, 24, 15, 24));
        centerPanel.add(scroll, BorderLayout.CENTER);

        // Total summary below table
        lblTotalSum = new JLabel("Tổng cộng: 0 phòng | Tạm tính: 0đ/đêm");
        lblTotalSum.setFont(UIConstants.FONT_BODY_BOLD);
        lblTotalSum.setForeground(UIConstants.TEXT_PRIMARY);
        centerPanel.add(lblTotalSum, BorderLayout.SOUTH);

        root.add(centerPanel, BorderLayout.CENTER);

        // --- Footer ---
        JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 12));
        footer.setBackground(new Color(0xF8FAFC));
        footer.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER));

        RoundedButton btnClearAll = new RoundedButton("Xóa tất cả", new Color(0xFEF2F2), new Color(0xEF4444));
        btnClearAll.setFont(UIConstants.FONT_SMALL_BOLD);
        btnClearAll.setPreferredSize(new Dimension(120, 36));
        btnClearAll.addActionListener(e -> doClearAll());

        RoundedButton btnClose = new RoundedButton("Đóng", new Color(0xE2E8F0), UIConstants.TEXT_PRIMARY);
        btnClose.setFont(UIConstants.FONT_SMALL_BOLD);
        btnClose.setPreferredSize(new Dimension(100, 36));
        btnClose.addActionListener(e -> dispose());

        footer.add(btnClearAll);
        footer.add(btnClose);
        root.add(footer, BorderLayout.SOUTH);

        add(root);
    }

    private void styleTable() {
        table.setRowHeight(38);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setResizingAllowed(false);
        table.getTableHeader().setBackground(new Color(0xF1F5F9));
        table.getTableHeader().setForeground(UIConstants.TEXT_PRIMARY);
        table.getTableHeader().setFont(UIConstants.FONT_SMALL_BOLD);
        table.getTableHeader().setPreferredSize(new Dimension(0, 34));
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.BORDER));

        // Custom Cell Renderer
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object v, boolean isS, boolean hasF, int r, int c) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(t, v, isS, hasF, r, c);
                l.setFont(UIConstants.FONT_BODY);
                l.setForeground(UIConstants.TEXT_PRIMARY);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                l.setBorder(BorderFactory.createEmptyBorder(0, 10, 0, 10));
                
                // Zebra stripe background
                if (r % 2 == 0) {
                    l.setBackground(Color.WHITE);
                } else {
                    l.setBackground(new Color(0xF8FAFC));
                }
                
                if (c == 0) {
                    l.setFont(UIConstants.FONT_BODY_BOLD);
                } else if (c == 4) {
                    l.setForeground(UIConstants.SUCCESS);
                    l.setFont(UIConstants.FONT_BODY_BOLD);
                }
                return l;
            }
        };

        for (int i = 0; i < 5; i++) {
            table.getColumnModel().getColumn(i).setCellRenderer(centerRenderer);
        }

        // Action Column Button
        table.getColumnModel().getColumn(5).setCellRenderer(new ButtonRenderer());
        table.getColumnModel().getColumn(5).setCellEditor(new ButtonEditor(new JCheckBox()));

        // Column widths
        table.getColumnModel().getColumn(0).setPreferredWidth(90);
        table.getColumnModel().getColumn(1).setPreferredWidth(80);
        table.getColumnModel().getColumn(2).setPreferredWidth(120);
        table.getColumnModel().getColumn(3).setPreferredWidth(110);
        table.getColumnModel().getColumn(4).setPreferredWidth(130);
        table.getColumnModel().getColumn(5).setPreferredWidth(100);
    }

    private void loadData() {
        model.setRowCount(0);
        double totalSum = 0;
        
        List<Phong> list = new ArrayList<>(selectedRooms);
        for (Phong p : list) {
            double price = 0;
            if (p.getLoaiPhong() != null) {
                // Get price for standard base price (fallback or estimated)
                price = new service.BangGiaService().layGiaVaoThoiDiem(
                        p.getLoaiPhong().getMaLoaiPhong(),
                        java.time.LocalDateTime.now()
                );
            }
            totalSum += price;
            model.addRow(new Object[]{
                    "P." + p.getSoPhongDisplay(),
                    "Tầng " + p.getTang(),
                    p.getTenLoaiPhong(),
                    p.getView(),
                    String.format("%,.0fđ/đêm", price),
                    p // Pass Phong object to action column editor/renderer
            });
        }

        lblTotalSum.setText(String.format("Tổng cộng: %d phòng | Tổng tạm tính: %,.0fđ/đêm", selectedRooms.size(), totalSum));
    }

    private void doClearAll() {
        int ok = JOptionPane.showConfirmDialog(this,
                "Bạn có chắc muốn bỏ chọn TOÀN BỘ danh sách phòng đã chọn?",
                "Xác nhận xóa giỏ hàng", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok == JOptionPane.YES_OPTION) {
            selectedRooms.clear();
            if (onUpdate != null) {
                onUpdate.run();
            }
            dispose();
        }
    }

    // --- Cell Button Components ---
    private class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
            setContentAreaFilled(false);
            setBorderPainted(false);
            setFocusPainted(false);
            setFont(new Font("Segoe UI", Font.BOLD, 12));
            setForeground(new Color(0xEF4444));
            setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }

        @Override
        public Component getTableCellRendererComponent(JTable t, Object v, boolean isS, boolean hasF, int r, int c) {
            setText("Bỏ chọn");
            setBackground(r % 2 == 0 ? Color.WHITE : new Color(0xF8FAFC));
            return this;
        }
    }

    private class ButtonEditor extends DefaultCellEditor {
        private final JButton button;
        private Phong currentPhong;

        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.setContentAreaFilled(false);
            button.setBorderPainted(false);
            button.setFocusPainted(false);
            button.setFont(new Font("Segoe UI", Font.BOLD, 12));
            button.setForeground(new Color(0xEF4444));
            button.addActionListener(e -> {
                if (currentPhong != null) {
                    selectedRooms.remove(currentPhong);
                    fireEditingStopped();
                    loadData();
                    if (onUpdate != null) {
                        onUpdate.run();
                    }
                    if (selectedRooms.isEmpty()) {
                        dispose(); // Close modal if cart becomes empty
                    }
                }
            });
        }

        @Override
        public Component getTableCellEditorComponent(JTable t, Object v, boolean isS, int r, int c) {
            currentPhong = (Phong) v;
            button.setText("Bỏ chọn");
            button.setBackground(r % 2 == 0 ? Color.WHITE : new Color(0xF8FAFC));
            return button;
        }

        @Override
        public Object getCellEditorValue() {
            return currentPhong;
        }
    }
}
