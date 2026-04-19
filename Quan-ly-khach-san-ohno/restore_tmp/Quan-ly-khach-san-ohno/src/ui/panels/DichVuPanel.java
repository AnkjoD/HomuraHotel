package ui.panels;

import ui.components.RoundedComponents.ModernTextField;
import ui.components.RoundedComponents.RoundedButton;
import ui.components.RoundedComponents.RoundedPanel;
import ui.components.UIConstants;
import ui.dialogs.DichVuDialog;
import entity.DichVu;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import service.DichVuService;

public class DichVuPanel extends JPanel {

    private final DichVuService service = new DichVuService();

    private ModernTextField txtSearch;
    private JComboBox<String> cboLoai, cboTT;
    private DefaultTableModel tableModel;
    private JTable table;

    public DichVuPanel() {
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
        JLabel t = new JLabel("Quản lý dịch vụ");
        t.setFont(UIConstants.FONT_TITLE);
        JLabel s = new JLabel("Danh sách và cấu hình các dịch vụ bổ sung của khách sạn");
        s.setFont(UIConstants.FONT_BODY);
        s.setForeground(UIConstants.TEXT_SECONDARY);
        left.add(t);
        left.add(Box.createVerticalStrut(2));
        left.add(s);

        RoundedButton btnAdd = new RoundedButton("+ Thêm dịch vụ", UIConstants.PRIMARY, Color.WHITE);
        btnAdd.addActionListener(e -> showDialog(null));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);
        btns.add(btnAdd);

        p.add(left, BorderLayout.WEST);
        p.add(btns, BorderLayout.EAST);
        return p;
    }

    private JPanel buildStats() {
        int all = service.countAll(), active = service.countActive();
        double avg = service.getGiaTrungBinh();

        JPanel row = new JPanel(new GridLayout(1, 4, 16, 0));
        row.setOpaque(false);
        row.add(statCard("Tổng dịch vụ", all + " dịch vụ", UIConstants.PRIMARY));
        row.add(statCard("Đang cung cấp", active + " dịch vụ", UIConstants.SUCCESS));
        row.add(statCard("Tạm ngừng", (all - active) + " dịch vụ", UIConstants.DANGER));
        row.add(statCard("Giá trung bình", String.format("%,.0fđ", avg), UIConstants.WARNING));
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
        txtSearch = new ModernTextField("Tìm tên dịch vụ...");
        txtSearch.setPreferredSize(new Dimension(220, 34));

        cboLoai = new JComboBox<>(
                new String[] { "Tất cả loại", "Ăn uống", "Spa & Làm đẹp", "Vận chuyển", "Dịch vụ phòng", "Khác" });
        cboLoai.setFont(UIConstants.FONT_BODY);

        cboTT = new JComboBox<>(new String[] { "Tất cả", "Hoạt động", "Tạm ngừng" });
        cboTT.setFont(UIConstants.FONT_BODY);

        JPanel filterBar = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        filterBar.setOpaque(false);
        filterBar.add(txtSearch);
        filterBar.add(cboLoai);
        filterBar.add(cboTT);

        // Table
        String[] cols = { "_ma", "#", "Tên dịch vụ", "Loại", "Giá", "Đơn vị", "SL tối thiểu", "Trạng thái" };
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        table = buildStyledTable(tableModel);
        TableColumn colMa = table.getColumn("_ma");
        table.removeColumn(colMa);
        TableColumn colStt = table.getColumn("#");
        colStt.setMinWidth(40);
        colStt.setMaxWidth(80);
        colStt.setPreferredWidth(60);

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

        // Listeners
        DocumentListener dl = new DocumentListener() {
            public void insertUpdate(DocumentEvent e) {
                runFilter();
            }

            public void removeUpdate(DocumentEvent e) {
                runFilter();
            }

            public void changedUpdate(DocumentEvent e) {
            }
        };
        txtSearch.getDocument().addDocumentListener(dl);
        cboLoai.addActionListener(e -> runFilter());
        cboTT.addActionListener(e -> runFilter());

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER));

        JLabel footer = new JLabel("  Double-click hoặc Right-click để Xem/Sửa • Double-click để chỉnh sửa");
        footer.setFont(UIConstants.FONT_SMALL);
        footer.setForeground(UIConstants.TEXT_MUTED);
        footer.setBackground(Color.WHITE);
        footer.setOpaque(true);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER),
                BorderFactory.createEmptyBorder(6, 0, 6, 0)));

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
        JMenuItem miAdd = new JMenuItem("Thêm mới");
        JMenuItem miDel = new JMenuItem("Xóa dịch vụ");
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
                if (v.equals("Hoạt động")) {
                    lbl.setForeground(UIConstants.SUCCESS);
                    lbl.setFont(UIConstants.FONT_SMALL_BOLD);
                } else if (v.equals("Tạm ngừng")) {
                    lbl.setForeground(UIConstants.DANGER);
                    lbl.setFont(UIConstants.FONT_SMALL_BOLD);
                }
                return lbl;
            }
        });
        return t;
    }

    private void loadData() {
        String kw = txtSearch != null ? txtSearch.getText() : null;
        String loai = cboLoai != null ? cboLoai.getSelectedItem().toString() : null;
        String tt = cboTT != null ? cboTT.getSelectedItem().toString() : null;

        String l = (loai == null || loai.equals("Tất cả loại")) ? null : loai;
        String t = (tt == null || tt.equals("Tất cả")) ? null : tt;
        List<DichVu> list = service.search(
                (kw == null || kw.isBlank()) ? null : kw, l, t);

        tableModel.setRowCount(0);
        int i = 1;
        for (DichVu dv : list) {
            tableModel.addRow(new Object[] {
                    dv.getMaDV(), i++, dv.getTenDV(), dv.getLoai(),
                    String.format("%,.0fđ", dv.getGia()),
                    dv.getDonVi(), dv.getSoLuongMin(), dv.getTrangThai()
            });
        }
    }

    private void runFilter() {
        loadData();
    }

    private DichVu getSelected() {
        int row = table.getSelectedRow();
        if (row < 0)
            return null;
        return service.getById((String) tableModel.getValueAt(row, 0));
    }

    private void showDialog(DichVu dv) {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        DichVuDialog dlg = new DichVuDialog(owner, dv);
        dlg.setVisible(true);
        if (dlg.isConfirmed())
            refresh();
    }

    private void editSelected() {
        DichVu dv = getSelected();
        if (dv == null) {
            JOptionPane.showMessageDialog(this, "Chọn một dịch vụ để sửa!");
            return;
        }
        showDialog(dv);
    }

    private void deleteSelected() {
        DichVu dv = getSelected();
        if (dv == null) {
            JOptionPane.showMessageDialog(this, "Chọn một dịch vụ để xóa!");
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this,
                "Xóa dịch vụ \"" + dv.getTenDV() + "\"?",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok == JOptionPane.YES_OPTION) {
            String err = service.xoa(dv.getMaDV());
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
