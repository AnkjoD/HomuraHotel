package ui.panels;

import service.AuthService;

import service.NhanVienService;
import entity.NhanVien;
import ui.MainFrame;
import ui.components.UIConstants;
import util.ExcelExporter;
import ui.components.RoundedComponents.*;
import ui.dialogs.NhanVienDialog;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class NhanVienPanel extends JPanel {

    private final NhanVienService service = new NhanVienService();
    /** true nếu user hiện tại có quyền thêm/sửa/xóa nhân viên (chỉ ADMIN) */
    private final boolean canEdit = AuthService.getInstance().canEdit("nhanvien");

    private ModernTextField txtSearch;
    private JTable table;
    private DefaultTableModel tableModel;
    private String currentFilter = "Tất cả";
    private JLabel lblFooter;

    public NhanVienPanel(MainFrame mainFrame) {
        setOpaque(false);
        setLayout(new BorderLayout());
        buildUI();
    }

    private void buildUI() {
        JPanel main = new JPanel(new BorderLayout());
        main.setOpaque(false);
        main.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));
        main.add(buildHeader(), BorderLayout.NORTH);
        main.add(buildStats(), BorderLayout.CENTER);

        JPanel bot = new JPanel(new BorderLayout());
        bot.setOpaque(false);
        bot.add(Box.createVerticalStrut(14), BorderLayout.NORTH);
        bot.add(buildFilterBar(), BorderLayout.CENTER);
        bot.add(buildTableArea(), BorderLayout.SOUTH);
        main.add(bot, BorderLayout.SOUTH);
        add(main, BorderLayout.CENTER);
    }

    private JPanel buildHeader() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 16, 0));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        JLabel t = new JLabel("Nhân sự & Tài khoản");
        t.setFont(UIConstants.FONT_TITLE);
        // === PHÂN QUYỀN: thay đổi subtitle khi chỉ xem ===
        JLabel s = new JLabel(canEdit
                ? "Quản lý trạng thái và thông tin nhân sự"
                : "Chế độ xem — Bạn không có quyền chỉnh sửa");
        s.setFont(UIConstants.FONT_BODY);
        s.setForeground(canEdit ? UIConstants.TEXT_SECONDARY : UIConstants.WARNING);
        left.add(t);
        left.add(Box.createVerticalStrut(2));
        left.add(s);

        RoundedButton btnThem = new RoundedButton("+ Thêm nhân viên", UIConstants.PRIMARY, Color.WHITE);
        RoundedButton btnExcel = new RoundedButton("↓ Xuất Excel", new Color(0x16A34A), Color.WHITE);

        btnThem.addActionListener(e -> showDialog(null));
        btnExcel.addActionListener(e -> {
            String kw = txtSearch != null ? txtSearch.getText().trim() : "";
            String cv = currentFilter.equals("Tất cả") ? null : currentFilter;
            ExcelExporter.exportNhanVien(NhanVienPanel.this, service.search(kw.isEmpty() ? null : kw, cv));
        });

        // === PHÂN QUYỀN: Ẩn nút Thêm nếu không có quyền ===
        if (!canEdit) {
            btnThem.setEnabled(false);
            btnThem.setToolTipText("Chỉ Admin mới có quyền thêm nhân viên");
        }

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btns.setOpaque(false);
        btns.add(btnExcel);
        btns.add(btnThem);
        p.add(left, BorderLayout.WEST);
        p.add(btns, BorderLayout.EAST);
        return p;
    }

    private JPanel buildStats() {
        JPanel row = new JPanel(new GridLayout(1, 3, 12, 0));
        row.setOpaque(false);
        row.setMaximumSize(new Dimension(Integer.MAX_VALUE, 90));

        lblTongNV = new JLabel("...");
        lblDangLam = new JLabel("...");
        lblDaNghi = new JLabel("...");

        row.add(statCard("Tổng nhân lực", lblTongNV, UIConstants.PRIMARY));
        row.add(statCard("Đang làm", lblDangLam, UIConstants.SUCCESS));
        row.add(statCard("Đã nghỉ", lblDaNghi, UIConstants.DANGER));

        loadStats();
        return row;
    }

    private void loadStats() {
        SwingWorker<int[], Void> worker = new SwingWorker<>() {
            @Override
            protected int[] doInBackground() throws Exception {
                List<NhanVien> all = service.getAll();
                int tong = 0, active = 0, nghi = 0;
                if (all != null) {
                    tong = all.size();
                    for (NhanVien nv : all)
                        if (nv.isDangLamViec())
                            active++;
                    nghi = tong - active;
                }
                return new int[] { tong, active, nghi };
            }

            @Override
            protected void done() {
                try {
                    int[] res = get();
                    lblTongNV.setText(res[0] + " NV");
                    lblDangLam.setText(res[1] + " NV");
                    lblDaNghi.setText(res[2] + " NV");
                } catch (Exception ignored) {}
            }
        };
        worker.execute();
    }

    private JLabel lblTongNV, lblDangLam, lblDaNghi;

    private RoundedPanel statCard(String label, JLabel lblVal, Color c) {
        RoundedPanel card = new RoundedPanel(UIConstants.CARD_RADIUS);
        card.setBackground(Color.WHITE);
        card.setShadow(true);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(14, 16, 8, 16));
        JLabel l1 = new JLabel(label);
        l1.setFont(UIConstants.FONT_SMALL);
        l1.setForeground(UIConstants.TEXT_SECONDARY);
        
        lblVal.setFont(new Font("Segoe UI", Font.BOLD, 22));
        
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
        ct.add(lblVal);
        card.add(ct, BorderLayout.CENTER);
        card.add(bar, BorderLayout.SOUTH);
        return card;
    }

    private JPanel buildFilterBar() {
        JPanel p = new JPanel(new BorderLayout(8, 0));
        p.setOpaque(false);
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));
        txtSearch = new ModernTextField("Tìm tên, mã, SĐT...");
        txtSearch.setPreferredSize(new Dimension(240, 36));
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) {
                loadTable();
            }

            @Override
            public void removeUpdate(DocumentEvent e) {
                loadTable();
            }

            @Override
            public void changedUpdate(DocumentEvent e) {
                loadTable();
            }
        });
        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        tabs.setOpaque(false);
        String[] dbRoles = { "Tất cả", "ADMIN", "MANAGER", "RECEPTIONIST" };
        String[] displayRoles = { "Tất cả", "Quản trị viên", "Quản lý", "Lễ tân" };
        ButtonGroup bg = new ButtonGroup();
        for (int i = 0; i < dbRoles.length; i++) {
            String dbRole = dbRoles[i];
            JToggleButton btn = new JToggleButton(displayRoles[i]);
            btn.setFont(UIConstants.FONT_SMALL_BOLD);
            btn.setFocusPainted(false);
            btn.setSelected(dbRole.equals(currentFilter));
            styleTab(btn, dbRole.equals(currentFilter));
            btn.addActionListener(e -> {
                currentFilter = dbRole;
                for (Component c : tabs.getComponents())
                    if (c instanceof JToggleButton) {
                        JToggleButton tb = (JToggleButton) c;
                        styleTab(tb, tb == btn);
                    }
                loadTable();
            });
            bg.add(btn);
            tabs.add(btn);
        }
        p.add(txtSearch, BorderLayout.WEST);
        p.add(tabs, BorderLayout.CENTER);
        return p;
    }

    private void styleTab(JToggleButton btn, boolean on) {
        if (on) {
            btn.setBackground(UIConstants.PRIMARY);
            btn.setForeground(Color.WHITE);
            btn.setBorder(BorderFactory.createCompoundBorder(new RoundedBorder(16, UIConstants.PRIMARY),
                    BorderFactory.createEmptyBorder(5, 12, 5, 12)));
        } else {
            btn.setBackground(Color.WHITE);
            btn.setForeground(UIConstants.TEXT_SECONDARY);
            btn.setBorder(BorderFactory.createCompoundBorder(new RoundedBorder(16, UIConstants.BORDER),
                    BorderFactory.createEmptyBorder(5, 12, 5, 12)));
        }
    }

    private JPanel buildTableArea() {
        String[] cols = { "_ma", "Mã NV", "Họ tên", "Chức vụ", "SĐT", "Email", "Trạng thái" };
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) {
                return false;
            }
        };
        table = new JTable(tableModel);
        styleTable();
        loadTable();
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
        sp.setPreferredSize(new Dimension(0, 400));

        lblFooter = new JLabel();
        lblFooter.setFont(UIConstants.FONT_SMALL);
        lblFooter.setForeground(UIConstants.TEXT_MUTED);
        JLabel hint = new JLabel("Double-click hoặc Chuột phải để Xem, Sửa, Xóa  ");
        hint.setFont(UIConstants.FONT_SMALL);
        hint.setForeground(UIConstants.TEXT_MUTED);
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UIConstants.BORDER),
                BorderFactory.createEmptyBorder(6, 12, 6, 12)));
        footer.add(lblFooter, BorderLayout.WEST);
        footer.add(hint, BorderLayout.EAST);

        JPanel wrap = new JPanel(new BorderLayout());
        wrap.setOpaque(false);
        wrap.add(sp, BorderLayout.CENTER);
        wrap.add(footer, BorderLayout.SOUTH);
        return wrap;
    }

    private JPopupMenu buildContextMenu() {
        JPopupMenu menu = new JPopupMenu();
        JMenuItem miXem = new JMenuItem(canEdit ? "Xem / Sửa thông tin" : "Xem thông tin");
        JMenuItem miXoa = new JMenuItem("Xóa nhân viên này");
        miXem.setFont(UIConstants.FONT_BODY);
        miXoa.setFont(UIConstants.FONT_BODY);
        miXoa.setForeground(UIConstants.DANGER);
        miXem.addActionListener(e -> editSelected());
        miXoa.addActionListener(e -> deleteSelected());

        // === PHÂN QUYỀN: Disable xóa nếu không phải ADMIN ===
        if (!canEdit) {
            miXoa.setEnabled(false);
        }

        menu.add(miXem);
        menu.addSeparator();
        menu.add(miXoa);
        return menu;
    }

    private void styleTable() {
        table.setFont(UIConstants.FONT_BODY);
        table.setRowHeight(48);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(UIConstants.PRIMARY_LIGHT);
        table.getTableHeader().setFont(UIConstants.FONT_SMALL_BOLD);
        table.getTableHeader().setBackground(UIConstants.BG_TABLE_HEADER);
        table.getTableHeader().setForeground(UIConstants.TEXT_SECONDARY);
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, UIConstants.BORDER));
        table.getColumnModel().removeColumn(table.getColumnModel().getColumn(0));
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean foc, int row,
                    int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                lbl.setBorder(BorderFactory.createEmptyBorder(0, 12, 0, 12));

                // Lấy trạng thái từ Model (cột 6 là "Trạng thái")
                int modelRow = t.convertRowIndexToModel(row);
                boolean active = "Đang làm".equals(t.getModel().getValueAt(modelRow, 6));

                if (sel) {
                    lbl.setBackground(UIConstants.PRIMARY_LIGHT);
                } else {
                    lbl.setBackground(row % 2 == 0 ? Color.WHITE : new Color(0xFAFAFA));
                }
                lbl.setFont(UIConstants.FONT_BODY);
                lbl.setForeground(active ? UIConstants.TEXT_PRIMARY : UIConstants.TEXT_MUTED);
                return lbl;
            }
        });
    }

    private void loadTable() {
        tableModel.setRowCount(0);
        String kw = txtSearch != null ? txtSearch.getText().trim() : "";
        String filterRole = currentFilter.equals("Tất cả") ? null : currentFilter;
        List<NhanVien> list = service.search(kw.isEmpty() ? null : kw, filterRole);
        for (NhanVien nv : list) {
            tableModel.addRow(new Object[] {
                    nv.getMaNhanVien(),
                    nv.getMaNhanVien(),
                    nv.getHoTen(),
                    nv.getChucVu(),
                    nv.getSoDienThoai(),
                    nv.getEmail() != null ? nv.getEmail() : "",
                    nv.isDangLamViec() ? "Đang làm" : "Đã nghỉ"
            });
        }
        if (lblFooter != null)
            lblFooter.setText("Hiển thị " + list.size() + " nhân viên");
    }

    private NhanVien getSelected() {
        int row = table.getSelectedRow();
        if (row < 0)
            return null;
        return service.getById((String) tableModel.getValueAt(row, 0));
    }

    private void showDialog(NhanVien nv) {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        // === PHÂN QUYỀN: mở read-only nếu không có quyền chỉnh sửa ===
        boolean isReadOnly = !canEdit && nv != null; // Thêm mới luôn cần quyền, chỉ xem cần entity
        NhanVienDialog dlg = new NhanVienDialog(owner, nv, isReadOnly);
        dlg.setVisible(true);
        if (dlg.isConfirmed())
            refresh();
    }

    private void editSelected() {
        NhanVien nv = getSelected();
        if (nv == null) {
            JOptionPane.showMessageDialog(this, "Chọn một nhân viên để xem!");
            return;
        }
        // === PHÂN QUYỀN: cho xem nhưng dialog sẽ read-only nếu không phải ADMIN ===
        showDialog(nv);
    }

    private void deleteSelected() {
        // === PHÂN QUYỀN: Không cho xóa nếu không phải ADMIN ===
        if (!canEdit) {
            JOptionPane.showMessageDialog(this,
                    "Bạn không có quyền xóa nhân viên! Chỉ Admin mới được phép.",
                    "Không có quyền", JOptionPane.WARNING_MESSAGE);
            return;
        }

        NhanVien nv = getSelected();
        if (nv == null) {
            JOptionPane.showMessageDialog(this, "Chọn một nhân viên để xóa!");
            return;
        }

        // Không cho xóa chính mình
        String maNVHienTai = AuthService.getInstance().getCurrentMaNV();
        if (nv.getMaNhanVien().equals(maNVHienTai)) {
            JOptionPane.showMessageDialog(this, "Không thể xóa tài khoản đang đăng nhập!", "Không thể xóa",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        int ok = JOptionPane.showConfirmDialog(this,
                "Xóa nhân viên: \"" + nv.getHoTen() + "\" (" + nv.getMaNhanVien()
                        + ")?\nHành động này không thể hoàn tác!",
                "Xác nhận xóa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (ok == JOptionPane.YES_OPTION) {
            String err = service.xoa(nv.getMaNhanVien());
            if (err == null) {
                JOptionPane.showMessageDialog(this, "Đã xóa thành công!");
                refresh();
            } else
                JOptionPane.showMessageDialog(this, "Lỗi: " + err, "Không thể xóa", JOptionPane.ERROR_MESSAGE);
        }
    }

    public void refresh() {
        removeAll();
        buildUI();
        revalidate();
        repaint();
    }
}
