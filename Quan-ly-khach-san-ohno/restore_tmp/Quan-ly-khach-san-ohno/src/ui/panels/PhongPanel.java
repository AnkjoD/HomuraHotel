package ui.panels;

import ui.components.RoundedComponents.ModernComboBox;
import ui.components.RoundedComponents.RoundedButton;
import ui.components.RoundedComponents.RoundedPanel;
import ui.components.UIConstants;
import ui.dialogs.PhongDialog;
import entity.Phong;
import entity.enums.TrangThaiPhong;
import java.awt.*;
import java.awt.event.*;
import java.util.Map;
import javax.swing.*;
import javax.swing.table.*;
import service.PhongService;

public class PhongPanel extends JPanel {

    private final PhongService service = new PhongService();

    private DefaultTableModel tableModel;
    private JTable table;
    private ModernComboBox<String> cboTrangThai;

    public PhongPanel() {
        setOpaque(false);
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        main.add(buildHeader(), BorderLayout.NORTH);

        JPanel center = new JPanel(new BorderLayout(0, 12));
        center.setOpaque(false);
        center.add(buildStats(), BorderLayout.NORTH);
        center.add(buildTableArea(), BorderLayout.CENTER);

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
        JLabel t = new JLabel("Quản lý phòng");
        t.setFont(UIConstants.FONT_TITLE);
        JLabel s = new JLabel("Quản lý tình trạng và thông tin từng phòng trong khách sạn");
        s.setFont(UIConstants.FONT_BODY);
        s.setForeground(UIConstants.TEXT_SECONDARY);
        left.add(t);
        left.add(Box.createVerticalStrut(2));
        left.add(s);

        RoundedButton btnAdd = new RoundedButton("+ Thêm phòng mới", UIConstants.PRIMARY, Color.WHITE);
        btnAdd.addActionListener(e -> showDialog(null));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);
        btns.add(btnAdd);

        p.add(left, BorderLayout.WEST);
        p.add(btns, BorderLayout.EAST);
        return p;
    }

    private JPanel buildStats() {
        Map<String, Integer> tt = service.getThongKeTrangThai();
        JPanel row = new JPanel(new GridLayout(1, 5, 12, 0));
        row.setOpaque(false);
        row.add(statCard("Có sẵn", tt.getOrDefault("Có sẵn", 0) + " phòng", UIConstants.SUCCESS));
        row.add(statCard("Đang thuê", tt.getOrDefault("Đang thuê", 0) + " phòng", UIConstants.PRIMARY));
        row.add(statCard("Đã đặt", tt.getOrDefault("Đã đặt", 0) + " phòng", UIConstants.WARNING));
        row.add(statCard("Vệ sinh", tt.getOrDefault("Vệ sinh", 0) + " phòng", new Color(0x06B6D4)));
        row.add(statCard("Bảo trì", tt.getOrDefault("Bảo trì", 0) + " phòng", UIConstants.DANGER));
        return row;
    }

    private RoundedPanel statCard(String label, String val, Color c) {
        RoundedPanel card = new RoundedPanel(UIConstants.CARD_RADIUS);
        card.setBackground(Color.WHITE);
        card.setShadow(true);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(12, 14, 8, 14));

        JLabel l1 = new JLabel(label);
        l1.setFont(UIConstants.FONT_SMALL);
        l1.setForeground(UIConstants.TEXT_SECONDARY);
        JLabel l2 = new JLabel(val);
        l2.setFont(new Font("Segoe UI", Font.BOLD, 18));

        JPanel bar = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setColor(c);
                g2.fillRoundRect(0, 3, getWidth(), 3, 3, 3);
                g2.dispose();
            }
        };
        bar.setOpaque(false);
        bar.setPreferredSize(new Dimension(0, 8));

        JPanel ct = new JPanel();
        ct.setOpaque(false);
        ct.setLayout(new BoxLayout(ct, BoxLayout.Y_AXIS));
        ct.add(l1);
        ct.add(Box.createVerticalStrut(4));
        ct.add(l2);
        card.add(ct, BorderLayout.CENTER);
        card.add(bar, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildTableArea() {
        // Filter bar
        cboTrangThai = new ModernComboBox<>();
        cboTrangThai.addItem("Tất cả trạng thái");
        cboTrangThai.addItem("Có sẵn");
        cboTrangThai.addItem("Đang thuê");
        cboTrangThai.addItem("Đã đặt");
        cboTrangThai.addItem("Vệ sinh");
        cboTrangThai.addItem("Bảo trì");
        cboTrangThai.setPreferredSize(new Dimension(180, 34));
        cboTrangThai.addActionListener(e -> loadData());

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterBar.setOpaque(false);
        filterBar.add(new JLabel("Lọc trạng thái:"));
        filterBar.add(cboTrangThai);

        // Table
        String[] cols = { "_sp", "Số phòng", "Loại phòng", "View", "Tầng", "Sức chứa", "Giá/đêm", "Khách hiện tại",
                "Trạng thái" };
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        table = buildStyledTable(tableModel);
        TableColumn colSp = table.getColumn("_sp");
        table.removeColumn(colSp);

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
            }
        });
        table.setComponentPopupMenu(buildContextMenu());

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER));

        JLabel footer = buildFooter("Right-click chọn Xem/Sửa, Xoá • Double-click để chỉnh sửa phòng");

        JPanel wrap = new JPanel(new BorderLayout(0, 8));
        wrap.setOpaque(false);
        wrap.add(filterBar, BorderLayout.NORTH);
        wrap.add(sp, BorderLayout.CENTER);
        wrap.add(footer, BorderLayout.SOUTH);

        loadData();
        return wrap;
    }

    private JPopupMenu buildContextMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem miEdit = new JMenuItem("Xem / Sửa");
        JMenuItem miAdd = new JMenuItem("Thêm phòng mới");
        JMenuItem miDel = new JMenuItem("Xóa phòng");
        miEdit.setFont(UIConstants.FONT_BODY);
        miAdd.setFont(UIConstants.FONT_BODY);
        miDel.setFont(UIConstants.FONT_BODY);
        miDel.setForeground(UIConstants.DANGER);
        miEdit.addActionListener(e -> editSelected());
        miAdd.addActionListener(e -> showDialog(null));
        miDel.addActionListener(e -> deleteSelected());
        menu.add(miEdit);
        menu.add(miAdd);
        menu.addSeparator();
        menu.add(miDel);
        return menu;
    }

    private JTable buildStyledTable(DefaultTableModel model) {
        JTable t = new JTable(model);
        t.setFont(UIConstants.FONT_BODY);
        t.setRowHeight(40);
        t.setShowGrid(false);
        t.setIntercellSpacing(new Dimension(0, 0));
        t.setBackground(Color.WHITE);
        t.setSelectionBackground(UIConstants.PRIMARY_LIGHT);
        t.getTableHeader().setFont(UIConstants.FONT_SMALL_BOLD);
        t.getTableHeader().setBackground(UIConstants.BG_TABLE_HEADER);
        t.getTableHeader().setForeground(UIConstants.TEXT_SECONDARY);
        t.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.BORDER));
        t.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable tbl, Object val, boolean sel, boolean foc, int row,
                    int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(tbl, val, sel, foc, row, col);
                lbl.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));
                lbl.setBackground(sel ? UIConstants.PRIMARY_LIGHT : (row % 2 == 0 ? Color.WHITE : new Color(0xFAFAFA)));
                lbl.setFont(UIConstants.FONT_BODY);
                lbl.setForeground(UIConstants.TEXT_PRIMARY);
                String v = val != null ? val.toString() : "";
                if (v.contains("Có sẵn")) {
                    lbl.setForeground(UIConstants.SUCCESS);
                    lbl.setFont(UIConstants.FONT_SMALL_BOLD);
                } else if (v.contains("Đang thuê")) {
                    lbl.setForeground(UIConstants.PRIMARY);
                    lbl.setFont(UIConstants.FONT_SMALL_BOLD);
                } else if (v.contains("Đã đặt")) {
                    lbl.setForeground(UIConstants.WARNING);
                    lbl.setFont(UIConstants.FONT_SMALL_BOLD);
                } else if (v.contains("Bảo trì")) {
                    lbl.setForeground(UIConstants.DANGER);
                    lbl.setFont(UIConstants.FONT_SMALL_BOLD);
                }
                return lbl;
            }
        });
        return t;
    }

    private JLabel buildFooter(String hint) {
        JLabel lbl = new JLabel("  " + hint);
        lbl.setFont(UIConstants.FONT_SMALL);
        lbl.setForeground(UIConstants.TEXT_MUTED);
        lbl.setBackground(Color.WHITE);
        lbl.setOpaque(true);
        lbl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER),
                BorderFactory.createEmptyBorder(6, 0, 6, 0)));
        return lbl;
    }

    private void loadData() {
        String filterTT = cboTrangThai != null && cboTrangThai.getSelectedIndex() > 0
                ? cboTrangThai.getSelectedItem().toString()
                : null;
        tableModel.setRowCount(0);
        for (Phong p : service.getAllPhong()) {
            String ttLabel = UIConstants.getTrangThaiPhongLabel(p.getTrangThai());
            if (filterTT != null && !ttLabel.contains(filterTT))
                continue;
            tableModel.addRow(new Object[] {
                    p.getSoPhong(), p.getSoPhong(), p.getTenLoaiPhong(), p.getView(),
                    "Tầng " + p.getTang(),
                    p.getSucChua() + " người",
                    String.format("%,.0fđ", (double) p.getGiaTheoNgay()),
                    p.getTenKhachHienTai() != null ? p.getTenKhachHienTai() : "—",
                    ttLabel
            });
        }
    }

    private Phong getSelected() {
        int row = table.getSelectedRow();
        if (row < 0)
            return null;
        return service.getPhongById((String) tableModel.getValueAt(row, 0));
    }

    private void showDialog(Phong p) {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        PhongDialog dlg = new PhongDialog(owner, p);
        dlg.setVisible(true);
        if (dlg.isConfirmed())
            refresh();
    }

    private void editSelected() {
        Phong p = getSelected();
        if (p == null) {
            JOptionPane.showMessageDialog(this, "Chọn một phòng để sửa!");
            return;
        }
        showDialog(p);
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Chọn một phòng để xóa!");
            return;
        }
        String sp = (String) tableModel.getValueAt(row, 0);
        Phong p = service.getPhongById(sp);
        if (p == null)
            return;
        TrangThaiPhong tt = p.getTrangThai();
        if (tt == TrangThaiPhong.OCCUPIED || tt == TrangThaiPhong.CLEANING) {
            JOptionPane.showMessageDialog(this,
                    "Không thể xóa phòng đang có khách hoặc đang vệ sinh!", "Không thể xóa",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this,
                "Xóa phòng " + sp + "?\nHành động này không thể hoàn tác!",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok == JOptionPane.YES_OPTION) {
            String err = service.xoaPhong(sp);
            if (err == null) {
                JOptionPane.showMessageDialog(this, "Đã xóa thành công!");
                refresh();
            } else
                JOptionPane.showMessageDialog(this, "Lỗi: " + err, "Không thể xóa", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void refresh() {
        loadData();
    }
}
