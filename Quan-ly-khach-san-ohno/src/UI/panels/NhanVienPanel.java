package ui.panels;

import service.AuthService;
import service.NhanVienService;
import entity.NhanVien;
import ui.MainFrame;
import ui.components.UIConstants;
import util.ExcelExporter;
import ui.components.NotificationManager;
import ui.components.PaginationPanel;
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
    private final boolean canEdit = AuthService.getInstance().canEdit("nhanvien");

    private ModernTextField txtSearch;
    private JTable table;
    private DefaultTableModel tableModel;
    private String currentFilter = "Tất cả";
    private PaginationPanel pagination;
    private List<NhanVien> fullList;

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
        p.setBorder(BorderFactory.createEmptyBorder(0, 0, 10, 0));

        JPanel left = new JPanel();
        left.setOpaque(false);
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        JLabel t = new JLabel("Nhân sự & Tài khoản");
        t.setFont(new Font("Segoe UI", Font.BOLD, 28));
        t.setForeground(new Color(0x0F172A)); 
        
        JLabel s = new JLabel(canEdit
                ? "Quản lý trạng thái và thông tin nhân sự"
                : "Chế độ xem — Bạn không có quyền chỉnh sửa");
        s.setFont(UIConstants.FONT_BODY);
        s.setForeground(canEdit ? new Color(0x64748B) : UIConstants.WARNING);
        left.add(t);
        left.add(Box.createVerticalStrut(4));
        left.add(s);

        RoundedButton btnThem = new RoundedButton("+ Thêm nhân viên", UIConstants.PRIMARY, Color.WHITE);
        btnThem.setPreferredSize(new Dimension(185, 42));
        btnThem.setFont(new Font("Segoe UI", Font.BOLD, 13));

        RoundedButton btnExcel = new RoundedButton("Xuất Excel", new Color(0x10B981), Color.WHITE);
        btnExcel.setPreferredSize(new Dimension(130, 42));
        btnExcel.setFont(new Font("Segoe UI", Font.BOLD, 13));

        btnThem.addActionListener(e -> showDialog(null));
        btnExcel.addActionListener(e -> {
            String kw = txtSearch != null ? txtSearch.getText().trim() : "";
            String cv = currentFilter.equals("Tất cả") ? null : currentFilter;
            ExcelExporter.exportNhanVien(NhanVienPanel.this, service.search(kw.isEmpty() ? null : kw, cv));
        });

        if (!canEdit) {
            btnThem.setEnabled(false);
            btnThem.setToolTipText("Chỉ Admin mới có quyền thêm nhân viên");
        }

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        btns.setOpaque(false);
        btns.add(btnExcel);
        btns.add(btnThem);
        p.add(left, BorderLayout.WEST);
        p.add(btns, BorderLayout.EAST);
        return p;
    }

    private JPanel buildStats() {
        JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 0));
        row.setOpaque(false);

        lblTongNV = new JLabel("...");
        lblDangLam = new JLabel("...");
        lblDaNghi = new JLabel("...");

        row.add(statCard("TỔNG NHÂN LỰC", lblTongNV, UIConstants.PRIMARY));
        row.add(statCard("ĐANG LÀM", lblDangLam, UIConstants.SUCCESS));
        row.add(statCard("ĐÃ NGHỈ", lblDaNghi, UIConstants.TEXT_MUTED));

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

    private RoundedPanel statCard(String label, JLabel lblValue, Color c) {
        RoundedPanel card = new RoundedPanel(12);
        card.setBackground(Color.WHITE);
        card.setShadow(true);
        card.setLayout(new BorderLayout());
        card.setBorder(BorderFactory.createEmptyBorder(16, 20, 10, 20));
        card.setPreferredSize(new Dimension(240, 95));

        JLabel l1 = new JLabel(label);
        l1.setFont(UIConstants.FONT_SMALL);
        l1.setForeground(UIConstants.TEXT_SECONDARY);

        lblValue.setFont(new Font("Segoe UI", Font.BOLD, 26));
        lblValue.setForeground(UIConstants.TEXT_PRIMARY);

        card.add(l1, BorderLayout.NORTH);
        card.add(lblValue, BorderLayout.CENTER);
        return card;
    }

    private JPanel buildFilterBar() {
        RoundedPanel card = new RoundedPanel(12);
        card.setBackground(Color.WHITE);
        card.setShadow(true);
        card.setLayout(new GridBagLayout());
        card.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));

        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.insets = new Insets(0, 0, 0, 12);

        txtSearch = new ModernTextField("Tìm tên, mã, SĐT...");
        txtSearch.setPreferredSize(new Dimension(250, 40));
        txtSearch.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { loadTable(); }
            @Override
            public void removeUpdate(DocumentEvent e) { loadTable(); }
            @Override
            public void changedUpdate(DocumentEvent e) { loadTable(); }
        });
        
        gbc.gridx = 0;
        card.add(txtSearch, gbc);

        JPanel tabs = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        tabs.setOpaque(false);
        String[] dbRoles = { "Tất cả", "ADMIN", "MANAGER", "RECEPTIONIST" };
        String[] displayRoles = { "Tất cả", "Quản trị viên", "Quản lý", "Lễ tân" };
        ButtonGroup bg = new ButtonGroup();
        for (int i = 0; i < dbRoles.length; i++) {
            String dbRole = dbRoles[i];
            JToggleButton btn = new JToggleButton(displayRoles[i]);
            btn.setFont(new Font("Segoe UI", Font.BOLD, 13));
            btn.setFocusPainted(false);
            btn.setSelected(dbRole.equals(currentFilter));
            btn.setPreferredSize(new Dimension(btn.getPreferredSize().width + 10, 40));
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
        
        gbc.gridx = 1;
        card.add(tabs, gbc);

        // Spacer to push everything to the left
        gbc.gridx = 2;
        gbc.weightx = 1.0;
        card.add(new JPanel() {{ setOpaque(false); }}, gbc);

        return card;
    }

    private void styleTab(JToggleButton btn, boolean on) {
        btn.setBorderPainted(false);
        btn.setContentAreaFilled(false);
        btn.setOpaque(true);
        if (on) {
            btn.setBackground(new Color(0xE0E7FF)); 
            btn.setForeground(new Color(0x3730A3)); 
            btn.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
        } else {
            btn.setBackground(new Color(0xF1F5F9)); 
            btn.setForeground(new Color(0x64748B));
            btn.setBorder(BorderFactory.createEmptyBorder(0, 16, 0, 16));
        }
    }

    private JPanel buildTableArea() {
        String[] cols = { "_ma", "Mã NV", "Họ tên", "Chức vụ", "SĐT", "Email", "Trạng thái" };
        tableModel = new DefaultTableModel(cols, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        table = new JTable(tableModel);
        styleTable();
        loadTable();
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) editSelected();
            }
            @Override
            public void mousePressed(MouseEvent e) {
                int r = table.rowAtPoint(e.getPoint());
                if (r >= 0) table.setRowSelectionInterval(r, r);
            }
        });
        table.setComponentPopupMenu(buildContextMenu());

        JScrollPane sp = new JScrollPane(table);
        sp.setBorder(BorderFactory.createLineBorder(new Color(0xE2E8F0))); 
        sp.setPreferredSize(new Dimension(0, 400));
        sp.getViewport().setBackground(Color.WHITE);

        pagination = new PaginationPanel();
        pagination.setPageChangeListener(page -> loadTable());

        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(Color.WHITE);
        footer.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0xE2E8F0)),
                BorderFactory.createEmptyBorder(0, 0, 0, 0)));
        footer.add(pagination, BorderLayout.CENTER);

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

        if (!canEdit) { miXoa.setEnabled(false); }

        menu.add(miXem);
        menu.addSeparator();
        menu.add(miXoa);
        return menu;
    }

    private void styleTable() {
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.getTableHeader().setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xE2E8F0))); 
        table.setRowHeight(40);
        table.setShowGrid(false);
        table.setIntercellSpacing(new Dimension(0, 0));
        table.setBackground(Color.WHITE);
        table.setSelectionBackground(new Color(0xF8FAFC));
        
        table.getColumnModel().removeColumn(table.getColumnModel().getColumn(0));
        
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable t, Object val, boolean sel, boolean foc, int row, int col) {
                JLabel lbl = (JLabel) super.getTableCellRendererComponent(t, val, sel, foc, row, col);
                
                lbl.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createMatteBorder(0, 0, 1, 0, new Color(0xF1F5F9)),
                    BorderFactory.createEmptyBorder(0, 16, 0, 16)
                ));

                int modelRow = t.convertRowIndexToModel(row);
                boolean active = "Đang làm".equals(t.getModel().getValueAt(modelRow, 6));

                lbl.setBackground(sel ? new Color(0xF4F6F8) : Color.WHITE);
                
                if (col == 5) {
                    lbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
                    if (active) {
                        lbl.setForeground(new Color(0x10B981)); 
                        lbl.setText("Đang làm");
                    } else {
                        lbl.setForeground(new Color(0x94A3B8)); 
                        lbl.setText("Đã nghỉ");
                    }
                } else {
                    lbl.setFont(new Font("Segoe UI", Font.PLAIN, 14));
                    lbl.setForeground(sel ? new Color(0x0F172A) : new Color(0x334155));
                }
                return lbl;
            }
        });
    }

    private void loadTable() {
        String kw = txtSearch != null ? txtSearch.getText().trim() : "";
        String filterRole = currentFilter.equals("Tất cả") ? null : currentFilter;

        // Reset page if filtering changes significantly
        // For simplicity here, we re-fetch everything and paginate
        fullList = service.search(kw.isEmpty() ? null : kw, filterRole);

        int pageSize = 12;
        int currentPage = pagination != null ? pagination.getCurrentPage() : 1;
        
        // If filtering and current page is out of bounds
        int totalPages = (int) Math.ceil((double) fullList.size() / pageSize);
        if (currentPage > totalPages && totalPages > 0) {
            currentPage = totalPages;
            if (pagination != null) pagination.setCurrentPage(currentPage);
        }

        tableModel.setRowCount(0);
        int start = (currentPage - 1) * pageSize;
        int end = Math.min(start + pageSize, fullList.size());

        for (int i = start; i < end; i++) {
            NhanVien nv = fullList.get(i);
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
        
        if (pagination != null) {
            pagination.update(fullList.size(), pageSize, currentPage);
        }
    }

    private NhanVien getSelected() {
        int row = table.getSelectedRow();
        if (row < 0) return null;
        return service.getById((String) tableModel.getValueAt(row, 0));
    }

    private void showDialog(NhanVien nv) {
        Frame owner = (Frame) SwingUtilities.getWindowAncestor(this);
        boolean isReadOnly = !canEdit && nv != null; 
        NhanVienDialog dlg = new NhanVienDialog(owner, nv, isReadOnly);
        dlg.setVisible(true);
        if (dlg.isConfirmed()) {
            NotificationManager.showSuccess("Thành công", 
                (nv == null ? "Đã thêm nhân viên mới" : "Đã cập nhật thông tin nhân viên"));
            refresh();
        }
    }

    private void editSelected() {
        NhanVien nv = getSelected();
        if (nv == null) {
            JOptionPane.showMessageDialog(this, "Chọn một nhân viên để xem!");
            return;
        }
        showDialog(nv);
    }

    private void deleteSelected() {
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
                NotificationManager.showSuccess("Thành công", "Đã xóa nhân viên " + nv.getHoTen());
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