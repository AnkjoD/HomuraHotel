package ui.panels;

import ui.components.RoundedComponents.RoundedButton;
import ui.components.UIConstants;
import ui.dialogs.BangGiaDialog;
import entity.BangGia;
import java.awt.*;
import java.awt.event.*;
import java.text.SimpleDateFormat;
import java.util.List;
import javax.swing.*;
import javax.swing.table.*;
import service.BangGiaService;

public class BangGiaPanel extends JPanel {

    private final BangGiaService service = new BangGiaService();

    private DefaultTableModel tableModel;
    private JTable table;

    public BangGiaPanel() {
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
        JLabel t = new JLabel("Quản lý cách tính tiền");
        t.setFont(UIConstants.FONT_TITLE);
        JLabel s = new JLabel("Cấu hình bảng giá phòng và chính sách giá theo mùa / sự kiện");
        s.setFont(UIConstants.FONT_BODY);
        s.setForeground(UIConstants.TEXT_SECONDARY);
        left.add(t);
        left.add(Box.createVerticalStrut(2));
        left.add(s);

        RoundedButton btnAdd = new RoundedButton("+ Thêm bảng giá", UIConstants.PRIMARY, Color.WHITE);
        btnAdd.addActionListener(e -> showDialog(null));

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);
        btns.add(btnAdd);

        p.add(left, BorderLayout.WEST);
        p.add(btns, BorderLayout.EAST);
        return p;
    }

    private JPanel buildTableArea() {
        String[] cols = { "_ma", "#", "Tên bảng giá", "Ngày bắt đầu", "Ngày kết thúc", "Trạng thái" };
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

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(UIConstants.BORDER));

        // Info hint
        JPanel hintPanel = new JPanel(new BorderLayout());
        hintPanel.setBackground(new Color(0xEFF6FF));
        hintPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(0xBFDBFE)),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));
        JLabel hintLbl = new JLabel(
                "<html>💡 <b>Bảng giá kích hoạt</b> sẽ ghi đè giá cơ sở của từng Loại phòng trong khoảng thời gian xác định. "
                        + "Chỉ nên có <b>một bảng giá kích hoạt</b> tại một thời điểm.</html>");
        hintLbl.setFont(UIConstants.FONT_SMALL);
        hintLbl.setForeground(UIConstants.PRIMARY);
        hintPanel.add(hintLbl, BorderLayout.CENTER);

        JLabel footer = buildFooter("Right-click chọn Xem/Sửa, Xoá • Double-click để chỉnh sửa bảng giá");

        JPanel wrap = new JPanel(new BorderLayout(0, 8));
        wrap.setOpaque(false);
        wrap.add(sp, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        south.add(hintPanel, BorderLayout.NORTH);
        south.add(footer, BorderLayout.SOUTH);
        wrap.add(south, BorderLayout.SOUTH);

        loadData();
        return wrap;
    }

    private JPopupMenu buildContextMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem miEdit = new JMenuItem("Xem / Sửa");
        JMenuItem miAdd = new JMenuItem("Thêm bảng giá mới");
        JMenuItem miDel = new JMenuItem("Xóa bảng giá");
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
        t.setRowHeight(44);
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
                // Trạng thái boolean hiển thị
                if (val instanceof Boolean) {
                    Boolean active = (Boolean) val;
                    lbl.setText(active ? "✔ Đang kích hoạt" : "Ngừng");
                    lbl.setForeground(active ? UIConstants.SUCCESS : UIConstants.TEXT_MUTED);
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
        tableModel.setRowCount(0);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy");
        int i = 1;
        List<BangGia> list = service.getAll();
        for (BangGia bg : list) {
            tableModel.addRow(new Object[] {
                    bg.getMaBangGia(), i++, bg.getTenBangGia(),
                    bg.getNgayBatDau() != null ? sdf.format(java.sql.Timestamp.valueOf(bg.getNgayBatDau())) : "",
                    bg.getNgayKetThuc() != null ? sdf.format(java.sql.Timestamp.valueOf(bg.getNgayKetThuc())) : "",
                    bg.isTrangThai()
            });
        }
    }

    private BangGia getSelected() {
        int row = table.getSelectedRow();
        if (row < 0)
            return null;
        return service.getById((String) tableModel.getValueAt(row, 0));
    }

    private void showDialog(BangGia bg) {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        BangGiaDialog dlg = new BangGiaDialog(owner, bg);
        dlg.setVisible(true);
        if (dlg.isConfirmed())
            refresh();
    }

    private void editSelected() {
        BangGia bg = getSelected();
        if (bg == null) {
            JOptionPane.showMessageDialog(this, "Chọn một bảng giá để sửa!");
            return;
        }
        showDialog(bg);
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Chọn một bảng giá để xóa!");
            return;
        }
        String ma = (String) tableModel.getValueAt(row, 0);
        String ten = (String) tableModel.getValueAt(row, 2);
        int ok = JOptionPane.showConfirmDialog(this,
                "Xóa bảng giá \"" + ten + "\"?\nHành động này không thể hoàn tác!",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok == JOptionPane.YES_OPTION) {
            String err = service.xoa(ma);
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
